package com.java.myapplication.data.model

data class Enemy(
    val id: String,
    val name: String,
    val count: Int,
    val weaknesses: Set<Element>,
    val type: EnemyType,
    val hp: Double,
    val toughness: Double = 0.0,
    val mechanics: Set<String> = emptySet()
)