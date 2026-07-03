package com.java.myapplication.ui.scraper

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.Path
import com.java.myapplication.data.model.Role
import com.java.myapplication.data.model.Scaling
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
class ScraperViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `init populates counts from repository`() = runTest {
        val repo = FakeRepository(chars = listOf(
            sampleChar("a"), sampleChar("b"), sampleChar("c")
        ))
        val vm = ScraperViewModel(repo, reimportCallback = { /* noop */ })
        advanceUntilIdle()
        val s = vm.state.value
        assertThat(s.characterCount).isEqualTo(3)
        assertThat(s.isLoadingStatus).isFalse()
    }

    @Test fun `setUrl updates state`() = runTest {
        val repo = FakeRepository()
        val vm = ScraperViewModel(repo, reimportCallback = {})
        advanceUntilIdle()
        vm.setUrl("https://example.com")
        assertThat(vm.state.value.url).isEqualTo("https://example.com")
    }

    @Test fun `fetch with empty url sets error`() = runTest {
        val repo = FakeRepository()
        val vm = ScraperViewModel(repo, reimportCallback = {})
        advanceUntilIdle()
        vm.setUrl("")
        vm.fetch()
        assertThat(vm.state.value.lastError).isEqualTo("URL 为空")
        assertThat(vm.state.value.isFetching).isFalse()
    }

    @Test fun `reimportSeed invokes callback and shows success`() = runTest {
        var invoked = false
        val repo = FakeRepository()
        val vm = ScraperViewModel(repo, reimportCallback = { invoked = true })
        advanceUntilIdle()
        vm.reimportSeed()
        advanceUntilIdle()
        assertThat(invoked).isTrue()
        assertThat(vm.state.value.lastResult).contains("种子数据已重新导入")
    }

    private fun sampleChar(id: String) = Character(
        id = id, name = id, rarity = 5, path = Path.HUNT,
        element = Element.QUANTUM, role = Role.DPS,
        tags = setOf(Tag.DOT),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 1
    )
}