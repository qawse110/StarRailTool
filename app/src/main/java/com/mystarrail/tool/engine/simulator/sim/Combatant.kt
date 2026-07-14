package com.mystarrail.tool.engine.simulator.sim

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Eidolon
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.engine.build.BuildEffectResolver
import com.mystarrail.tool.engine.simulator.buffs.Buff

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
    eidolons: Map<Int, Eidolon> = emptyMap(),
    build: PlayerBuild? = null,
    relicSet2: RelicSet? = null,
    eidolonList: List<Eidolon> = emptyList()
): Combatant {
    val resolvedStats = BuildEffectResolver.applyFlatStats(baseStats, build)
    val combatant = Combatant(
        character = this,
        stats = resolvedStats,
        lightCone = lightCone,
        relicSet = relicSet,
        eidolons = eidolons.ifEmpty {
            eidolonList.filter { it.level in (build?.eidolons ?: emptySet()) }
                .associateBy { it.level }
        },
        level = build?.level ?: 80,
        hp = resolvedStats.hp * 100
    )
    val buildBuffs = BuildEffectResolver.resolveBuffs(
        build = build,
        lightCone = lightCone,
        relicSet4 = relicSet,
        relicSet2 = relicSet2,
        eidolons = eidolonList.ifEmpty { eidolons.values.toList() }
    )
    combatant.buffs.addAll(buildBuffs)
    return combatant
}

fun com.mystarrail.tool.data.model.Enemy.toCombatant(): Combatant {
    val atk = hp * 0.05
    val def = hp * 0.03
    val stats = com.mystarrail.tool.data.model.Stats(hp, atk, def, 100.0)
    val placeholder = com.mystarrail.tool.data.model.Character(
        id = id, name = name, rarity = 0,
        path = com.mystarrail.tool.data.model.Path.DESTRUCTION,
        element = if (weaknesses.isNotEmpty()) weaknesses.first()
                   else com.mystarrail.tool.data.model.Element.PHYSICAL,
        role = com.mystarrail.tool.data.model.Role.DPS,
        tags = emptySet(),
        baseStats = stats,
        scaling = com.mystarrail.tool.data.model.Scaling(0.0, 0.0, 0.0, 0.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 0
    )
    return Combatant(
        character = placeholder,
        stats = stats,
        lightCone = null, relicSet = null, eidolons = emptyMap(),
        hp = hp
    )
}