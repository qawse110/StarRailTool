package com.java.myapplication.engine.simulator.sim

import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Eidolon
import com.java.myapplication.data.model.LightCone
import com.java.myapplication.data.model.RelicSet
import com.java.myapplication.data.model.Stats
import com.java.myapplication.engine.simulator.buffs.Buff

data class Combatant(
    val character: Character,
    val stats: Stats,
    val lightCone: LightCone?,
    val relicSet: RelicSet?,
    val eidolons: Map<Int, Eidolon>,
    val level: Int = 80,
    var hp: Double,
    var sp: Double = 3.0,
    var ultCharge: Double = 0.0,
    var actionValue: Double = 0.0,
    val buffs: MutableList<Buff> = mutableListOf(),
    val debuffs: MutableList<Buff> = mutableListOf()
) {
    fun isDead() = hp <= 0.0
    val effectiveSpd: Double
        get() = stats.spd * (1 + buffs.filterIsInstance<Buff.SpeedMod>().sumOf { it.value })
    val charId: String get() = character.id
}

fun Character.toCombatant(
    lightCone: LightCone? = null,
    relicSet: RelicSet? = null,
    eidolons: Map<Int, Eidolon> = emptyMap()
): Combatant = Combatant(
    character = this,
    stats = baseStats,
    lightCone = lightCone,
    relicSet = relicSet,
    eidolons = eidolons,
    hp = baseStats.hp * 100
)

fun com.java.myapplication.data.model.Enemy.toCombatant(): Combatant {
    val atk = hp * 0.05
    val def = hp * 0.03
    val stats = com.java.myapplication.data.model.Stats(hp, atk, def, 100.0)
    val placeholder = com.java.myapplication.data.model.Character(
        id = id, name = name, rarity = 0,
        path = com.java.myapplication.data.model.Path.DESTRUCTION,
        element = if (weaknesses.isNotEmpty()) weaknesses.first()
                   else com.java.myapplication.data.model.Element.PHYSICAL,
        role = com.java.myapplication.data.model.Role.DPS,
        tags = emptySet(),
        baseStats = stats,
        scaling = com.java.myapplication.data.model.Scaling(0.0, 0.0, 0.0, 0.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 0
    )
    return Combatant(
        character = placeholder,
        stats = stats,
        lightCone = null, relicSet = null, eidolons = emptyMap(),
        hp = hp
    )
}