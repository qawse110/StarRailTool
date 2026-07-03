package com.java.myapplication.ui.battle

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.engine.simulator.sim.SimulationResult
import com.java.myapplication.util.SimulationResultStore
import org.junit.Test

class BattleLogViewModelTest {

    @Test fun `empty when no result in store`() {
        val store = InMemoryStore(null)
        val vm = BattleLogViewModel(store)
        val s = vm.state.value
        assertThat(s.events).isEmpty()
        assertThat(s.totalRounds).isEqualTo(0)
        assertThat(s.totalDmg).isEqualTo(0.0)
    }

    @Test fun `reads from store on init`() {
        // 构造空 SimulationResult
        val result = SimulationResult(
            log = emptyList(),
            totalDamage = emptyMap(),
            totalHealing = emptyMap(),
            totalShielding = emptyMap(),
            totalBuffUptime = emptyMap(),
            enemyKills = 0,
            roundsToKill = null,
            ultsCast = emptyMap(),
            actions = emptyMap(),
            damageBreakdown = com.java.myapplication.engine.simulator.sim.DamageBreakdown()
        )
        val store = InMemoryStore(result)
        val vm = BattleLogViewModel(store)
        val s = vm.state.value
        assertThat(s.totalDmg).isEqualTo(0.0)
        assertThat(s.totalHealing).isEqualTo(0.0)
    }
}

private class InMemoryStore(override var lastSimulationResult: SimulationResult?) : SimulationResultStore