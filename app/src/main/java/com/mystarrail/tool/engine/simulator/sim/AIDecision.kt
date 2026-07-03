package com.mystarrail.tool.engine.simulator.sim

/**
 * 简单 AI 决策：
 * 1. 充能满 = 放终结技
 * 2. 战技点 > 0 且有战技倍率 = 放战技
 * 3. 有追击倍率且有敌人 = 追击
 * 4. 充能 ≥ 50 = 终结技
 * 5. 默认 PASS
 */
object AIDecision {
    fun decide(c: Combatant, team: List<Combatant>, enemies: List<Combatant>): ActionType {
        if (c.ultCharge >= 100.0) return ActionType.ULT
        if (c.sp > 0 && c.character.scaling.skillMult > 0) return ActionType.SKILL
        if (c.character.scaling.followUpMult > 0 && enemies.any { !it.isDead() }) {
            return ActionType.FOLLOW_UP
        }
        if (c.ultCharge >= 50.0) return ActionType.ULT
        return ActionType.PASS
    }
}