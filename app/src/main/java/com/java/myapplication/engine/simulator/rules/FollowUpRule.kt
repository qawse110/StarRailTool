package com.java.myapplication.engine.simulator.rules

import com.java.myapplication.engine.simulator.sim.ActionType
import com.java.myapplication.engine.simulator.sim.Combatant
import com.java.myapplication.engine.simulator.sim.MechanicEvent
import com.java.myapplication.engine.simulator.sim.RoundEvent
import com.java.myapplication.engine.simulator.sim.TargetHit

class FollowUpRule : MechanicRule {
    override val name = "FOLLOW_UP"

    override fun onActionStart(c: Combatant, team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        if (c.character.scaling.followUpMult <= 0) return
        val target = enemies.firstOrNull { !it.isDead() } ?: return

        val dmg = c.character.scaling.followUpMult * c.stats.atk * 0.5
        target.hp -= dmg
        events.add(RoundEvent(
            round = events.lastOrNull()?.round ?: 0,
            actorId = c.charId,
            action = ActionType.FOLLOW_UP,
            targets = listOf(TargetHit(
                targetId = target.charId,
                element = c.character.element,
                damage = dmg,
                isCrit = false
            )),
            damageDealt = dmg,
            healingDone = 0.0,
            buffsApplied = emptyList(),
            mechanicsTriggered = listOf(MechanicEvent("FOLLOW_UP", c.charId, target.charId, dmg)),
            actionValueBefore = c.actionValue, actionValueAfter = c.actionValue,
            ultChargeBefore = c.ultCharge, ultChargeAfter = c.ultCharge
        ))
    }
}