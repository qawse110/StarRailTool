package com.mystarrail.tool.engine.simulator.sim

import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.EnemyType
import com.mystarrail.tool.engine.simulator.buffs.Buff
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.rules.MechanicEngine
import com.mystarrail.tool.engine.simulator.tables.ActionValueTable

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
                c.actionValue += avTable.advance(c.effectiveSpd)
                val avAfter = c.actionValue

                mechanics.onActionStart(c, team, enemies, log)

                val action = AIDecision.decide(c, team, enemies)
                if (action == ActionType.PASS) {
                    // 普攻充能：每次行动获得少量能量
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

                // 构造 enemies 数组供 DamageCalculator（仅需要 Enemy 模型信息）
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
                val isCrit = rollCrit(c)
                val dmg = damageCalc.expectedDamage(
                    character = c.character,
                    action = action,
                    enemy = targetEnemy,
                    attackerLevel = c.level,
                    enemyLevel = 80,
                    buffs = c.buffs.toList(),
                    debuffsOnEnemy = target.debuffs.toList()
                )

                if (dmg > 0) {
                    target.hp -= dmg
                    dmgByChar[c.charId] = dmgByChar[c.charId]!! + dmg
                }

                if (target.isDead()) enemyKills++

                // 终结技消耗全部能量
                if (action == ActionType.ULT) {
                    c.ultCharge = 0.0
                    ultsCast[c.charId] = ultsCast[c.charId]!! + 1
                } else {
                    // 非终结技行动获得充能（技能30，战技20）
                    val energyGain = when (action) {
                        ActionType.SKILL -> 20.0
                        ActionType.TALENT -> 15.0
                        ActionType.FOLLOW_UP -> 10.0
                        else -> 10.0
                    }
                    c.ultCharge = (c.ultCharge + energyGain).coerceAtMost(100.0)
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
                    healingDone = 0.0,
                    buffsApplied = emptyList(),
                    mechanicsTriggered = emptyList(),
                    actionValueBefore = avBefore,
                    actionValueAfter = avAfter,
                    ultChargeBefore = ultBefore,
                    ultChargeAfter = c.ultCharge
                ))

                // 检查追击触发
                if (mechanics.onFollowUpCheck(c, team, enemies)) {
                    val followUpDmg = damageCalc.expectedDamage(
                        character = c.character,
                        action = ActionType.FOLLOW_UP,
                        enemy = targetEnemy,
                        attackerLevel = c.level,
                        enemyLevel = 80,
                        buffs = c.buffs.toList(),
                        debuffsOnEnemy = target.debuffs.toList()
                    )
                    if (followUpDmg > 0 && !target.isDead()) {
                        target.hp -= followUpDmg
                        dmgByChar[c.charId] = dmgByChar[c.charId]!! + followUpDmg
                        if (target.isDead()) enemyKills++
                        log.add(RoundEvent(
                            round = roundNum,
                            actorId = c.charId,
                            action = ActionType.FOLLOW_UP,
                            targets = listOf(TargetHit(
                                targetId = target.charId,
                                element = c.character.element,
                                damage = followUpDmg,
                                isCrit = isCrit
                            )),
                            damageDealt = followUpDmg,
                            healingDone = 0.0,
                            buffsApplied = emptyList(),
                            mechanicsTriggered = listOf(
                                com.mystarrail.tool.engine.simulator.sim.MechanicEvent(
                                    type = "FOLLOW_UP",
                                    source = c.charId,
                                    target = target.charId,
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

    /** 确定性暴击判定（基于 buff 暴击率） */
    private fun rollCrit(c: Combatant): Boolean {
        val buffSnap = damageCalc.buffEval.evaluate(c.buffs.toList())
        val critRate = (0.5 + buffSnap.critRateBoost).coerceAtMost(1.0)
        return critRate >= 0.5 // 确定性：暴击率 >= 50% 时视为暴击
    }
}