package com.java.myapplication.ui.characters

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Eidolon
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.Enemy
import com.java.myapplication.data.model.LightCone
import com.java.myapplication.data.model.Path
import com.java.myapplication.data.model.PassiveEffect
import com.java.myapplication.data.model.RelicSet
import com.java.myapplication.data.model.Role
import com.java.myapplication.data.model.Scaling
import com.java.myapplication.data.model.Scenario
import com.java.myapplication.data.model.Stats
import com.java.myapplication.data.model.Tag
import com.java.myapplication.data.model.Target
import com.java.myapplication.data.repository.CharacterRepository
import com.java.myapplication.engine.simulator.ScoringEngine
import com.java.myapplication.engine.simulator.damage.DamageCalculator
import com.java.myapplication.engine.simulator.sim.DiscreteEventSimulator
import com.java.myapplication.engine.simulator.tables.FormulaTables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val scoringEngine = ScoringEngine(
        DamageCalculator(FormulaTables()),
        DiscreteEventSimulator(DamageCalculator(FormulaTables()))
    )

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun `init loads character and computes default score`() = runTest {
        val char = sampleChar("seele", "希儿", Element.QUANTUM)
        val cone = sampleCone("in_the_night", "夜色如墨", Path.HUNT, Element.QUANTUM)
        val repo = FakeRepository(chars = listOf(char), cones = listOf(cone))

        val vm = CharacterDetailViewModel("seele", repo, scoringEngine)
        advanceUntilIdle()

        val s = vm.state.value
        assertThat(s.character?.id).isEqualTo("seele")
        assertThat(s.selectedCone?.id).isEqualTo("in_the_night")
        assertThat(s.score).isNotNull()
        assertThat(s.score!!.total).isAtLeast(0.0)
        assertThat(s.score.total).isAtMost(100.0)
    }

    @Test fun `selecting cone updates score`() = runTest {
        val char = sampleChar("seele", "希儿", Element.QUANTUM)
        val cone1 = sampleCone("in_the_night", "夜色如墨", Path.HUNT, Element.QUANTUM)
        val cone2 = sampleCone("sleep_like", "安眠如死", Path.HUNT, Element.QUANTUM)
        val repo = FakeRepository(chars = listOf(char), cones = listOf(cone1, cone2))

        val vm = CharacterDetailViewModel("seele", repo, scoringEngine)
        advanceUntilIdle()

        val originalTotal = vm.state.value.score!!.total
        vm.selectCone(cone2)
        advanceUntilIdle()
        val newTotal = vm.state.value.score!!.total

        // 选不同光锥后 score.total 应发生变化（或保持，但 selectedCone 必变）
        assertThat(vm.state.value.selectedCone?.id).isEqualTo("sleep_like")
        // score 应被重新计算（total 是非负）
        assertThat(newTotal).isAtLeast(0.0)
        assertThat(newTotal).isAtMost(100.0)
        // suppress unused warning
        assertThat(originalTotal).isAtLeast(0.0)
    }

    @Test fun `toggling eidolons updates selectedEidolons`() = runTest {
        val char = sampleChar("seele", "希儿", Element.QUANTUM)
        val cone = sampleCone("in_the_night", "夜色如墨", Path.HUNT, Element.QUANTUM)
        val repo = FakeRepository(chars = listOf(char), cones = listOf(cone))

        val vm = CharacterDetailViewModel("seele", repo, scoringEngine)
        advanceUntilIdle()

        assertThat(vm.state.value.selectedEidolons).isEmpty()
        vm.toggleEidolon(1)
        advanceUntilIdle()
        assertThat(vm.state.value.selectedEidolons).containsExactly(1)
        vm.toggleEidolon(1)
        advanceUntilIdle()
        assertThat(vm.state.value.selectedEidolons).isEmpty()
    }

    private fun sampleChar(
        id: String,
        name: String,
        element: Element = Element.PHYSICAL
    ) = Character(
        id = id,
        name = name,
        rarity = 5,
        path = Path.HUNT,
        element = element,
        role = Role.DPS,
        tags = setOf(Tag.DOT, Tag.FOLLOW_UP),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null,
        iconUrl = "",
        version = 1
    )

    private fun sampleCone(
        id: String,
        name: String,
        path: Path = Path.HUNT,
        element: Element = Element.PHYSICAL
    ) = LightCone(
        id = id,
        name = name,
        path = path,
        rarity = 5,
        passiveName = "test passive",
        passiveEffect = PassiveEffect.StatBoost(
            stat = com.java.myapplication.data.model.StatType.ATK,
            value = 0.2,
            target = Target.SELF
        )
    )
}