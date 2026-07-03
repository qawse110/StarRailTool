package com.mystarrail.tool.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.PassiveEffect
import com.mystarrail.tool.data.model.Path

@Entity(tableName = "light_cones")
data class LightConeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: Path,
    val rarity: Int,
    val passiveName: String,
    val passiveEffectJson: String,
    val s5Multiplier: Double
) {
    fun toModel(passiveEffect: PassiveEffect): LightCone = LightCone(
        id = id, name = name, path = path, rarity = rarity,
        passiveName = passiveName, passiveEffect = passiveEffect,
        s5Multiplier = s5Multiplier
    )

    companion object {
        fun fromModel(lc: LightCone): LightConeEntity = LightConeEntity(
            id = lc.id, name = lc.name, path = lc.path, rarity = lc.rarity,
            passiveName = lc.passiveName,
            passiveEffectJson = PassiveEffectJson.encode(lc.passiveEffect),
            s5Multiplier = lc.s5Multiplier
        )
    }
}