package com.java.myapplication.engine.simulator.rules

import com.java.myapplication.engine.simulator.sim.Combatant
import com.java.myapplication.engine.simulator.sim.RoundEvent

class UltChargeRule : MechanicRule {
    override val name = "ULT_CHARGE"

    override fun onActionEnd(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        c.ultCharge = (c.ultCharge + 10.0).coerceAtMost(100.0)
    }
}