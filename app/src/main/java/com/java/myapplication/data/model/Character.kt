package com.java.myapplication.data.model

data class Character(
    val id: String,
    val name: String,
    val rarity: Int,
    val path: Path,
    val element: Element,
    val role: Role,
    val tags: Set<Tag>,
    val baseStats: Stats,
    val scaling: Scaling,
    val cycleProfile: CycleProfile?,
    val iconUrl: String,
    val version: Int
)