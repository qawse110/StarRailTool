package com.mystarrail.tool.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.mystarrail.tool.data.local.AppDatabase
import com.mystarrail.tool.data.local.CharacterEntity
import com.mystarrail.tool.data.local.EidolonEntity
import com.mystarrail.tool.data.local.EnemyEntity
import com.mystarrail.tool.data.local.LightConeEntity
import com.mystarrail.tool.data.local.RelicSetEntity
import com.mystarrail.tool.data.local.ScenarioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class SeedImporter(
    private val context: Context,
    private val db: AppDatabase
) {
    suspend fun importFromAssets(assetPath: String = "seed-data-v1.json"): ImportResult =
        withContext(Dispatchers.IO) {
            val rawJson = readAsset(assetPath) ?: run {
                Log.e(TAG, "Cannot read asset: $assetPath")
                return@withContext ImportResult.Failed("Cannot read $assetPath")
            }

            when (val parsed = SeedParser.parse(rawJson)) {
                is SeedParser.ParseResult.Failed -> {
                    Log.e(TAG, "Seed parse failed: ${parsed.reason}", parsed.cause)
                    ImportResult.Failed(parsed.reason)
                }
                is SeedParser.ParseResult.Success -> try {
                    db.withTransaction {
                        db.characterDao().insertAll(parsed.characters.map(CharacterEntity::fromModel))
                        db.lightConeDao().insertAll(parsed.lightCones.map(LightConeEntity::fromModel))
                        db.relicSetDao().insertAll(parsed.relicSets.map(RelicSetEntity::fromModel))
                        db.enemyDao().insertAll(parsed.enemies.map(EnemyEntity::fromModel))
                        db.scenarioDao().insertAll(parsed.scenarios.map(ScenarioEntity::fromModel))
                        db.eidolonDao().insertAll(parsed.eidolons.map(EidolonEntity::fromModel))
                    }
                    Log.i(TAG, "Seed imported: ${parsed.characters.size} chars / ${parsed.lightCones.size} cones / ${parsed.relicSets.size} relics / ${parsed.enemies.size} enemies / ${parsed.scenarios.size} scenarios / ${parsed.eidolons.size} eidolons")
                    ImportResult.Success(
                        characters = parsed.characters.size,
                        lightCones = parsed.lightCones.size,
                        relicSets = parsed.relicSets.size,
                        enemies = parsed.enemies.size,
                        scenarios = parsed.scenarios.size,
                        eidolons = parsed.eidolons.size
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "DB write failed", e)
                    ImportResult.Failed("DB error: ${e.message}")
                }
            }
        }

    sealed interface ImportResult {
        data class Success(
            val characters: Int, val lightCones: Int, val relicSets: Int,
            val enemies: Int, val scenarios: Int, val eidolons: Int
        ) : ImportResult
        data class Failed(val reason: String) : ImportResult
    }

    private fun readAsset(path: String): String? = try {
        context.assets.open(path).use { it.readBytes().decodeToString() }
    } catch (e: IOException) {
        Log.e(TAG, "Asset open failed: $path", e)
        null
    } catch (e: Exception) {
        Log.e(TAG, "Asset read failed: $path", e)
        null
    }

    companion object {
        private const val TAG = "SeedImporter"
    }
}