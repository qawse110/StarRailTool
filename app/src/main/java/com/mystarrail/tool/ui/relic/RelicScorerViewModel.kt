package com.mystarrail.tool.ui.relic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.MainStats
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.SubStat
import com.mystarrail.tool.data.repository.CharacterRepository
import com.mystarrail.tool.engine.relic.RelicOptimizer
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.tables.FormulaTables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainStatRecommendation(
    val body: StatType,
    val boots: StatType,
    val sphere: StatType,
    val rope: StatType
) {
    val asModel: MainStats get() = MainStats(body, boots, sphere, rope)

    companion object {
        fun from(main: MainStats) = MainStatRecommendation(
            body = main.body,
            boots = main.boots,
            sphere = main.sphere,
            rope = main.rope
        )
    }
}

data class RelicScorerUiState(
    val allChars: List<Character> = emptyList(),
    val allRelicSets: List<RelicSet> = emptyList(),
    val selectedChar: Character? = null,
    val mainStatRec: MainStatRecommendation? = null,
    val subStatWeights: Map<StatType, Double> = emptyMap(),
    val inputSubStats: List<SubStat> = emptyList(),
    val subStatScore: Double = 0.0,
    val bestSet: RelicSet? = null,
    val recommendations: List<RelicOptimizer.Recommendation> = emptyList(),
    val isOptimizing: Boolean = false,
    val applyMessage: String? = null,
    val isLoading: Boolean = true
)

class RelicScorerViewModel(
    private val repository: CharacterRepository,
    private val relicOptimizer: RelicOptimizer
) : ViewModel() {

    private val _state = MutableStateFlow(RelicScorerUiState())
    val state: StateFlow<RelicScorerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val chars = repository.observeAllCharacters().first()
            val sets = repository.observeAllRelicSets().first()
            _state.value = _state.value.copy(
                allChars = chars,
                allRelicSets = sets,
                selectedChar = chars.firstOrNull(),
                isLoading = false
            )
            chars.firstOrNull()?.let { selectChar(it) }
        }
    }

    fun selectChar(c: Character) {
        val mainStat = relicOptimizer.recommendMainStats(c.role)
        val weights = relicOptimizer.subStatWeights(c.role)
        val best = _state.value.allRelicSets.firstOrNull { c.role in it.suitableFor }
            ?: _state.value.allRelicSets.firstOrNull()
        _state.value = _state.value.copy(
            selectedChar = c,
            mainStatRec = MainStatRecommendation.from(mainStat),
            subStatWeights = weights,
            bestSet = best,
            subStatScore = relicOptimizer.scoreSubStats(_state.value.inputSubStats, c.role),
            recommendations = emptyList(),
            applyMessage = null
        )
    }

    fun updateSubStats(subs: List<SubStat>) {
        val c = _state.value.selectedChar
        _state.value = _state.value.copy(
            inputSubStats = subs,
            subStatScore = if (c != null) relicOptimizer.scoreSubStats(subs, c.role) else 0.0
        )
    }

    fun optimize() {
        val c = _state.value.selectedChar ?: return
        val sets = _state.value.allRelicSets
        if (sets.isEmpty()) return
        _state.value = _state.value.copy(isOptimizing = true, applyMessage = null)
        viewModelScope.launch {
            val enemy = Enemy(
                id = "relic_opt",
                name = "Relic Dummy",
                count = 1,
                weaknesses = setOf(c.element),
                type = EnemyType.BOSS,
                hp = 200_000.0,
                toughness = 240.0
            )
            val cone = repository.observeAllLightCones().first()
                .firstOrNull { it.path == c.path }
            val recs = withContext(Dispatchers.Default) {
                relicOptimizer.optimize(
                    RelicOptimizer.Request(
                        character = c,
                        relicSets = sets,
                        enemy = enemy,
                        lightCone = cone,
                        topN = 5
                    )
                )
            }
            val top = recs.firstOrNull()
            _state.value = _state.value.copy(
                isOptimizing = false,
                recommendations = recs,
                bestSet = top?.let { r -> sets.firstOrNull { it.id == r.relicBuild.set4 } }
                    ?: _state.value.bestSet,
                mainStatRec = top?.let { MainStatRecommendation.from(it.relicBuild.mainStats) }
                    ?: _state.value.mainStatRec
            )
        }
    }

    fun applyRecommendation(rec: RelicOptimizer.Recommendation) {
        val c = _state.value.selectedChar ?: return
        viewModelScope.launch {
            val cone = repository.observeAllLightCones().first()
                .firstOrNull { it.path == c.path }
            val build = PlayerBuild(
                characterId = c.id,
                lightConeId = cone?.id.orEmpty(),
                relicSet4 = rec.relicBuild.set4,
                relicSet2 = rec.relicBuild.set2,
                mainStats = rec.relicBuild.mainStats,
                subStats = emptyList(),
                notes = "自动遗器: ${rec.notes}"
            )
            repository.upsertPlayerBuild(build)
            _state.value = _state.value.copy(
                applyMessage = "已写入 ${c.name} 的玩家面板",
                bestSet = _state.value.allRelicSets.firstOrNull { it.id == rec.relicBuild.set4 },
                mainStatRec = MainStatRecommendation.from(rec.relicBuild.mainStats)
            )
        }
    }

    companion object {
        fun factory(repo: CharacterRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val optimizer = RelicOptimizer(DamageCalculator(FormulaTables()))
                return RelicScorerViewModel(repo, optimizer) as T
            }
        }

        fun factory(repo: CharacterRepository, optimizer: RelicOptimizer) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RelicScorerViewModel(repo, optimizer) as T
            }
    }
}
