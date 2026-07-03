package com.java.myapplication.ui.characters

import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Eidolon
import com.java.myapplication.data.model.Enemy
import com.java.myapplication.data.model.LightCone
import com.java.myapplication.data.model.RelicSet
import com.java.myapplication.data.model.Scenario
import com.java.myapplication.data.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 内存实现 [CharacterRepository]。所有方法返回内存里的 [MutableStateFlow]，VM 测试不需要 Room/网络。
 */
class FakeRepository(
    chars: List<Character> = emptyList(),
    cones: List<LightCone> = emptyList()
) : CharacterRepository {
    private val charFlow = MutableStateFlow(chars)
    private val coneFlow = MutableStateFlow(cones)
    private val relicFlow = MutableStateFlow<List<RelicSet>>(emptyList())
    private val enemyFlow = MutableStateFlow<List<Enemy>>(emptyList())
    private val scenarioFlow = MutableStateFlow<List<Scenario>>(emptyList())

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
}