package com.mystarrail.tool.engine.simulator.buffs

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.engine.simulator.buffs.Buff.StatBoost
import com.mystarrail.tool.engine.simulator.buffs.Buff.DamageBonus
import com.mystarrail.tool.engine.simulator.buffs.Buff.EasyDmg
import org.junit.Test

class BuffEvaluatorTest {
    private val eval = BuffEvaluator()

    @Test fun `empty buffs gives zero snapshot`() {
        val snap = eval.evaluate(emptyList())
        assertThat(snap.atkBoost).isEqualTo(0.0)
        assertThat(snap.damageBonus).isEqualTo(0.0)
    }

    @Test fun `multiple ATK buffs accumulate`() {
        val snap = eval.evaluate(listOf(
            StatBoost("a", 1, StatType.ATK, 0.2),
            StatBoost("b", 1, StatType.ATK, 0.3)
        ))
        assertThat(snap.atkBoost).isEqualTo(0.5)
    }

    @Test fun `damage bonus accumulates`() {
        val snap = eval.evaluate(listOf(
            DamageBonus("a", 1, 0.3),
            DamageBonus("b", 1, 0.2)
        ))
        assertThat(snap.damageBonus).isEqualTo(0.5)
    }

    @Test fun `easy dmg accumulates`() {
        val snap = eval.evaluate(listOf(
            EasyDmg("a", 2, 0.3),
            EasyDmg("b", 2, 0.15)
        ))
        assertThat(snap.easyDmgTaken).isWithin(1e-6).of(0.45)
    }

    @Test fun `different stats are isolated`() {
        val snap = eval.evaluate(listOf(
            StatBoost("a", 1, StatType.ATK, 0.5),
            StatBoost("b", 1, StatType.CRIT_RATE, 0.3)
        ))
        assertThat(snap.atkBoost).isEqualTo(0.5)
        assertThat(snap.critRateBoost).isEqualTo(0.3)
    }

    // B2: EHR / BRK_EFF / EFFECT_RES StatBoost coverage
    @Test fun `EHR StatBoost accumulates effectHitRate`() {
        val snap = eval.evaluate(listOf(
            StatBoost("a", 1, StatType.EHR, 0.20),
            StatBoost("b", 1, StatType.EHR, 0.10)
        ))
        assertThat(snap.effectHitRate).isWithin(1e-6).of(0.30)
    }

    @Test fun `BRK_EFF StatBoost accumulates breakEffect`() {
        val snap = eval.evaluate(listOf(
            StatBoost("a", 1, StatType.BRK_EFF, 0.15)
        ))
        assertThat(snap.breakEffect).isEqualTo(0.15)
    }

    @Test fun `EFFECT_RES StatBoost accumulates effectRes`() {
        val snap = eval.evaluate(listOf(
            StatBoost("a", 1, StatType.EFFECT_RES, 0.20)
        ))
        assertThat(snap.effectRes).isEqualTo(0.20)
    }
}