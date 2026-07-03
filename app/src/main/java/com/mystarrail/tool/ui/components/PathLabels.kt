package com.mystarrail.tool.ui.components

import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.Role

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