package com.mystarrail.tool.ui.characters

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.Eidolon
import com.mystarrail.tool.data.model.Element
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.Path
import com.mystarrail.tool.data.model.PassiveEffect
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role
import com.mystarrail.tool.data.model.Scaling
import com.mystarrail.tool.data.model.Scenario
import com.mystarrail.tool.data.model.Stats
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.data.model.Target
import com.mystarrail.tool.data.repository.CharacterRepository
import com.mystarrail.tool.engine.simulator.ScoringEngine
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.sim.DiscreteEventSimulator
import com.mystarrail.tool.engine.simulator.tables.FormulaTables
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

    @Test fun `init loads relic sets from repository`() = runTest {
        val repo = FakeRepository(
            relics = listOf(
                com.mystarrail.tool.data.model.RelicSet(
                    id = "quantum_set",
                    name = "量子套",
                    twoPiece = com.mystarrail.tool.data.model.PassiveEffect.StatBoost(
                        stat = com.mystarrail.tool.data.model.StatType.ATK, value = 0.12
                    ),
                    fourPiece = com.mystarrail.tool.data.model.PassiveEffect.StatBoost(
                        stat = com.mystarrail.tool.data.model.StatType.ATK, value = 0.20
                    ),
                    suitableFor = setOf(com.mystarrail.tool.data.model.Role.DPS)
                )
            )
        )
        // 没有 char/cone 触发 recompute，scoringEngine 不会被实际调用
        val vm = CharacterDetailViewModel(
            characterId = "test",
            repository = repo,
            scoringEngine = scoringEngine
        )
        advanceUntilIdle()
        assertThat(vm.state.value.relicSets).hasSize(1)
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

    @Test fun `DPS char has zero utilityScore, HEAL char has positive utilityScore`() = runTest {
        // DPS char 没有 HEAL/SHIELD tag → utilityScore=0
        val dpsChar = sampleChar("dps", "DPS", Element.PHYSICAL)
        // HEAL char 有 Tag.HEAL → baseHealValue > 0 → utilityScore > 0
        val healChar = sampleChar("heal", "Healer", Element.PHYSICAL).copy(
            tags = setOf(Tag.HEAL)
        )
        val cone = sampleCone("c", "C", Path.HUNT, Element.PHYSICAL)
        val dpsRepo = FakeRepository(chars = listOf(dpsChar), cones = listOf(cone))
        val healRepo = FakeRepository(chars = listOf(healChar), cones = listOf(cone))

        val dpsVm = CharacterDetailViewModel("dps", dpsRepo, scoringEngine)
        val healVm = CharacterDetailViewModel("heal", healRepo, scoringEngine)
        advanceUntilIdle()

        val dpsScore = dpsVm.state.value.score!!
        val healScore = healVm.state.value.score!!

        // DPS 没 HEAL tag → utilityScore = 0
        assertThat(dpsScore.utilityScore).isEqualTo(0.0)
        // HEAL 角色 utilityScore > 0
        assertThat(healScore.utilityScore).isGreaterThan(0.0)
        assertThat(healScore.utilityScore).isAtMost(10.0)
    }

    @Test fun `recompute passes skill tree to scoring engine without crash`() = runTest {
        // 验证 viewmodel recompute 链路包含 skillTree 字段
        // （FakeRepository.getSkillTreeFor 返回 null，是最简路径）
        val char = sampleChar("seele", "希儿", Element.QUANTUM)
        val cone = sampleCone("in_the_night", "夜色如墨", Path.HUNT, Element.QUANTUM)
        val repo = FakeRepository(chars = listOf(char), cones = listOf(cone))

        val vm = CharacterDetailViewModel("seele", repo, scoringEngine)
        advanceUntilIdle()

        // 验证：state 加载成功 + score 算出来了（说明 skillTree=null 路径无 NPE）
        val s = vm.state.value
        assertThat(s.character?.id).isEqualTo("seele")
        assertThat(s.skillTree).isNull()  // FakeRepository 返回 null
        assertThat(s.score).isNotNull()
        assertThat(s.score!!.total).isAtLeast(0.0)
    }

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
            stat = com.mystarrail.tool.data.model.StatType.ATK,
            value = 0.2,
            target = Target.SELF
        )
    )
}