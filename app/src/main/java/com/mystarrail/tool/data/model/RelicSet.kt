package com.mystarrail.tool.data.model

data class RelicSet(
    val id: String,
    val name: String,
    val twoPiece: PassiveEffect,
    val fourPiece: PassiveEffect,
    val suitableFor: Set<Role>
)