package com.java.myapplication.engine.simulator.rules

import com.java.myapplication.engine.simulator.buffs.Buff
import com.java.myapplication.engine.simulator.sim.Combatant
import com.java.myapplication.engine.simulator.sim.RoundEvent

class ActionAdvanceRule : MechanicRule {
    override val name = "ACTION_ADVANCE"

    override fun onActionStart(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        c.buffs.filterIsInstance<Buff.ActionAdvance>().forEach { aa ->
            val delta = -10000.0 * aa.percent
            c.actionValue = (c.actionValue + delta).coerceAtMost(0.0)
        }
    }
}