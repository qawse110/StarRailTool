package com.java.myapplication.engine.simulator.tables

/**
 * 击破伤害系数：击破 = baseHp * 0.05
 */
class BreakTable {
    fun breakDamage(baseHp: Double): Double = baseHp * 0.05
}