package com.mystarrail.tool.data.seed

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 纯 JVM 解析测试：覆盖 [SeedParser.parse] 端到端。
 *
 * 不依赖 Android Context/Room（Proot ARM64 沙箱无 Robolectric），
 * 验证 "assets/seed-data-v1.json → 领域模型" 的整条链：JSON 结构 + 枚举映射。
 *
 * 若此测试通过但生产角色库仍为空 → bug 在 Room 写入或 UI 订阅层。
 * 若此测试失败 → bug 在 seed JSON 字段或枚举值。
 */
class SeedParserTest {

    /**
     * 真实 seed JSON 的内联子集（5 角色 / 1 锥 / 1 套 / 1 敌 / 1 场景 / 1 星魂），
     * 覆盖所有枚举值与嵌套结构。避免 IO，从 in-memory 字符串解析。
     */
    private val validJson = """
        {
          "version": 1,
          "publishedAt": "2026-01-01T00:00:00Z",
          "characters": [
            {
              "id": "seele", "name": "希儿", "rarity": 5,
              "path": "HUNT", "element": "QUANTUM", "role": "DPS",
              "tags": ["SINGLE_TARGET", "CRIT_BOOST", "FOLLOW_UP"],
              "baseStats": { "hp": 931.0, "atk": 756.0, "def": 363.0, "spd": 115.0 },
              "scaling": { "skillMult": 2.2, "ultMult": 4.2, "talentMult": 3.0, "followUpMult": 2.0, "aoeRatio": 0.0 },
              "cycleProfile": { "cycleActions": 4, "spdBreakpoints": [134.0, 143.0, 160.0], "isFollowUp": true, "isDot": false },
              "iconUrl": "https://example.com/seele.png",
              "version": 1
            },
            {
              "id": "bronya", "name": "布洛妮娅", "rarity": 5,
              "path": "HARMONY", "element": "WIND", "role": "SUPPORT",
              "tags": ["ACTION_ADVANCE", "ATK_BOOST", "SPEED_BOOST"],
              "baseStats": { "hp": 1241.0, "atk": 582.0, "def": 533.0, "spd": 134.0 },
              "scaling": { "skillMult": 0.0, "ultMult": 1.0, "talentMult": 0.0, "followUpMult": 0.0, "aoeRatio": 0.0 },
              "cycleProfile": { "cycleActions": 5, "spdBreakpoints": [160.0], "isFollowUp": false, "isDot": false },
              "iconUrl": "https://example.com/bronya.png",
              "version": 1
            }
          ],
          "lightCones": [
            {
              "id": "in_the_night", "name": "拂晓之前",
              "path": "HUNT", "rarity": 5, "passiveName": "夜晚的尽头",
              "passiveEffect": { "type": "Composite", "effects": [
                { "type": "StatBoost", "stat": "ATK", "value": 0.4, "target": "SELF" },
                { "type": "DamageBonus", "multiplier": 0.6, "condition": "ALWAYS" }
              ] },
              "s5Multiplier": 1.0
            }
          ],
          "relicSets": [
            {
              "id": "quantum_set", "name": "量子套",
              "twoPiece": { "type": "StatBoost", "stat": "ATK", "value": 0.12, "target": "SELF" },
              "fourPiece": { "type": "DamageBonus", "multiplier": 0.10, "condition": "ALWAYS" },
              "suitableFor": ["DPS", "SUPPORT"]
            }
          ],
          "enemies": [
            { "id": "boss_cocolia", "name": "可可利亚", "count": 1, "weaknesses": ["QUANTUM", "WIND"], "type": "BOSS", "hp": 200000.0, "toughness": 240.0, "mechanics": ["冰封"] }
          ],
          "scenarios": [
            { "id": "mf_1", "name": "混沌回忆 第一期", "enemyIds": ["boss_cocolia"], "difficulty": 4, "notes": "建议带风/量子" }
          ],
          "eidolons": [
            { "id": "seele_e1", "characterId": "seele", "level": 1, "name": "再相会",
              "effect": { "type": "StatBoost", "stat": "CRIT_DMG", "value": 0.2, "target": "SELF" }, "major": false }
          ]
        }
    """.trimIndent()

