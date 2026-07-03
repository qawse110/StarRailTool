package com.mystarrail.tool.data.local

import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.SubStat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object SubStatJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(subs: List<SubStat>): String = json.encodeToString(
        JsonElement.serializer(),
        buildJsonArray {
            subs.forEach { sub ->
                add(buildJsonObject {
                    put("type", sub.type.name)
                    put("value", sub.value)
                    put("rolls", sub.rolls)
                })
            }
        }
    )

    fun decode(s: String): List<SubStat> {
        if (s.isEmpty()) return emptyList()
        val arr = json.parseToJsonElement(s).jsonArray
        return arr.map {
            val obj = it.jsonObject
            SubStat(
                type = StatType.valueOf(obj["type"]!!.jsonPrimitive.content),
                value = obj["value"]!!.jsonPrimitive.double,
                rolls = obj["rolls"]!!.jsonPrimitive.int
            )
        }
    }
}