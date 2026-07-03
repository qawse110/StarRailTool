package com.mystarrail.tool.engine.simulator.tables

import com.mystarrail.tool.data.model.Element

/**
 * 元素抗性表。0 = 无抗性，0.2 = 20% 抗性（受 80% 伤害）
 */
class ElementTable {
    private val defaultResist = 0.20

    @Suppress("UNUSED_PARAMETER")
    fun resist(enemy: Element, vs: Element): Double = defaultResist
}