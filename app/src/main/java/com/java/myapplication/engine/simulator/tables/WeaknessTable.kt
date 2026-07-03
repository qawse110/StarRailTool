package com.java.myapplication.engine.simulator.tables

import com.java.myapplication.data.model.Element

/**
 * 弱点命中倍率：命中弱点 = 1.0，无弱点 = 0.5
 */
class WeaknessTable {
    fun multiplier(attacker: Element, weaknesses: Set<Element>): Double =
        if (attacker in weaknesses) 1.0 else 0.5
}