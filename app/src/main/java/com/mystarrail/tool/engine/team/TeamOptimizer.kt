package com.mystarrail.tool.engine.team

import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.data.model.TeamScore
import com.mystarrail.tool.engine.simulator.GearLookup
import com.mystarrail.tool.engine.simulator.ScoringEngine

/**
 * 自动配队优化：Role 剪枝 + 弱点/标签启发式粗排 + DES 精排。
 */
class TeamOptimizer(
    private val scoringEngine: ScoringEngine
) {

    data class Request(
        val pool: List<Character>,
        val enemy: Enemy,
        val lockedIds: Set<String> = emptySet(),
        val builds: Map<String, PlayerBuild> = emptyMap(),
        val gearLookup: GearLookup = GearLookup.Empty,
        val topK: Int = 5,
        /** 粗排后送入 DES 的候选上限 */
        val simulateLimit: Int = 24
    )

    data class Recommendation(
        val team: List<Character>,
        val teamScore: TeamScore,
        val reasons: List<String>
    )

    fun optimize(request: Request): List<Recommendation> {
        val pool = request.pool
        if (pool.size < 4) return emptyList()

        val locked = pool.filter { it.id in request.lockedIds }
        if (locked.size > 4) return emptyList()
        if (locked.any { it.id !in pool.map { c -> c.id }.toSet() }) return emptyList()

        val free = pool.filter { it.id !in request.lockedIds }
        val need = 4 - locked.size
        if (free.size < need) return emptyList()

        val weaknesses = request.enemy.weaknesses
        val candidates = generateCandidates(locked, free, need, weaknesses)
            .asSequence()
            .map { team -> team to heuristicScore(team, weaknesses) }
            .sortedByDescending { it.second }
            .take(request.simulateLimit.coerceAtLeast(request.topK))
            .map { it.first }
            .toList()

        return candidates.map { team ->
            val score = scoringEngine.scoreTeam(
                team = team,
                enemy = request.enemy,
                builds = request.builds,
                gearLookup = request.gearLookup
            )
            Recommendation(
                team = team,
                teamScore = score,
                reasons = reasonsFor(team, weaknesses, score)
            )
        }.sortedByDescending { it.teamScore.score }
            .take(request.topK)
    }

    private fun generateCandidates(
        locked: List<Character>,
        free: List<Character>,
        need: Int,
        weaknesses: Set<Element>
    ): List<List<Character>> {
        if (need == 0) return listOf(locked)

        // 按「主C / 生存 / 辅助」优先填充，避免 4 主C
        val dps = free.filter { it.role == Role.DPS || it.role == Role.SUB_DPS }
            .sortedWith(compareByDescending<Character> { it.rarity }
                .thenByDescending { weaknessBonus(it, weaknesses) })
        val sustain = free.filter { it.role == Role.HEALER || it.role == Role.SHIELD }
            .sortedByDescending { it.rarity }
        val support = free.filter { it.role == Role.SUPPORT }
            .sortedWith(compareByDescending<Character> { it.rarity }
                .thenByDescending { tagSynergy(it) })
        val rest = free.sortedWith(
            compareByDescending<Character> { it.rarity }
                .thenByDescending { weaknessBonus(it, weaknesses) }
        )

        val seeds = mutableListOf<List<Character>>()
        val hasDpsLocked = locked.any { it.role == Role.DPS || it.role == Role.SUB_DPS }
        val hasSustainLocked = locked.any { it.role == Role.HEALER || it.role == Role.SHIELD }

        val dpsPool = if (hasDpsLocked) emptyList() else dps.take(6)
        val sustainPool = if (hasSustainLocked) emptyList() else sustain.take(4)
        val supportPool = support.take(8)

        fun combos(from: List<Character>, k: Int): List<List<Character>> {
            if (k <= 0) return listOf(emptyList())
            if (from.size < k) return emptyList()
            val out = mutableListOf<List<Character>>()
            fun rec(start: Int, acc: MutableList<Character>) {
                if (acc.size == k) {
                    out += acc.toList()
                    return
                }
                for (i in start until from.size) {
                    acc += from[i]
                    rec(i + 1, acc)
                    acc.removeAt(acc.lastIndex)
                    if (out.size > 80) return
                }
            }
            rec(0, mutableListOf())
            return out
        }

        // 模板：1 DPS + 1 sustain + 2 support（可随 locked 缩减）
        val roleTemplates = listOf(
            listOf("dps", "sustain", "support", "support"),
            listOf("dps", "support", "support", "support"),
            listOf("dps", "dps", "sustain", "support"),
            listOf("any", "any", "any", "any")
        )

        for (template in roleTemplates) {
            val remainingRoles = template.drop(locked.size).take(need)
            // 简化：从对应池取组合
            val poolsForSlots = remainingRoles.map { role ->
                when (role) {
                    "dps" -> if (dpsPool.isNotEmpty()) dpsPool else rest
                    "sustain" -> if (sustainPool.isNotEmpty()) sustainPool else rest
                    "support" -> if (supportPool.isNotEmpty()) supportPool else rest
                    else -> rest
                }
            }
            // 用并集再取 need 人组合，避免交叉笛卡尔爆炸
            val union = poolsForSlots.flatten().distinctBy { it.id }.take(14)
            for (pick in combos(union, need)) {
                val team = locked + pick
                if (team.map { it.id }.toSet().size == 4) {
                    seeds += team
                }
                if (seeds.size >= 120) break
            }
            if (seeds.size >= 120) break
        }

        if (seeds.isEmpty()) {
            // 兜底：稀有度最高的 4 人组合若干
            for (pick in combos(rest.take(10), need)) {
                seeds += locked + pick
            }
        }
        return seeds.distinctBy { t -> t.map { it.id }.sorted().joinToString() }
    }

    private fun heuristicScore(team: List<Character>, weaknesses: Set<Element>): Double {
        var score = 0.0
        val roles = team.map { it.role }
        if (roles.any { it == Role.DPS || it == Role.SUB_DPS }) score += 30
        if (roles.any { it == Role.HEALER || it == Role.SHIELD }) score += 20
        if (roles.count { it == Role.SUPPORT } >= 1) score += 15
        if (roles.count { it == Role.DPS } >= 3) score -= 15

        score += team.sumOf { weaknessBonus(it, weaknesses) * 12.0 }
        score += team.sumOf { it.rarity.toDouble() * 3.0 }
        score += team.flatMap { it.tags }.toSet().size * 2.0
        // 标签协同
        val tags = team.flatMap { it.tags }.toSet()
        if (Tag.DOT in tags && roles.any { it == Role.DPS || it == Role.SUB_DPS }) score += 8
        if (Tag.FOLLOW_UP in tags && Tag.CRIT_BOOST in tags) score += 6
        if (Tag.ACTION_ADVANCE in tags) score += 5
        return score
    }

    private fun weaknessBonus(c: Character, weaknesses: Set<Element>): Int =
        if (c.element in weaknesses) 1 else 0

    private fun tagSynergy(c: Character): Int =
        c.tags.count {
            it in setOf(
                Tag.ATK_BOOST, Tag.CRIT_BOOST, Tag.ACTION_ADVANCE,
                Tag.SPEED_BOOST, Tag.ULT_CHARGE, Tag.ENERGY_REGEN
            )
        }

    private fun reasonsFor(
        team: List<Character>,
        weaknesses: Set<Element>,
        score: TeamScore
    ): List<String> {
        val reasons = mutableListOf<String>()
        val weakHit = team.count { it.element in weaknesses }
        if (weakHit > 0) reasons += "弱点覆盖 $weakHit/4"
        val roles = team.groupBy { it.role }.mapValues { it.value.size }
        reasons += "定位: " + roles.entries.joinToString { "${it.key.name}×${it.value}" }
        if (score.roundsToKill != null) reasons += "预计 ${score.roundsToKill} 回合击杀"
        else reasons += "未在时限内击杀"
        reasons += "模拟总分 ${"%.1f".format(score.score)}"
        val tags = team.flatMap { it.tags }.toSet()
        if (tags.isNotEmpty()) {
            reasons += "机制: " + tags.take(4).joinToString { it.name }
        }
        return reasons
    }
}
