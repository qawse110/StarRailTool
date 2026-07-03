package com.mystarrail.tool.ui.characters

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Eidolon
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.Scenario
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.repository.CharacterRepository
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
class CharactersViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun `search filters characters by name`() = runTest {
        val repo = FakeRepository(
            listOf(
                sampleChar("seele", "希儿"),
                sampleChar("himeko", "姬子")
            )
        )
        val vm = CharactersViewModel(repo)
        // 触发 stateIn 内部 combine 收集完成
        vm.uiState.value
        advanceUntilIdle()
        vm.setSearch("希")
        advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(1)
        assertThat(state.filtered.first().id).isEqualTo("seele")
    }

    @Test fun `element filter excludes other elements`() = runTest {
        val repo = FakeRepository(
            listOf(
                sampleChar("seele", "希儿", Element.QUANTUM),
                sampleChar("himeko", "姬子", Element.FIRE)
            )
        )
        val vm = CharactersViewModel(repo)
        vm.uiState.value
        advanceUntilIdle()
        vm.setElementFilter(Element.FIRE)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(1)
        assertThat(state.filtered.first().id).isEqualTo("himeko")
    }

    @Test fun `no filter shows all characters`() = runTest {
        val repo = FakeRepository(
            listOf(
                sampleChar("seele", "希儿"),
                sampleChar("himeko", "姬子"),
                sampleChar("kafka", "卡芙卡")
            )
        )
        val vm = CharactersViewModel(repo)
        vm.uiState.value
        advanceUntilIdle()
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(3)
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
        tags = emptySet(),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null,
        iconUrl = "",
        version = 1
    )
}