package com.java.myapplication.data.model

data class Eidolon(
    val id: String,
    val characterId: String,
    val level: Int,
    val name: String,
    val effect: EidolonEffect,
    val major: Boolean = false
)