package com.mystarrail.tool.ui.relic

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.PassiveEffect
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.ui.characters.FakeRepository
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
class RelicScorerViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `selecting DPS char recommends CRIT_DMG body and ATK sphere`() = runTest {
        val dpsSet = sampleSet("quantum_dps", Role.DPS)
        val repo = FakeRepository(
            chars = listOf(sampleChar("a", Role.DPS)),
            relics = listOf(dpsSet)
        )
        val vm = RelicScorerViewModel(repo)
        advanceUntilIdle()
        val rec = vm.state.value.mainStatRec
        assertThat(rec).isNotNull()
        assertThat(rec!!.body).isEqualTo(StatType.CRIT_DMG)
        assertThat(rec.sphere).isEqualTo(StatType.ATK)
        assertThat(rec.rope).isEqualTo(StatType.ATK)
    }

    @Test fun `selecting HEALER char recommends HP body`() = runTest {
        val repo = FakeRepository(
            chars = listOf(sampleChar("a", Role.HEALER)),
            relics = listOf(sampleSet("hp_set", Role.HEALER))
        )
        val vm = RelicScorerViewModel(repo)
        advanceUntilIdle()
        val rec = vm.state.value.mainStatRec
        assertThat(rec).isNotNull()
        assertThat(rec!!.body).isEqualTo(StatType.HP)
        assertThat(rec.sphere).isEqualTo(StatType.HP)
    }

    @Test fun `best set is the one matching role`() = runTest {
        val dpsSet = sampleSet("dps_set", Role.DPS)
        val healSet = sampleSet("heal_set", Role.HEALER)
        val repo = FakeRepository(
            chars = listOf(sampleChar("a", Role.DPS)),
            relics = listOf(dpsSet, healSet)
        )
        val vm = RelicScorerViewModel(repo)
        advanceUntilIdle()
        val best = vm.state.value.bestSet
        assertThat(best).isNotNull()
        assertThat(best!!.id).isEqualTo("dps_set")
    }

    private fun sampleSet(id: String, role: Role) = RelicSet(
        id = id,
        name = id,
        twoPiece = PassiveEffect.StatBoost(StatType.ATK, 0.1),
        fourPiece = PassiveEffect.StatBoost(StatType.ATK, 0.2),
        suitableFor = setOf(role)
    )

    private fun sampleChar(id: String, role: Role) = Character(
        id = id,
        name = id,
        rarity = 5,
        path = Path.HUNT,
        element = Element.QUANTUM,
        role = role,
        tags = setOf(Tag.DOT),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null,
        iconUrl = "",
        version = 1
    )
}