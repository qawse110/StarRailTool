package com.mystarrail.tool.engine.simulator.rules

import com.mystarrail.tool.engine.simulator.sim.Combatant
import com.mystarrail.tool.engine.simulator.sim.RoundEvent

class UltChargeRule : MechanicRule {
    override val name = "ULT_CHARGE"

    override fun onActionEnd(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        c.ultCharge = (c.ultCharge + 10.0).coerceAtMost(100.0)
    }
}