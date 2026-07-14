package com.mystarrail.tool.engine.build

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.DmgCondition
import com.mystarrail.tool.data.model.Eidolon
import com.mystarrail.tool.data.model.EidolonEffect
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.MainStats
import com.mystarrail.tool.data.model.PassiveEffect
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.SubStat
import com.mystarrail.tool.engine.simulator.buffs.Buff

/**
 * 将 [PlayerBuild] / 光锥 / 遗器套装效果折算为战斗可用的属性与 Buff。
 *
 * 主词条数值采用遗器 +15 级常见终值（百分比以小数表示）。
 */
object BuildEffectResolver {

    /** 主词条 +15 终值（百分比类为小数；SPD 为固定速度）。 */
    fun mainStatValue(stat: StatType): Double = when (stat) {
        StatType.ATK, StatType.HP, StatType.EHR, StatType.EFFECT_RES -> 0.432
        StatType.DEF -> 0.540
        StatType.CRIT_RATE -> 0.324
        StatType.CRIT_DMG, StatType.BRK_EFF -> 0.648
        StatType.SPD -> 25.0
    }

    fun defaultMainStats(role: Role): MainStats = when (role) {
        Role.DPS, Role.SUB_DPS -> MainStats(
            body = StatType.CRIT_DMG,
            boots = StatType.SPD,
            sphere = StatType.ATK,
            rope = StatType.ATK
        )
        Role.SUPPORT -> MainStats(
            body = StatType.HP,
            boots = StatType.SPD,
            sphere = StatType.HP,
            rope = StatType.EHR
        )
        Role.HEALER, Role.SHIELD -> MainStats(
            body = StatType.HP,
            boots = StatType.SPD,
            sphere = StatType.HP,
            rope = StatType.HP
        )
    }

    fun defaultTargetSubs(role: Role): Set<StatType> = when (role) {
        Role.DPS, Role.SUB_DPS -> setOf(
            StatType.CRIT_RATE, StatType.CRIT_DMG, StatType.SPD, StatType.ATK
        )
        Role.SUPPORT -> setOf(StatType.SPD, StatType.HP, StatType.EHR, StatType.EFFECT_RES)
        Role.HEALER, Role.SHIELD -> setOf(StatType.SPD, StatType.HP, StatType.DEF, StatType.EFFECT_RES)
    }

    fun defaultBuild(character: Character, lightConeId: String = "", relicSet4: String = ""): PlayerBuild =
        PlayerBuild(
            characterId = character.id,
            lightConeId = lightConeId,
            relicSet4 = relicSet4,
            mainStats = defaultMainStats(character.role),
            subStats = emptyList()
        )

    /**
     * 解析配装带来的永久 Buff 列表（可直接喂给 DamageCalculator / Combatant）。
     */
    fun resolveBuffs(
        build: PlayerBuild?,
        lightCone: LightCone? = null,
        relicSet4: RelicSet? = null,
        relicSet2: RelicSet? = null,
        eidolons: List<Eidolon> = emptyList()
    ): List<Buff> {
        if (build == null && lightCone == null && relicSet4 == null && eidolons.isEmpty()) {
            return emptyList()
        }
        val buffs = mutableListOf<Buff>()

        if (build != null) {
            buffs += mainStatBuffs(build.mainStats)
            buffs += subStatBuffs(build.subStats)
        }

        lightCone?.let { cone ->
            val superimpose = (build?.lightConeSuperimposition ?: 1).coerceIn(1, 5)
            val scale = 1.0 + (cone.s5Multiplier - 1.0) * ((superimpose - 1) / 4.0)
            buffs += passiveToBuffs(cone.passiveEffect, "lc_${cone.id}", scale)
        }

        relicSet4?.let { set ->
            buffs += passiveToBuffs(set.twoPiece, "relic2_${set.id}", 1.0)
            buffs += passiveToBuffs(set.fourPiece, "relic4_${set.id}", 1.0)
        }
        // 双 2：第二套只吃 2 件效果
        if (relicSet2 != null && relicSet2.id != relicSet4?.id) {
            buffs += passiveToBuffs(relicSet2.twoPiece, "relic2b_${relicSet2.id}", 1.0)
        }

        val selected = build?.eidolons.orEmpty()
        eidolons.filter { it.level in selected }.forEach { e ->
            buffs += eidolonToBuffs(e.effect, "eidolon_${e.level}")
        }

        return buffs
    }

