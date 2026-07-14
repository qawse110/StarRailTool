package com.mystarrail.tool.engine.team

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.engine.simulator.ScoringEngine
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.sim.DiscreteEventSimulator
import com.mystarrail.tool.engine.simulator.tables.FormulaTables
import org.junit.Test

class TeamOptimizerTest {
    private val dmg = DamageCalculator(FormulaTables())
    private val engine = ScoringEngine(dmg, DiscreteEventSimulator(dmg))
    private val optimizer = TeamOptimizer(engine)

    @Test
    fun `optimize returns top teams of size 4`() {
        val pool = listOf(
            char("dps1", Role.DPS, Element.QUANTUM),
            char("dps2", Role.DPS, Element.FIRE),
            char("sup1", Role.SUPPORT, Element.WIND),
            char("sup2", Role.SUPPORT, Element.ICE),
            char("heal", Role.HEALER, Element.PHYSICAL),
            char("shd", Role.SHIELD, Element.LIGHTNING)
        )
        val enemy = Enemy(
            id = "e", name = "Boss", count = 1,
            weaknesses = setOf(Element.QUANTUM, Element.FIRE),
            type = EnemyType.BOSS, hp = 200_000.0, toughness = 240.0
        )
        val recs = optimizer.optimize(
            TeamOptimizer.Request(pool = pool, enemy = enemy, topK = 3, simulateLimit = 12)
        )
        assertThat(recs).isNotEmpty()
        assertThat(recs.size).isAtMost(3)
        recs.forEach {
            assertThat(it.team).hasSize(4)
            assertThat(it.teamScore.score).isAtLeast(0.0)
            assertThat(it.teamScore.score).isAtMost(100.0)
            assertThat(it.reasons).isNotEmpty()
        }
    }

    @Test
    fun `locked character always appears in recommendations`() {
        val pool = listOf(
            char("lock", Role.DPS, Element.QUANTUM),
            char("a", Role.SUPPORT, Element.WIND),
            char("b", Role.SUPPORT, Element.ICE),
            char("c", Role.HEALER, Element.PHYSICAL),
            char("d", Role.SHIELD, Element.FIRE)
        )
        val enemy = Enemy(
            id = "e", name = "Boss", count = 1,
            weaknesses = setOf(Element.QUANTUM),
            type = EnemyType.BOSS, hp = 200_000.0, toughness = 200.0
        )
        val recs = optimizer.optimize(
            TeamOptimizer.Request(
                pool = pool,
                enemy = enemy,
                lockedIds = setOf("lock"),
                topK = 3,
                simulateLimit = 10
            )
        )
        assertThat(recs).isNotEmpty()
        recs.forEach { assertThat(it.team.map { c -> c.id }).contains("lock") }
    }

    private fun char(id: String, role: Role, element: Element) = Character(
        id = id,
        name = id,
        rarity = 5,
        path = Path.HUNT,
        element = element,
        role = role,
        tags = setOf(Tag.ATK_BOOST),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null,
        iconUrl = "",
        version = 1
    )
}
