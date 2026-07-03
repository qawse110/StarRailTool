package com.mystarrail.tool.data.model

data class BuildPreset(
    val id: String,
    val name: String,
    val characterId: String,
    val source: PresetSource,
    val build: PlayerBuild
)