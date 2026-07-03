package com.java.myapplication.engine.simulator.buffs

import com.java.myapplication.data.model.StatType

sealed interface Buff {
    val sourceId: String
    val duration: Int

    data class StatBoost(
        override val sourceId: String,
        override val duration: Int,
        val stat: StatType,
        val value: Double,
        val target: BuffTarget = BuffTarget.SELF
    ) : Buff

    data class DamageBonus(
        override val sourceId: String,
        override val duration: Int,
        val multiplier: Double
    ) : Buff

    data class EasyDmg(
        override val sourceId: String,
        override val duration: Int,
        val multiplier: Double
    ) : Buff

    data class SpeedMod(
        override val sourceId: String,
        override val duration: Int,
        val value: Double
    ) : Buff

    data class ActionAdvance(
        override val sourceId: String,
        override val duration: Int,
        val percent: Double
    ) : Buff

    data class UltCharge(
        override val sourceId: String,
        override val duration: Int,
        val amount: Double
    ) : Buff

    data class Dot(
        override val sourceId: String,
        override val duration: Int,
        val damageMult: Double,
        val dotType: String
    ) : Buff

    data class Break(
        override val sourceId: String,
        override val duration: Int
    ) : Buff
}

enum class BuffTarget { SELF, ALLY, TEAM, ENEMY }