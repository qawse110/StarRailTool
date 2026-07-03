package com.java.myapplication.data.seed

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.model.DmgCondition
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.EidolonEffect
import com.java.myapplication.data.model.PassiveEffect
import com.java.myapplication.data.model.Path
import com.java.myapplication.data.model.Role
import com.java.myapplication.data.model.SkillType
import com.java.myapplication.data.model.Tag
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * 纯 JVM 单测：只测 JSON 反序列化 + 域对象映射逻辑。
 * 不依赖 Android Context 或 Room（避免 Robolectric 在 ARM64 上的 native lib 缺失问题）。
 */
class SeedJsonParseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `seed JSON parses to expected counts`() {
        val raw = SeedJsonParseTest::class.java.classLoader
            ?.getResourceAsStream("seed-data-v1.json")
            ?.bufferedReader()
            ?.readText()
            ?: error("seed-data-v1.json not found in test resources")

        val root = json.decodeFromString(SeedRoot.serializer(), raw)

        assertThat(root.version).isEqualTo(1)
        assertThat(root.characters).hasSize(5)
        assertThat(root.lightCones).hasSize(5)
        assertThat(root.relicSets).hasSize(3)
        assertThat(root.enemies).hasSize(10)
        assertThat(root.scenarios).hasSize(3)
        assertThat(root.eidolons).hasSize(30)
    }

    @Test
    fun `seele character parses with correct attributes`() {
        val raw = SeedJsonParseTest::class.java.classLoader
            ?.getResourceAsStream("seed-data-v1.json")
            ?.bufferedReader()
            ?.readText() ?: error("missing json")

        val root = json.decodeFromString(SeedRoot.serializer(), raw)
        val seele = root.characters.first { it.id == "seele" }

        assertThat(seele.name).isEqualTo("希儿")
        assertThat(seele.rarity).isEqualTo(5)
        assertThat(seele.path).isEqualTo("HUNT")
        assertThat(seele.element).isEqualTo("QUANTUM")
        assertThat(seele.role).isEqualTo("DPS")
        assertThat(seele.tags).containsExactly(
            Tag.SINGLE_TARGET.name, Tag.CRIT_BOOST.name, Tag.FOLLOW_UP.name
        )
        assertThat(seele.baseStats.atk).isEqualTo(756.0)
        assertThat(seele.scaling.ultMult).isEqualTo(4.2)
    }

    @Test
    fun `composite light cone passive effect parses with 2 sub-effects`() {
        val raw = SeedJsonParseTest::class.java.classLoader
            ?.getResourceAsStream("seed-data-v1.json")
            ?.bufferedReader()
            ?.readText() ?: error("missing json")

        val root = json.decodeFromString(SeedRoot.serializer(), raw)
        val cone = root.lightCones.first { it.id == "galactic_railway_night" }
        assertThat(cone.passiveEffect.type).isEqualTo("Composite")
        assertThat(cone.passiveEffect.effects).hasSize(2)
        assertThat(cone.passiveEffect.effects[0].type).isEqualTo("DamageBonus")
        assertThat(cone.passiveEffect.effects[1].type).isEqualTo("StatBoost")
    }

    @Test
    fun `kafka E2 eidolon is major new mechanic DOT`() {
        val raw = SeedJsonParseTest::class.java.classLoader
            ?.getResourceAsStream("seed-data-v1.json")
            ?.bufferedReader()
            ?.readText() ?: error("missing json")

        val root = json.decodeFromString(SeedRoot.serializer(), raw)
        val kafkaE2 = root.eidolons.first { it.id == "kafka_e2" }
        assertThat(kafkaE2.major).isTrue()
        assertThat(kafkaE2.effect.type).isEqualTo("NewMechanic")
        assertThat(kafkaE2.effect.mechanic).isEqualTo(Tag.DOT.name)
        assertThat(kafkaE2.effect.param).isEqualTo(2.0)
    }

    @Test
    fun `enemy BOSS type parses with mechanics list`() {
        val raw = SeedJsonParseTest::class.java.classLoader
            ?.getResourceAsStream("seed-data-v1.json")
            ?.bufferedReader()
            ?.readText() ?: error("missing json")

        val root = json.decodeFromString(SeedRoot.serializer(), raw)
        val cocolia = root.enemies.first { it.id == "boss_cocolia" }
        assertThat(cocolia.type).isEqualTo("BOSS")
        assertThat(cocolia.weaknesses).contains(Element.QUANTUM.name)
        assertThat(cocolia.weaknesses).contains(Element.WIND.name)
        assertThat(cocolia.mechanics).contains("冰封")
    }

    @Test
    fun `scenario references enemy ids by string list`() {
        val raw = SeedJsonParseTest::class.java.classLoader
            ?.getResourceAsStream("seed-data-v1.json")
            ?.bufferedReader()
            ?.readText() ?: error("missing json")

        val root = json.decodeFromString(SeedRoot.serializer(), raw)
        val mf1 = root.scenarios.first { it.id == "mf_1" }
        assertThat(mf1.enemyIds).containsExactly(
            "boss_cocolia", "elite_watcher", "mob_thief"
        )
        assertThat(mf1.difficulty).isEqualTo(4)
    }

    @Test
    fun `all enum strings in seed data are recognized`() {
        val raw = SeedJsonParseTest::class.java.classLoader
            ?.getResourceAsStream("seed-data-v1.json")
            ?.bufferedReader()
            ?.readText() ?: error("missing json")

        val root = json.decodeFromString(SeedRoot.serializer(), raw)

        root.characters.forEach {
            Path.valueOf(it.path)
            Element.valueOf(it.element)
            Role.valueOf(it.role)
            it.tags.forEach { t -> Tag.valueOf(t) }
        }
        root.lightCones.forEach { Path.valueOf(it.path) }
        root.relicSets.forEach { it.suitableFor.forEach { r -> Role.valueOf(r) } }
        root.enemies.forEach { e ->
            e.weaknesses.forEach { Element.valueOf(it) }
        }
    }
}