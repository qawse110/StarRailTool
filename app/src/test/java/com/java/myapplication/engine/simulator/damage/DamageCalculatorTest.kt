package com.java.myapplication.engine.simulator.damage

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.Enemy
import com.java.myapplication.data.model.EnemyType
import com.java.myapplication.data.model.Path
import com.java.myapplication.data.model.Role
import com.java.myapplication.data.model.Scaling
import com.java.myapplication.data.model.Stats
import com.java.myapplication.data.model.StatType
import com.java.myapplication.data.model.Tag
import com.java.myapplication.engine.simulator.buffs.Buff.DamageBonus
import com.java.myapplication.engine.simulator.buffs.Buff.EasyDmg
import com.java.myapplication.engine.simulator.buffs.Buff.StatBoost
import com.java.myapplication.engine.simulator.sim.ActionType
import com.java.myapplication.engine.simulator.tables.FormulaTables
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
}