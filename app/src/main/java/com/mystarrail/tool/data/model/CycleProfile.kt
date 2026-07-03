package com.mystarrail.tool.data.model

data class CycleProfile(
    val cycleActions: Int,
    val spdBreakpoints: List<Double>,
    val isFollowUp: Boolean = false,
    val isDot: Boolean = false
)