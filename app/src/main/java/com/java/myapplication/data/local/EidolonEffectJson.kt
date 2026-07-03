package com.java.myapplication.data.local

import com.java.myapplication.data.model.DmgCondition
import com.java.myapplication.data.model.EidolonEffect
import com.java.myapplication.data.model.StatType
import com.java.myapplication.data.model.Tag
import com.java.myapplication.data.model.Target
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object EidolonEffectJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(effect: EidolonEffect): String = json.encodeToString(
        JsonElement.serializer(), toJsonElement(effect)
    )

    fun decode(s: String): EidolonEffect = fromJsonElement(json.parseToJsonElement(s))

    private fun toJsonElement(e: EidolonEffect): JsonElement = buildJsonObject {
        when (e) {
            is EidolonEffect.StatBoost -> {
                put("type", "StatBoost")
                put("stat", e.stat.name)
                put("value", e.value)
                put("target", e.target.name)
            }
            is EidolonEffect.NewMechanic -> {
                put("type", "NewMechanic")
                put("mechanic", e.mechanic.name)
                put("param", e.param)
                put("note", e.note)
            }
            is EidolonEffect.DamageBonus -> {
                put("type", "DamageBonus")
                put("multiplier", e.multiplier)
                put("condition", e.condition.name)
            }
            is EidolonEffect.EnemyDebuff -> {
                put("type", "EnemyDebuff")
                put("stat", e.stat.name)
                put("value", e.value)
            }
            is EidolonEffect.Composite -> {
                put("type", "Composite")
                put("effects", buildJsonArray { e.effects.forEach { add(toJsonElement(it)) } })
            }
        }
    }

    private fun fromJsonElement(el: JsonElement): EidolonEffect {
        val obj = el.jsonObject
        return when (val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: error("Missing type")) {
            "StatBoost" -> EidolonEffect.StatBoost(
                stat = StatType.valueOf(obj["stat"]!!.jsonPrimitive.content),
                value = obj["value"]!!.jsonPrimitive.double,
                target = Target.valueOf(obj["target"]!!.jsonPrimitive.content)
            )
            "NewMechanic" -> EidolonEffect.NewMechanic(
                mechanic = Tag.valueOf(obj["mechanic"]!!.jsonPrimitive.content),
                param = obj["param"]!!.jsonPrimitive.double,
                note = obj["note"]!!.jsonPrimitive.content
            )
            "DamageBonus" -> EidolonEffect.DamageBonus(
                multiplier = obj["multiplier"]!!.jsonPrimitive.double,
                condition = DmgCondition.valueOf(obj["condition"]!!.jsonPrimitive.content)
            )
            "EnemyDebuff" -> EidolonEffect.EnemyDebuff(
                stat = StatType.valueOf(obj["stat"]!!.jsonPrimitive.content),
                value = obj["value"]!!.jsonPrimitive.double
            )
            "Composite" -> EidolonEffect.Composite(
                effects = obj["effects"]!!.jsonArray.map { fromJsonElement(it) }
            )
            else -> error("Unknown EidolonEffect type: $type")
        }
    }
}