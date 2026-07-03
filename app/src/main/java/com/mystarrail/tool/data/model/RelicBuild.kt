package com.mystarrail.tool.data.model

data class RelicBuild(
    val set4: String,
    val set2: String? = null,
    val mainStats: MainStats,
    val targetSubs: Set<StatType>,
    val notes: String = ""
)