package com.mystarrail.tool.engine.simulator.sim

import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Tag

/**
 * 增强 AI 决策系统：
 * 1. 充能满 = 放终结技（优先级最高）
 * 2. 战技点 > 0 且敌人属性弱于我方 → 战技
 * 3. 战技点 > 0 且有战技倍率 → 战技
 * 4. 充能 ≥ 50 且不是纯辅助 → 终结技
 * 5. 有 DOT 标签且 enemy 有对应 debuff → 普攻/战技续 DOT
 * 6. 默认 PASS（普攻充能）
 */
object AIDecision {
    fun decide(c: Combatant, team: List<Combatant>, enemies: List<Combatant>): ActionType {
        // 1. 充能满优先放终结技
        if (c.ultCharge >= 100.0) return ActionType.ULT
        val aliveEnemies = enemies.filter { !it.isDead() }
        if (aliveEnemies.isEmpty()) return ActionType.PASS

        // 2. 检查是否存在与我方属性匹配的弱点
        val hasWeaknessMatch = aliveEnemies.any { e ->
            c.character.element in e.character.element.getWeaknessSet()
        }

        // 3. 战技决策
        if (c.sp > 0 && c.character.scaling.skillMult > 0) {
            // 若敌人有弱点匹配，战技优先级提高
            if (hasWeaknessMatch) return ActionType.SKILL
            // DOT 角色优先用战技续 DOT
            if (c.character.tags.contains(Tag.DOT) && c.character.scaling.skillMult > 0) {
                return ActionType.SKILL
            }
            return ActionType.SKILL
        }

        // 4. 充能过半且角色有输出能力 → 终结技
        if (c.ultCharge >= 50.0 && c.character.scaling.ultMult > 0) return ActionType.ULT

        // 5. 退回到普攻充能
        return ActionType.PASS
    }

    /** 获取单个属性对应的弱点集合（单元素自身） */
    private fun Element.getWeaknessSet(): Set<Element> = setOf(this)
}