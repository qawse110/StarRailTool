package com.java.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.java.myapplication.data.model.PassiveEffect
import com.java.myapplication.data.model.RelicSet
import com.java.myapplication.data.model.Role

@Entity(tableName = "relic_sets")
data class RelicSetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val twoPieceJson: String,
    val fourPieceJson: String,
    val suitableFor: Set<Role>
) {
    fun toModel(two: PassiveEffect, four: PassiveEffect): RelicSet = RelicSet(
        id = id, name = name, twoPiece = two, fourPiece = four,
        suitableFor = suitableFor
    )

    companion object {
        fun fromModel(r: RelicSet): RelicSetEntity = RelicSetEntity(
            id = r.id, name = r.name,
            twoPieceJson = PassiveEffectJson.encode(r.twoPiece),
            fourPieceJson = PassiveEffectJson.encode(r.fourPiece),
            suitableFor = r.suitableFor
        )
    }
}