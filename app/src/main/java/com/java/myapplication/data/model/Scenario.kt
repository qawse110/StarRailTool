package com.java.myapplication.data.model

data class Scenario(
    val id: String,
    val name: String,
    val enemies: List<Enemy>,
    val difficulty: Int,
    val notes: String = ""
)