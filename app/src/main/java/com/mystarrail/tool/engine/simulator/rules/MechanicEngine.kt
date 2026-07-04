package com.mystarrail.tool.engine.simulator.rules

import com.mystarrail.tool.engine.simulator.buffs.Buff
import com.mystarrail.tool.engine.simulator.sim.Combatant
import com.mystarrail.tool.engine.simulator.sim.RoundEvent

class MechanicEngine(
    val rules: List<MechanicRule> = listOf(
        DotRule(),
        FollowUpRule(),
        ActionAdvanceRule(),
        UltChargeRule(),
        BreakRule(),
        SummonRule(),
        CleanseRule(),
        EasyDmgRule()
    )
) {
    fun onActionStart(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        rules.forEach { it.onActionStart(c, team, enemies, events) }
    }

    fun onActionEnd(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        rules.forEach { it.onActionEnd(c, team, enemies, events) }
    }

    fun onFollowUpCheck(c: Combatant, team: List<Combatant>, enemies: List<Combatant>): Boolean =
        rules.any { it.onFollowUpCheck(c, team, enemies) }

    fun onRoundEnd(team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        rules.forEach { it.onRoundEnd(team, enemies, events) }
        (team + enemies).forEach { c ->
            c.buffs.replaceAll { decrementDuration(it) }
            c.buffs.removeAll { it.duration == 0 }
            c.debuffs.replaceAll { decrementDuration(it) }
            c.debuffs.removeAll { it.duration == 0 }
        }
    }

    private fun decrementDuration(buff: Buff): Buff {
        if (buff.duration <= 0) return buff
        val newDur = buff.duration - 1
        return when (buff) {
            is Buff.StatBoost -> buff.copy(duration = newDur)
            is Buff.DamageBonus -> buff.copy(duration = newDur)
            is Buff.EasyDmg -> buff.copy(duration = newDur)
            is Buff.SpeedMod -> buff.copy(duration = newDur)
            is Buff.ActionAdvance -> buff.copy(duration = newDur)
            is Buff.UltCharge -> buff.copy(duration = newDur)
            is Buff.Dot -> buff.copy(duration = newDur)
            is Buff.Break -> buff.copy(duration = newDur)
            is Buff.HealingBoost -> buff.copy(duration = newDur)
            is Buff.ShieldBoost -> buff.copy(duration = newDur)
        }
    }
}