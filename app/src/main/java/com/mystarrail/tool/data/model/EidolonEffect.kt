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

    /** 战技/终结技/天赋/追击/DOT 等指定类型的直接乘区加成。 */
    data class SkillBoost(
        val type: SkillType,
        val multiplier: Double
    ) : EidolonEffect

    data class Composite(val effects: List<EidolonEffect>) : EidolonEffect
}