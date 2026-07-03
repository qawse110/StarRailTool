package com.mystarrail.tool.data.seed.remote

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.CycleProfile
import com.mystarrail.tool.data.model.DmgCondition
import com.mystarrail.tool.data.model.Eidolon
import com.mystarrail.tool.data.model.EidolonEffect
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.PassiveEffect
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.SkillType
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.data.model.Target
import com.mystarrail.tool.data.seed.SeedParser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Mar-7th/StarRailRes → SeedRoot 转换。
 *
 * 已知不完美（transformer 注释里说明）：
 *  - 命途 12 选 7；StarRailRes 12 命途覆盖了我们 7 命途，做了别名映射
 *  - 技能倍率取满级 params 第一个；AOE 判定粗略
 *  - 遗器效果靠 properties.effects 数组推断，不准
 *  - 星魂效果仅 desc 文本 + 简单正则
 */
object Mar7thToSeedTransformer {

    private val PATH_MAP: Map<String, Path> = mapOf(
        "Hunt" to Path.HUNT,
        "Erudition" to Path.ERUDITION,
        "Harmony" to Path.HARMONY,
        "Destruction" to Path.DESTRUCTION,
        "Knight" to Path.PRESERVATION,
        "Mage" to Path.ERUDITION,
        "Priest" to Path.ABUNDANCE,
        "Warrior" to Path.DESTRUCTION,
        "Rogue" to Path.HUNT,
        "Shaman" to Path.HARMONY,
        "Warlock" to Path.NIHILITY,
        "Memory" to Path.HARMONY
    )

    private val ELEMENT_MAP: Map<String, Element> = mapOf(
        "Physical" to Element.PHYSICAL,
        "Fire" to Element.FIRE,
        "Ice" to Element.ICE,
        "Lightning" to Element.LIGHTNING,
        "Wind" to Element.WIND,
        "Quantum" to Element.QUANTUM,
        "Imaginary" to Element.IMAGINARY
    )

    private val SKILL_TYPE_MAP: Map<String, SkillType> = mapOf(
        "Normal" to SkillType.SKILL,
        "Skill" to SkillType.SKILL,
        "BPSkill" to SkillType.SKILL,
        "Ultra" to SkillType.ULT,
        "Talent" to SkillType.TALENT,
        "Technique" to SkillType.SKILL,
        "FollowUp" to SkillType.FOLLOW_UP,
        "Dot" to SkillType.DOT
    )

    private val PATH_DEFAULT = Path.ERUDITION
    private val ELEMENT_DEFAULT = Element.PHYSICAL
    private val AOE_EFFECTS = setOf("AOEAttack", "All", "Blast", "Bounce")

    fun transform(
        files: Map<RemoteSeedSource.File, JsonElement>
    ): SeedParser.ParseResult.Success {
        val characters = files[RemoteSeedSource.File.CHARACTERS]?.jsonObject ?: JsonObject(emptyMap())
        val promotions = files[RemoteSeedSource.File.CHARACTER_PROMOTIONS]?.jsonObject ?: JsonObject(emptyMap())
        val skills = files[RemoteSeedSource.File.CHARACTER_SKILLS]?.jsonObject ?: JsonObject(emptyMap())
        val ranks = files[RemoteSeedSource.File.CHARACTER_RANKS]?.jsonObject ?: JsonObject(emptyMap())
        val lightCones = files[RemoteSeedSource.File.LIGHT_CONES]?.jsonObject ?: JsonObject(emptyMap())
        val conePromos = files[RemoteSeedSource.File.LIGHT_CONE_PROMOTIONS]?.jsonObject ?: JsonObject(emptyMap())
        val coneRanks = files[RemoteSeedSource.File.LIGHT_CONE_RANKS]?.jsonObject ?: JsonObject(emptyMap())
        val relicSets = files[RemoteSeedSource.File.RELIC_SETS]?.jsonObject ?: JsonObject(emptyMap())

        val outChars = characters.values.mapNotNull { el ->
            runCatching { el.toCharacter(promotions, skills) }.getOrNull()
        }
        val outCones = lightCones.values.mapNotNull { el ->
            runCatching { el.toLightCone(conePromos, coneRanks) }.getOrNull()
        }
        val outSets = relicSets.values.mapNotNull { el ->
            runCatching { el.toRelicSet() }.getOrNull()
        }
        val outEidolons = outChars.flatMap { ch ->
            val rawId = ch.id.removePrefix("mar7th_")
            val rankIds = characters[rawId]?.jsonObject
                ?.get("ranks")?.jsonArray
                ?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() }
                ?: emptyList()
            rankIds.mapNotNull { rid ->
                val node = ranks[rid]?.jsonObject ?: return@mapNotNull null
                runCatching {
                    val level = runCatching { node["rank"]?.jsonPrimitive?.content?.toInt() }
                        .getOrNull() ?: 0
                    Eidolon(
                        id = "mar7th_$rid",
                        characterId = ch.id,
                        level = level,
                        name = runCatching { node["name"]?.jsonPrimitive?.content }.getOrNull().orEmpty(),
                        effect = node.toEidolonEffect(),
                        major = (level % 2 == 0) && level > 0
                    )
                }.getOrNull()
            }
        }

