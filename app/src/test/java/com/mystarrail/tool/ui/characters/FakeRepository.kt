package com.mystarrail.tool.ui.characters

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Eidolon
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Scenario
import com.mystarrail.tool.data.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * 内存实现 [CharacterRepository]。所有方法返回内存里的 [MutableStateFlow]，VM 测试不需要 Room/网络。
 */
class FakeRepository(
    chars: List<Character> = emptyList(),
    cones: List<LightCone> = emptyList(),
    relics: List<RelicSet> = emptyList(),
    enemies: List<Enemy> = emptyList(),
    scenarios: List<Scenario> = emptyList()
) : CharacterRepository {
    private val charFlow = MutableStateFlow(chars)
    private val coneFlow = MutableStateFlow(cones)
    private val relicFlow = MutableStateFlow(relics)
    private val enemyFlow = MutableStateFlow(enemies)
    private val scenarioFlow = MutableStateFlow(scenarios)
    private val buildFlow = MutableStateFlow<List<PlayerBuild>>(emptyList())

    override fun observeAllCharacters(): Flow<List<Character>> = charFlow.asStateFlow()
    override suspend fun getCharacter(id: String): Character? =
        charFlow.value.firstOrNull { it.id == id }

    override fun observeAllLightCones(): Flow<List<LightCone>> = coneFlow.asStateFlow()
    override suspend fun getLightCone(id: String): LightCone? =
        coneFlow.value.firstOrNull { it.id == id }

    override fun observeAllRelicSets(): Flow<List<RelicSet>> = relicFlow.asStateFlow()
    override fun observeAllEnemies(): Flow<List<Enemy>> = enemyFlow.asStateFlow()
    override fun observeAllScenarios(): Flow<List<Scenario>> = scenarioFlow.asStateFlow()
    override suspend fun getEidolonsFor(characterId: String): List<Eidolon> = emptyList()

    // --- M10 玩家面板 ---
    override fun observeAllPlayerBuilds(): Flow<List<PlayerBuild>> = buildFlow.asStateFlow()
    override fun observePlayerBuild(characterId: String): Flow<List<PlayerBuild>> =
        buildFlow.map { list -> list.filter { it.characterId == characterId } }

    override suspend fun upsertPlayerBuild(build: PlayerBuild) {
        val current = buildFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == build.id }
        if (idx >= 0) {
            current[idx] = build
        } else {
            // 新建：自动分配 id
            val newId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
            current.add(build.copy(id = newId))
        }
        buildFlow.value = current
    }

    override suspend fun deletePlayerBuild(id: Long) {
        buildFlow.value = buildFlow.value.filter { it.id != id }
    }
}