package com.mystarrail.tool.engine.simulator.damage

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.engine.simulator.buffs.Buff.DamageBonus
import com.mystarrail.tool.engine.simulator.buffs.Buff.EasyDmg
import com.mystarrail.tool.engine.simulator.buffs.Buff.StatBoost
import com.mystarrail.tool.engine.simulator.sim.ActionType
import com.mystarrail.tool.engine.simulator.tables.FormulaTables
import org.junit.Test

class DamageCalculatorTest {
    private val calc = DamageCalculator(FormulaTables())

    private val seele = Character(
        id = "seele", name = "希儿", rarity = 5,
        path = Path.HUNT, element = Element.QUANTUM, role = Role.DPS,
        tags = setOf(Tag.SINGLE_TARGET, Tag.CRIT_BOOST, Tag.FOLLOW_UP),
        baseStats = Stats(931.0, 756.0, 363.0, 115.0),
        scaling = Scaling(2.2, 4.2, 3.0, 2.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 1
    )

    private val enemy = Enemy(
        id = "boss", name = "Boss", count = 1,
        weaknesses = setOf(Element.QUANTUM), type = EnemyType.BOSS,
        hp = 200000.0, toughness = 240.0
    )

    @Test fun `skill damage is positive and deterministic`() {
        val dmg = calc.expectedDamage(seele, ActionType.SKILL, enemy)
        assertThat(dmg).isGreaterThan(0.0)
        assertThat(calc.expectedDamage(seele, ActionType.SKILL, enemy)).isEqualTo(dmg)
    }

    @Test fun `ult damage higher than skill damage`() {
        val skill = calc.expectedDamage(seele, ActionType.SKILL, enemy)
        val ult = calc.expectedDamage(seele, ActionType.ULT, enemy)
        assertThat(ult).isGreaterThan(skill)
    }

    @Test fun `weakness hit deals more than non-weakness`() {
        val nonWeaknessEnemy = enemy.copy(weaknesses = setOf(Element.FIRE))
        val weaknessDmg = calc.expectedDamage(seele, ActionType.SKILL, enemy)
        val nonWeakDmg = calc.expectedDamage(seele, ActionType.SKILL, nonWeaknessEnemy)
        assertThat(weaknessDmg).isGreaterThan(nonWeakDmg)
    }

    @Test fun `damage bonus buff increases damage by 30 percent`() {
        val base = calc.expectedDamage(seele, ActionType.SKILL, enemy)
        val buffed = calc.expectedDamage(seele, ActionType.SKILL, enemy,
            buffs = listOf(DamageBonus("test", 1, 0.3)))
        assertThat(buffed / base).isWithin(0.01).of(1.3)
    }

    @Test fun `easy dmg debuff on enemy increases damage by 30 percent`() {
        val base = calc.expectedDamage(seele, ActionType.SKILL, enemy)
        val debuffed = calc.expectedDamage(seele, ActionType.SKILL, enemy,
            debuffsOnEnemy = listOf(EasyDmg("test", 1, 0.3)))
        assertThat(debuffed / base).isWithin(0.01).of(1.3)
    }

    @Test fun `ATK stat boost increases damage`() {
        val base = calc.expectedDamage(seele, ActionType.SKILL, enemy)
        val boosted = calc.expectedDamage(seele, ActionType.SKILL, enemy,
            buffs = listOf(StatBoost("test", 1, StatType.ATK, 0.5)))
        // ATK 进入 defenseMul 公式产生非线性放大（>1.5）
        assertThat(boosted).isGreaterThan(base * 1.5)
    }

    @Test fun `higher attacker level increases damage`() {
        val base = calc.expectedDamage(seele, ActionType.SKILL, enemy, attackerLevel = 80)
        val higher = calc.expectedDamage(seele, ActionType.SKILL, enemy, attackerLevel = 85)
        assertThat(higher).isGreaterThan(base)
    }

    @Test fun `unit value contains all expected fields`() {
        val uv = calc.unitValue(seele, enemy)
        assertThat(uv.expectedSkillDmg).isGreaterThan(0.0)
        assertThat(uv.expectedUltDmg).isGreaterThan(uv.expectedSkillDmg)
        assertThat(uv.expectedFollowUpDmg).isGreaterThan(0.0)
        assertThat(uv.effectiveActionValue).isGreaterThan(0.0)
    }

    @Test fun `support character has positive support value`() {
        val bronya = Character(
            id = "bronya", name = "布洛妮娅", rarity = 5,
            path = Path.HARMONY, element = Element.WIND, role = Role.SUPPORT,
            tags = setOf(Tag.ACTION_ADVANCE, Tag.ATK_BOOST),
            baseStats = Stats(1241.0, 582.0, 533.0, 134.0),
            scaling = Scaling(0.0, 1.0, 0.0, 0.0, 0.0),
            cycleProfile = null, iconUrl = "", version = 1
        )
        val uv = calc.unitValue(bronya, enemy)
        assertThat(uv.baseSupportValue).isGreaterThan(0.0)
    }

    // B7: critRate clamp
    @Test fun `critRate clamped to 1_0 when boost exceeds 0_5`() {
        val highCritBuffs = listOf(
            StatBoost("crit", 1, StatType.CRIT_RATE, 0.80)  // 50% + 80% = 130% (clamp to 100%)
        )
        val noCritBuffs = emptyList<com.mystarrail.tool.engine.simulator.buffs.Buff>()
        val dmgHigh = calc.expectedDamage(seele, ActionType.SKILL, enemy, buffs = highCritBuffs)
        val dmgNo = calc.expectedDamage(seele, ActionType.SKILL, enemy, buffs = noCritBuffs)
        // 100% 暴击期望伤害应高于 50% 暴击
        assertThat(dmgHigh).isGreaterThan(dmgNo)
        // 上限：1 + 1*1.5 = 2.5 倍，不应超过 3 倍（容差 0.5 留给非暴击因素）
        assertThat(dmgHigh).isAtMost(dmgNo * 3.0)
    }

    // B1: DOT wiring
    @Test fun `dotDps non-zero for character with dotMult`() {
        val dotChar = Character(
            id = "kafka", name = "卡芙卡", rarity = 5,
            path = Path.NIHILITY, element = Element.LIGHTNING, role = Role.DPS,
            tags = setOf(Tag.DOT),
            baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
            scaling = Scaling(skillMult = 2.0, ultMult = 3.0, talentMult = 1.0,
                              followUpMult = 0.0, aoeRatio = 0.0, dotMult = 1.5),
            cycleProfile = null, iconUrl = "", version = 1
        )
        val uv = calc.unitValue(dotChar, enemy)
        assertThat(uv.dotDps).isGreaterThan(0.0)
    }

    @Test fun `dotDps zero for character without dotMult`() {
        val uv = calc.unitValue(seele, enemy)
        assertThat(uv.dotDps).isEqualTo(0.0)
    }
}