        return SeedParser.ParseResult.Success(
            characters = outChars,
            lightCones = outCones,
            relicSets = outSets,
            enemies = emptyList(),
            scenarios = emptyList(),
            eidolons = outEidolons
        )
    }

    // ===== Element mappers =====

    private fun JsonElement.toCharacter(
        promotions: JsonObject,
        skills: JsonObject
    ): Character {
        val obj = jsonObject
        val rawId = obj.str("id") ?: error("Missing id")
        val path = PATH_MAP[obj.str("path")] ?: PATH_DEFAULT
        val element = ELEMENT_MAP[obj.str("element")] ?: ELEMENT_DEFAULT
        val rarity = obj.int("rarity") ?: 4
        val role = inferRole(path)
        val baseStats = promotions[rawId]?.jsonObject
            ?.get("values")?.jsonArray
            ?.firstOrNull()?.jsonObject?.toBaseStats()
            ?: Stats(hp = 1000.0, atk = 500.0, def = 300.0, spd = 100.0)
        val scaling = obj.toScaling(skills)
        val tags = inferTags(path, element)
        val cycleProfile = inferCycleProfile(obj, skills)
        val iconUrl = obj.str("icon")
            ?.let { "https://raw.githubusercontent.com/Mar-7th/StarRailRes/master/$it" }
            ?: ""
        return Character(
            id = "mar7th_$rawId",
            name = obj.str("name").orEmpty(),
            rarity = rarity,
            path = path,
            element = element,
            role = role,
            tags = tags,
            baseStats = baseStats,
            scaling = scaling,
            cycleProfile = cycleProfile,
            iconUrl = iconUrl,
            version = 1
        )
    }

    private fun JsonElement.toLightCone(
        conePromos: JsonObject,
        coneRanks: JsonObject
    ): LightCone {
        val obj = jsonObject
        val rawId = obj.str("id") ?: error("Missing id")
        val path = PATH_MAP[obj.str("path")] ?: PATH_DEFAULT
        val baseStats = conePromos[rawId]?.jsonObject
            ?.get("values")?.jsonArray
            ?.firstOrNull()?.jsonObject?.toBaseStats()
            ?: Stats(hp = 0.0, atk = 0.0, def = 0.0, spd = 0.0)
        val rankNode = coneRanks[rawId]?.jsonObject
        val passive = rankNode?.toPassiveEffect() ?: PassiveEffect.StatBoost(
            stat = StatType.ATK, value = 0.0
        )
        return LightCone(
            id = "mar7th_$rawId",
            name = obj.str("name").orEmpty(),
            path = path,
            rarity = obj.int("rarity") ?: 3,
            passiveName = obj.str("name").orEmpty(),
            passiveEffect = passive,
            s5Multiplier = 1.0
        )
    }

    private fun JsonElement.toRelicSet(): RelicSet {
        val obj = jsonObject
        val rawId = obj.str("id") ?: error("Missing id")
        val propsArr = obj["properties"]?.jsonArray ?: JsonArray(emptyList())
        val twoPiece = propsArr.getOrNull(0)?.toPassiveEffectFromRelic()
            ?: PassiveEffect.StatBoost(stat = StatType.ATK, value = 0.0)
        val fourPiece = propsArr.getOrNull(1)?.toPassiveEffectFromRelic() ?: twoPiece
        return RelicSet(
            id = "mar7th_$rawId",
            name = obj.str("name").orEmpty(),
            twoPiece = twoPiece,
            fourPiece = fourPiece,
            suitableFor = emptySet()
        )
    }

    // ===== Inferrers =====

    private fun inferRole(path: Path): Role = when (path) {
        Path.HARMONY, Path.PRESERVATION, Path.ABUNDANCE -> Role.SUPPORT
        else -> Role.DPS
    }

    private fun inferTags(path: Path, element: Element): Set<Tag> {
        val tags = mutableSetOf<Tag>()
        when (path) {
            Path.HUNT -> tags += Tag.SINGLE_TARGET
            Path.ERUDITION -> tags += Tag.AOE
            else -> {}
        }
        tags += when (element) {
            Element.QUANTUM -> Tag.DOT
            Element.FIRE -> Tag.SUMMON
            else -> Tag.IMPULSE
        }
        return tags
    }

    private fun inferCycleProfile(character: JsonObject, skills: JsonObject): CycleProfile? {
        val skillIds = character["skills"]?.jsonArray
            ?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() }
            ?: return null
        val isDot = skillIds.any { sid ->
            runCatching { skills[sid]?.jsonObject?.get("type")?.jsonPrimitive?.content }
                .getOrNull() == "Dot"
        }
        val isFollowUp = skillIds.any { sid ->
            runCatching { skills[sid]?.jsonObject?.get("type")?.jsonPrimitive?.content }
                .getOrNull() == "FollowUp"
        }
        return CycleProfile(
            cycleActions = 4,
            spdBreakpoints = listOf(134.0, 143.0, 160.0),
            isFollowUp = isFollowUp,
            isDot = isDot
        )
    }

    private fun JsonObject.toScaling(skills: JsonObject): Scaling {
        val skillIds = this["skills"]?.jsonArray
            ?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull() }
            ?: emptyList()
        var skillMult = 1.0
        var ultMult = 2.0
        var talentMult = 1.5
        var followUpMult = 1.0
        var aoeRatio = 0.0

        for (sid in skillIds) {
            val s = skills[sid]?.jsonObject ?: continue
            val type = runCatching { s["type"]?.jsonPrimitive?.content }.getOrNull() ?: continue
            val mult = s.extractMaxParam() ?: continue
            when (SKILL_TYPE_MAP[type]) {
                SkillType.SKILL -> skillMult = mult
                SkillType.ULT -> ultMult = mult
                SkillType.TALENT -> talentMult = mult
                SkillType.FOLLOW_UP, SkillType.DOT -> followUpMult = mult
                else -> {}
            }
            val effect = runCatching { s["effect"]?.jsonPrimitive?.content }.getOrNull()
            if (effect in AOE_EFFECTS) aoeRatio = 0.6
        }
        return Scaling(
            skillMult = skillMult,
            ultMult = ultMult,
            talentMult = talentMult,
            followUpMult = followUpMult,
            aoeRatio = aoeRatio
        )
    }

    // ===== Object-level helpers =====

    private fun JsonObject.toBaseStats(): Stats {
        fun n(path: String, default: Double = 0.0): Double {
            val baseEl = this[path]?.jsonObject?.get("base") ?: return default
            return runCatching { baseEl.jsonPrimitive.double }.getOrNull() ?: default
        }
        return Stats(
            hp = n("hp", 1000.0),
            atk = n("atk", 500.0),
            def = n("def", 300.0),
            spd = n("spd", 100.0)
        )
    }

    private fun JsonObject.toPassiveEffect(): PassiveEffect {
        val desc = this.str("desc").orEmpty()
        val firstValue = this["params"]?.jsonArray?.firstOrNull()?.jsonArray
            ?.firstOrNull()?.let { runCatching { it.jsonPrimitive.double }.getOrNull() } ?: 0.0
        return when {
            desc.contains("CRIT Rate", ignoreCase = true) -> PassiveEffect.StatBoost(
                stat = StatType.CRIT_RATE, value = firstValue
            )
            desc.contains("CRIT DMG", ignoreCase = true) -> PassiveEffect.StatBoost(
                stat = StatType.CRIT_DMG, value = firstValue
            )
            desc.contains("ATK", ignoreCase = true) -> PassiveEffect.StatBoost(
                stat = StatType.ATK, value = firstValue
            )
            desc.contains("HP", ignoreCase = true) -> PassiveEffect.StatBoost(
                stat = StatType.HP, value = firstValue
            )
            desc.contains("DMG", ignoreCase = true) -> PassiveEffect.DamageBonus(
                multiplier = firstValue, condition = DmgCondition.ALWAYS
            )
            else -> PassiveEffect.StatBoost(stat = StatType.ATK, value = firstValue)
        }
    }

    private fun JsonElement.toPassiveEffectFromRelic(): PassiveEffect {
        val obj = runCatching { jsonObject }.getOrNull() ?: return PassiveEffect.StatBoost(
            stat = StatType.ATK, value = 0.0
        )
        val effects = obj["effects"]?.jsonArray
            ?.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            ?: emptyList()
        val first = effects.firstOrNull()?.let {
            val type = runCatching { it["type"]?.jsonPrimitive?.content }.getOrNull()
            val value = runCatching { it["value"]?.jsonPrimitive?.double }.getOrNull() ?: 0.0
            when (type) {
                "HealRatioBase" -> PassiveEffect.StatBoost(stat = StatType.HP, value = value)
                "AttackDelta" -> PassiveEffect.StatBoost(stat = StatType.ATK, value = value)
                "DefenceDelta" -> PassiveEffect.StatBoost(stat = StatType.DEF, value = value)
                else -> PassiveEffect.DamageBonus(multiplier = value, condition = DmgCondition.ALWAYS)
            }
        }
        return first ?: PassiveEffect.StatBoost(stat = StatType.ATK, value = 0.0)
    }

    private fun JsonObject.toEidolonEffect(): EidolonEffect {
        val desc = this.str("desc").orEmpty()
        val firstParam = this["params"]?.jsonArray?.firstOrNull()?.jsonArray
            ?.firstOrNull()?.let { runCatching { it.jsonPrimitive.double }.getOrNull() }
        return when {
            desc.contains("CRIT Rate", ignoreCase = true) && firstParam != null ->
                EidolonEffect.StatBoost(stat = StatType.CRIT_RATE, value = firstParam)
            desc.contains("CRIT DMG", ignoreCase = true) && firstParam != null ->
                EidolonEffect.StatBoost(stat = StatType.CRIT_DMG, value = firstParam)
            desc.contains("ATK", ignoreCase = true) && firstParam != null ->
                EidolonEffect.StatBoost(
                    stat = StatType.ATK, value = firstParam,
                    target = if (desc.contains("Ally", ignoreCase = true)) Target.ALLY else Target.SELF
                )
            desc.contains("DMG", ignoreCase = true) && firstParam != null ->
                EidolonEffect.DamageBonus(multiplier = firstParam, condition = DmgCondition.ALWAYS)
            firstParam != null -> EidolonEffect.NewMechanic(
                mechanic = Tag.IMPULSE, param = firstParam, note = desc.take(80)
            )
            else -> EidolonEffect.NewMechanic(
                mechanic = Tag.IMPULSE, param = 1.0, note = desc.take(80)
            )
        }
    }

    private fun JsonObject.extractMaxParam(): Double? {
        val params = this["params"]?.jsonArray ?: return null
        return params.lastOrNull { it.jsonArray.isNotEmpty() }
            ?.jsonArray?.firstOrNull()
            ?.let { runCatching { it.jsonPrimitive.double }.getOrNull() }
    }

    // ===== Safe accessors =====

    private fun JsonObject.str(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.content }.getOrNull()

    private fun JsonObject.int(key: String): Int? =
        runCatching { this[key]?.jsonPrimitive?.content?.toInt() }.getOrNull()
}