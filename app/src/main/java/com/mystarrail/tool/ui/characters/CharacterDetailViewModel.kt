package com.mystarrail.tool.ui.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.CharacterScore
import com.mystarrail.tool.data.model.Eidolon
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.MainStats
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.ScoringConfig
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.repository.CharacterRepository
import com.mystarrail.tool.engine.simulator.ScoringEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharacterDetailUiState(
    val character: Character? = null,
    val lightCones: List<LightCone> = emptyList(),
    val selectedCone: LightCone? = null,
    val selectedEidolons: Set<Int> = emptySet(),
    val eidolons: List<Eidolon> = emptyList(),
    val score: CharacterScore? = null,
    val relicSets: List<RelicSet> = emptyList(),
    val skillTree: SkillTree? = null
)

class CharacterDetailViewModel(
    private val characterId: String,
    private val repository: CharacterRepository,
    private val scoringEngine: ScoringEngine
) : ViewModel() {

    private val _state = MutableStateFlow(CharacterDetailUiState())
    val state: StateFlow<CharacterDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val char = repository.getCharacter(characterId)
            val cones = repository.observeAllLightCones().first()
            val relics = repository.observeAllRelicSets().first()
            val eidolons = repository.getEidolonsFor(characterId)
            val skillTree = repository.getSkillTreeFor(characterId)
            // 默认选择第一个光锥（强制光锥：UI 必须至少选一个）
            val defaultCone = cones.firstOrNull()
            _state.update {
                it.copy(
                    character = char,
                    lightCones = cones,
                    selectedCone = defaultCone,
                    eidolons = eidolons,
                    relicSets = relics,
                    skillTree = skillTree
                )
            }
            if (char != null && defaultCone != null) {
                recompute(char, defaultCone, emptySet())
            }
        }
    }

    fun selectCone(cone: LightCone?) {
        _state.update { it.copy(selectedCone = cone) }
        val s = _state.value
        val char = s.character ?: return
        val c = cone ?: return
        viewModelScope.launch {
            recompute(char, c, s.selectedEidolons)
        }
    }

    fun toggleEidolon(level: Int) {
        _state.update {
            val newSet = it.selectedEidolons.toMutableSet()
            if (level in newSet) newSet.remove(level) else newSet.add(level)
            it.copy(selectedEidolons = newSet)
        }
        val s = _state.value
        val char = s.character ?: return
        val cone = s.selectedCone ?: return
        viewModelScope.launch {
            recompute(char, cone, s.selectedEidolons)
        }
    }

    private suspend fun recompute(char: Character, cone: LightCone, eidolons: Set<Int>) {
        val skillTree = _state.value.skillTree
        val build = PlayerBuild(
            characterId = char.id,
            lightConeId = cone.id,
            relicSet4 = "quantum_set", // 占位：默认套
            mainStats = MainStats(
                body = StatType.CRIT_DMG,
                boots = StatType.SPD,
                sphere = StatType.EHR,
                rope = StatType.ATK
            ),
            subStats = emptyList(),
            eidolons = eidolons
        )
        val allChars = repository.observeAllCharacters().first()
        val defaultEnemy = Enemy(
            id = "default",
            name = "Default",
            count = 1,
            weaknesses = setOf(char.element),
            type = EnemyType.BOSS,
            hp = 200_000.0,
            toughness = 240.0
        )
        val score = scoringEngine.scoreCharacter(
            character = char,
            config = ScoringConfig(playerBuild = build),
            allCharacters = allChars,
            defaultEnemy = defaultEnemy,
            skillTree = skillTree
        )
        _state.update { it.copy(score = score) }
    }

    companion object {
        fun factory(
            characterId: String,
            repository: CharacterRepository,
            scoringEngine: ScoringEngine
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CharacterDetailViewModel(characterId, repository, scoringEngine) as T
        }
    }
}