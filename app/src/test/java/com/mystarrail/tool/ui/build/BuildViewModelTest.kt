package com.mystarrail.tool.ui.build

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.MainStats
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.PlayerBuild
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.Stats
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
class BuildViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `observeAllPlayerBuilds emits initial empty list`() = runTest {
        val repo = FakeRepository(chars = listOf(sampleChar("seele")))
        val vm = BuildViewModel(repo)
        advanceUntilIdle()
        assertThat(vm.uiState.value.builds).isEmpty()
        assertThat(vm.uiState.value.charMap).containsKey("seele")
    }

    @Test fun `addBuild and upsert reflect in uiState`() = runTest {
        val repo = FakeRepository(chars = listOf(sampleChar("seele")))
        val vm = BuildViewModel(repo)
        advanceUntilIdle()
        val template = vm.newTemplate("seele")
        vm.upsert(template)
        advanceUntilIdle()
        val builds = vm.uiState.value.builds
        assertThat(builds).hasSize(1)
        assertThat(builds.first().characterId).isEqualTo("seele")
        // 模拟编辑：更新 level
        val updated = builds.first().copy(level = 80)
        vm.upsert(updated)
        advanceUntilIdle()
        assertThat(vm.uiState.value.builds.first().level).isEqualTo(80)
    }

    @Test fun `delete removes build from uiState`() = runTest {
        val repo = FakeRepository(chars = listOf(sampleChar("seele")))
        val vm = BuildViewModel(repo)
        advanceUntilIdle()
        vm.upsert(vm.newTemplate("seele"))
        advanceUntilIdle()
        val id = vm.uiState.value.builds.first().id
        vm.delete(id)
        advanceUntilIdle()
        assertThat(vm.uiState.value.builds).isEmpty()
    }

    private fun sampleChar(id: String) = Character(
        id = id,
        name = "希儿",
        rarity = 5,
        path = Path.HUNT,
        element = Element.QUANTUM,
        role = Role.DPS,
        tags = setOf(Tag.DOT),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null,
        iconUrl = "",
        version = 1
    )
}