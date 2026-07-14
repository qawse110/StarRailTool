package com.mystarrail.tool.ui.teambuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.Scenario
import com.mystarrail.tool.data.model.TeamScore
import com.mystarrail.tool.data.repository.CharacterRepository
import com.mystarrail.tool.engine.simulator.GearLookup
import com.mystarrail.tool.engine.simulator.ScoringEngine
import com.mystarrail.tool.engine.simulator.sim.SimulationResult
import com.mystarrail.tool.engine.simulator.sim.toCombatant
import com.mystarrail.tool.engine.team.TeamOptimizer
import com.mystarrail.tool.util.SimulationResultStore
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
    val lockedIds: Set<String> = emptySet(),
    val scenarios: List<Scenario> = emptyList(),
    val selectedScenarioId: String? = null,
    val isSimulating: Boolean = false,
    val isOptimizing: Boolean = false,
    val lastResult: SimulationResult? = null,
    val lastTeamScore: TeamScore? = null,
    val lastTotalDmg: Double = 0.0,
    val recommendations: List<TeamOptimizer.Recommendation> = emptyList(),
    val optimizeError: String? = null
) {
    val canSimulate: Boolean get() = selectedIds.size == 4 && !isSimulating
    val canOptimize: Boolean get() = allChars.size >= 4 && !isOptimizing
}

