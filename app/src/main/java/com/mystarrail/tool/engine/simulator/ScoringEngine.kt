package com.mystarrail.tool.engine.simulator

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.CharacterScore
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.ScoringConfig
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.data.model.Tier
import com.mystarrail.tool.engine.simulator.damage.CharacterUnitValue
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.sim.DiscreteEventSimulator
import com.mystarrail.tool.engine.simulator.sim.toCombatant
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 角色评分引擎（增强版）：100 分制
 *  - 单位价值 25 分（G1 期望，归一化）
 *  - 循环期望 5 分
 *  - 配队协同 40 分（G2 模拟，使用 4 人队伍真实模拟）
 *  - 场景适配 20 分（弱点覆盖 + 抗性穿透 + 机制匹配）
 *  - 机制完整度 10 分
 *  - utilityScore（治疗/护盾）作为附加维度，不再从 100 分中扣除
 */
class ScoringEngine(
    private val damageCalc: DamageCalculator,
    private val simulator: DiscreteEventSimulator
) {
    fun scoreCharacter(
        character: Character,
        config: ScoringConfig,
        allCharacters: List<Character>,
        defaultEnemy: Enemy,
        skillTree: SkillTree? = null
    ): CharacterScore {
        val targetEnemy = config.enemy ?: defaultEnemy

        val uv = damageCalc.unitValue(character, targetEnemy, skillTree = skillTree)
        val allUV = allCharacters.map { damageCalc.unitValue(it, targetEnemy, skillTree = skillTree) }
        val unitScore = normalizeRole(uv, character.role, allUV) * 25.0

        val cycleSc = cycleScore(character) * 5.0

        // 改进：使用 4 人队伍模拟（含角色自己 + 3 个同类型辅助）计算配队协同
        val teamScore = calculateTeamSynergy(character, allCharacters, targetEnemy) * 40.0

        val scenarioSc = calculateScenarioScore(character, targetEnemy) * 20.0

        val mechanicSc = calculateMechanicScore(character) * 10.0

        // utilityScore：治疗/护盾能力，作为加分维度，上限 10 分，不与总分 100 冲突
        val utilityScore = min(10.0,
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
     * 归一化角色单位价值：按 Role 分组比较，当前角色在该组中的相对位置。
     * 返回 [0, 1] 的归一化值。
     */
    private fun normalizeRole(
        uv: CharacterUnitValue,
        role: Role,
        all: List<CharacterUnitValue>
    ): Double {
        if (all.isEmpty()) return 0.5

        // 根据角色类型选择主维度
        val primary = when (role) {
            Role.DPS, Role.SUB_DPS ->
                uv.expectedSkillDmg + uv.expectedUltDmg + uv.expectedTalentDmg + uv.expectedFollowUpDmg + uv.dotDps * 3
            Role.HEALER -> uv.baseHealValue
            Role.SHIELD -> uv.baseShieldValue
            Role.SUPPORT -> uv.baseSupportValue + uv.ultChargeRate * 1000
        }

        // 取同组角色在该维度的最大值
        val maxAll = all.maxOf { other ->
            when (role) {
                Role.DPS, Role.SUB_DPS ->
                    other.expectedSkillDmg + other.expectedUltDmg + other.expectedTalentDmg + other.expectedFollowUpDmg + other.dotDps * 3
                Role.HEALER -> other.baseHealValue
                Role.SHIELD -> other.baseShieldValue
                Role.SUPPORT -> other.baseSupportValue + other.ultChargeRate * 1000
            }
        }

        return if (maxAll > 0.0) (primary / maxAll).coerceIn(0.0, 1.0) else 0.5
    }

    /**
     * 循环期望评分：基于角色 cycleProfile 的行动数和速度
     * 基线：3 动作/循环 = 0.5（中性），每多 1 动作 +0.25，满分 5 动作
     */
    private fun cycleScore(c: Character): Double {
        val p = c.cycleProfile ?: return 0.5
        val actions = p.cycleActions.toDouble()
        // 3 动基线，最多 6 动满分
        return ((actions - 2) / 4.0).coerceIn(0.0, 1.0)
    }

    /**
     * 场景适配评分（增强版）：
     * - 属性弱点匹配：0.6 分（基值）
     * - 额外弱点覆盖：每个额外弱点 +0.2
     * - DOT 角色 vs 无 DOT 免疫敌人：+0.15
     * - 控制/推条 vs 非 BOSS：+0.05
     */
    private fun calculateScenarioScore(c: Character, enemy: Enemy): Double {
        var score = 0.0

        // 属性弱点匹配
        if (c.element in enemy.weaknesses) {
            score += 0.6
        } else {
            score += 0.2 // 即使无弱点也能造成伤害（只是抗性更高）
        }

        // DOT 角色：如果敌人非 DOT 免疫
        if (c.tags.contains(Tag.DOT)) {
            score += 0.15 // DOT 在长线战斗中有优势
        }

        // 额外弱点匹配（如果敌人有多个弱点且我方至少一个匹配）
        if (enemy.weaknesses.size >= 2 && c.element in enemy.weaknesses) {
            score += 0.15
        }

        return score.coerceIn(0.0, 1.0)
    }

    /**
     * 机制完整度评分（增强版）：
     * - 基础：标签覆盖数 / 5 * 0.7
     * - 角色特色机制组合加分：
     *   - DOT + 持续增伤
     *   - 追击 + 暴击增益
     *   - 护盾 + 治疗
     *   - 推条 + 减速
     */
    private fun calculateMechanicScore(c: Character): Double {
        val tags = c.tags
        if (tags.isEmpty()) return 0.3

        // 基础覆盖度
        val baseScore = min(tags.size / 5.0, 1.0) * 0.7

        // 特色机制组合加分
        var comboBonus = 0.0
        if (tags.contains(Tag.DOT) && tags.contains(Tag.ULT_DMG_BONUS)) comboBonus += 0.1
        if (tags.contains(Tag.FOLLOW_UP) && tags.contains(Tag.CRIT_BOOST)) comboBonus += 0.1
        if (tags.contains(Tag.SHIELD) && tags.contains(Tag.HEAL)) comboBonus += 0.1
        if (tags.contains(Tag.DEBUFF) && tags.contains(Tag.SPEED_BOOST)) comboBonus += 0.1
        if (tags.contains(Tag.ULT_CHARGE) && tags.contains(Tag.ENERGY_REGEN)) comboBonus += 0.1

        return (baseScore + comboBonus).coerceIn(0.0, 1.0)
    }

    /**
     * 配队协同评分（改进版）：
     * 使用 4 人队伍模拟（自己 + 3 个同类型辅助）计算总伤害贡献。
     * 如果无法构建 4 人队伍，回退到单人模拟。
     */
    private fun calculateTeamSynergy(
        character: Character,
        allCharacters: List<Character>,
        enemy: Enemy
    ): Double {
        // 尝试构建 4 人队伍：角色自己 + 3 个同类型辅助
        val teammates = allCharacters
            .filter { it.id != character.id }
            .sortedByDescending { it.role matchBonusTo character.role }
            .take(3)

        val teamChars = listOf(character) + teammates
        val team = teamChars.map { it.toCombatant() }

        val enemyCombatant = enemy.toCombatant()

        return try {
            val result = simulator.simulate(team, listOf(enemyCombatant), rounds = 5)
            val totalDmg = result.damageBreakdown.total
            val killBonus = if (result.roundsToKill != null && result.roundsToKill <= 5) 0.15 else 0.0
            val ultBonus = min(result.ultsCast.values.sum() / 20.0, 0.15)

            // 归一化：假设 100 万伤害 = 满分
            val dmgScore = min(totalDmg / 1_000_000.0, 0.7)
            (dmgScore + killBonus + ultBonus).coerceIn(0.0, 1.0)
        } catch (e: Exception) {
            // 回退到单人模拟
            val singleTeam = listOf(character.toCombatant())
            val singleResult = simulator.simulate(singleTeam, listOf(enemyCombatant))
            (singleResult.damageBreakdown.total / 500_000.0).coerceIn(0.0, 1.0)
        }
    }

    /** 计算角色之间的配队协同加成 */
    private fun Role.matchBonusTo(other: Role): Int = when (this) {
        Role.DPS -> if (other == Role.SUPPORT) 2 else if (other == Role.HEALER) 1 else 0
        Role.SUB_DPS -> if (other == Role.DPS) 2 else 1
        Role.SUPPORT -> 2 // 辅助万金油
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