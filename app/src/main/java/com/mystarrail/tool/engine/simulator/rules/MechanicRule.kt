package com.mystarrail.tool.engine.simulator.rules

import com.mystarrail.tool.engine.simulator.sim.Combatant
import com.mystarrail.tool.engine.simulator.sim.RoundEvent

interface MechanicRule {
    val name: String
    fun onActionStart(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {}
    fun onActionEnd(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {}
    fun onFollowUpCheck(c: Combatant, team: List<Combatant>, enemies: List<Combatant>): Boolean = false
    fun onRoundEnd(team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {}
    fun onTurnStart(c: Combatant, events: MutableList<RoundEvent>) {}
}