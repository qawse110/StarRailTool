package com.mystarrail.tool.engine.simulator

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.CharacterScore
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.ScoringConfig
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.Tier
import com.mystarrail.tool.engine.simulator.damage.CharacterUnitValue
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.sim.DiscreteEventSimulator
import com.mystarrail.tool.engine.simulator.sim.toCombatant
import kotlin.math.min

/**
 * 角色评分引擎（顶层）：100 分制
 *  - 单位价值 25 分（G1 期望，归一化）
 *  - 循环期望 5 分
 *  - 配队协同 40 分（G2 模拟）
 *  - 场景适配 20 分
 *  - 机制完整度 10 分
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

        val cycleScore = cycleScore(character) * 5.0

        val singleTeam = listOf(character.toCombatant())
        val singleResult = simulator.simulate(singleTeam, listOf(targetEnemy.toCombatant()))
        val teamScore = (singleResult.damageBreakdown.total / 10000.0).coerceIn(0.0, 1.0) * 40.0

        val scenarioScore = scenarioScore(character, targetEnemy) * 20.0

        val mechanicScore = min(character.tags.size / 5.0, 1.0) * 10.0

        // B8: utilityScore = 治疗/护盾能力的 6th 维评分
        val utilityScore = min(10.0,
            ((uv.baseHealValue / 2000.0) + (uv.baseShieldValue / 2000.0)).coerceAtMost(1.0) * 10.0
        )

        val total = (unitScore + cycleScore + teamScore + scenarioScore + mechanicScore + utilityScore)
            .coerceIn(0.0, 100.0)
        return CharacterScore(
            characterId = character.id,
            unitValueScore = unitScore,
            cycleScore = cycleScore,
            teamSynergyScore = teamScore,
            scenarioScore = scenarioScore,
            mechanicCoverage = mechanicScore,
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
                uv.expectedSkillDmg + uv.expectedUltDmg + uv.expectedTalentDmg + uv.expectedFollowUpDmg
            Role.HEALER -> uv.baseHealValue
            Role.SHIELD -> uv.baseShieldValue
            Role.SUPPORT -> uv.baseSupportValue
        }

        // 取同组角色在该维度的最大值
        val maxAll = all.maxOf { other ->
            when (role) {
                Role.DPS, Role.SUB_DPS ->
                    other.expectedSkillDmg + other.expectedUltDmg + other.expectedTalentDmg + other.expectedFollowUpDmg
                Role.HEALER -> other.baseHealValue
                Role.SHIELD -> other.baseShieldValue
                Role.SUPPORT -> other.baseSupportValue
            }
        }

        return if (maxAll > 0.0) (primary / maxAll).coerceIn(0.0, 1.0) else 0.5
    }

    private fun cycleScore(c: Character): Double {
        val p = c.cycleProfile ?: return 0.5
        val actions = p.cycleActions.toDouble()
        return ((actions - 3) / 2.0).coerceIn(0.0, 1.0)
    }

    private fun scenarioScore(c: Character, enemy: Enemy): Double {
        return if (c.element in enemy.weaknesses) 0.9 else 0.5
    }

    private fun tierOf(score: Double): Tier = when {
        score >= 90 -> Tier.S
        score >= 80 -> Tier.A
        score >= 65 -> Tier.B
        else -> Tier.C
    }
}