package com.java.myapplication.ui.scenario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.Scenario
import com.java.myapplication.data.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ScenarioScore(
    val scenario: Scenario,
    /** 0..100 — 越高越适合 */
    val fitScore: Double,
    /** 该场景覆盖的元素弱点（取自 enemies.weaknesses 并集） */
    val coveredElements: Set<Element>,
    val isBestMatch: Boolean = false
)

data class ScenarioUiState(
    val allChars: List<Character> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val scenarios: List<ScenarioScore> = emptyList(),
    val isLoading: Boolean = true
)

class ScenarioViewModel(
    private val repository: CharacterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScenarioUiState())
    val state: StateFlow<ScenarioUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val chars = repository.observeAllCharacters().first()
            val scenarios = repository.observeAllScenarios().first()
            _state.value = _state.value.copy(
                allChars = chars,
                scenarios = score(scenarios, emptySet(), chars),
                isLoading = false
            )
        }
    }

    fun toggleChar(id: String) {
        _state.value = _state.value.copy(
            selectedIds = if (id in _state.value.selectedIds) {
                _state.value.selectedIds - id
            } else {
                _state.value.selectedIds + id
            }
        )
        // 重新计算分数
        viewModelScope.launch {
            val scenarios = repository.observeAllScenarios().first()
            val bestId = _state.value.scenarios.maxByOrNull { it.fitScore }?.scenario?.id
            _state.value = _state.value.copy(
                scenarios = score(
                    scenarios,
                    _state.value.selectedIds,
                    _state.value.allChars,
                    bestId
                )
            )
        }
    }

    private fun score(
        scenarios: List<Scenario>,
        selectedIds: Set<String>,
        allChars: List<Character>,
        previousBestId: String? = null
    ): List<ScenarioScore> {
        return scenarios.map { s ->
            val covered = s.enemies.flatMap { it.weaknesses.toList() }.toSet()

            // 列表评分：覆盖元素越多分越高（每覆盖一个元素 +15，封顶 100）
            val baseScore = (covered.size * 15.0).coerceAtMost(100.0)

            // 队伍加成：选中角色中元素 = 覆盖元素的，加 +20
            val selectedChars = allChars.filter { it.id in selectedIds }
            val teamBonus = if (selectedChars.isEmpty()) 0.0 else {
                val matchedCount = selectedChars.count { it.element in covered }
                (matchedCount.toDouble() / selectedChars.size.coerceAtLeast(1)) * 20.0
            }

            ScenarioScore(
                scenario = s,
                fitScore = (baseScore + teamBonus).coerceAtMost(100.0),
                coveredElements = covered
            )
        }.sortedByDescending { it.fitScore }.mapIndexed { idx, sc ->
            sc.copy(isBestMatch = idx == 0 && sc.fitScore > 0)
        }
    }

    companion object {
        fun factory(repo: CharacterRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ScenarioViewModel(repo) as T
        }
    }
}