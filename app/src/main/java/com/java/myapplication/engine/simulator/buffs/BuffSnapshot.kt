package com.java.myapplication.engine.simulator.buffs

data class BuffSnapshot(
    val atkBoost: Double = 0.0,
    val hpBoost: Double = 0.0,
    val defBoost: Double = 0.0,
    val spdBoost: Double = 0.0,
    val critRateBoost: Double = 0.0,
    val critDmgBoost: Double = 0.0,
    val damageBonus: Double = 0.0,
    val easyDmgTaken: Double = 0.0,
    val defShred: Double = 0.0
) {
    operator fun plus(other: BuffSnapshot) = BuffSnapshot(
        atkBoost = atkBoost + other.atkBoost,
        hpBoost = hpBoost + other.hpBoost,
        defBoost = defBoost + other.defBoost,
        spdBoost = spdBoost + other.spdBoost,
        critRateBoost = critRateBoost + other.critRateBoost,
        critDmgBoost = critDmgBoost + other.critDmgBoost,
        damageBonus = damageBonus + other.damageBonus,
        easyDmgTaken = easyDmgTaken + other.easyDmgTaken,
        defShred = defShred + other.defShred
    )
}