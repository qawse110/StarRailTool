package com.java.myapplication.engine.simulator.tables

/**
 * 行动值：10000 / 速度 = 行动后回到 0 所需时间
 */
class ActionValueTable {
    fun advance(speed: Double): Double = 10000.0 / speed
}