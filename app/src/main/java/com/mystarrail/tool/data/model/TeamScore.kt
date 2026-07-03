package com.mystarrail.tool.data.model

data class TeamScore(
    val totalDamage: Double,
    val totalHealing: Double,
    val totalShielding: Double,
    val roundsToKill: Int?,
    val ultsCast: Map<String, Int>,
    val buffUptime: Map<String, Double>,
    val breakdown: Map<String, Double>,
    val score: Double
)