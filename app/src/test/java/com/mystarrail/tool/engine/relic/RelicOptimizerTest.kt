package com.mystarrail.tool.engine.relic

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.PassiveEffect
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.tables.FormulaTables
import org.junit.Test

class RelicOptimizerTest {
    private val optimizer = RelicOptimizer(DamageCalculator(FormulaTables()))

    @Test
    fun `optimize returns ranked relic builds`() {
        val char = Character(
            id = "seele",
            name = "希儿",
            rarity = 5,
            path = Path.HUNT,
            element = Element.QUANTUM,
            role = Role.DPS,
            tags = setOf(Tag.CRIT_BOOST),
            baseStats = Stats(931.0, 756.0, 363.0, 115.0),
            scaling = Scaling(2.2, 4.2, 3.0, 2.0, 0.0),
            cycleProfile = null,
            iconUrl = "",
            version = 1
        )
        val sets = listOf(
            RelicSet(
                id = "q", name = "Quantum",
                twoPiece = PassiveEffect.StatBoost(StatType.ATK, 0.12),
                fourPiece = PassiveEffect.DamageBonus(0.2, com.mystarrail.tool.data.model.DmgCondition.ALWAYS),
                suitableFor = setOf(Role.DPS)
            ),
            RelicSet(
                id = "spd", name = "Messenger",
                twoPiece = PassiveEffect.StatBoost(StatType.SPD, 0.06),
                fourPiece = PassiveEffect.StatBoost(StatType.SPD, 0.12),
                suitableFor = setOf(Role.SUPPORT)
            )
        )
        val enemy = Enemy(
            id = "e", name = "Boss", count = 1,
            weaknesses = setOf(Element.QUANTUM),
            type = EnemyType.BOSS, hp = 200_000.0, toughness = 240.0
        )
        val recs = optimizer.optimize(
            RelicOptimizer.Request(character = char, relicSets = sets, enemy = enemy, topN = 3)
        )
        assertThat(recs).isNotEmpty()
        assertThat(recs.first().relicBuild.set4).isNotEmpty()
        assertThat(recs.first().estimatedUnitScore).isGreaterThan(0.0)
    }

    @Test
    fun `dps main stats prefer crit body`() {
        val main = optimizer.recommendMainStats(Role.DPS)
        assertThat(main.body).isEqualTo(StatType.CRIT_DMG)
        assertThat(main.boots).isEqualTo(StatType.SPD)
    }
}
