package com.mystarrail.tool.ui.build

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.MainStats
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.repository.CharacterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BuildUiState(
    val builds: List<PlayerBuild> = emptyList(),
    val charMap: Map<String, Character> = emptyMap()
)

class BuildViewModel(
    private val repository: CharacterRepository
) : ViewModel() {

    val uiState: StateFlow<BuildUiState> = combine(
        repository.observeAllPlayerBuilds(),
        repository.observeAllCharacters()
    ) { builds, chars ->
        BuildUiState(
            builds = builds,
            charMap = chars.associateBy { it.id }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        BuildUiState()
    )

    /** 新建空 Build 模板（id=0 让 Room 自动生成）。 */
    fun newTemplate(characterId: String): PlayerBuild = PlayerBuild(
        id = 0,
        characterId = characterId,
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

    fun addBuild(characterId: String) {
        viewModelScope.launch {
            repository.upsertPlayerBuild(newTemplate(characterId))
        }
    }

    fun upsert(build: PlayerBuild) {
        viewModelScope.launch { repository.upsertPlayerBuild(build) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deletePlayerBuild(id) }
    }

    companion object {
        fun factory(repo: CharacterRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BuildViewModel(repo) as T
        }
    }
}