package com.mystarrail.tool.engine.simulator.damage

data class CharacterUnitValue(
    val expectedSkillDmg: Double,
    val expectedUltDmg: Double,
    val expectedTalentDmg: Double,
    val expectedFollowUpDmg: Double,
    val dotDps: Double,
    val effectiveActionValue: Double,
    val ultChargeRate: Double,
    val baseSupportValue: Double,
    val baseHealValue: Double,
    val baseShieldValue: Double
)