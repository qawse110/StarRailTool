package com.java.myapplication.data.repository

import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Eidolon
import com.java.myapplication.data.model.Enemy
import com.java.myapplication.data.model.LightCone
import com.java.myapplication.data.model.RelicSet
import com.java.myapplication.data.model.Scenario
import kotlinx.coroutines.flow.Flow

/**
 * 角色/光锥/遗器/敌人/场景的仓库接口。
 *
 * 实现：
 *  - [RoomCharacterRepository] — Room 持久化（生产）
 *  - FakeRepository — 内存实现（ViewModel 测试）
 */
interface CharacterRepository {
    fun observeAllCharacters(): Flow<List<Character>>
    suspend fun getCharacter(id: String): Character?

    fun observeAllLightCones(): Flow<List<LightCone>>
    suspend fun getLightCone(id: String): LightCone?

    fun observeAllRelicSets(): Flow<List<RelicSet>>

    fun observeAllEnemies(): Flow<List<Enemy>>

    fun observeAllScenarios(): Flow<List<Scenario>>

    suspend fun getEidolonsFor(characterId: String): List<Eidolon>
}