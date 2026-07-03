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
import com.mystarrail.tool.engine.simulator.tables.FormulaTables
import org.junit.Test

class ScoringEngineTest {
    private val engine = ScoringEngine(
        DamageCalculator(FormulaTables()),
        DiscreteEventSimulator(DamageCalculator(FormulaTables()))
    )

    private val seele = Character(
        id = "seele", name = "希儿", rarity = 5,
        path = Path.HUNT, element = Element.QUANTUM, role = Role.DPS,
        tags = setOf(Tag.SINGLE_TARGET, Tag.CRIT_BOOST, Tag.FOLLOW_UP),
        baseStats = Stats(931.0, 756.0, 363.0, 115.0),
        scaling = Scaling(2.2, 4.2, 3.0, 2.0, 0.0),
        cycleProfile = CycleProfile(4, listOf(134.0, 143.0, 160.0), isFollowUp = true),
        iconUrl = "", version = 1
    )

    private val enemy = Enemy(
        id = "boss", name = "Boss", count = 1,
        weaknesses = setOf(Element.QUANTUM), type = EnemyType.BOSS,
        hp = 200000.0, toughness = 240.0
    )

    @Test fun `score is within 0-100`() {
        val score = engine.scoreCharacter(
            seele,
            ScoringConfig(
                playerBuild = PlayerBuild(
                    characterId = "seele", lightConeId = "in_the_night",
                    relicSet4 = "quantum_set", mainStats = MainStats(
                        StatType.CRIT_DMG, StatType.SPD, StatType.EHR, StatType.ATK
                    ),
                    subStats = emptyList()
                ),
                enemy = enemy
            ),
            allCharacters = listOf(seele),
            defaultEnemy = enemy
        )
        assertThat(score.total).isAtLeast(0.0)
        assertThat(score.total).isAtMost(100.0)
    }

    @Test fun `tier is assigned based on score`() {
        val score = engine.scoreCharacter(
            seele,
            ScoringConfig(
                playerBuild = PlayerBuild(
                    characterId = "seele", lightConeId = "in_the_night",
                    relicSet4 = "quantum_set", mainStats = MainStats(
                        StatType.CRIT_DMG, StatType.SPD, StatType.EHR, StatType.ATK
                    ),
                    subStats = emptyList()
                )
            ),
            allCharacters = listOf(seele),
            defaultEnemy = enemy
        )
        assertThat(score.tier).isNotNull()
    }

    @Test fun `breakdown sum approximately equals total`() {
        val score = engine.scoreCharacter(
            seele,
            ScoringConfig(
                playerBuild = PlayerBuild(
                    characterId = "seele", lightConeId = "in_the_night",
                    relicSet4 = "quantum_set", mainStats = MainStats(
                        StatType.CRIT_DMG, StatType.SPD, StatType.EHR, StatType.ATK
                    ),
                    subStats = emptyList()
                )
            ),
            allCharacters = listOf(seele),
            defaultEnemy = enemy
        )
        val sum = score.unitValueScore + score.cycleScore + score.teamSynergyScore +
                  score.scenarioScore + score.mechanicCoverage
        assertThat(score.total).isWithin(5.0).of(sum)
    }
}