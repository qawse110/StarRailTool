package com.mystarrail.tool.engine.simulator

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.CharacterScore
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.ScoringConfig
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.data.model.TeamScore
import com.mystarrail.tool.data.model.Tier
import com.mystarrail.tool.engine.build.BuildEffectResolver
import com.mystarrail.tool.engine.simulator.damage.CharacterUnitValue
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.sim.DiscreteEventSimulator
import com.mystarrail.tool.engine.simulator.sim.SimulationResult
import com.mystarrail.tool.engine.simulator.sim.toCombatant
import kotlin.math.min

/**
 * 角色 / 队伍评分引擎。
 *
 * 角色分：100 分制（单位价值 + 循环 + 配队协同 + 场景 + 机制 + utility）
 * 队伍分：[TeamScore] 由 DES 模拟结果归一化得到。
 */
class ScoringEngine(
    private val damageCalc: DamageCalculator,
    private val simulator: DiscreteEventSimulator
) {
    /**
     * @param gearLookup 可选：解析 build 中的光锥 / 遗器实体；为空则仅用主副词条折算。
     */
    fun scoreCharacter(
        character: Character,
        config: ScoringConfig,
        allCharacters: List<Character>,
        defaultEnemy: Enemy,
        skillTree: SkillTree? = null,
        gearLookup: GearLookup = GearLookup.Empty
    ): CharacterScore {
        val targetEnemy = config.enemy ?: defaultEnemy
        val build = config.playerBuild
        val lightCone = gearLookup.lightCone(build.lightConeId)
        val set4 = gearLookup.relicSet(build.relicSet4)
        val set2 = build.relicSet2?.let { gearLookup.relicSet(it) }
        val buildBuffs = BuildEffectResolver.resolveBuffs(
            build = build,
            lightCone = lightCone,
            relicSet4 = set4,
            relicSet2 = set2
        )
        val flatStats = BuildEffectResolver.applyFlatStats(character.baseStats, build)

        val uv = damageCalc.unitValue(
            character = character,
            enemy = targetEnemy,
            skillTree = skillTree,
            buildBuffs = buildBuffs,
            effectiveSpd = flatStats.spd,
            attackerLevel = build.level
        )
        val allUV = allCharacters.map {
            damageCalc.unitValue(it, targetEnemy, skillTree = skillTree)
        }
        val unitScore = normalizeRole(uv, character.role, allUV) * 25.0

        val cycleSc = cycleScore(character, flatStats.spd) * 5.0

        val teamScore = calculateTeamSynergy(
            character = character,
            allCharacters = allCharacters,
            enemy = targetEnemy,
            selfBuild = build,
            gearLookup = gearLookup
        ) * 40.0

        val scenarioSc = calculateScenarioScore(character, targetEnemy) * 20.0
        val mechanicSc = calculateMechanicScore(character) * 10.0

        val utilityScore = min(
            10.0,
            ((uv.baseHealValue / 2000.0) + (uv.baseShieldValue / 2000.0)).coerceAtMost(1.0) * 10.0
        )

        val total = (unitScore + cycleSc + teamScore + scenarioSc + mechanicSc + utilityScore)
            .coerceIn(0.0, 100.0)
        return CharacterScore(
            characterId = character.id,
            unitValueScore = unitScore,
            cycleScore = cycleSc,
            teamSynergyScore = teamScore,
            scenarioScore = scenarioSc,
            mechanicCoverage = mechanicSc,
            utilityScore = utilityScore,
            total = total,
            tier = tierOf(total)
        )
    }

    /**
     * 对 4 人队伍跑 DES，聚合成 [TeamScore]。
     */
    fun scoreTeam(
        team: List<Character>,
        enemy: Enemy,
        builds: Map<String, PlayerBuild> = emptyMap(),
        gearLookup: GearLookup = GearLookup.Empty,
        rounds: Int = 5
    ): TeamScore {
        require(team.isNotEmpty()) { "team must not be empty" }
        val combatants = team.map { c ->
            val build = builds[c.id]
            val lc = build?.lightConeId?.let { gearLookup.lightCone(it) }
            val s4 = build?.relicSet4?.let { gearLookup.relicSet(it) }
            val s2 = build?.relicSet2?.let { gearLookup.relicSet(it) }
            c.toCombatant(
                lightCone = lc,
                relicSet = s4,
                build = build,
                relicSet2 = s2
            )
        }
        val result = simulator.simulate(combatants, listOf(enemy.toCombatant()), rounds = rounds)
        return teamScoreFrom(result, team)
    }

    fun teamScoreFrom(result: SimulationResult, team: List<Character> = emptyList()): TeamScore {
        val totalDmg = result.damageBreakdown.total
        val totalHeal = result.totalHealing.values.sum()
        val totalShield = result.totalShielding.values.sum()

        // 归一化：约 100 万总伤满分段、击杀与大招加成
        val dmgPart = min(totalDmg / 1_000_000.0, 1.0) * 55.0
        val killPart = when (val r = result.roundsToKill) {
            null -> 0.0
            else -> ((6 - r).coerceIn(0, 5) / 5.0) * 20.0
        }
        val ultPart = min(result.ultsCast.values.sum() / 12.0, 1.0) * 15.0
        val utilPart = min((totalHeal + totalShield) / 50_000.0, 1.0) * 10.0
        val score = (dmgPart + killPart + ultPart + utilPart).coerceIn(0.0, 100.0)

        val tags = team.flatMap { it.tags }.toSet()
        val breakdown = linkedMapOf(
            "damage" to dmgPart,
            "kill" to killPart,
            "ult" to ultPart,
            "utility" to utilPart,
            "tagCoverage" to min(tags.size / 8.0, 1.0) * 100.0
        )

        return TeamScore(
            totalDamage = totalDmg,
            totalHealing = totalHeal,
            totalShielding = totalShield,
            roundsToKill = result.roundsToKill,
            ultsCast = result.ultsCast,
            buffUptime = result.totalBuffUptime,
            breakdown = breakdown,
            score = score
        )
    }

    private fun normalizeRole(
        uv: CharacterUnitValue,
        role: Role,
        all: List<CharacterUnitValue>
    ): Double {
        if (all.isEmpty()) return 0.5

        val primary = when (role) {
            Role.DPS, Role.SUB_DPS ->
                uv.expectedSkillDmg + uv.expectedUltDmg + uv.expectedTalentDmg +
                    uv.expectedFollowUpDmg + uv.dotDps * 3
            Role.HEALER -> uv.baseHealValue
            Role.SHIELD -> uv.baseShieldValue
            Role.SUPPORT -> uv.baseSupportValue + uv.ultChargeRate * 1000
        }

        val maxAll = all.maxOf { other ->
            when (role) {
                Role.DPS, Role.SUB_DPS ->
                    other.expectedSkillDmg + other.expectedUltDmg + other.expectedTalentDmg +
                        other.expectedFollowUpDmg + other.dotDps * 3
                Role.HEALER -> other.baseHealValue
                Role.SHIELD -> other.baseShieldValue
                Role.SUPPORT -> other.baseSupportValue + other.ultChargeRate * 1000
            }
        }

        return if (maxAll > 0.0) (primary / maxAll).coerceIn(0.0, 1.0) else 0.5
    }

    private fun cycleScore(c: Character, effectiveSpd: Double): Double {
        val p = c.cycleProfile
        val actions = p?.cycleActions?.toDouble()
            ?: ((effectiveSpd / 100.0) * 3.0).coerceIn(2.0, 6.0)
        // 2 动基线，6 动满分
        return ((actions - 2) / 4.0).coerceIn(0.0, 1.0)
    }

    private fun calculateScenarioScore(c: Character, enemy: Enemy): Double {
        var score = 0.0
        if (c.element in enemy.weaknesses) {
            score += 0.6
        } else {
            score += 0.2
        }
        if (c.tags.contains(Tag.DOT)) score += 0.15
        if (enemy.weaknesses.size >= 2 && c.element in enemy.weaknesses) score += 0.15
        return score.coerceIn(0.0, 1.0)
    }

    private fun calculateMechanicScore(c: Character): Double {
        val tags = c.tags
        if (tags.isEmpty()) return 0.3
        val baseScore = min(tags.size / 5.0, 1.0) * 0.7
        var comboBonus = 0.0
        if (tags.contains(Tag.DOT) && tags.contains(Tag.ULT_DMG_BONUS)) comboBonus += 0.1
        if (tags.contains(Tag.FOLLOW_UP) && tags.contains(Tag.CRIT_BOOST)) comboBonus += 0.1
        if (tags.contains(Tag.SHIELD) && tags.contains(Tag.HEAL)) comboBonus += 0.1
        if (tags.contains(Tag.DEBUFF) && tags.contains(Tag.SPEED_BOOST)) comboBonus += 0.1
        if (tags.contains(Tag.ULT_CHARGE) && tags.contains(Tag.ENERGY_REGEN)) comboBonus += 0.1
        return (baseScore + comboBonus).coerceIn(0.0, 1.0)
    }

    private fun calculateTeamSynergy(
        character: Character,
        allCharacters: List<Character>,
        enemy: Enemy,
        selfBuild: PlayerBuild?,
        gearLookup: GearLookup
    ): Double {
        val teammates = allCharacters
            .filter { it.id != character.id }
            .sortedByDescending { it.role matchBonusTo character.role }
            .take(3)

        val teamChars = listOf(character) + teammates
        val team = teamChars.mapIndexed { index, c ->
            if (index == 0 && selfBuild != null) {
                c.toCombatant(
                    lightCone = gearLookup.lightCone(selfBuild.lightConeId),
                    relicSet = gearLookup.relicSet(selfBuild.relicSet4),
                    build = selfBuild,
                    relicSet2 = selfBuild.relicSet2?.let { gearLookup.relicSet(it) }
                )
            } else {
                c.toCombatant()
            }
        }

        val enemyCombatant = enemy.toCombatant()
        return try {
            val result = simulator.simulate(team, listOf(enemyCombatant), rounds = 5)
            val totalDmg = result.damageBreakdown.total
            val killBonus = if (result.roundsToKill != null && result.roundsToKill <= 5) 0.15 else 0.0
            val ultBonus = min(result.ultsCast.values.sum() / 20.0, 0.15)
            val dmgScore = min(totalDmg / 1_000_000.0, 0.7)
            (dmgScore + killBonus + ultBonus).coerceIn(0.0, 1.0)
        } catch (_: Exception) {
            val singleTeam = listOf(character.toCombatant(build = selfBuild))
            val singleResult = simulator.simulate(singleTeam, listOf(enemyCombatant))
            (singleResult.damageBreakdown.total / 500_000.0).coerceIn(0.0, 1.0)
        }
    }

    /** 计算角色之间的配队协同加成 */
    private infix fun Role.matchBonusTo(other: Role): Int = when (this) {
        Role.DPS -> if (other == Role.SUPPORT) 2 else if (other == Role.HEALER) 1 else 0
        Role.SUB_DPS -> if (other == Role.DPS) 2 else 1
        Role.SUPPORT -> 2
        Role.HEALER -> if (other != Role.HEALER) 1 else 0
        Role.SHIELD -> if (other != Role.SHIELD) 1 else 0
    }

    private fun tierOf(score: Double): Tier = when {
        score >= 90 -> Tier.S
        score >= 80 -> Tier.A
        score >= 65 -> Tier.B
        else -> Tier.C
    }
}

/** 评分时解析 build 引用的光锥 / 遗器。 */
interface GearLookup {
    fun lightCone(id: String): LightCone?
    fun relicSet(id: String): RelicSet?

    object Empty : GearLookup {
        override fun lightCone(id: String): LightCone? = null
        override fun relicSet(id: String): RelicSet? = null
    }

    class Maps(
        private val cones: Map<String, LightCone> = emptyMap(),
        private val relics: Map<String, RelicSet> = emptyMap()
    ) : GearLookup {
        override fun lightCone(id: String): LightCone? =
            if (id.isBlank()) null else cones[id]

        override fun relicSet(id: String): RelicSet? =
            if (id.isBlank()) null else relics[id]
    }
}
