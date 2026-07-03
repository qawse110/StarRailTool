package com.mystarrail.tool.ui.relic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.MainStats
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.SubStat
import com.mystarrail.tool.data.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MainStatRecommendation(
    val body: StatType,
    val boots: StatType,
    val sphere: StatType,
    val rope: StatType
) {
    val asModel: MainStats get() = MainStats(body, boots, sphere, rope)
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
    val isLoading: Boolean = true
)

class RelicScorerViewModel(
    private val repository: CharacterRepository
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
        val mainStat = recommendMainStats(c.role)
        val weights = subStatWeights(c.role)
        val best = pickBestSet(c, _state.value.allRelicSets)
        _state.value = _state.value.copy(
            selectedChar = c,
            mainStatRec = mainStat,
            subStatWeights = weights,
            bestSet = best,
            subStatScore = scoreSubStats(_state.value.inputSubStats, weights)
        )
    }

    fun updateSubStats(subs: List<SubStat>) {
        val weights = _state.value.subStatWeights
        _state.value = _state.value.copy(
            inputSubStats = subs,
            subStatScore = scoreSubStats(subs, weights)
        )
    }

    /** 主词条推荐（按角色定位）。 */
    private fun recommendMainStats(role: Role): MainStatRecommendation = when (role) {
        Role.DPS, Role.SUB_DPS -> MainStatRecommendation(
            body = StatType.CRIT_DMG,
            boots = StatType.SPD,
            sphere = StatType.ATK,
            rope = StatType.ATK
        )
        Role.SUPPORT, Role.HEALER, Role.SHIELD -> MainStatRecommendation(
            body = StatType.HP,
            boots = StatType.SPD,
            sphere = StatType.HP,
            rope = StatType.HP
        )
    }

    /** 副词条权重（按角色定位）。DPS 偏好暴击/暴伤/速度/攻击。 */
    private fun subStatWeights(role: Role): Map<StatType, Double> = when (role) {
        Role.DPS, Role.SUB_DPS -> mapOf(
            StatType.CRIT_DMG to 1.0,
            StatType.CRIT_RATE to 1.0,
            StatType.SPD to 0.8,
            StatType.ATK to 0.5,
            StatType.HP to 0.1,
            StatType.DEF to 0.1,
            StatType.EHR to 0.3,
            StatType.BRK_EFF to 0.4
        )
        Role.SUPPORT -> mapOf(
            StatType.SPD to 1.0,
            StatType.HP to 0.7,
            StatType.EHR to 0.6,
            StatType.BRK_EFF to 0.5,
            StatType.CRIT_RATE to 0.2,
            StatType.CRIT_DMG to 0.2
        )
        Role.HEALER, Role.SHIELD -> mapOf(
            StatType.SPD to 1.0,
            StatType.HP to 1.0,
            StatType.DEF to 0.7,
            StatType.EHR to 0.3
        )
    }

    /** 副词条评分：每条 sub 值 × weight 求和 / max → 0..100。 */
    private fun scoreSubStats(
        subs: List<SubStat>,
        weights: Map<StatType, Double>
    ): Double {
        if (subs.isEmpty() || weights.isEmpty()) return 0.0
        val maxWeight = weights.values.max()
        val raw = subs.sumOf { (weights[it.type] ?: 0.0) * it.value }
        val max = (subs.size * maxWeight * 10.0).coerceAtLeast(1.0)
        return (raw / max * 100.0).coerceIn(0.0, 100.0)
    }

    private fun pickBestSet(c: Character, sets: List<RelicSet>): RelicSet? {
        return sets.firstOrNull { c.role in it.suitableFor }
            ?: sets.firstOrNull()
    }

    companion object {
        fun factory(repo: CharacterRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RelicScorerViewModel(repo) as T
        }
    }
}