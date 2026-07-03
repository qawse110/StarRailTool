package com.mystarrail.tool.data.model

data class LightCone(
    val id: String,
    val name: String,
    val path: Path,
    val rarity: Int,
    val passiveName: String,
    val passiveEffect: PassiveEffect,
    val s5Multiplier: Double = 1.0
)