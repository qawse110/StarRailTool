package com.java.myapplication.engine.simulator.rules

import com.java.myapplication.data.model.Element
import com.java.myapplication.engine.simulator.buffs.Buff
import com.java.myapplication.engine.simulator.sim.ActionType
import com.java.myapplication.engine.simulator.sim.Combatant
import com.java.myapplication.engine.simulator.sim.MechanicEvent
import com.java.myapplication.engine.simulator.sim.RoundEvent
import com.java.myapplication.engine.simulator.sim.TargetHit

class DotRule : MechanicRule {
    override val name = "DOT"

    override fun onRoundEnd(team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        enemies.forEach { enemy ->
            enemy.debuffs.filterIsInstance<Buff.Dot>().forEach { dot ->
                if (enemy.isDead()) return@forEach
                val dmg = dot.damageMult * 100
                enemy.hp -= dmg
                events.add(RoundEvent(
                    round = events.lastOrNull()?.round ?: 0,
                    actorId = dot.sourceId,
                    action = ActionType.DOT,
                    targets = listOf(TargetHit(
                        targetId = enemy.charId,
                        element = dot.dotType.toElement(),
                        damage = dmg,
                        isCrit = false
                    )),
                    damageDealt = dmg,
                    healingDone = 0.0,
                    buffsApplied = emptyList(),
                    mechanicsTriggered = listOf(MechanicEvent("DOT_TICK", dot.sourceId, enemy.charId, dmg)),
                    actionValueBefore = 0.0, actionValueAfter = 0.0,
                    ultChargeBefore = 0.0, ultChargeAfter = 0.0
                ))
            }
        }
    }

    private fun String.toElement() = when (this) {
        "BURN" -> Element.FIRE
        "BLEED" -> Element.PHYSICAL
        "SHOCK" -> Element.LIGHTNING
        "WIND_SHEAR" -> Element.WIND
        else -> Element.PHYSICAL
    }
}