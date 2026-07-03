package com.java.myapplication.engine.simulator.sim

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.Path
import com.java.myapplication.data.model.Role
import com.java.myapplication.data.model.Scaling
import com.java.myapplication.data.model.Stats
import com.java.myapplication.engine.simulator.damage.DamageCalculator
import com.java.myapplication.engine.simulator.tables.FormulaTables
import org.junit.Test

class DiscreteEventSimulatorTest {
    private val sim = DiscreteEventSimulator(DamageCalculator(FormulaTables()))

    private val seele = Character(
        id = "seele", name = "希儿", rarity = 5,
        path = Path.HUNT, element = Element.QUANTUM, role = Role.DPS,
        tags = emptySet(),
        baseStats = Stats(931.0, 756.0, 363.0, 115.0),
        scaling = Scaling(2.2, 4.2, 3.0, 2.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 1
    )

    private val bronya = Character(
        id = "bronya", name = "布洛妮娅", rarity = 5,
        path = Path.HARMONY, element = Element.WIND, role = Role.SUPPORT,
        tags = emptySet(),
        baseStats = Stats(1241.0, 582.0, 533.0, 134.0),
        scaling = Scaling(0.0, 1.0, 0.0, 0.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 1
    )

    private val boss = Character(
        id = "boss", name = "Boss", rarity = 0,
        path = Path.DESTRUCTION, element = Element.QUANTUM, role = Role.DPS,
        tags = emptySet(),
        baseStats = Stats(100000.0, 1000.0, 500.0, 100.0),
        scaling = Scaling(0.0, 0.0, 0.0, 0.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 1
    )

    private fun enemy(): Combatant = Combatant(
        character = boss, stats = boss.baseStats,
        lightCone = null, relicSet = null, eidolons = emptyMap(),
        hp = boss.baseStats.hp * 100
    )

    @Test fun `simulate 5 rounds always returns non-empty log`() {
        val team = listOf(seele.toCombatant())
        val enemies = listOf(enemy())
        val result = sim.simulate(team, enemies, rounds = 5)
        assertThat(result.log).isNotEmpty()
    }

    @Test fun `team actions count is positive after simulation`() {
        val team = listOf(seele.toCombatant())
        val enemies = listOf(enemy())
        val result = sim.simulate(team, enemies, rounds = 5)
        assertThat(result.actions["seele"]).isAtLeast(1)
    }

    @Test fun `empty team throws`() {
        try {
            sim.simulate(emptyList(), listOf(enemy()))
            error("should have thrown")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("team cannot be empty")
        }
    }

    @Test fun `4 character team produces damage from multiple sources`() {
        val team = listOf(
            seele.toCombatant(),
            bronya.toCombatant(),
            seele.copy(id = "himeko", element = Element.FIRE, baseStats = Stats(1041.0, 756.0, 363.0, 112.0)).toCombatant(),
            bronya.copy(id = "tingyun", element = Element.LIGHTNING, baseStats = Stats(800.0, 600.0, 400.0, 130.0)).toCombatant()
        )
        val enemies = listOf(enemy())
        val result = sim.simulate(team, enemies, rounds = 5)
        assertThat(result.damageBreakdown.total).isGreaterThan(0.0)
        assertThat(result.actions.keys).hasSize(4)
    }

    @Test fun `dead enemy stops taking damage in log`() {
        val tinyEnemy = enemy()
        tinyEnemy.hp = 1.0
        val team = listOf(seele.toCombatant())
        val result = sim.simulate(team, listOf(tinyEnemy), rounds = 1)
        val hits = result.log.flatMap { it.targets }
        // 1 回合内最多 1 次行动（实际可能 0 或 1），总伤害不超过一次 follow-up
        assertThat(hits.size).isAtMost(2)  // 至多 followUp + skill
        val totalDmg = hits.sumOf { it.damage }
        assertThat(totalDmg).isAtMost(2000.0)
    }
}