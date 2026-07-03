package com.mystarrail.tool.data.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedRoot(
    val version: Int,
    val publishedAt: String,
    val characters: List<SeedCharacter>,
    val lightCones: List<SeedLightCone>,
    val relicSets: List<SeedRelicSet>,
    val enemies: List<SeedEnemy>,
    val scenarios: List<SeedScenario>,
    val eidolons: List<SeedEidolon>
)

@Serializable
data class SeedCharacter(
    val id: String, val name: String, val rarity: Int,
    val path: String, val element: String, val role: String,
    val tags: List<String>,
    val baseStats: SeedStats,
    val scaling: SeedScaling,
    val cycleProfile: SeedCycleProfile? = null,
    val iconUrl: String,
    val version: Int
)

@Serializable
data class SeedStats(val hp: Double, val atk: Double, val def: Double, val spd: Double)

@Serializable
data class SeedScaling(
    val skillMult: Double, val ultMult: Double, val talentMult: Double,
    val followUpMult: Double = 0.0, val aoeRatio: Double = 0.0
)

@Serializable
data class SeedCycleProfile(
    val cycleActions: Int,
    val spdBreakpoints: List<Double> = emptyList(),
    val isFollowUp: Boolean = false,
    val isDot: Boolean = false
)

@Serializable
data class SeedLightCone(
    val id: String, val name: String, val path: String, val rarity: Int,
    val passiveName: String,
    val passiveEffect: SeedPassiveEffect,
    val s5Multiplier: Double = 1.0
)

@Serializable
data class SeedPassiveEffect(
    val type: String,
    val stat: String? = null,
    val value: Double? = null,
    val target: String? = null,
    val multiplier: Double? = null,
    val condition: String? = null,
    val skillType: String? = null,
    val perTurn: Double? = null,
    val effects: List<SeedPassiveEffect> = emptyList()
)

@Serializable
data class SeedRelicSet(
    val id: String, val name: String,
    val twoPiece: SeedPassiveEffect,
    val fourPiece: SeedPassiveEffect,
    val suitableFor: List<String>
)

@Serializable
data class SeedEnemy(
    val id: String, val name: String, val count: Int,
    val weaknesses: List<String>, val type: String,
    val hp: Double, val toughness: Double = 0.0,
    val mechanics: List<String> = emptyList()
)

@Serializable
data class SeedScenario(
    val id: String, val name: String,
    val enemyIds: List<String>,
    val difficulty: Int, val notes: String = ""
)

@Serializable
data class SeedEidolon(
    val id: String, val characterId: String, val level: Int,
    val name: String,
    val effect: SeedEidolonEffect,
    val major: Boolean = false
)

@Serializable
data class SeedEidolonEffect(
    val type: String,
    val stat: String? = null,
    val value: Double? = null,
    val target: String? = null,
    val mechanic: String? = null,
    val param: Double? = null,
    val note: String? = null,
    val multiplier: Double? = null,
    val condition: String? = null,
    val skillType: String? = null,
    val effects: List<SeedEidolonEffect> = emptyList()
)