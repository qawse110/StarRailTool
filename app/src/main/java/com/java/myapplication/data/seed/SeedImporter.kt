package com.java.myapplication.data.seed

import android.content.Context
import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.data.local.CharacterEntity
import com.java.myapplication.data.local.EidolonEntity
import com.java.myapplication.data.local.EnemyEntity
import com.java.myapplication.data.local.LightConeEntity
import com.java.myapplication.data.local.RelicSetEntity
import com.java.myapplication.data.local.ScenarioEntity
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.CycleProfile
import com.java.myapplication.data.model.DmgCondition
import com.java.myapplication.data.model.Eidolon
import com.java.myapplication.data.model.EidolonEffect
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.Enemy
import com.java.myapplication.data.model.EnemyType
import com.java.myapplication.data.model.LightCone
import com.java.myapplication.data.model.PassiveEffect
import com.java.myapplication.data.model.Path
import com.java.myapplication.data.model.RelicSet
import com.java.myapplication.data.model.Role
import com.java.myapplication.data.model.Scaling
import com.java.myapplication.data.model.Scenario
import com.java.myapplication.data.model.SkillType
import com.java.myapplication.data.model.Stats
import com.java.myapplication.data.model.Tag
import com.java.myapplication.data.model.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

class SeedImporter(
    private val context: Context,
    private val db: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importFromAssets(assetPath: String = "seed-data-v1.json"): ImportResult =
        withContext(Dispatchers.IO) {
            val root = try {
                context.assets.open(assetPath).use { input ->
                    json.decodeFromString(SeedRoot.serializer(), input.readBytes().decodeToString())
                }
            } catch (e: IOException) {
                return@withContext ImportResult.Failed("Cannot read $assetPath: ${e.message}")
            } catch (e: Exception) {
                return@withContext ImportResult.Failed("Parse error: ${e.message}")
            }

            try {
                db.runInTransaction {
                    val chars = root.characters.map { it.toModel() }
                    db.characterDao().insertAll(chars.map(CharacterEntity::fromModel))

                    val cones = root.lightCones.map { it.toModel() }
                    db.lightConeDao().insertAll(cones.map(LightConeEntity::fromModel))

                    val sets = root.relicSets.map { it.toModel() }
                    db.relicSetDao().insertAll(sets.map(RelicSetEntity::fromModel))

                    val enemies = root.enemies.map { it.toModel() }
                    db.enemyDao().insertAll(enemies.map(EnemyEntity::fromModel))

                    val scenarios = root.scenarios.map { it.toModel() }
                    db.scenarioDao().insertAll(scenarios.map(ScenarioEntity::fromModel))

                    val eidolons = root.eidolons.map { it.toModel() }
                    db.eidolonDao().insertAll(eidolons.map(EidolonEntity::fromModel))
                }
                ImportResult.Success(
                    characters = root.characters.size,
                    lightCones = root.lightCones.size,
                    relicSets = root.relicSets.size,
                    enemies = root.enemies.size,
                    scenarios = root.scenarios.size,
                    eidolons = root.eidolons.size
                )
            } catch (e: Exception) {
                ImportResult.Failed("DB error: ${e.message}")
            }
        }

    sealed interface ImportResult {
        data class Success(
            val characters: Int, val lightCones: Int, val relicSets: Int,
            val enemies: Int, val scenarios: Int, val eidolons: Int
        ) : ImportResult
        data class Failed(val reason: String) : ImportResult
    }

    // ===== Mappers =====

    private fun SeedCharacter.toModel(): Character = Character(
        id = id, name = name, rarity = rarity,
        path = Path.valueOf(path), element = Element.valueOf(element), role = Role.valueOf(role),
        tags = tags.map(Tag::valueOf).toSet(),
        baseStats = Stats(baseStats.hp, baseStats.atk, baseStats.def, baseStats.spd),
        scaling = Scaling(scaling.skillMult, scaling.ultMult, scaling.talentMult,
            scaling.followUpMult, scaling.aoeRatio),
        cycleProfile = cycleProfile?.let { cp ->
            CycleProfile(cp.cycleActions, cp.spdBreakpoints, cp.isFollowUp, cp.isDot)
        },
        iconUrl = iconUrl, version = version
    )

    private fun SeedLightCone.toModel(): LightCone = LightCone(
        id = id, name = name, path = Path.valueOf(path), rarity = rarity,
        passiveName = passiveName,
        passiveEffect = passiveEffect.toModel(),
        s5Multiplier = s5Multiplier
    )

    private fun SeedPassiveEffect.toModel(): PassiveEffect = when (type) {
        "StatBoost" -> PassiveEffect.StatBoost(
            stat = StatType_valueOf(stat),
            value = value ?: 0.0,
            target = target?.let { Target.valueOf(it) } ?: Target.SELF
        )
        "DamageBonus" -> PassiveEffect.DamageBonus(
            multiplier = multiplier ?: 0.0,
            condition = DmgCondition.valueOf(condition ?: "ALWAYS")
        )
        "SkillBoost" -> PassiveEffect.SkillBoost(
            type = SkillType.valueOf(skillType ?: "SKILL"),
            multiplier = multiplier ?: 0.0
        )
        "EnergyRegen" -> PassiveEffect.EnergyRegen(perTurn = perTurn ?: 0.0)
        "Composite" -> PassiveEffect.Composite(effects.map { it.toModel() })
        else -> error("Unknown PassiveEffect type: $type")
    }

    private fun SeedRelicSet.toModel(): RelicSet = RelicSet(
        id = id, name = name,
        twoPiece = twoPiece.toModel(),
        fourPiece = fourPiece.toModel(),
        suitableFor = suitableFor.map(Role::valueOf).toSet()
    )

    private fun SeedEnemy.toModel(): Enemy = Enemy(
        id = id, name = name, count = count,
        weaknesses = weaknesses.map(Element::valueOf).toSet(),
        type = EnemyType.valueOf(type),
        hp = hp, toughness = toughness, mechanics = mechanics.toSet()
    )

    private fun SeedScenario.toModel(): Scenario = Scenario(
        id = id, name = name, enemyIds = enemyIds,
        difficulty = difficulty, notes = notes
    )

    private fun SeedEidolon.toModel(): Eidolon = Eidolon(
        id = id, characterId = characterId, level = level,
        name = name, effect = effect.toModel(), major = major
    )

    private fun SeedEidolonEffect.toModel(): EidolonEffect = when (type) {
        "StatBoost" -> EidolonEffect.StatBoost(
            stat = StatType_valueOf(stat),
            value = value ?: 0.0,
            target = target?.let { Target.valueOf(it) } ?: Target.SELF
        )
        "NewMechanic" -> EidolonEffect.NewMechanic(
            mechanic = Tag.valueOf(mechanic ?: "DOT"),
            param = param ?: 1.0,
            note = note ?: ""
        )
        "DamageBonus" -> EidolonEffect.DamageBonus(
            multiplier = multiplier ?: 0.0,
            condition = DmgCondition.valueOf(condition ?: "ALWAYS")
        )
        "EnemyDebuff" -> EidolonEffect.EnemyDebuff(
            stat = StatType_valueOf(stat),
            value = value ?: 0.0
        )
        "Composite" -> EidolonEffect.Composite(effects.map { it.toModel() })
        else -> error("Unknown EidolonEffect type: $type")
    }

    private fun StatType_valueOf(name: String?) =
        com.java.myapplication.data.model.StatType.valueOf(name ?: "ATK")
}