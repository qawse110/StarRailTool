package com.mystarrail.tool.data.seed.remote

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.Element
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Test

/**
 * 单元测试：Mar7thToSeedTransformer 端到端。
 *
 * 关键覆盖：
 *  - 角色基础信息 + 数值 + 命途/元素映射
 *  - 跨文件 ID 引用 join（ranks + skills）
 *  - 技能倍率提取（取满级 params）
 *  - 边界：未知命途/元素降级
 *  - 整个流程不崩
 */
class Mar7thToSeedTransformerTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(text: String): JsonElement = json.parseToJsonElement(text)

    /**
     * 内联 fixture：1 个完整角色（希儿）+ 1 个光锥 + 1 个遗器套 + 1 个星魂。
     * 覆盖路径名映射、跨文件 join、技能倍率提取。
     */
    private val charactersJson = """
        {
          "1001": {
            "id": "1001",
            "name": "Seele",
            "rarity": 5,
            "path": "Hunt",
            "element": "Quantum",
            "skills": ["100101", "100102", "100103", "100104"],
            "ranks": ["100101", "100102"]
          }
        }
    """.trimIndent()

    private val promotionsJson = """
        {
          "1001": {
            "values": [
              {
                "hp": { "base": 931.0 },
                "atk": { "base": 756.0 },
                "def": { "base": 363.0 },
                "spd": { "base": 115.0 }
              }
            ]
          }
        }
    """.trimIndent()

    private val skillsJson = """
        {
          "100101": { "id": "100101", "name": "Basic ATK", "type": "Normal", "effect": "SingleAttack",
                      "params": [[0.5], [1.0]] },
          "100102": { "id": "100102", "name": "Skill", "type": "Skill", "effect": "SingleAttack",
                      "params": [[1.5], [2.2]] },
          "100103": { "id": "100103", "name": "Ultimate", "type": "Ultra", "effect": "SingleAttack",
                      "params": [[3.0], [4.2]] },
          "100104": { "id": "100104", "name": "Talent", "type": "Talent", "effect": "SingleAttack",
                      "params": [[2.0], [3.0]] }
        }
    """.trimIndent()

    private val ranksJson = """
        {
          "100101": { "id": "100101", "name": "E1 Power", "rank": 1,
                      "desc": "Increases CRIT Rate by 12%.",
                      "params": [[0.12]] },
          "100102": { "id": "100102", "name": "E2 Major", "rank": 2,
                      "desc": "Ultimate DMG +25%.",
                      "params": [[0.25]] }
        }
    """.trimIndent()

    private val lightConesJson = """
        {
          "20000": { "id": "20000", "name": "In the Night", "rarity": 5, "path": "Hunt" }
        }
    """.trimIndent()

    private val conePromotionsJson = """
        {
          "20000": { "values": [{ "hp": {"base": 1058.0}, "atk": {"base": 635.0}, "def": {"base": 463.0}, "spd": {"base": 0.0} }] }
        }
    """.trimIndent()

    private val coneRanksJson = """
        {
          "20000": { "id": "20000", "skill": "Power",
                     "desc": "Increases CRIT Rate by 12%.",
                     "params": [[0.12]] }
        }
    """.trimIndent()

    private val relicSetsJson = """
        {
          "101": { "id": "101", "name": "Test Set",
                   "desc": ["2pc: ATK +12%", "4pc: ATK +20%"],
                   "properties": [[{"type": "AttackDelta", "value": 0.12}], [{"type": "AttackDelta", "value": 0.20}]] }
        }
    """.trimIndent()

    private fun buildFiles() = mapOf(
        RemoteSeedSource.File.CHARACTERS to parse(charactersJson),
        RemoteSeedSource.File.CHARACTER_PROMOTIONS to parse(promotionsJson),
        RemoteSeedSource.File.CHARACTER_SKILLS to parse(skillsJson),
        RemoteSeedSource.File.CHARACTER_RANKS to parse(ranksJson),
        RemoteSeedSource.File.LIGHT_CONES to parse(lightConesJson),
        RemoteSeedSource.File.LIGHT_CONE_PROMOTIONS to parse(conePromotionsJson),
        RemoteSeedSource.File.LIGHT_CONE_RANKS to parse(coneRanksJson),
        RemoteSeedSource.File.RELIC_SETS to parse(relicSetsJson)
    )

    @Test
    fun `transforms character with correct basic info`() {
        val result = Mar7thToSeedTransformer.transform(buildFiles())
        assertThat(result.characters).hasSize(1)
        val seele = result.characters.first()
        assertThat(seele.id).isEqualTo("mar7th_1001")
        assertThat(seele.name).isEqualTo("Seele")
        assertThat(seele.rarity).isEqualTo(5)
        assertThat(seele.path).isEqualTo(Path.HUNT)
        assertThat(seele.element).isEqualTo(Element.QUANTUM)
    }

    @Test
    fun `transforms base stats from character_promotions`() {
        val result = Mar7thToSeedTransformer.transform(buildFiles())
        val stats = result.characters.first().baseStats
        assertThat(stats.hp).isEqualTo(931.0)
        assertThat(stats.atk).isEqualTo(756.0)
        assertThat(stats.def).isEqualTo(363.0)
        assertThat(stats.spd).isEqualTo(115.0)
    }

    @Test
    fun `extracts max-level skill multipliers`() {
        val result = Mar7thToSeedTransformer.transform(buildFiles())
        val scaling = result.characters.first().scaling
        // params 最后一个非空数组的第一个 = 满级倍率
        assertThat(scaling.skillMult).isEqualTo(2.2)
        assertThat(scaling.ultMult).isEqualTo(4.2)
        assertThat(scaling.talentMult).isEqualTo(3.0)
    }

    @Test
    fun `joins eidolons across files via rank ids`() {
        val result = Mar7thToSeedTransformer.transform(buildFiles())
        assertThat(result.eidolons).hasSize(2)
        val e1 = result.eidolons.first { it.level == 1 }
        assertThat(e1.characterId).isEqualTo("mar7th_1001")
        assertThat(e1.name).isEqualTo("E1 Power")
        val e2 = result.eidolons.first { it.level == 2 }
        assertThat(e2.major).isTrue()  // rank 2 = major
    }

    @Test
    fun `infers eidolon effect type from desc keyword`() {
        val result = Mar7thToSeedTransformer.transform(buildFiles())
        val e1 = result.eidolons.first { it.level == 1 }
        // "CRIT Rate" + value 0.12 → StatBoost(CRIT_RATE, 0.12)
        assertThat(e1.effect).isInstanceOf(
            com.mystarrail.tool.data.model.EidolonEffect.StatBoost::class.java
        )
        val sb = e1.effect as com.mystarrail.tool.data.model.EidolonEffect.StatBoost
        assertThat(sb.stat).isEqualTo(com.mystarrail.tool.data.model.StatType.CRIT_RATE)
        assertThat(sb.value).isEqualTo(0.12)
    }

    @Test
    fun `transforms light cone with passive effect`() {
        val result = Mar7thToSeedTransformer.transform(buildFiles())
        assertThat(result.lightCones).hasSize(1)
        val cone = result.lightCones.first()
        assertThat(cone.id).isEqualTo("mar7th_20000")
        assertThat(cone.path).isEqualTo(Path.HUNT)
        assertThat(cone.passiveEffect).isInstanceOf(
            com.mystarrail.tool.data.model.PassiveEffect.StatBoost::class.java
        )
    }

    @Test
    fun `transforms relic set with properties effects`() {
        val result = Mar7thToSeedTransformer.transform(buildFiles())
        assertThat(result.relicSets).hasSize(1)
        val set = result.relicSets.first()
        assertThat(set.id).isEqualTo("mar7th_101")
        assertThat(set.twoPiece).isInstanceOf(
            com.mystarrail.tool.data.model.PassiveEffect.StatBoost::class.java
        )
    }

    @Test
    fun `handles empty files map without crashing`() {
        val empty = mapOf<RemoteSeedSource.File, JsonElement>()
        val result = Mar7thToSeedTransformer.transform(empty)
        assertThat(result.characters).isEmpty()
        assertThat(result.lightCones).isEmpty()
        assertThat(result.relicSets).isEmpty()
        assertThat(result.eidolons).isEmpty()
        assertThat(result.enemies).isEmpty()
        assertThat(result.scenarios).isEmpty()
    }

    @Test
    fun `falls back to defaults for unknown path and element`() {
        val weirdChar = """
            {
              "9999": {
                "id": "9999",
                "name": "Mystery",
                "rarity": 4,
                "path": "UnknownPath",
                "element": "UnknownElement",
                "skills": [],
                "ranks": []
              }
            }
        """.trimIndent()
        val files = mapOf(
            RemoteSeedSource.File.CHARACTERS to parse(weirdChar)
        )
        val result = Mar7thToSeedTransformer.transform(files)
        assertThat(result.characters).hasSize(1)
        // 未知命途 → 降级 ERUDITION
        assertThat(result.characters.first().path).isEqualTo(Path.ERUDITION)
        // 未知元素 → 降级 PHYSICAL
        assertThat(result.characters.first().element).isEqualTo(Element.PHYSICAL)
    }
}