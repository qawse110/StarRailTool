package com.mystarrail.tool.ui.components

import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Tag

/** 把崩铁 7 命途渲染为中文标签。 */
fun Path.label(): String = when (this) {
    Path.HUNT -> "巡猎"
    Path.ERUDITION -> "智识"
    Path.HARMONY -> "同谐"
    Path.DESTRUCTION -> "毁灭"
    Path.PRESERVATION -> "存护"
    Path.NIHILITY -> "虚无"
    Path.ABUNDANCE -> "丰饶"
}

/** 元素的中文标签 + emoji 简写。 */
fun Element.shortLabel(): String = when (this) {
    Element.PHYSICAL -> "物理"
    Element.FIRE -> "火"
    Element.ICE -> "冰"
    Element.LIGHTNING -> "雷"
    Element.WIND -> "风"
    Element.QUANTUM -> "量子"
    Element.IMAGINARY -> "虚数"
}

/** 角色的角色定位。 */
fun Role.label(): String = when (this) {
    Role.DPS -> "主C"
    Role.SUB_DPS -> "副C"
    Role.SUPPORT -> "辅助"
    Role.HEALER -> "治疗"
    Role.SHIELD -> "存护"
}

/** 角色标签的中文展示。 */
fun Tag.label(): String = when (this) {
    Tag.DOT -> "持续伤害"
    Tag.ULT_CHARGE -> "终结充能"
    Tag.ACTION_ADVANCE -> "拉条"
    Tag.SPEED_BOOST -> "速度加成"
    Tag.ATK_BOOST -> "攻击加成"
    Tag.CRIT_BOOST -> "暴击加成"
    Tag.DEBUFF -> "减益"
    Tag.SHIELD -> "护盾"
    Tag.HEAL -> "治疗"
    Tag.CLEANSE -> "净化"
    Tag.BREAK_EFFECT -> "击破特攻"
    Tag.FOLLOW_UP -> "追击"
    Tag.ULT_DMG_BONUS -> "终结增伤"
    Tag.ENERGY_REGEN -> "能量回复"
    Tag.SINGLE_TARGET -> "单体"
    Tag.AOE -> "群体"
    Tag.IMPULSE -> "多段"
    Tag.SUMMON -> "召唤物"
}