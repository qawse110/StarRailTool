package com.mystarrail.tool.engine.simulator.rules

import com.mystarrail.tool.engine.simulator.sim.ActionType
import com.mystarrail.tool.engine.simulator.sim.Combatant
import com.mystarrail.tool.engine.simulator.sim.MechanicEvent
import com.mystarrail.tool.engine.simulator.sim.RoundEvent
import com.mystarrail.tool.engine.simulator.sim.TargetHit

class FollowUpRule : MechanicRule {
    override val name = "FOLLOW_UP"

    /**
     * FollowUp 触发检查：角色有追击倍率且敌人存活，且不是每次行动都触发
     * 实际追击伤害由 DiscreteEventSimulator 通过 DamageCalculator 计算
     */
    override fun onFollowUpCheck(c: Combatant, team: List<Combatant>, enemies: List<Combatant>): Boolean {
        if (c.character.scaling.followUpMult <= 0) return false
        val target = enemies.firstOrNull { !it.isDead() } ?: return false
        // 简化：50% 概率触发追击（确定性：基于角色 id hash）
        return (c.charId.hashCode() and 0x1) == 0
    }
}