package com.mystarrail.tool.ui.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.CharacterScore
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.ScoringConfig
import com.mystarrail.tool.data.repository.CharacterRepository
import com.mystarrail.tool.engine.build.BuildEffectResolver
import com.mystarrail.tool.engine.simulator.GearLookup
import com.mystarrail.tool.engine.simulator.ScoringEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AssessmentUiState(
    val rows: List<CharacterScore> = emptyList(),
    val charMap: Map<String, Character> = emptyMap(),
    val useSavedBuilds: Boolean = true,
    val isLoading: Boolean = true
)

class AssessmentViewModel(
    private val repository: CharacterRepository,
    private val scoringEngine: ScoringEngine
) : ViewModel() {

    private val _state = MutableStateFlow(AssessmentUiState())
    val state: StateFlow<AssessmentUiState> = _state.asStateFlow()

    // 兼容旧字段名
    val uiState: StateFlow<AssessmentUiState> = state

    init {
        refresh()
    }

    fun setUseSavedBuilds(use: Boolean) {
        if (_state.value.useSavedBuilds == use) return
        _state.value = _state.value.copy(useSavedBuilds = use, isLoading = true)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val chars = repository.observeAllCharacters().first()
            val charMap = chars.associateBy { it.id }
            val builds = repository.observeAllPlayerBuilds().first()
                .groupBy { it.characterId }
                .mapValues { (_, list) -> list.maxByOrNull { it.id } }
            val cones = repository.observeAllLightCones().first().associateBy { it.id }
            val relics = repository.observeAllRelicSets().first().associateBy { it.id }
            val gear = GearLookup.Maps(cones, relics)

            val defaultEnemy = Enemy(
                id = "default",
                name = "Default Boss",
                count = 1,
                weaknesses = setOf(com.mystarrail.tool.data.model.Element.QUANTUM),
                type = EnemyType.BOSS,
                hp = 200_000.0,
                toughness = 240.0
            )
            val useSaved = _state.value.useSavedBuilds
            val scores = chars.map { c ->
                val saved = if (useSaved) builds[c.id] else null
                val build: PlayerBuild = saved ?: BuildEffectResolver.defaultBuild(c)
                scoringEngine.scoreCharacter(
                    character = c,
                    config = ScoringConfig(playerBuild = build),
                    allCharacters = chars,
                    defaultEnemy = defaultEnemy,
                    gearLookup = gear
                )
            }.sortedByDescending { it.total }

            _state.value = AssessmentUiState(
                rows = scores,
                charMap = charMap,
                useSavedBuilds = useSaved,
                isLoading = false
            )
        }
    }

    companion object {
        fun factory(repo: CharacterRepository, scoringEngine: ScoringEngine) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AssessmentViewModel(repo, scoringEngine) as T
            }
    }
}
