package com.mystarrail.tool.ui.components

import com.mystarrail.tool.data.model.SkillType

/** 行迹分组的中文标签。UI 渲染时把 SkillType 枚举映射到显示文本。 */
fun SkillType.label(): String = when (this) {
    SkillType.SKILL -> "战技"
    SkillType.ULT -> "终结技"
    SkillType.TALENT -> "天赋"
    SkillType.FOLLOW_UP -> "追击"
    SkillType.DOT -> "持续伤害"
    SkillType.ALL -> "通用"
}
