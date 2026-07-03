package com.java.myapplication.engine.simulator.rules

import com.java.myapplication.engine.simulator.sim.Combatant
import com.java.myapplication.engine.simulator.sim.RoundEvent

class BreakRule : MechanicRule {
    override val name = "BREAK_EFFECT"

    override fun onActionEnd(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        val target = enemies.firstOrNull { !it.isDead() } ?: return
        if (target.stats.def <= 0) return
        target.hp -= target.stats.def * 0.05
    }
}