class TeamBuilderViewModel(
    private val repository: CharacterRepository,
    private val scoringEngine: ScoringEngine,
    private val teamOptimizer: TeamOptimizer,
    private val resultStore: SimulationResultStore
) : ViewModel() {

    /** 测试兼容：无显式 TeamOptimizer 时自动创建。 */
    constructor(
        repository: CharacterRepository,
        scoringEngine: ScoringEngine,
        resultStore: SimulationResultStore
    ) : this(repository, scoringEngine, TeamOptimizer(scoringEngine), resultStore)

    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val lockedIds = MutableStateFlow<Set<String>>(emptySet())
    private val selectedScenarioId = MutableStateFlow<String?>(null)
    private val internalState = MutableStateFlow(InternalState())

    private data class InternalState(
        val isSimulating: Boolean = false,
        val isOptimizing: Boolean = false,
        val lastResult: SimulationResult? = null,
        val lastTeamScore: TeamScore? = null,
        val lastTotalDmg: Double = 0.0,
        val recommendations: List<TeamOptimizer.Recommendation> = emptyList(),
        val optimizeError: String? = null,
        val scenarios: List<Scenario> = emptyList()
    )

    val uiState: StateFlow<TeamUiState> = combine(
        repository.observeAllCharacters(),
        selectedIds,
        lockedIds,
        selectedScenarioId,
        internalState
    ) { chars, ids, locked, scenarioId, internal ->
        TeamUiState(
            allChars = chars,
            selectedIds = ids,
            lockedIds = locked,
            scenarios = internal.scenarios,
            selectedScenarioId = scenarioId,
            isSimulating = internal.isSimulating,
            isOptimizing = internal.isOptimizing,
            lastResult = internal.lastResult,
            lastTeamScore = internal.lastTeamScore,
            lastTotalDmg = internal.lastTotalDmg,
            recommendations = internal.recommendations,
            optimizeError = internal.optimizeError
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TeamUiState()
    )

    init {
        viewModelScope.launch {
            val scenarios = repository.observeAllScenarios().first()
            internalState.update { it.copy(scenarios = scenarios) }
            if (selectedScenarioId.value == null) {
                selectedScenarioId.value = scenarios.firstOrNull()?.id
            }
        }
    }

    fun toggleChar(id: String) {
        selectedIds.update { current ->
            if (id in current) current - id
            else if (current.size < 4) current + id
            else current
        }
        lockedIds.update { locks -> locks.intersect(selectedIds.value) }
    }

    fun toggleLock(id: String) {
        if (id !in selectedIds.value) return
        lockedIds.update { if (id in it) it - id else it + id }
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
        lockedIds.value = emptySet()
    }

    fun selectScenario(id: String?) {
        selectedScenarioId.value = id
    }

    fun applyRecommendation(rec: TeamOptimizer.Recommendation) {
        selectedIds.value = rec.team.map { it.id }.toSet()
        lockedIds.update { it.intersect(selectedIds.value) }
    }

    fun simulate() {
        val ids = selectedIds.value
        if (ids.size != 4) return
        internalState.update { it.copy(isSimulating = true) }
        viewModelScope.launch {
            val (result, teamScore) = withContext(Dispatchers.Default) {
                runSimulationAndScore(ids.toList())
            }
            resultStore.lastSimulationResult = result
            internalState.update {
                it.copy(
                    isSimulating = false,
                    lastResult = result,
                    lastTeamScore = teamScore,
                    lastTotalDmg = result.damageBreakdown.total
                )
            }
        }
    }

    fun optimizeTeam() {
        internalState.update { it.copy(isOptimizing = true, optimizeError = null) }
        viewModelScope.launch {
            try {
                val chars = repository.observeAllCharacters().first()
                val builds = repository.observeAllPlayerBuilds().first()
                    .groupBy { it.characterId }
                    .mapValues { (_, list) -> list.maxByOrNull { it.id }!! }
                val cones = repository.observeAllLightCones().first().associateBy { it.id }
                val relics = repository.observeAllRelicSets().first().associateBy { it.id }
                val gear = GearLookup.Maps(cones, relics)
                val enemy = resolveEnemy(chars.filter { it.id in selectedIds.value })

                val recs = withContext(Dispatchers.Default) {
                    teamOptimizer.optimize(
                        TeamOptimizer.Request(
                            pool = chars,
                            enemy = enemy,
                            lockedIds = lockedIds.value,
                            builds = builds,
                            gearLookup = gear,
                            topK = 5,
                            simulateLimit = 20
                        )
                    )
                }
                internalState.update {
                    it.copy(
                        isOptimizing = false,
                        recommendations = recs,
                        optimizeError = if (recs.isEmpty()) "角色池不足或约束过严" else null
                    )
                }
                recs.firstOrNull()?.let { applyRecommendation(it) }
            } catch (e: Exception) {
                internalState.update {
                    it.copy(
                        isOptimizing = false,
                        optimizeError = e.message ?: "优化失败"
                    )
                }
            }
        }
    }

    private suspend fun runSimulationAndScore(charIds: List<String>): Pair<SimulationResult, TeamScore> {
        val allChars = repository.observeAllCharacters().first()
        val selected = charIds.mapNotNull { id -> allChars.firstOrNull { it.id == id } }
        val builds = loadBuilds()
        val gear = loadGear()
        val team = selected.map { c ->
            val build = builds[c.id]
            c.toCombatant(
                lightCone = build?.lightConeId?.let { gear.lightCone(it) },
                relicSet = build?.relicSet4?.let { gear.relicSet(it) },
                build = build,
                relicSet2 = build?.relicSet2?.let { gear.relicSet(it) }
            )
        }
        val enemy = resolveEnemy(selected)
        val sim = com.mystarrail.tool.engine.simulator.sim.DiscreteEventSimulator(
            com.mystarrail.tool.engine.simulator.damage.DamageCalculator(
                com.mystarrail.tool.engine.simulator.tables.FormulaTables()
            )
        )
        val result = sim.simulate(team, listOf(enemy.toCombatant()))
        val teamScore = scoringEngine.teamScoreFrom(result, selected)
        return result to teamScore
    }

    private suspend fun loadBuilds(): Map<String, PlayerBuild> =
        repository.observeAllPlayerBuilds().first()
            .groupBy { it.characterId }
            .mapValues { (_, list) -> list.maxByOrNull { it.id }!! }

    private suspend fun loadGear(): GearLookup {
        val cones = repository.observeAllLightCones().first().associateBy { it.id }
        val relics = repository.observeAllRelicSets().first().associateBy { it.id }
        return GearLookup.Maps(cones, relics)
    }

    private suspend fun resolveEnemy(selected: List<Character>): Enemy {
        val scenarioId = selectedScenarioId.value
        val scenarios = internalState.value.scenarios.ifEmpty {
            repository.observeAllScenarios().first()
        }
        val scenario = scenarios.firstOrNull { it.id == scenarioId }
        if (scenario != null && scenario.enemies.isNotEmpty()) {
            return scenario.enemies.first()
        }
        val enemyElements = selected.map { it.element }.toSet()
            .ifEmpty { setOf(com.mystarrail.tool.data.model.Element.QUANTUM) }
        return Enemy(
            id = "team_enemy",
            name = "Team Dummy",
            count = 1,
            weaknesses = enemyElements,
            type = EnemyType.BOSS,
            hp = 200_000.0,
            toughness = 240.0
        )
    }

    companion object {
        fun factory(
            repository: CharacterRepository,
            scoringEngine: ScoringEngine,
            teamOptimizer: TeamOptimizer,
            resultStore: SimulationResultStore
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TeamBuilderViewModel(repository, scoringEngine, teamOptimizer, resultStore) as T
        }

        fun factory(
            repository: CharacterRepository,
            scoringEngine: ScoringEngine,
            resultStore: SimulationResultStore
        ) = factory(repository, scoringEngine, TeamOptimizer(scoringEngine), resultStore)
    }
}
