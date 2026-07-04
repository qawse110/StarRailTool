package com.mystarrail.tool.data.model

data class CharacterScore(
    val characterId: String,
    val unitValueScore: Double,
    val cycleScore: Double,
    val teamSynergyScore: Double,
    val scenarioScore: Double,
    val mechanicCoverage: Double,
    val total: Double,
    val tier: Tier,
    // B8: 6th dimension
    val utilityScore: Double = 0.0
) {
    init {
        require(total in 0.0..100.0) { "total must be 0..100, was $total" }
    }
}