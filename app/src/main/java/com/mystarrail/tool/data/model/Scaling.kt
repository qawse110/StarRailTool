package com.mystarrail.tool.data.model

data class Scaling(
    val skillMult: Double,
    val ultMult: Double,
    val talentMult: Double,
    val followUpMult: Double = 0.0,
    val aoeRatio: Double = 0.0,
    val dotMult: Double = 0.0
)