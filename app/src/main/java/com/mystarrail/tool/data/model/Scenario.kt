package com.mystarrail.tool.data.model

data class Scenario(
    val id: String,
    val name: String,
    val enemies: List<Enemy>,
    val difficulty: Int,
    val notes: String = ""
)