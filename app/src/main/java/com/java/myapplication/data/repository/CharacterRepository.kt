package com.java.myapplication.data.repository

import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.data.local.EidolonEffectJson
import com.java.myapplication.data.local.PassiveEffectJson
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Eidolon
import com.java.myapplication.data.model.Enemy
import com.java.myapplication.data.model.LightCone
import com.java.myapplication.data.model.RelicSet
import com.java.myapplication.data.model.Scenario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CharacterRepository(private val db: AppDatabase) {

    fun observeAllCharacters(): Flow<List<Character>> =
        db.characterDao().observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getCharacter(id: String): Character? =
        db.characterDao().getById(id)?.toModel()

    fun observeAllLightCones(): Flow<List<LightCone>> =
        db.lightConeDao().observeAll().map { list ->
            list.map { entity ->
                entity.toModel(PassiveEffectJson.decode(entity.passiveEffectJson))
            }
        }

    suspend fun getLightCone(id: String): LightCone? =
        db.lightConeDao().getById(id)?.let { entity ->
            entity.toModel(PassiveEffectJson.decode(entity.passiveEffectJson))
        }

    fun observeAllRelicSets(): Flow<List<RelicSet>> =
        db.relicSetDao().observeAll().map { list ->
            list.map { entity ->
                entity.toModel(
                    PassiveEffectJson.decode(entity.twoPieceJson),
                    PassiveEffectJson.decode(entity.fourPieceJson)
                )
            }
        }

    fun observeAllEnemies(): Flow<List<Enemy>> =
        db.enemyDao().observeAll().map { list -> list.map { it.toModel() } }

    fun observeAllScenarios(): Flow<List<Scenario>> =
        db.scenarioDao().observeAll().map { list ->
            list.map { entity ->
                val enemies = entity.enemyIds.mapNotNull { eid ->
                    db.enemyDao().getById(eid)?.toModel()
                }
                entity.toModel(enemies)
            }
        }

    suspend fun getEidolonsFor(characterId: String): List<Eidolon> =
        db.eidolonDao().getForCharacter(characterId).map { entity ->
            entity.toModel(EidolonEffectJson.decode(entity.effectJson))
        }
}