package com.mystarrail.tool.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mystarrail.tool.engine.simulator.sim.DamageBreakdown
import com.mystarrail.tool.engine.simulator.sim.RoundEvent
import com.mystarrail.tool.engine.simulator.sim.SimulationResult
import com.mystarrail.tool.util.SimulationResultStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BattleLogUiState(
    val events: List<RoundEvent> = emptyList(),
    val totalRounds: Int = 0,
    val totalDmg: Double = 0.0,
    val totalHealing: Double = 0.0,
    val totalShielding: Double = 0.0,
    val dmgByChar: Map<String, Double> = emptyMap(),
    val healingByChar: Map<String, Double> = emptyMap(),
    val ultsByChar: Map<String, Int> = emptyMap(),
    val actionsByChar: Map<String, Int> = emptyMap(),
    val damageBreakdown: DamageBreakdown = DamageBreakdown(),
    val roundsToKill: Int? = null,
    val enemyKills: Int = 0
)

class BattleLogViewModel(
    private val resultStore: SimulationResultStore
) : ViewModel() {

    private val _state = MutableStateFlow(loadFromStore())
    val state: StateFlow<BattleLogUiState> = _state.asStateFlow()

    private fun loadFromStore(): BattleLogUiState {
        val result = resultStore.lastSimulationResult
        return if (result == null) {
            BattleLogUiState()
        } else {
            BattleLogUiState(
                events = result.log,
                totalRounds = result.log.maxOfOrNull { it.round } ?: 0,
                totalDmg = result.totalDamage.values.sum(),
                totalHealing = result.totalHealing.values.sum(),
                totalShielding = result.totalShielding.values.sum(),
                dmgByChar = result.totalDamage,
                healingByChar = result.totalHealing,
                ultsByChar = result.ultsCast,
                actionsByChar = result.actions,
                damageBreakdown = result.damageBreakdown,
                roundsToKill = result.roundsToKill,
                enemyKills = result.enemyKills
            )
        }
    }

    companion object {
        fun factory(resultStore: SimulationResultStore) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BattleLogViewModel(resultStore) as T
        }
    }
}