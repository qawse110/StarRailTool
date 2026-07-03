package com.java.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.java.myapplication.data.model.Eidolon
import com.java.myapplication.data.model.EidolonEffect

@Entity(tableName = "eidolons")
data class EidolonEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val level: Int,
    val name: String,
    val effectJson: String,
    val major: Boolean
) {
    fun toModel(effect: EidolonEffect): Eidolon = Eidolon(
        id = id, characterId = characterId, level = level,
        name = name, effect = effect, major = major
    )

    companion object {
        fun fromModel(e: Eidolon): EidolonEntity = EidolonEntity(
            id = e.id, characterId = e.characterId, level = e.level,
            name = e.name,
            effectJson = EidolonEffectJson.encode(e.effect),
            major = e.major
        )
    }
}