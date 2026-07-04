package com.mystarrail.tool.engine.simulator.rules

import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.engine.simulator.buffs.Buff
import com.mystarrail.tool.engine.simulator.sim.ActionType
import com.mystarrail.tool.engine.simulator.sim.Combatant
import com.mystarrail.tool.engine.simulator.sim.MechanicEvent
import com.mystarrail.tool.engine.simulator.sim.RoundEvent
import com.mystarrail.tool.engine.simulator.sim.TargetHit

class DotRule : MechanicRule {
    override val name = "DOT"

    override fun onRoundEnd(team: List<Combatant>, enemies: List<Combatant>, events: MutableList<RoundEvent>) {
        enemies.forEach { enemy ->
            enemy.debuffs.filterIsInstance<Buff.Dot>().forEach { dot ->
                if (enemy.isDead()) return@forEach

                // 查找 DOT 来源角色以获取 ATK
                val source = team.firstOrNull { it.charId == dot.sourceId }
                val sourceAtk = source?.stats?.atk ?: 1000.0

                // 使用来源 ATK 和 DOT 倍率计算伤害
                val dmg = dot.damageMult * sourceAtk * 0.6
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