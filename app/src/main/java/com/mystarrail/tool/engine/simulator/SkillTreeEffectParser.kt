package com.mystarrail.tool.engine.simulator

import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.SkillTreeNode
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.seed.remote.KeywordEffect
import com.mystarrail.tool.data.seed.remote.KeywordMatcher

/** 解析结果：stat 加成 map + 增伤总和（百分比小数）。 */
data class SkillTreeEffects(
    val statBoosts: Map<StatType, Double> = emptyMap(),
    val damageBonus: Double = 0.0
) {
    fun isEmpty(): Boolean = statBoosts.isEmpty() && damageBonus == 0.0
}

/**
 * 把 SkillTree 节点的 desc 解析为加成数据。
 *
 * 复用 [KeywordMatcher] 中英文 11 组双语关键词。
 *
 * 设计：
 *  - 一般 stat (ATK/HP/CRIT_RATE/...) → `statBoosts[StatType]`
 *  - DMG 增伤 → 单独累计到 `damageBonus`（不挂在 StatType 上，因为：
 *    a) `StatType` 不含 `DAMAGE_BONUS`；
 *    b) `BuffEvaluator.applyStat` 不识别 DAMAGE_BONUS，
 *    转 Buff 时必须用 `Buff.DamageBonus` 单独的 case）
 */
object SkillTreeEffectParser {

    private val PERCENT_REGEX = Regex("""([+\-]?\d+(?:\.\d+)?)\s*%""")

    fun parse(skillTree: SkillTree?): SkillTreeEffects {
        if (skillTree == null || skillTree.nodes.isEmpty()) return SkillTreeEffects()
        val statBoosts = mutableMapOf<StatType, Double>()
        var damageBonus = 0.0
        for (node in skillTree.nodes) {
            val effect = KeywordMatcher.infer(node.desc, 0.0) ?: continue
            val value = node.extractValue() ?: continue
            when (effect) {
                is KeywordEffect.StatBoost -> {
                    statBoosts[effect.stat] = (statBoosts[effect.stat] ?: 0.0) + value
                }
                is KeywordEffect.DamageBonus -> {
                    damageBonus += value
                }
            }
        }
        return SkillTreeEffects(statBoosts = statBoosts, damageBonus = damageBonus)
    }

    private fun SkillTreeNode.extractValue(): Double? {
        // 优先从 desc 里找百分比（+8% / 暴击率提高 8%）
        // 抽到的是整数百分比，除以 100 转小数
        PERCENT_REGEX.find(desc)?.groupValues?.get(1)?.toDoubleOrNull()?.let {
            return it / 100.0
        }
        // fallback: paramList 最后一级第一个值
        // 来自 Mar-7th 远程源，**已经是小数**（0.10 = 10%），直接使用
        return paramList.lastOrNull()?.firstOrNull()
    }
}