package com.mystarrail.tool.ui.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.CharacterScore
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.MainStats
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.ScoringConfig
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.repository.CharacterRepository
import com.mystarrail.tool.engine.simulator.ScoringEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AssessmentUiState(
    val rows: List<CharacterScore> = emptyList(),
    val charMap: Map<String, Character> = emptyMap(),
    val isLoading: Boolean = true
)

class AssessmentViewModel(
    private val repository: CharacterRepository,
    private val scoringEngine: ScoringEngine
) : ViewModel() {

    val uiState: StateFlow<AssessmentUiState> =
        MutableStateFlow(AssessmentUiState()).asStateFlow()

    private val _state = MutableStateFlow(AssessmentUiState())
    val state: StateFlow<AssessmentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val chars = repository.observeAllCharacters().first()
            val charMap = chars.associateBy { it.id }
            val defaultEnemy = Enemy(
                id = "default",
                name = "Default Boss",
                count = 1,
                weaknesses = setOf(com.mystarrail.tool.data.model.Element.QUANTUM),
                type = EnemyType.BOSS,
                hp = 200_000.0,
                toughness = 240.0
            )
            val scores = chars.map { c ->
                val build = PlayerBuild(
                    characterId = c.id,
                    lightConeId = "",
                    relicSet4 = "",
                    mainStats = MainStats(
                        body = StatType.CRIT_DMG,
                        boots = StatType.SPD,
                        sphere = StatType.ATK,
                        rope = StatType.ATK
                    ),
                    subStats = emptyList()
                )
                scoringEngine.scoreCharacter(
                    character = c,
                    config = ScoringConfig(playerBuild = build),
                    allCharacters = chars,
                    defaultEnemy = defaultEnemy
                )
            }.sortedByDescending { it.total }

            _state.value = AssessmentUiState(
                rows = scores,
                charMap = charMap,
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