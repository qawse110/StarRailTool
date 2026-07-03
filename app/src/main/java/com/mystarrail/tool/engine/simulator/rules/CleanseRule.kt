package com.mystarrail.tool.engine.simulator.rules

import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.engine.simulator.buffs.Buff
import com.mystarrail.tool.engine.simulator.buffs.BuffTarget
import com.mystarrail.tool.engine.simulator.sim.Combatant
import com.mystarrail.tool.engine.simulator.sim.RoundEvent

class CleanseRule : MechanicRule {
    override val name = "CLEANSE"

    override fun onActionStart(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        if (c.character.tags.contains(Tag.CLEANSE)) {
            c.debuffs.removeAll { it is Buff.StatBoost && it.target == BuffTarget.ENEMY }
        }
    }
}