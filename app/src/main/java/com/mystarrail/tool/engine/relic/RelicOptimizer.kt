package com.mystarrail.tool.engine.relic

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.MainStats
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.RelicBuild
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.SubStat
import com.mystarrail.tool.engine.build.BuildEffectResolver
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator

/**
 * 自动遗器调优：在套装库上搜索 4+2 / 主词条，用 unitValue 期望作为目标函数。
 */
class RelicOptimizer(
    private val damageCalc: DamageCalculator
) {

    data class Request(
        val character: Character,
        val relicSets: List<RelicSet>,
        val enemy: Enemy,
        val lightCone: LightCone? = null,
        val topN: Int = 5
    )

    data class Recommendation(
        val relicBuild: RelicBuild,
        val estimatedUnitScore: Double,
        val upliftVsBaseline: Double,
        val notes: String
    )

    fun optimize(request: Request): List<Recommendation> {
        val sets = request.relicSets
        if (sets.isEmpty()) return emptyList()

        val main = BuildEffectResolver.defaultMainStats(request.character.role)
        val targetSubs = BuildEffectResolver.defaultTargetSubs(request.character.role)
        val syntheticSubs = syntheticSubStats(request.character.role)

        val baselineBuild = BuildEffectResolver.defaultBuild(request.character)
        val baseline = unitPrimary(
            request.character,
            request.enemy,
            baselineBuild,
            lightCone = request.lightCone,
            set4 = null,
            set2 = null
        )

        val suitable = sets.filter { request.character.role in it.suitableFor }
            .ifEmpty { sets }
            .take(12)
        val ornamentPool = sets.take(10)

        val candidates = mutableListOf<Triple<RelicSet, RelicSet?, MainStats>>()
        for (set4 in suitable) {
            candidates += Triple(set4, null, main)
            // 双 2：主套 + 另一套 2 件
            for (set2 in ornamentPool) {
                if (set2.id == set4.id) continue
                candidates += Triple(set4, set2, main)
            }
        }

        // 主词条变体：鞋 ATK vs SPD（DPS）
        val mainVariants = mainStatVariants(request.character.role, main)

        val scored = mutableListOf<Recommendation>()
        for ((set4, set2, baseMain) in candidates.take(80)) {
            for (m in mainVariants) {
                val build = PlayerBuild(
                    characterId = request.character.id,
                    lightConeId = request.lightCone?.id.orEmpty(),
                    relicSet4 = set4.id,
                    relicSet2 = set2?.id,
                    mainStats = m,
                    subStats = syntheticSubs
                )
                val primary = unitPrimary(
                    request.character,
                    request.enemy,
                    build,
                    lightCone = request.lightCone,
                    set4 = set4,
                    set2 = set2
                )
                val uplift = if (baseline > 0) (primary - baseline) / baseline else 0.0
                val roleFit = if (request.character.role in set4.suitableFor) 1.05 else 1.0
                val adjusted = primary * roleFit
                scored += Recommendation(
                    relicBuild = RelicBuild(
                        set4 = set4.id,
                        set2 = set2?.id,
                        mainStats = m,
                        targetSubs = targetSubs,
                        notes = "${set4.name}" + (set2?.let { " + ${it.name}(2)" } ?: " (4件)")
                    ),
                    estimatedUnitScore = adjusted,
                    upliftVsBaseline = uplift,
                    notes = buildString {
                        append(set4.name)
                        if (set2 != null) append(" + ${set2.name} 2件")
                        append(" · 主词条 ")
                        append(listOf(m.body, m.boots, m.sphere, m.rope).joinToString("/") { it.name })
                    }
                )
            }
        }

        return scored
            .sortedByDescending { it.estimatedUnitScore }
            .distinctBy { "${it.relicBuild.set4}|${it.relicBuild.set2}|${it.relicBuild.mainStats}" }
            .take(request.topN)
    }

    /** 按 Role 的副词条权重（0..1），供 UI 手填评分复用。 */
    fun subStatWeights(role: Role): Map<StatType, Double> = when (role) {
        Role.DPS, Role.SUB_DPS -> mapOf(
            StatType.CRIT_DMG to 1.0,
            StatType.CRIT_RATE to 1.0,
            StatType.SPD to 0.8,
            StatType.ATK to 0.5,
            StatType.HP to 0.1,
            StatType.DEF to 0.1,
            StatType.EHR to 0.3,
            StatType.BRK_EFF to 0.4,
            StatType.EFFECT_RES to 0.1
        )
        Role.SUPPORT -> mapOf(
            StatType.SPD to 1.0,
            StatType.HP to 0.7,
            StatType.EHR to 0.6,
            StatType.BRK_EFF to 0.5,
            StatType.CRIT_RATE to 0.2,
            StatType.CRIT_DMG to 0.2,
            StatType.EFFECT_RES to 0.4,
            StatType.ATK to 0.2,
            StatType.DEF to 0.2
        )
        Role.HEALER, Role.SHIELD -> mapOf(
            StatType.SPD to 1.0,
            StatType.HP to 1.0,
            StatType.DEF to 0.7,
            StatType.EHR to 0.3,
            StatType.EFFECT_RES to 0.5,
            StatType.ATK to 0.1,
            StatType.CRIT_RATE to 0.1,
            StatType.CRIT_DMG to 0.1,
            StatType.BRK_EFF to 0.1
        )
    }

    fun scoreSubStats(subs: List<SubStat>, role: Role): Double {
        val weights = subStatWeights(role)
        if (subs.isEmpty()) return 0.0
        val maxWeight = weights.values.maxOrNull() ?: 1.0
        val raw = subs.sumOf { (weights[it.type] ?: 0.0) * it.value }
        val max = (subs.size * maxWeight * 10.0).coerceAtLeast(1.0)
        return (raw / max * 100.0).coerceIn(0.0, 100.0)
    }

    fun recommendMainStats(role: Role): MainStats =
        BuildEffectResolver.defaultMainStats(role)

    private fun mainStatVariants(role: Role, base: MainStats): List<MainStats> {
        return when (role) {
            Role.DPS, Role.SUB_DPS -> listOf(
                base,
                base.copy(boots = StatType.ATK),
                base.copy(body = StatType.CRIT_RATE),
                base.copy(rope = StatType.BRK_EFF)
            )
            Role.SUPPORT -> listOf(
                base,
                base.copy(body = StatType.EFFECT_RES),
                base.copy(rope = StatType.HP)
            )
            Role.HEALER, Role.SHIELD -> listOf(
                base,
                base.copy(body = StatType.DEF),
                base.copy(sphere = StatType.DEF)
            )
        }.distinct()
    }

    /** 用期望有效词条模拟一组副词条（非背包真实件）。 */
    private fun syntheticSubStats(role: Role): List<SubStat> {
        val weights = subStatWeights(role)
        return weights.entries
            .sortedByDescending { it.value }
            .take(4)
            .mapIndexed { index, (type, w) ->
                val value = when (type) {
                    StatType.SPD -> 8.0 + w * 8.0
                    StatType.CRIT_RATE -> 0.08 + w * 0.08
                    StatType.CRIT_DMG -> 0.12 + w * 0.16
                    StatType.ATK, StatType.HP, StatType.DEF -> 0.08 + w * 0.10
                    StatType.EHR, StatType.BRK_EFF, StatType.EFFECT_RES -> 0.08 + w * 0.12
                }
                SubStat(type = type, value = value, rolls = 3 + index % 2)
            }
    }

    private fun unitPrimary(
        character: Character,
        enemy: Enemy,
        build: PlayerBuild,
        lightCone: LightCone?,
        set4: RelicSet?,
        set2: RelicSet?
    ): Double {
        val buffs = BuildEffectResolver.resolveBuffs(
            build = build,
            lightCone = lightCone,
            relicSet4 = set4,
            relicSet2 = set2
        )
        val stats = BuildEffectResolver.applyFlatStats(character.baseStats, build)
        val uv = damageCalc.unitValue(
            character = character,
            enemy = enemy,
            buildBuffs = buffs,
            effectiveSpd = stats.spd,
            attackerLevel = build.level
        )
        return when (character.role) {
            Role.DPS, Role.SUB_DPS ->
                uv.expectedSkillDmg + uv.expectedUltDmg + uv.expectedTalentDmg +
                    uv.expectedFollowUpDmg + uv.dotDps * 3
            Role.HEALER -> uv.baseHealValue
            Role.SHIELD -> uv.baseShieldValue
            Role.SUPPORT -> uv.baseSupportValue + uv.ultChargeRate * 1000 + stats.spd * 10
        }
    }
}