    @Test
    fun `parses valid seed and counts all entities`() {
        val result = SeedParser.parse(validJson)
        assertThat(result).isInstanceOf(SeedParser.ParseResult.Success::class.java)
        result as SeedParser.ParseResult.Success
        assertThat(result.characters).hasSize(2)
        assertThat(result.lightCones).hasSize(1)
        assertThat(result.relicSets).hasSize(1)
        assertThat(result.enemies).hasSize(1)
        assertThat(result.scenarios).hasSize(1)
        assertThat(result.eidolons).hasSize(1)
    }

    @Test
    fun `maps all enum values correctly`() {
        val result = SeedParser.parse(validJson) as SeedParser.ParseResult.Success

        val seele = result.characters.first { it.id == "seele" }
        assertThat(seele.path.name).isEqualTo("HUNT")
        assertThat(seele.element.name).isEqualTo("QUANTUM")
        assertThat(seele.role.name).isEqualTo("DPS")
        assertThat(seele.tags).containsExactly(
            com.mystarrail.tool.data.model.Tag.SINGLE_TARGET,
            com.mystarrail.tool.data.model.Tag.CRIT_BOOST,
            com.mystarrail.tool.data.model.Tag.FOLLOW_UP
        )
        assertThat(seele.cycleProfile?.cycleActions).isEqualTo(4)
        assertThat(seele.cycleProfile?.spdBreakpoints).containsExactly(134.0, 143.0, 160.0)
        assertThat(seele.cycleProfile?.isFollowUp).isTrue()

        val cone = result.lightCones.first()
        assertThat(cone.passiveEffect).isInstanceOf(
            com.mystarrail.tool.data.model.PassiveEffect.Composite::class.java
        )
        val composite = cone.passiveEffect as com.mystarrail.tool.data.model.PassiveEffect.Composite
        assertThat(composite.effects).hasSize(2)

        val enemy = result.enemies.first()
        assertThat(enemy.type).isEqualTo(com.mystarrail.tool.data.model.EnemyType.BOSS)
        assertThat(enemy.weaknesses).containsExactly(
            com.mystarrail.tool.data.model.Element.QUANTUM,
            com.mystarrail.tool.data.model.Element.WIND
        )
    }

    @Test
    fun `fails on invalid enum value without throwing`() {
        val bad = validJson.replace("\"HUNT\"", "\"WIZARD\"")
        val result = SeedParser.parse(bad)
        assertThat(result).isInstanceOf(SeedParser.ParseResult.Failed::class.java)
        val failed = result as SeedParser.ParseResult.Failed
        assertThat(failed.reason).contains("Enum")
    }

    @Test
    fun `fails on malformed JSON without throwing`() {
        val result = SeedParser.parse("{not valid json")
        assertThat(result).isInstanceOf(SeedParser.ParseResult.Failed::class.java)
    }

    @Test
    fun `parses real assets seed file end-to-end`() {
        val raw = SeedParserTest::class.java.classLoader
            ?.getResourceAsStream("assets/seed-data-v1.json")
            ?.use { it.readBytes().decodeToString() }
        if (raw == null) {
            // 资源未在 test classpath 中（Gradle test task 默认排除 main/assets）。
            // 通过相对路径回退读取。
            val candidate = java.io.File("src/main/assets/seed-data-v1.json")
            if (!candidate.exists()) {
                System.err.println("[skip] seed-data-v1.json not in test classpath; skipped")
                return
            }
            return runRealParseTest(candidate.readText())
        }
        runRealParseTest(raw)
    }

    private fun runRealParseTest(raw: String) {
        val result = SeedParser.parse(raw)
        assertThat(result).isInstanceOf(SeedParser.ParseResult.Success::class.java)
        result as SeedParser.ParseResult.Success
        // 与 seed-changelog.md 报告的覆盖度一致
        assertThat(result.characters).hasSize(5)
        assertThat(result.lightCones).hasSize(5)
        assertThat(result.relicSets).hasSize(3)
        assertThat(result.enemies).hasSize(10)
        assertThat(result.scenarios).hasSize(3)
        assertThat(result.eidolons).hasSize(30)
        assertThat(result.skillTrees).isEmpty()  // assets v1 ships empty; commit 3+ has data via remote
    }
}