package com.java.myapplication.engine.simulator.rules

import com.java.myapplication.data.model.Tag
import com.java.myapplication.engine.simulator.buffs.Buff
import com.java.myapplication.engine.simulator.buffs.BuffTarget
import com.java.myapplication.engine.simulator.sim.Combatant
import com.java.myapplication.engine.simulator.sim.RoundEvent

class CleanseRule : MechanicRule {
    override val name = "CLEANSE"

    override fun onActionStart(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        if (c.character.tags.contains(Tag.CLEANSE)) {
            c.debuffs.removeAll { it is Buff.StatBoost && it.target == BuffTarget.ENEMY }
        }
    }
}