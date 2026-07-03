package com.java.myapplication.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.java.myapplication.engine.simulator.sim.SimulationResult
import com.java.myapplication.util.SimulationResultStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BattleLogUiState(
    val events: List<com.java.myapplication.engine.simulator.sim.RoundEvent> = emptyList(),
    val totalRounds: Int = 0,
    val totalDmg: Double = 0.0,
    val totalHealing: Double = 0.0
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
                totalHealing = result.totalHealing.values.sum()
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