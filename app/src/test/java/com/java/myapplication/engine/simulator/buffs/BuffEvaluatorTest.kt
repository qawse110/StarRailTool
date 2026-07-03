package com.java.myapplication.engine.simulator.buffs

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.model.StatType
import com.java.myapplication.engine.simulator.buffs.Buff.StatBoost
import com.java.myapplication.engine.simulator.buffs.Buff.DamageBonus
import com.java.myapplication.engine.simulator.buffs.Buff.EasyDmg
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
}