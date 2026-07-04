package com.mystarrail.tool.engine.simulator.buffs

import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.engine.simulator.buffs.Buff.StatBoost
import com.mystarrail.tool.engine.simulator.buffs.Buff.DamageBonus
import com.mystarrail.tool.engine.simulator.buffs.Buff.EasyDmg
import com.mystarrail.tool.engine.simulator.buffs.Buff.SpeedMod
import com.mystarrail.tool.engine.simulator.buffs.Buff.HealingBoost
import com.mystarrail.tool.engine.simulator.buffs.Buff.ShieldBoost

class BuffEvaluator {

    fun evaluate(buffs: List<Buff>): BuffSnapshot {
        var snap = BuffSnapshot()
        buffs.forEach { b ->
            when (b) {
                is StatBoost -> snap = applyStat(snap, b)
                is DamageBonus -> snap = snap.copy(damageBonus = snap.damageBonus + b.multiplier)
                is EasyDmg -> snap = snap.copy(easyDmgTaken = snap.easyDmgTaken + b.multiplier)
                is SpeedMod -> snap = snap.copy(spdBoost = snap.spdBoost + b.value)
                is HealingBoost -> snap = snap.copy(healingBoost = snap.healingBoost + b.multiplier)
                is ShieldBoost -> snap = snap.copy(shieldBoost = snap.shieldBoost + b.multiplier)
                else -> { }
            }
        }
        return snap
    }

    private fun applyStat(snap: BuffSnapshot, b: StatBoost): BuffSnapshot = when (b.stat) {
        StatType.ATK -> snap.copy(atkBoost = snap.atkBoost + b.value)
        StatType.HP -> snap.copy(hpBoost = snap.hpBoost + b.value)
        StatType.DEF -> snap.copy(defBoost = snap.defBoost + b.value)
        StatType.SPD -> snap.copy(spdBoost = snap.spdBoost + b.value)
        StatType.CRIT_RATE -> snap.copy(critRateBoost = snap.critRateBoost + b.value)
        StatType.CRIT_DMG -> snap.copy(critDmgBoost = snap.critDmgBoost + b.value)
        StatType.EHR -> snap.copy(effectHitRate = snap.effectHitRate + b.value)
        StatType.BRK_EFF -> snap.copy(breakEffect = snap.breakEffect + b.value)
        StatType.EFFECT_RES -> snap.copy(effectRes = snap.effectRes + b.value)
        else -> snap
    }
}