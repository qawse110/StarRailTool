package com.java.myapplication.data.model

sealed interface PassiveEffect {
    data class StatBoost(
        val stat: StatType,
        val value: Double,
        val target: Target = Target.SELF
    ) : PassiveEffect

    data class DamageBonus(
        val multiplier: Double,
        val condition: DmgCondition
    ) : PassiveEffect

    data class SkillBoost(
        val type: SkillType,
        val multiplier: Double
    ) : PassiveEffect

    data class EnergyRegen(val perTurn: Double) : PassiveEffect

    data class Composite(val effects: List<PassiveEffect>) : PassiveEffect
}