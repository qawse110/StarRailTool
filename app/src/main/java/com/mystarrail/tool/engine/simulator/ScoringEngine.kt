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

        val total = unitScore + cycleScore + teamScore + scenarioScore + mechanicScore
        return CharacterScore(
            characterId = character.id,
            unitValueScore = unitScore,
            cycleScore = cycleScore,
            teamSynergyScore = teamScore,
            scenarioScore = scenarioScore,
            mechanicCoverage = mechanicScore,
            total = total.coerceIn(0.0, 100.0),
            tier = tierOf(total)
        )
    }

    private fun normalizeRole(
        uv: CharacterUnitValue,
        role: Role,
        all: List<CharacterUnitValue>
    ): Double = 0.7

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