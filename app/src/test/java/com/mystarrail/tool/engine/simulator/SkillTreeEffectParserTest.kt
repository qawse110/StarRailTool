package com.mystarrail.tool.engine.simulator

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.SkillTreeNode
import com.mystarrail.tool.data.model.StatType
import org.junit.Test

class SkillTreeEffectParserTest {

    @Test fun `null skill tree returns empty effects`() {
        val effects = SkillTreeEffectParser.parse(null)
        assertThat(effects.statBoosts).isEmpty()
        assertThat(effects.damageBonus).isEqualTo(0.0)
    }

    @Test fun `parses CRIT Rate from Chinese desc`() {
        val tree = SkillTree("x", listOf(node("暴击率提高 8%")))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects.statBoosts[StatType.CRIT_RATE]).isEqualTo(0.08)
    }

    @Test fun `parses ATK from English desc`() {
        val tree = SkillTree("x", listOf(node("ATK +10%")))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects.statBoosts[StatType.ATK]).isEqualTo(0.10)
    }

    @Test fun `parses DMG as damage bonus`() {
        val tree = SkillTree("x", listOf(node("终结技伤害提高 25%")))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects.damageBonus).isEqualTo(0.25)
    }

    @Test fun `parses HP from Max HP keyword`() {
        val tree = SkillTree("x", listOf(node("最大生命值提升 12%")))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects.statBoosts[StatType.HP]).isEqualTo(0.12)
    }

    @Test fun `aggregates multiple nodes`() {
        val tree = SkillTree("x", listOf(
            node("暴击率提高 8%"),
            node("暴击率提高 4%")
        ))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects.statBoosts[StatType.CRIT_RATE]).isEqualTo(0.12)
    }

    @Test fun `uses paramList when desc has no number`() {
        val tree = SkillTree("x", listOf(
            SkillTreeNode(id = "n1", name = "Buff", desc = "ATK Boost",
                          paramList = listOf(listOf(0.05), listOf(0.10)))
        ))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects.statBoosts[StatType.ATK]).isEqualTo(0.10)
    }

    @Test fun `unrecognized desc returns no effect`() {
        val tree = SkillTree("x", listOf(node("这是一个完全无关键词的描述")))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects.statBoosts).isEmpty()
        assertThat(effects.damageBonus).isEqualTo(0.0)
    }

    private fun node(desc: String) = SkillTreeNode(id = "n1", name = "Test", desc = desc)
}