package com.mystarrail.tool.engine.simulator.sim

data class SimulationResult(
    val log: List<RoundEvent>,
    val totalDamage: Map<String, Double>,
    val totalHealing: Map<String, Double>,
    val totalShielding: Map<String, Double>,
    val totalBuffUptime: Map<String, Double>,
    val enemyKills: Int,
    val roundsToKill: Int?,
    val ultsCast: Map<String, Int>,
    val actions: Map<String, Int>,
    val damageBreakdown: DamageBreakdown
)

data class DamageBreakdown(
    val skillDmg: Double = 0.0,
    val ultDmg: Double = 0.0,
    val followUpDmg: Double = 0.0,
    val dotDmg: Double = 0.0,
    val breakDmg: Double = 0.0
) {
    val total: Double get() = skillDmg + ultDmg + followUpDmg + dotDmg + breakDmg
}