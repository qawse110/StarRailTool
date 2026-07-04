package com.mystarrail.tool.engine.simulator.damage

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.engine.simulator.SkillTreeEffectParser
import com.mystarrail.tool.engine.simulator.buffs.Buff
import com.mystarrail.tool.engine.simulator.buffs.BuffEvaluator
import com.mystarrail.tool.engine.simulator.sim.ActionType
import com.mystarrail.tool.engine.simulator.tables.FormulaTables
import kotlin.math.max

class DamageCalculator(
    private val tables: FormulaTables,
    private val buffEval: BuffEvaluator = BuffEvaluator()
) {
    /**
     * 单次行动伤害期望（确定性，无随机）
     * 公式：攻击 × 倍率 × 暴击期望 × (1+增伤) × (1+易伤) × 弱点倍率 × (1-抗性) × 等级压制 × 防御减免
     */
    fun expectedDamage(
        character: Character,
        action: ActionType,
        enemy: Enemy,
        attackerLevel: Int = 80,
        enemyLevel: Int = 80,
        buffs: List<Buff> = emptyList(),
        debuffsOnEnemy: List<Buff> = emptyList()
    ): Double {
        val attackerBuffs = buffEval.evaluate(buffs)
        val debuffSnap = buffEval.evaluate(debuffsOnEnemy)

        val baseAtk = character.baseStats.atk
        val atk = baseAtk * (1 + attackerBuffs.atkBoost)
        val mult = multFor(action, character.scaling)
        val critRate = 0.5 + attackerBuffs.critRateBoost
        val critDmg = 1.0 + attackerBuffs.critDmgBoost
        val critExpect = 1.0 + critRate * critDmg

        val dmgBonusMul = 1.0 + attackerBuffs.damageBonus
        val easyDmgMul = 1.0 + debuffSnap.easyDmgTaken
        val weaknessMul = tables.weakness.multiplier(character.element, enemy.weaknesses)
        val res = tables.element.resist(character.element, character.element)
        val resMul = 1.0 - res
        val lvSuppress = tables.level.suppression(attackerLevel, enemyLevel)
        val def = enemy.hp * (if (enemy.type == EnemyType.BOSS) 0.05 else 0.03)
        val defMul = defenseMul(atk, def, debuffSnap.defShred)

        return atk * mult * critExpect * dmgBonusMul * easyDmgMul *
                weaknessMul * resMul * lvSuppress * defMul
    }

    private fun multFor(action: ActionType, s: Scaling): Double = when (action) {
        ActionType.SKILL -> s.skillMult
        ActionType.ULT -> s.ultMult
        ActionType.TALENT -> s.talentMult
        ActionType.FOLLOW_UP -> s.followUpMult
        ActionType.DOT, ActionType.PASS -> 0.0
    }

    private fun defenseMul(atk: Double, def: Double, shred: Double): Double {
        val effectiveDef = def * (1 - shred.coerceIn(0.0, 1.0))
        val denom = effectiveDef + atk * 10 + 200
        return atk * 10 / denom
    }

    /**
     * 角色单位价值（综合期望）
     */
    fun unitValue(
        character: Character,
        enemy: Enemy,
        buffs: List<Buff> = emptyList(),
        skillTree: SkillTree? = null
    ): CharacterUnitValue {
        // 行迹 → Buff 翻译
        val skillTreeBuffs: List<Buff> = skillTree?.let { st ->
            val effects = SkillTreeEffectParser.parse(st)
            val statBuffs = effects.statBoosts.map { (stat, value) ->
                Buff.StatBoost(
                    sourceId = "skilltree_${stat.name}",
                    duration = 999,
                    stat = stat,
                    value = value
                )
            }
            val dmgBuff = if (effects.damageBonus > 0.0) {
                listOf(
                    Buff.DamageBonus(
                        sourceId = "skilltree_dmg",
                        duration = 999,
                        multiplier = effects.damageBonus
                    )
                )
            } else emptyList()
            statBuffs + dmgBuff
        } ?: emptyList()
        val allBuffs = buffs + skillTreeBuffs

        val skill = expectedDamage(character, ActionType.SKILL, enemy, buffs = allBuffs)
        val ult = expectedDamage(character, ActionType.ULT, enemy, buffs = allBuffs)
        val talent = expectedDamage(character, ActionType.TALENT, enemy, buffs = allBuffs)
        val followUp = if (character.scaling.followUpMult > 0)
            expectedDamage(character, ActionType.FOLLOW_UP, enemy, buffs = allBuffs) else 0.0

        val spd = character.baseStats.spd
        val actionValue = tables.actionValue.advance(spd)
        val effectiveAV = 1.0 / actionValue * 10000.0

        val ultChargeRate = (skill * 0.1) / max(ult, 1.0)

        val supportValue = if (character.role == Role.SUPPORT) (skill + ult) * 0.3 else 0.0
        // B3: baseHealValue scales with healingBoost (1 + boost) * baseHeal
        val healValue = if (character.tags.contains(Tag.HEAL)) {
            val baseHeal = character.baseStats.atk
            val healerBuffs = buffEval.evaluate(allBuffs)
            baseHeal * (1 + healerBuffs.healingBoost) * 0.5
        } else 0.0
        // B4: baseShieldValue scales with shieldBoost (1 + boost) * baseShield
        val shieldValue = if (character.tags.contains(Tag.SHIELD)) {
            val baseShield = character.baseStats.def * 0.5
            val shielderBuffs = buffEval.evaluate(allBuffs)
            baseShield * (1 + shielderBuffs.shieldBoost)
        } else 0.0

        return CharacterUnitValue(
            expectedSkillDmg = skill,
            expectedUltDmg = ult,
            expectedTalentDmg = talent,
            expectedFollowUpDmg = followUp,
            dotDps = 0.0,
            effectiveActionValue = effectiveAV,
            ultChargeRate = ultChargeRate,
            baseSupportValue = supportValue,
            baseHealValue = healValue,
            baseShieldValue = shieldValue
        )
    }
}