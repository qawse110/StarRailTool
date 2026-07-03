package com.java.myapplication.data.model

data class Scaling(
    val skillMult: Double,
    val ultMult: Double,
    val talentMult: Double,
    val followUpMult: Double = 0.0,
    val aoeRatio: Double = 0.0
)