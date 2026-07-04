package com.mystarrail.tool.data.seed

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.CycleProfile
import com.mystarrail.tool.data.model.DmgCondition
import com.mystarrail.tool.data.model.Eidolon
import com.mystarrail.tool.data.model.EidolonEffect
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.PassiveEffect
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.Scenario
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.SkillTreeNode
import com.mystarrail.tool.data.model.SkillType
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.data.model.Target
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 纯 JVM 可测的 seed JSON 解析与映射。
 *
 * 与 [SeedImporter] 分离：导入器持有 Context+DB（Android-only），
 * 解析器只做文本→领域模型，让 unit test 能在 Proot/纯 JVM 环境验证逻辑。
 */
object SeedParser {

    private val json = Json { ignoreUnknownKeys = true }

    sealed interface ParseResult {
        data class Success(
            val characters: List<Character>,
            val lightCones: List<LightCone>,
            val relicSets: List<RelicSet>,
            val enemies: List<Enemy>,
            val scenarios: List<Scenario>,
            val eidolons: List<Eidolon>,
            val skillTrees: List<SkillTree> = emptyList()
        ) : ParseResult

        data class Failed(val reason: String, val cause: Throwable? = null) : ParseResult
    }

    fun parse(jsonText: String): ParseResult = try {
        val root = json.decodeFromString(SeedRoot.serializer(), jsonText)
        ParseResult.Success(
            characters = root.characters.map { it.toModel() },
            lightCones = root.lightCones.map { it.toModel() },
            relicSets = root.relicSets.map { it.toModel() },
            enemies = root.enemies.map { it.toModel() },
            scenarios = root.scenarios.map { it.toModel() },
            eidolons = root.eidolons.map { it.toModel() },
            skillTrees = root.skillTrees.map { it.toModel() }
        )
    } catch (e: SerializationException) {
        ParseResult.Failed("JSON parse error: ${e.message}", e)
    } catch (e: IllegalArgumentException) {
        // valueOf 抛此异常（未知枚举值）
        ParseResult.Failed("Enum mapping error: ${e.message}", e)
    } catch (e: Exception) {
        ParseResult.Failed("Unexpected: ${e.message}", e)
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
            stat = StatType.valueOf(stat ?: "ATK"),
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
        id = id, name = name, enemies = emptyList(),  // resolved at Repository layer
        difficulty = difficulty, notes = notes
    )

    private fun SeedEidolon.toModel(): Eidolon = Eidolon(
        id = id, characterId = characterId, level = level,
        name = name, effect = effect.toModel(), major = major
    )

    private fun SeedEidolonEffect.toModel(): EidolonEffect = when (type) {
        "StatBoost" -> EidolonEffect.StatBoost(
            stat = StatType.valueOf(stat ?: "ATK"),
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
            stat = StatType.valueOf(stat ?: "ATK"),
            value = value ?: 0.0
        )
        "SkillBoost" -> EidolonEffect.SkillBoost(
            type = SkillType.valueOf(skillType ?: "SKILL"),
            multiplier = multiplier ?: 0.0
        )
        "Composite" -> EidolonEffect.Composite(effects.map { it.toModel() })
        else -> error("Unknown EidolonEffect type: $type")
    }

    private fun SeedSkillTree.toModel(): SkillTree = SkillTree(
        characterId = characterId,
        nodes = nodes.map { it.toModel() }
    )

    private fun SeedSkillTreeNode.toModel(): SkillTreeNode = SkillTreeNode(
        id = id, name = name, desc = desc, maxLevel = maxLevel,
        skillType = skillType?.let { runCatching { SkillType.valueOf(it) }.getOrNull() },
        effectType = effectType,
        paramList = paramList
    )
}