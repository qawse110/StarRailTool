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
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.ScoringConfig
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.repository.CharacterRepository
import com.mystarrail.tool.engine.build.BuildEffectResolver
import com.mystarrail.tool.engine.simulator.GearLookup
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
    val skillTree: SkillTree? = null,
    val savedBuild: PlayerBuild? = null,
    val usingSavedBuild: Boolean = false
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
            val saved = repository.observePlayerBuild(characterId).first().maxByOrNull { it.id }
            val defaultCone = saved?.lightConeId?.let { id -> cones.firstOrNull { it.id == id } }
                ?: cones.firstOrNull()
            _state.update {
                it.copy(
                    character = char,
                    lightCones = cones,
                    selectedCone = defaultCone,
                    eidolons = eidolons,
                    relicSets = relics,
                    skillTree = skillTree,
                    savedBuild = saved,
                    usingSavedBuild = saved != null,
                    selectedEidolons = saved?.eidolons ?: emptySet()
                )
            }
            if (char != null) {
                recompute()
            }
        }
    }

    fun selectCone(cone: LightCone?) {
        _state.update { it.copy(selectedCone = cone, usingSavedBuild = false) }
        viewModelScope.launch { recompute() }
    }

    fun toggleEidolon(level: Int) {
        _state.update {
            val newSet = it.selectedEidolons.toMutableSet()
            if (level in newSet) newSet.remove(level) else newSet.add(level)
            it.copy(selectedEidolons = newSet, usingSavedBuild = false)
        }
        viewModelScope.launch { recompute() }
    }

    fun useSavedBuild(use: Boolean) {
        val saved = _state.value.savedBuild
        if (use && saved != null) {
            val cone = _state.value.lightCones.firstOrNull { it.id == saved.lightConeId }
            _state.update {
                it.copy(
                    usingSavedBuild = true,
                    selectedCone = cone ?: it.selectedCone,
                    selectedEidolons = saved.eidolons
                )
            }
        } else {
            _state.update { it.copy(usingSavedBuild = false) }
        }
        viewModelScope.launch { recompute() }
    }

    fun rescore() {
        viewModelScope.launch { recompute() }
    }

    private suspend fun recompute() {
        val s = _state.value
        val char = s.character ?: return
        val skillTree = s.skillTree
        val cones = s.lightCones.associateBy { it.id }
        val relics = s.relicSets.associateBy { it.id }
        val gear = GearLookup.Maps(cones, relics)

        val build: PlayerBuild = if (s.usingSavedBuild && s.savedBuild != null) {
            s.savedBuild
        } else {
            val coneId = s.selectedCone?.id.orEmpty()
            BuildEffectResolver.defaultBuild(char, lightConeId = coneId).copy(
                eidolons = s.selectedEidolons,
                relicSet4 = s.relicSets.firstOrNull { char.role in it.suitableFor }?.id
                    ?: s.relicSets.firstOrNull()?.id.orEmpty()
            )
        }

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
            skillTree = skillTree,
            gearLookup = gear
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
