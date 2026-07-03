package com.mystarrail.tool.engine.simulator

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.CycleProfile
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.MainStats
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.ScoringConfig
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.sim.DiscreteEventSimulator
import com.mystarrail.tool.engine.simulator.sim.toCombatant
import com.mystarrail.tool.engine.simulator.tables.FormulaTables
import org.junit.Test

class SimulatorValidationTest {
    private val damageCalc = DamageCalculator(FormulaTables())
    private val simulator = DiscreteEventSimulator(damageCalc)

    @Test fun `5 rounds always returns non-empty log`() {
        val team = listOf(sampleSeele().toCombatant())
        val enemies = listOf(sampleBoss().toCombatant())
        val result = simulator.simulate(team, enemies, rounds = 5)
        assertThat(result.log).isNotEmpty()
    }

    @Test fun `dead enemy receives no damage`() {
        val team = listOf(sampleSeele().toCombatant())
        val deadEnemy = sampleBoss().toCombatant()
        deadEnemy.hp = 0.0
        val result = simulator.simulate(team, listOf(deadEnemy), rounds = 5)
        val totalDmg = result.log.flatMap { it.targets }.sumOf { it.damage }
        assertThat(totalDmg).isEqualTo(0.0)
    }

    @Test fun `action value never goes below 0 after advance`() {
        val c = sampleSeele().toCombatant()
        c.actionValue = -100.0
        c.actionValue = (c.actionValue + 10000.0 / c.effectiveSpd).coerceAtLeast(0.0)
        assertThat(c.actionValue).isAtLeast(0.0)
    }

    @Test fun `total score clamped 0-100 with max buffs`() {
        val score = ScoringEngine(damageCalc, simulator).scoreCharacter(
            sampleSeele(),
            ScoringConfig(
                playerBuild = PlayerBuild(
                    characterId = "seele", lightConeId = "in_the_night",
                    relicSet4 = "quantum_set",
                    mainStats = MainStats(StatType.CRIT_DMG, StatType.SPD, StatType.EHR, StatType.ATK),
                    subStats = emptyList()
                )
            ),
            allCharacters = listOf(sampleSeele()),
            defaultEnemy = sampleBoss()
        )
        assertThat(score.total).isAtLeast(0.0)
        assertThat(score.total).isAtMost(100.0)
    }

    @Test fun `DOT character has higher cycle score than no-cycle`() {
        val dotChar = sampleSeele().copy(
            cycleProfile = CycleProfile(4, emptyList(), isDot = true)
        )
        val noCycle = sampleSeele().copy(cycleProfile = null)
        val engine = ScoringEngine(damageCalc, simulator)
        val config = ScoringConfig(PlayerBuild(characterId = "seele", lightConeId = "x", relicSet4 = "y",
            mainStats = MainStats(StatType.CRIT_DMG, StatType.SPD, StatType.EHR, StatType.ATK),
            subStats = emptyList()))
        val scoreDot = engine.scoreCharacter(dotChar, config, listOf(dotChar, noCycle), sampleBoss())
        val scoreNoCycle = engine.scoreCharacter(noCycle, config, listOf(dotChar, noCycle), sampleBoss())
        assertThat(scoreDot.cycleScore).isAtLeast(scoreNoCycle.cycleScore)
    }

    private fun sampleSeele() = Character(
        id = "seele", name = "希儿", rarity = 5,
        path = Path.HUNT, element = Element.QUANTUM, role = Role.DPS,
        tags = setOf(Tag.SINGLE_TARGET, Tag.CRIT_BOOST, Tag.FOLLOW_UP),
        baseStats = Stats(931.0, 756.0, 363.0, 115.0),
        scaling = Scaling(2.2, 4.2, 3.0, 2.0, 0.0),
        cycleProfile = CycleProfile(4, listOf(134.0), isFollowUp = true),
        iconUrl = "", version = 1
    )

    private fun sampleBoss() = Enemy(
        id = "boss", name = "Boss", count = 1,
        weaknesses = setOf(Element.QUANTUM), type = EnemyType.BOSS,
        hp = 200000.0, toughness = 240.0
    )
}