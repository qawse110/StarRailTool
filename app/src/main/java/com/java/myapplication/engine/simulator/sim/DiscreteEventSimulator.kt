package com.java.myapplication.engine.simulator.sim

import com.java.myapplication.engine.simulator.damage.DamageCalculator
import com.java.myapplication.engine.simulator.rules.MechanicEngine
import com.java.myapplication.engine.simulator.tables.ActionValueTable

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
                    mechanics.onActionEnd(c, team, enemies, log)
                    return@forEach
                }

                val ultBefore = c.ultCharge

                val target = enemies.firstOrNull { !it.isDead() }
                if (target == null) return@forEach
                val mult = when (action) {
                    ActionType.SKILL -> c.character.scaling.skillMult
                    ActionType.ULT -> c.character.scaling.ultMult
                    ActionType.TALENT -> c.character.scaling.talentMult
                    ActionType.FOLLOW_UP -> c.character.scaling.followUpMult
                    else -> 0.0
                }
                val atk = c.stats.atk
                val dmg = atk * mult * 0.5

                if (dmg > 0) {
                    target.hp -= dmg
                    dmgByChar[c.charId] = dmgByChar[c.charId]!! + dmg
                }

                if (target.isDead()) enemyKills++

                if (action == ActionType.ULT) {
                    c.ultCharge = 0.0
                    ultsCast[c.charId] = ultsCast[c.charId]!! + 1
                }
                actions[c.charId] = actions[c.charId]!! + 1

                log.add(RoundEvent(
                    round = roundNum,
                    actorId = c.charId,
                    action = action,
                    targets = listOf(TargetHit(
                        targetId = target.charId,
                        element = c.character.element,
                        damage = dmg, isCrit = false
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

                mechanics.onActionEnd(c, team, enemies, log)
            }

            mechanics.onRoundEnd(team, enemies, log)
        }

        return SimulationResult(
            log = log,
            totalDamage = dmgByChar,
            totalHealing = emptyMap(),
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
}