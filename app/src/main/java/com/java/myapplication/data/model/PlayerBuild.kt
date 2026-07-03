package com.java.myapplication.data.model

data class PlayerBuild(
    val id: Long = 0,
    val characterId: String,
    val level: Int = 80,
    val ascension: Int = 6,
    val lightConeId: String,
    val lightConeLevel: Int = 80,
    val lightConeSuperimposition: Int = 1,
    val relicSet4: String,
    val relicSet2: String? = null,
    val mainStats: MainStats,
    val subStats: List<SubStat>,
    val eidolons: Set<Int> = emptySet(),
    val notes: String = ""
)