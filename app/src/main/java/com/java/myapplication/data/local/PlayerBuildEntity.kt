package com.java.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.java.myapplication.data.model.MainStats
import com.java.myapplication.data.model.PlayerBuild
import com.java.myapplication.data.model.StatType

@Entity(tableName = "player_builds")
data class PlayerBuildEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterId: String,
    val level: Int,
    val ascension: Int,
    val lightConeId: String,
    val lightConeLevel: Int,
    val lightConeSuperimposition: Int,
    val relicSet4: String,
    val relicSet2: String?,
    val body: StatType,
    val boots: StatType,
    val sphere: StatType,
    val rope: StatType,
    val subStatsJson: String,
    val eidolons: Set<Int>,
    val notes: String
) {
    fun toModel(): PlayerBuild = PlayerBuild(
        id = id, characterId = characterId, level = level, ascension = ascension,
        lightConeId = lightConeId, lightConeLevel = lightConeLevel,
        lightConeSuperimposition = lightConeSuperimposition,
        relicSet4 = relicSet4, relicSet2 = relicSet2,
        mainStats = MainStats(body, boots, sphere, rope),
        subStats = SubStatJson.decode(subStatsJson),
        eidolons = eidolons, notes = notes
    )

    companion object {
        fun fromModel(b: PlayerBuild): PlayerBuildEntity = PlayerBuildEntity(
            id = b.id, characterId = b.characterId, level = b.level, ascension = b.ascension,
            lightConeId = b.lightConeId, lightConeLevel = b.lightConeLevel,
            lightConeSuperimposition = b.lightConeSuperimposition,
            relicSet4 = b.relicSet4, relicSet2 = b.relicSet2,
            body = b.mainStats.body, boots = b.mainStats.boots,
            sphere = b.mainStats.sphere, rope = b.mainStats.rope,
            subStatsJson = SubStatJson.encode(b.subStats),
            eidolons = b.eidolons, notes = b.notes
        )
    }
}