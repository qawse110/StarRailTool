package com.java.myapplication.ui.scenario

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.Enemy
import com.java.myapplication.data.model.EnemyType
import com.java.myapplication.data.model.Path
import com.java.myapplication.data.model.Role
import com.java.myapplication.data.model.Scaling
import com.java.myapplication.data.model.Scenario
import com.java.myapplication.data.model.Stats
import com.java.myapplication.data.model.Tag
import com.java.myapplication.ui.characters.FakeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `scenarios sorted by fitScore desc`() = runTest {
        val s1 = sampleScenario("s1", listOf(sampleEnemy("e1", setOf(Element.QUANTUM))))
        val s2 = sampleScenario("s2", listOf(
            sampleEnemy("e1", setOf(Element.QUANTUM, Element.FIRE, Element.ICE, Element.LIGHTNING))
        ))
        val repo = FakeRepository(
            chars = listOf(sampleChar("a", Element.QUANTUM)),
            scenarios = listOf(s1, s2)
        )
        val vm = ScenarioViewModel(repo)
        advanceUntilIdle()
        val scenarios = vm.state.value.scenarios
        assertThat(scenarios).hasSize(2)
        // s2 覆盖更多元素 → 评分更高
        assertThat(scenarios[0].scenario.id).isEqualTo("s2")
        assertThat(scenarios[0].fitScore).isGreaterThan(scenarios[1].fitScore)
    }

    @Test fun `selecting matching char adds team bonus`() = runTest {
        val s = sampleScenario("s1", listOf(sampleEnemy("e1", setOf(Element.QUANTUM))))
        val repo = FakeRepository(
            chars = listOf(sampleChar("a", Element.QUANTUM)),
            scenarios = listOf(s)
        )
        val vm = ScenarioViewModel(repo)
        advanceUntilIdle()
        val scoreBefore = vm.state.value.scenarios.first().fitScore
        vm.toggleChar("a")
        advanceUntilIdle()
        val scoreAfter = vm.state.value.scenarios.first().fitScore
        // 选匹配角色后，team bonus = 20
        assertThat(scoreAfter).isGreaterThan(scoreBefore)
    }

    private fun sampleScenario(id: String, enemies: List<Enemy>) = Scenario(
        id = id, name = id, enemies = enemies, difficulty = 3
    )

    private fun sampleEnemy(id: String, weaknesses: Set<Element>) = Enemy(
        id = id, name = id, count = 1,
        weaknesses = weaknesses,
        type = EnemyType.BOSS,
        hp = 100_000.0,
        toughness = 200.0
    )

    private fun sampleChar(id: String, element: Element) = Character(
        id = id,
        name = id,
        rarity = 5,
        path = Path.HUNT,
        element = element,
        role = Role.DPS,
        tags = setOf(Tag.DOT),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null,
        iconUrl = "",
        version = 1
    )
}