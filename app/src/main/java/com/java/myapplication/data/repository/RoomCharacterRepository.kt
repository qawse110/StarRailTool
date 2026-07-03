package com.java.myapplication.data.repository

import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.data.local.EidolonEffectJson
import com.java.myapplication.data.local.PassiveEffectJson
import com.java.myapplication.data.local.PlayerBuildEntity
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Eidolon
import com.java.myapplication.data.model.Enemy
import com.java.myapplication.data.model.LightCone
import com.java.myapplication.data.model.PlayerBuild
import com.java.myapplication.data.model.RelicSet
import com.java.myapplication.data.model.Scenario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed 实现，从 [AppDatabase] 读取并把 Entity 解码为 domain model。 */
class RoomCharacterRepository(private val db: AppDatabase) : CharacterRepository {

    override fun observeAllCharacters(): Flow<List<Character>> =
        db.characterDao().observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun getCharacter(id: String): Character? =
        db.characterDao().getById(id)?.toModel()

    override fun observeAllLightCones(): Flow<List<LightCone>> =
        db.lightConeDao().observeAll().map { list ->
            list.map { entity ->
                entity.toModel(PassiveEffectJson.decode(entity.passiveEffectJson))
            }
        }

    override suspend fun getLightCone(id: String): LightCone? =
        db.lightConeDao().getById(id)?.let { entity ->
            entity.toModel(PassiveEffectJson.decode(entity.passiveEffectJson))
        }

    override fun observeAllRelicSets(): Flow<List<RelicSet>> =
        db.relicSetDao().observeAll().map { list ->
            list.map { entity ->
                entity.toModel(
                    PassiveEffectJson.decode(entity.twoPieceJson),
                    PassiveEffectJson.decode(entity.fourPieceJson)
                )
            }
        }

    override fun observeAllEnemies(): Flow<List<Enemy>> =
        db.enemyDao().observeAll().map { list -> list.map { it.toModel() } }

    override fun observeAllScenarios(): Flow<List<Scenario>> =
        db.scenarioDao().observeAll().map { list ->
            list.map { entity ->
                val enemies = entity.enemyIds.mapNotNull { eid ->
                    db.enemyDao().getById(eid)?.toModel()
                }
                entity.toModel(enemies)
            }
        }

    override suspend fun getEidolonsFor(characterId: String): List<Eidolon> =
        db.eidolonDao().getForCharacter(characterId).map { entity ->
            entity.toModel(EidolonEffectJson.decode(entity.effectJson))
        }

    // --- M10 玩家面板 ---
    override fun observeAllPlayerBuilds() =
        db.playerBuildDao().observeAll().map { list -> list.map { it.toModel() } }

    override fun observePlayerBuild(characterId: String) =
        db.playerBuildDao().observeForCharacter(characterId).map { list ->
            list.map { it.toModel() }
        }

    override suspend fun upsertPlayerBuild(build: PlayerBuild) {
        db.playerBuildDao().insert(PlayerBuildEntity.fromModel(build))
    }

    override suspend fun deletePlayerBuild(id: Long) {
        val existing = db.playerBuildDao().getById(id) ?: return
        db.playerBuildDao().delete(existing)
    }
}