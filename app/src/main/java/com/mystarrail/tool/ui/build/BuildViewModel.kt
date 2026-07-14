package com.mystarrail.tool.ui.build

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
import com.mystarrail.tool.engine.simulator.GearLookup
import com.mystarrail.tool.engine.simulator.ScoringEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BuildUiState(
    val builds: List<PlayerBuild> = emptyList(),
    val charMap: Map<String, Character> = emptyMap(),
    val lastScore: CharacterScore? = null,
    val lastScoredBuildId: Long? = null,
    val scoreMessage: String? = null
)

class BuildViewModel(
    private val repository: CharacterRepository,
    private val scoringEngine: ScoringEngine? = null
) : ViewModel() {

    private val scoreState = MutableStateFlow<ScoreBanner>(ScoreBanner())

    private data class ScoreBanner(
        val lastScore: CharacterScore? = null,
        val lastScoredBuildId: Long? = null,
        val scoreMessage: String? = null
    )

    val uiState: StateFlow<BuildUiState> = combine(
        repository.observeAllPlayerBuilds(),
        repository.observeAllCharacters(),
        scoreState
    ) { builds, chars, banner ->
        BuildUiState(
            builds = builds,
            charMap = chars.associateBy { it.id },
            lastScore = banner.lastScore,
            lastScoredBuildId = banner.lastScoredBuildId,
            scoreMessage = banner.scoreMessage
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

    /** 保存配装后一键用 ScoringEngine 重算该角色分数。 */
    fun rescore(build: PlayerBuild) {
        val engine = scoringEngine ?: return
        viewModelScope.launch {
            val char = repository.getCharacter(build.characterId)
            if (char == null) {
                scoreState.value = ScoreBanner(scoreMessage = "找不到角色")
                return@launch
            }
            val all = repository.observeAllCharacters().first()
            val cones = repository.observeAllLightCones().first().associateBy { it.id }
            val relics = repository.observeAllRelicSets().first().associateBy { it.id }
            val enemy = Enemy(
                id = "build_score",
                name = "Build Dummy",
                count = 1,
                weaknesses = setOf(char.element),
                type = EnemyType.BOSS,
                hp = 200_000.0,
                toughness = 240.0
            )
            val score = engine.scoreCharacter(
                character = char,
                config = ScoringConfig(playerBuild = build, enemy = enemy),
                allCharacters = all,
                defaultEnemy = enemy,
                gearLookup = GearLookup.Maps(cones, relics)
            )
            scoreState.value = ScoreBanner(
                lastScore = score,
                lastScoredBuildId = build.id,
                scoreMessage = "${char.name} 总分 ${"%.1f".format(score.total)}（${score.tier}）"
            )
        }
    }

    companion object {
        fun factory(repo: CharacterRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BuildViewModel(repo) as T
        }

        fun factory(repo: CharacterRepository, scoringEngine: ScoringEngine) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    BuildViewModel(repo, scoringEngine) as T
            }
    }
}
