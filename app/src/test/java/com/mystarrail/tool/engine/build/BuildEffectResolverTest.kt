package com.mystarrail.tool.engine.build

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.MainStats
import com.mystarrail.tool.data.model.PassiveEffect
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.SubStat
import com.mystarrail.tool.engine.simulator.buffs.Buff
import org.junit.Test

class BuildEffectResolverTest {

    @Test
    fun `main stat CRIT_DMG produces stat boost buff`() {
        val build = PlayerBuild(
            characterId = "x",
            lightConeId = "",
            relicSet4 = "",
            mainStats = MainStats(
                body = StatType.CRIT_DMG,
                boots = StatType.SPD,
                sphere = StatType.ATK,
                rope = StatType.ATK
            ),
            subStats = listOf(SubStat(StatType.CRIT_RATE, 0.12, 4))
        )
        val buffs = BuildEffectResolver.resolveBuffs(build)
        assertThat(buffs.filterIsInstance<Buff.StatBoost>().any {
            it.stat == StatType.CRIT_DMG && it.value > 0
        }).isTrue()
        assertThat(buffs.filterIsInstance<Buff.StatBoost>().any {
            it.stat == StatType.CRIT_RATE && it.value == 0.12
        }).isTrue()
        // SPD 走 flat，不进百分比 StatBoost
        assertThat(buffs.filterIsInstance<Buff.StatBoost>().none { it.stat == StatType.SPD }).isTrue()
    }

    @Test
    fun `applyFlatStats adds SPD main and sub`() {
        val base = Stats(1000.0, 700.0, 400.0, 100.0)
        val build = PlayerBuild(
            characterId = "x",
            lightConeId = "",
            relicSet4 = "",
            mainStats = MainStats(
                body = StatType.CRIT_DMG,
                boots = StatType.SPD,
                sphere = StatType.ATK,
                rope = StatType.ATK
            ),
            subStats = listOf(SubStat(StatType.SPD, 10.0, 3))
        )
        val stats = BuildEffectResolver.applyFlatStats(base, build)
        assertThat(stats.spd).isEqualTo(100.0 + 25.0 + 10.0)
    }

    @Test
    fun `relic two and four piece contribute buffs`() {
        val set = RelicSet(
            id = "q",
            name = "Quantum",
            twoPiece = PassiveEffect.StatBoost(StatType.ATK, 0.12),
            fourPiece = PassiveEffect.DamageBonus(0.2, com.mystarrail.tool.data.model.DmgCondition.ALWAYS),
            suitableFor = setOf(Role.DPS)
        )
        val build = PlayerBuild(
            characterId = "x",
            lightConeId = "",
            relicSet4 = "q",
            mainStats = BuildEffectResolver.defaultMainStats(Role.DPS),
            subStats = emptyList()
        )
        val buffs = BuildEffectResolver.resolveBuffs(build, relicSet4 = set)
        assertThat(buffs.filterIsInstance<Buff.StatBoost>().any {
            it.stat == StatType.ATK && it.value >= 0.12
        }).isTrue()
        assertThat(buffs.filterIsInstance<Buff.DamageBonus>()).isNotEmpty()
    }
}
