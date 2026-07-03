package com.java.myapplication.engine.simulator.tables

/**
 * 等级压制表：同等级 = 1.0，每差 1 级修正 2%，最高 10%
 */
class LevelTable {
    private val suppressPerLevel = 0.02
    private val maxSuppress = 0.10

    fun suppression(attackerLevel: Int, defenderLevel: Int): Double {
        val diff = (attackerLevel - defenderLevel).coerceIn(-5, 5)
        return 1.0 + (diff * suppressPerLevel).coerceIn(-maxSuppress, maxSuppress)
    }
}