package com.java.myapplication.ui.assessment

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.Path
import com.java.myapplication.data.model.Role
import com.java.myapplication.data.model.Scaling
import com.java.myapplication.data.model.Stats
import com.java.myapplication.data.model.Tag
import com.java.myapplication.engine.simulator.ScoringEngine
import com.java.myapplication.engine.simulator.damage.DamageCalculator
import com.java.myapplication.engine.simulator.sim.DiscreteEventSimulator
import com.java.myapplication.engine.simulator.tables.FormulaTables
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
class AssessmentViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val scoringEngine = ScoringEngine(
        DamageCalculator(FormulaTables()),
        DiscreteEventSimulator(DamageCalculator(FormulaTables()))
    )

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `init produces rows sorted by total descending`() = runTest {
        val repo = FakeRepository(chars = listOf(
            sampleChar("a", Role.DPS),
            sampleChar("b", Role.HEALER),
            sampleChar("c", Role.SUPPORT)
        ))
        val vm = AssessmentViewModel(repo, scoringEngine)
        advanceUntilIdle()

        val rows = vm.state.value.rows
        assertThat(rows).hasSize(3)
        // 验证排序：相邻 total 应递减
        for (i in 0 until rows.size - 1) {
            assertThat(rows[i].total).isAtLeast(rows[i + 1].total)
        }
        // 验证所有 characterId 都映射到 a/b/c
        assertThat(rows.map { it.characterId }).containsExactly(
            "a", "b", "c"
        )
    }

    @Test fun `isLoading flips to false after init`() = runTest {
        val repo = FakeRepository(chars = listOf(sampleChar("a", Role.DPS)))
        val vm = AssessmentViewModel(repo, scoringEngine)
        // 初始仍 true（init 协程还没跑完）
        // 跑完 init 后变 false
        advanceUntilIdle()
        assertThat(vm.state.value.isLoading).isFalse()
    }

    private fun sampleChar(id: String, role: Role, element: Element = Element.QUANTUM) = Character(
        id = id,
        name = id,
        rarity = 5,
        path = Path.HUNT,
        element = element,
        role = role,
        tags = setOf(Tag.DOT),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null,
        iconUrl = "",
        version = 1
    )
}