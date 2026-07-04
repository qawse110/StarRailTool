package com.mystarrail.tool.engine.simulator.sim

import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.engine.simulator.buffs.Buff
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.rules.MechanicEngine
import com.mystarrail.tool.engine.simulator.tables.ActionValueTable
import kotlin.random.Random

class DiscreteEventSimulator(
    private val damageCalc: DamageCalculator,
    private val mechanics: MechanicEngine = MechanicEngine()
) {
    private val avTable = ActionValueTable()

    fun simulate(
        team: List<Combatant>,
        enemies: List<Combatant>,
        rounds: Int = 5
    ): SimulationResult {
        require(team.isNotEmpty()) { "team cannot be empty" }
        require(enemies.isNotEmpty()) { "enemies cannot be empty" }

        val log = mutableListOf<RoundEvent>()
        val ultsCast = team.associate { it.charId to 0 }.toMutableMap()
        val actions = team.associate { it.charId to 0 }.toMutableMap()
        val dmgByChar = team.associate { it.charId to 0.0 }.toMutableMap()
        val healingByChar = team.associate { it.charId to 0.0 }.toMutableMap()

        var roundsToKill: Int? = null
        var enemyKills = 0

        // 全局战技点
        var teamSP = 3.0

        // 初始化行动值
        team.forEach { c ->
            c.actionValue = avTable.advance(c.effectiveSpd) * Random.nextDouble(0.5, 1.0)
        }

        repeat(rounds) { roundNum ->
            if (enemies.all { it.isDead() }) {
                if (roundsToKill == null) roundsToKill = roundNum
                return@repeat
            }

            val order = team.filter { !it.isDead() }.sortedBy { it.actionValue }

            order.forEach { c ->
                if (c.isDead()) return@forEach
                if (enemies.all { it.isDead() }) return@repeat

                val avBefore = c.actionValue

                // 保存当前 SP 状态给 AI 决策使用
                c.sp = teamSP

                val action = AIDecision.decide(c, team, enemies)

                // 执行行动
                c.actionValue += avTable.advance(c.effectiveSpd)
                val avAfter = c.actionValue

                mechanics.onActionStart(c, team, enemies, log)

                if (action == ActionType.PASS) {
                    // 普攻：获得 1 SP，少量充能
                    teamSP = (teamSP + 1.0).coerceAtMost(5.0)
                    c.ultCharge = (c.ultCharge + 10.0).coerceAtMost(100.0)
                    actions[c.charId] = actions[c.charId]!! + 1
                    log.add(RoundEvent(
                        round = roundNum,
                        actorId = c.charId,
                        action = ActionType.PASS,
                        targets = emptyList(),
                        damageDealt = 0.0,
                        healingDone = 0.0,
                        buffsApplied = emptyList(),
                        mechanicsTriggered = emptyList(),
                        actionValueBefore = avBefore,
                        actionValueAfter = avAfter,
                        ultChargeBefore = c.ultCharge - 10.0,
                        ultChargeAfter = c.ultCharge
                    ))
                    mechanics.onActionEnd(c, team, enemies, log)
                    return@forEach
                }

                val ultBefore = c.ultCharge

                val target = enemies.firstOrNull { !it.isDead() }
                if (target == null) return@forEach

                // 构造 enemies 数组供 DamageCalculator
                val targetEnemy = Enemy(
                    id = target.character.id,
                    name = target.character.name,
                    count = 1,
                    weaknesses = setOf(target.character.element),
                    type = EnemyType.ELITE,
                    hp = target.hp,
                    toughness = 0.0
                )

                // 使用 DamageCalculator 计算真实伤害
                val isCrit = rollCrit(c, target)
                val dmg = damageCalc.expectedDamage(
                    character = c.character,
                    action = action,
                    enemy = targetEnemy,
                    attackerLevel = c.level,
                    enemyLevel = 80,
                    buffs = c.buffs.toList(),
                    debuffsOnEnemy = target.debuffs.toList()
                )

                val buffsAppliedThisAction = mutableListOf<String>()

                if (dmg > 0) {
                    target.hp -= dmg
                    dmgByChar[c.charId] = dmgByChar[c.charId]!! + dmg
                }

                if (target.isDead()) enemyKills++

                // SP 和能量管理
                when (action) {
                    ActionType.SKILL -> {
                        teamSP = (teamSP - 1.0).coerceAtLeast(0.0)
                        c.ultCharge = (c.ultCharge + 20.0).coerceAtMost(100.0)
                        // 治疗角色使用战技时记录治疗量
                        if (c.character.tags.contains(Tag.HEAL)) {
                            val heal = c.stats.atk * 0.3
                            healingByChar[c.charId] = healingByChar[c.charId]!! + heal
                        }
                        // 护盾角色使用战技时记录护盾
                        if (c.character.tags.contains(Tag.SHIELD)) {
                            val shield = c.stats.def * 0.4
                            // 记录护盾
                        }
                        buffsAppliedThisAction.add("SKILL:${c.charId}")
                    }
                    ActionType.ULT -> {
                        c.ultCharge = 0.0
                        ultsCast[c.charId] = ultsCast[c.charId]!! + 1
                        // 终结技：对所有存活敌人造成伤害
                        enemies.filter { !it.isDead() }.forEach { e ->
                            val ultDmg = damageCalc.expectedDamage(
                                character = c.character,
                                action = ActionType.ULT,
                                enemy = Enemy(
                                    id = e.character.id,
                                    name = e.character.name,
                                    count = 1,
                                    weaknesses = setOf(e.character.element),
                                    type = EnemyType.ELITE,
                                    hp = e.hp,
                                    toughness = 0.0
                                ),
                                attackerLevel = c.level,
                                enemyLevel = 80,
                                buffs = c.buffs.toList(),
                                debuffsOnEnemy = e.debuffs.toList()
                            )
                            if (ultDmg > 0) {
                                e.hp -= ultDmg
                                dmgByChar[c.charId] = dmgByChar[c.charId]!! + ultDmg
                                if (e.isDead()) enemyKills++
                            }
                        }
                        buffsAppliedThisAction.add("ULT:${c.charId}")
                    }
                    ActionType.TALENT -> {
                        c.ultCharge = (c.ultCharge + 15.0).coerceAtMost(100.0)
                    }
                    ActionType.FOLLOW_UP -> {
                        c.ultCharge = (c.ultCharge + 10.0).coerceAtMost(100.0)
                    }
                    else -> {}
                }

                // 追击检测 (仅对 SKILL/ULT 触发)
                if (action == ActionType.SKILL || action == ActionType.ULT) {
                    if (mechanics.onFollowUpCheck(c, team, enemies)) {
                        val aliveTarget = enemies.firstOrNull { !it.isDead() }
                        if (aliveTarget != null) {
                            val followUpDmg = damageCalc.expectedDamage(
                                character = c.character,
                                action = ActionType.FOLLOW_UP,
                                enemy = Enemy(
                                    id = aliveTarget.character.id,
                                    name = aliveTarget.character.name,
                                    count = 1,
                                    weaknesses = setOf(aliveTarget.character.element),
                                    type = EnemyType.ELITE,
                                    hp = aliveTarget.hp,
                                    toughness = 0.0
                                ),
                                attackerLevel = c.level,
                                enemyLevel = 80,
                                buffs = c.buffs.toList(),
                                debuffsOnEnemy = aliveTarget.debuffs.toList()
                            )
                            if (followUpDmg > 0 && !aliveTarget.isDead()) {
                                aliveTarget.hp -= followUpDmg
                                dmgByChar[c.charId] = dmgByChar[c.charId]!! + followUpDmg
                                if (aliveTarget.isDead()) enemyKills++
                                log.add(RoundEvent(
                                    round = roundNum,
                                    actorId = c.charId,
                                    action = ActionType.FOLLOW_UP,
                                    targets = listOf(TargetHit(
                                        targetId = aliveTarget.charId,
                                        element = c.character.element,
                                        damage = followUpDmg,
                                        isCrit = isCrit
                                    )),
                                    damageDealt = followUpDmg,
                                    healingDone = 0.0,
                                    buffsApplied = emptyList(),
                                    mechanicsTriggered = listOf(
                                        MechanicEvent(
                                            type = "FOLLOW_UP",
                                            source = c.charId,
                                            target = aliveTarget.charId,
                                            param = followUpDmg
                                        )
                                    ),
                                    actionValueBefore = avBefore,
                                    actionValueAfter = avAfter,
                                    ultChargeBefore = ultBefore,
                                    ultChargeAfter = c.ultCharge
                                ))
                            }
                        }
                    }
                }

                actions[c.charId] = actions[c.charId]!! + 1

                log.add(RoundEvent(
                    round = roundNum,
                    actorId = c.charId,
                    action = action,
                    targets = listOf(TargetHit(
                        targetId = target.charId,
                        element = c.character.element,
                        damage = dmg,
                        isCrit = isCrit
                    )),
                    damageDealt = dmg,
                    healingDone = healingByChar[c.charId] ?: 0.0,
                    buffsApplied = buffsAppliedThisAction,
                    mechanicsTriggered = emptyList(),
                    actionValueBefore = avBefore,
                    actionValueAfter = avAfter,
                    ultChargeBefore = ultBefore,
                    ultChargeAfter = c.ultCharge
                ))

                mechanics.onActionEnd(c, team, enemies, log)
            }

            mechanics.onRoundEnd(team, enemies, log)
        }

        return SimulationResult(
            log = log,
            totalDamage = dmgByChar,
            totalHealing = healingByChar,
            totalShielding = emptyMap(),
            totalBuffUptime = emptyMap(),
            enemyKills = enemyKills,
            roundsToKill = roundsToKill,
            ultsCast = ultsCast,
            actions = actions,
            damageBreakdown = DamageBreakdown(
                skillDmg = log.filter { it.action == ActionType.SKILL }.sumOf { it.damageDealt },
                ultDmg = log.filter { it.action == ActionType.ULT }.sumOf { it.damageDealt },
                followUpDmg = log.filter { it.action == ActionType.FOLLOW_UP }.sumOf { it.damageDealt },
                dotDmg = log.filter { it.action == ActionType.DOT }.sumOf { it.damageDealt }
            )
        )
    }

    /** 确定性暴击判定 */
    private fun rollCrit(c: Combatant, target: Combatant? = null): Boolean {
        val buffSnap = damageCalc.buffEval.evaluate(c.buffs.toList())
        val critRate = (0.5 + buffSnap.critRateBoost).coerceAtMost(1.0)
        return critRate >= 0.5
    }
}