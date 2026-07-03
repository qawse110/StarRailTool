package com.java.myapplication.ui.teambuilder

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
import com.java.myapplication.engine.simulator.sim.SimulationResult
import com.java.myapplication.engine.simulator.tables.FormulaTables
import com.java.myapplication.ui.characters.FakeRepository
import com.java.myapplication.util.SimulationResultStore
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
class TeamBuilderViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val scoringEngine = ScoringEngine(
        DamageCalculator(FormulaTables()),
        DiscreteEventSimulator(DamageCalculator(FormulaTables()))
    )
    private val store = InMemoryResultStore()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `toggleChar adds and removes id from selectedIds`() = runTest {
        val repo = FakeRepository(chars = listOf(
            sampleChar("seele"), sampleChar("himeko"),
            sampleChar("kafka"), sampleChar("bronya")
        ))
        val vm = TeamBuilderViewModel(repo, scoringEngine, store)
        advanceUntilIdle()

        vm.toggleChar("seele")
        advanceUntilIdle()
        assertThat(vm.uiState.value.selectedIds).containsExactly("seele")

        vm.toggleChar("himeko")
        advanceUntilIdle()
        assertThat(vm.uiState.value.selectedIds).hasSize(2)

        vm.toggleChar("seele")
        advanceUntilIdle()
        assertThat(vm.uiState.value.selectedIds).containsExactly("himeko")
    }

    @Test fun `cannot select more than 4 chars`() = runTest {
        val repo = FakeRepository(chars = listOf(
            sampleChar("a"), sampleChar("b"), sampleChar("c"), sampleChar("d"), sampleChar("e")
        ))
        val vm = TeamBuilderViewModel(repo, scoringEngine, store)
        advanceUntilIdle()

        vm.toggleChar("a"); vm.toggleChar("b"); vm.toggleChar("c"); vm.toggleChar("d")
        vm.toggleChar("e")
        advanceUntilIdle()

        assertThat(vm.uiState.value.selectedIds).hasSize(4)
        assertThat(vm.uiState.value.selectedIds).doesNotContain("e")
    }

    @Test fun `simulate with 4 chars produces result and writes to store`() = runTest {
        val repo = FakeRepository(chars = listOf(
            sampleChar("seele", Element.QUANTUM),
            sampleChar("himeko", Element.FIRE),
            sampleChar("kafka", Element.LIGHTNING),
            sampleChar("bronya", Element.WIND)
        ))
        val vm = TeamBuilderViewModel(repo, scoringEngine, store)
        advanceUntilIdle()

        vm.toggleChar("seele")
        vm.toggleChar("himeko")
        vm.toggleChar("kafka")
        vm.toggleChar("bronya")
        advanceUntilIdle()

        vm.simulate()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertThat(state.lastResult).isNotNull()
        assertThat(state.lastTotalDmg).isAtLeast(0.0)
        assertThat(store.lastSimulationResult).isNotNull()
    }

    private fun sampleChar(id: String, element: Element = Element.PHYSICAL) = Character(
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

private class InMemoryResultStore : SimulationResultStore {
    override var lastSimulationResult: SimulationResult? = null
}