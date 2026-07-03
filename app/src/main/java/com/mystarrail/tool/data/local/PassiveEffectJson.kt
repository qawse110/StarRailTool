package com.mystarrail.tool.data.local

import com.mystarrail.tool.data.model.DmgCondition
import com.mystarrail.tool.data.model.PassiveEffect
import com.mystarrail.tool.data.model.SkillType
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Target
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object PassiveEffectJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(effect: PassiveEffect): String = json.encodeToString(
        JsonElement.serializer(), toJsonElement(effect)
    )

    fun decode(s: String): PassiveEffect = fromJsonElement(json.parseToJsonElement(s))

    private fun toJsonElement(e: PassiveEffect): JsonElement = buildJsonObject {
        when (e) {
            is PassiveEffect.StatBoost -> {
                put("type", "StatBoost")
                put("stat", e.stat.name)
                put("value", e.value)
                put("target", e.target.name)
            }
            is PassiveEffect.DamageBonus -> {
                put("type", "DamageBonus")
                put("multiplier", e.multiplier)
                put("condition", e.condition.name)
            }
            is PassiveEffect.SkillBoost -> {
                put("type", "SkillBoost")
                put("skillType", e.type.name)
                put("multiplier", e.multiplier)
            }
            is PassiveEffect.EnergyRegen -> {
                put("type", "EnergyRegen")
                put("perTurn", e.perTurn)
            }
            is PassiveEffect.Composite -> {
                put("type", "Composite")
                put("effects", buildJsonArray { e.effects.forEach { add(toJsonElement(it)) } })
            }
        }
    }

    private fun fromJsonElement(el: JsonElement): PassiveEffect {
        val obj = el.jsonObject
        return when (val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: error("Missing type")) {
            "StatBoost" -> PassiveEffect.StatBoost(
                stat = StatType.valueOf(obj["stat"]!!.jsonPrimitive.content),
                value = obj["value"]!!.jsonPrimitive.double,
                target = Target.valueOf(obj["target"]!!.jsonPrimitive.content)
            )
            "DamageBonus" -> PassiveEffect.DamageBonus(
                multiplier = obj["multiplier"]!!.jsonPrimitive.double,
                condition = DmgCondition.valueOf(obj["condition"]!!.jsonPrimitive.content)
            )
            "SkillBoost" -> PassiveEffect.SkillBoost(
                type = SkillType.valueOf(obj["skillType"]!!.jsonPrimitive.content),
                multiplier = obj["multiplier"]!!.jsonPrimitive.double
            )
            "EnergyRegen" -> PassiveEffect.EnergyRegen(
                perTurn = obj["perTurn"]!!.jsonPrimitive.double
            )
            "Composite" -> PassiveEffect.Composite(
                effects = obj["effects"]!!.jsonArray.map { fromJsonElement(it) }
            )
            else -> error("Unknown PassiveEffect type: $type")
        }
    }
}