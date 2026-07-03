package com.mystarrail.tool.engine.simulator.rules

import com.mystarrail.tool.engine.simulator.buffs.Buff
import com.mystarrail.tool.engine.simulator.sim.Combatant
import com.mystarrail.tool.engine.simulator.sim.RoundEvent

class ActionAdvanceRule : MechanicRule {
    override val name = "ACTION_ADVANCE"

    override fun onActionStart(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        c.buffs.filterIsInstance<Buff.ActionAdvance>().forEach { aa ->
            val delta = -10000.0 * aa.percent
            c.actionValue = (c.actionValue + delta).coerceAtMost(0.0)
        }
    }
}