package com.mystarrail.tool.data.repository

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Eidolon
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Scenario
import com.mystarrail.tool.data.model.SkillTree
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

    suspend fun getSkillTreeFor(characterId: String): SkillTree?

    // --- M10 玩家面板 ---
    fun observeAllPlayerBuilds(): Flow<List<PlayerBuild>>
    fun observePlayerBuild(characterId: String): Flow<List<PlayerBuild>>
    suspend fun upsertPlayerBuild(build: PlayerBuild)
    suspend fun deletePlayerBuild(id: Long)
}