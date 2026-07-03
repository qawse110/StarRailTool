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