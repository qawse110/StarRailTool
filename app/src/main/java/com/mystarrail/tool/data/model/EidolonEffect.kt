package com.mystarrail.tool.data.model

sealed interface EidolonEffect {
    data class StatBoost(
        val stat: StatType,
        val value: Double,
        val target: Target = Target.SELF
    ) : EidolonEffect

    data class NewMechanic(
        val mechanic: Tag,
        val param: Double = 1.0,
        val note: String = ""
    ) : EidolonEffect

    data class DamageBonus(
        val multiplier: Double,
        val condition: DmgCondition
    ) : EidolonEffect

    data class EnemyDebuff(
        val stat: StatType,
        val value: Double
    ) : EidolonEffect

    data class Composite(val effects: List<EidolonEffect>) : EidolonEffect
}