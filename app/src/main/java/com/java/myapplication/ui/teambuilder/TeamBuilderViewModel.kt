package com.java.myapplication.ui.teambuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Enemy
import com.java.myapplication.data.model.EnemyType
import com.java.myapplication.data.repository.CharacterRepository
import com.java.myapplication.engine.simulator.ScoringEngine
import com.java.myapplication.engine.simulator.sim.SimulationResult
import com.java.myapplication.engine.simulator.sim.toCombatant
import com.java.myapplication.util.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TeamUiState(
    val allChars: List<Character> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val isSimulating: Boolean = false,
    val lastResult: SimulationResult? = null,
    val lastTotalDmg: Double = 0.0
) {
    val canSimulate: Boolean get() = selectedIds.size == 4 && !isSimulating
}

class TeamBuilderViewModel(
    private val repository: CharacterRepository,
    @Suppress("unused") private val scoringEngine: ScoringEngine,
    private val services: ServiceLocator
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val internalState = MutableStateFlow(InternalState())

    private data class InternalState(
        val isSimulating: Boolean = false,
        val lastResult: SimulationResult? = null,
        val lastTotalDmg: Double = 0.0
    )

    val uiState: StateFlow<TeamUiState> = combine(
        repository.observeAllCharacters(),
        selectedIds
    ) { chars, ids ->
        // 不读 internalState：避免循环依赖（UI 不需要 isSimulating 之外的内部字段以外）
        TeamUiState(
            allChars = chars,
            selectedIds = ids
        )
    }.combine(internalState) { state, internal ->
        state.copy(
            isSimulating = internal.isSimulating,
            lastResult = internal.lastResult,
            lastTotalDmg = internal.lastTotalDmg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TeamUiState()
    )

    fun toggleChar(id: String) {
        selectedIds.update { current ->
            if (id in current) current - id
            else if (current.size < 4) current + id
            else current // 满了就不加
        }
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun simulate() {
        val ids = selectedIds.value
        if (ids.size != 4) return
        internalState.update { it.copy(isSimulating = true) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                runSimulation(ids.toList())
            }
            // 把 result 写到 ServiceLocator 给 BattleLog 用
            services.lastSimulationResult = result
            internalState.update {
                it.copy(
                    isSimulating = false,
                    lastResult = result,
                    lastTotalDmg = result.damageBreakdown.total
                )
            }
        }
    }

    private suspend fun runSimulation(charIds: List<String>): SimulationResult {
        val allChars = repository.observeAllCharacters().first()
        val selected = allChars.filter { it.id in charIds }
        val team = selected.map { it.toCombatant() }

        val enemyElements = selected.map { it.element }.toSet()
        val enemy = Enemy(
            id = "team_enemy",
            name = "Team Dummy",
            count = 1,
            weaknesses = enemyElements,
            type = EnemyType.BOSS,
            hp = 200_000.0,
            toughness = 240.0
        )
        // 用 ServiceLocator 的 DiscreteEventSimulator 跑（不是 ScoringEngine 内部那个）
        return services.simulator.simulate(team, listOf(enemy.toCombatant()))
    }

    companion object {
        fun factory(
            repository: CharacterRepository,
            scoringEngine: ScoringEngine,
            services: ServiceLocator
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TeamBuilderViewModel(repository, scoringEngine, services) as T
        }
    }
}