    /** 在基础面板上叠加速度等固定值（SPD 主/副词条）。 */
    fun applyFlatStats(base: Stats, build: PlayerBuild?): Stats {
        if (build == null) return base
        var spd = base.spd
        var atk = base.atk
        var hp = base.hp
        var def = base.def

        fun addFlat(stat: StatType, value: Double) {
            when (stat) {
                StatType.SPD -> spd += value
                // 固定 ATK/HP/DEF 较少见；百分比走 Buff，这里忽略
                else -> Unit
            }
        }

        listOf(build.mainStats.body, build.mainStats.boots, build.mainStats.sphere, build.mainStats.rope)
            .forEach { type ->
                if (type == StatType.SPD) addFlat(type, mainStatValue(type))
            }
        build.subStats.forEach { sub ->
            if (sub.type == StatType.SPD) addFlat(sub.type, sub.value)
        }
        return Stats(hp = hp, atk = atk, def = def, spd = spd)
    }

    private fun mainStatBuffs(main: MainStats): List<Buff> {
        val parts = listOf(
            "body" to main.body,
            "boots" to main.boots,
            "sphere" to main.sphere,
            "rope" to main.rope
        )
        return parts.flatMap { (slot, type) ->
            if (type == StatType.SPD) emptyList()
            else listOf(
                Buff.StatBoost(
                    sourceId = "main_$slot",
                    duration = 999,
                    stat = type,
                    value = mainStatValue(type)
                )
            ) + if (slot == "sphere" && type == StatType.ATK) {
                // 球常为属性伤害球：用 ATK 主词条时额外给一部分增伤近似
                listOf(
                    Buff.DamageBonus(
                        sourceId = "main_sphere_elem",
                        duration = 999,
                        multiplier = 0.3888
                    )
                )
            } else emptyList()
        }
    }

    private fun subStatBuffs(subs: List<SubStat>): List<Buff> =
        subs.mapNotNull { sub ->
            if (sub.type == StatType.SPD) return@mapNotNull null
            Buff.StatBoost(
                sourceId = "sub_${sub.type.name}",
                duration = 999,
                stat = sub.type,
                value = sub.value
            )
        }

    private fun passiveToBuffs(effect: PassiveEffect, sourceId: String, scale: Double): List<Buff> =
        when (effect) {
            is PassiveEffect.StatBoost -> listOf(
                Buff.StatBoost(
                    sourceId = sourceId,
                    duration = 999,
                    stat = effect.stat,
                    value = effect.value * scale
                )
            )
            is PassiveEffect.DamageBonus -> listOf(
                // 条件性增伤在简化模型里按 always 的 70% 期望计入
                Buff.DamageBonus(
                    sourceId = sourceId,
                    duration = 999,
                    multiplier = effect.multiplier * scale * conditionFactor(effect.condition)
                )
            )
            is PassiveEffect.SkillBoost -> listOf(
                Buff.DamageBonus(
                    sourceId = sourceId,
                    duration = 999,
                    multiplier = effect.multiplier * scale * 0.85
                )
            )
            is PassiveEffect.EnergyRegen -> listOf(
                Buff.UltCharge(
                    sourceId = sourceId,
                    duration = 999,
                    amount = effect.perTurn * scale
                )
            )
            is PassiveEffect.Composite ->
                effect.effects.flatMapIndexed { i, e ->
                    passiveToBuffs(e, "${sourceId}_$i", scale)
                }
        }

    private fun eidolonToBuffs(effect: EidolonEffect, sourceId: String): List<Buff> =
        when (effect) {
            is EidolonEffect.StatBoost -> listOf(
                Buff.StatBoost(
                    sourceId = sourceId,
                    duration = 999,
                    stat = effect.stat,
                    value = effect.value
                )
            )
            is EidolonEffect.DamageBonus -> listOf(
                Buff.DamageBonus(
                    sourceId = sourceId,
                    duration = 999,
                    multiplier = effect.multiplier * conditionFactor(effect.condition)
                )
            )
            is EidolonEffect.SkillBoost -> listOf(
                Buff.DamageBonus(
                    sourceId = sourceId,
                    duration = 999,
                    multiplier = effect.multiplier * 0.85
                )
            )
            is EidolonEffect.EnemyDebuff -> listOf(
                Buff.EasyDmg(
                    sourceId = sourceId,
                    duration = 999,
                    multiplier = effect.value.coerceAtLeast(0.0)
                )
            )
            is EidolonEffect.NewMechanic -> emptyList()
            is EidolonEffect.Composite ->
                effect.effects.flatMapIndexed { i, e ->
                    eidolonToBuffs(e, "${sourceId}_$i")
                }
        }

    private fun conditionFactor(condition: DmgCondition): Double = when (condition) {
        DmgCondition.ALWAYS -> 1.0
        DmgCondition.ULT_ACTIVE, DmgCondition.FOLLOW_UP, DmgCondition.DOT,
        DmgCondition.BREAK, DmgCondition.ULT_AFTER_SKILL, DmgCondition.AFTER_EAT_SP -> 0.7
    }
}
