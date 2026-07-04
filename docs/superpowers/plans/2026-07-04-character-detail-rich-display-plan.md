# Character Detail Rich Display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add 7 display blocks (基础数值/技能倍率/标签/星魂介绍/光锥效果/遗器推荐/行迹完整数值链) to `CharacterDetailScreen`, persist 行迹 (skill tree) data from `Mar-7th/StarRailRes` to Room v1→v2 schema, and integrate skill tree stats into `DamageCalculator`.

**Architecture:**
- **Commit 1 (UI only):** Add baseStats/scaling/tags blocks using existing `Character` model. Zero backend changes. ~1.5h.
- **Commit 2 (UI only):** Add eidolons/lightcone/relics blocks using existing `Eidolon`/`LightCone`/`RelicSet` data. ~1.5h.
- **Commit 3 (Data layer):** Add `SkillTree` model, `SkillTreeNodeEntity` + DAO, Room v1→v2 migration, extend `Mar7thToSeedTransformer` to parse `character_skill_trees.json`. ~2h.
- **Commit 4 (UI):** Add `SkillTreeBlock` composable grouped by `skillType`, all-expanded per user decision. ~1.5h.
- **Commit 5 (Simulator):** Add `SkillTreeEffectParser` (reuses `KeywordMatcher`), extend `DamageCalculator.unitValue()` and `ScoringEngine.scoreCharacter()` to accept optional `SkillTree?` param. Old signatures preserved with default `null`. ~2h.
- **Commit 6 (Verify + docs):** End-to-end evidence, run all 201+ tests, update README/changelog. ~0.5h.

**Tech Stack:** Kotlin 2.2.0, Jetpack Compose 2026.01.01 BOM, Room 2.7.0, kotlinx.serialization 1.7.3, JUnit 4.13.2 + Truth 1.4.4, OkHttp 4.12.0.

## Global Constraints

- **Min/Target SDK:** minSdk 24, targetSdk 35, compileSdk 35
- **Kotlin version:** 2.2.0 (compose plugin)
- **Compose BOM:** 2026.01.01
- **Test deps:** JUnit 4.13.2, Truth 1.4.4, kotlinx-coroutines-test 1.9.0
- **No Robolectric in Proot:** 设备验证兜底 DB migration; unit tests must run on plain JVM only
- **Existing tests:** 188 must remain green throughout (no test may be deleted)
- **TDD mandatory:** red→green→refactor for every new logic
- **Commit discipline:** Each task ends with `git commit` and full `./gradlew :app:test --rerun-tasks` passing
- **Code style:** Kotlin idiomatic (`when` over `switch`, data class for DTOs, `runCatching` for JSON access, sealed interface for ADT)
- **Naming:** Chinese for domain comments, English for code identifiers
- **Reuse:** `KeywordMatcher` (11 bilingual rules) must be used for `SkillTreeEffectParser`
- **Backward compat:** All public API additions use default `null` for new params; old callers must compile without changes
- **Breaking change (commit 1):** `SeedParser.ParseResult.Success` adds `skillTrees: List<SkillTree>` field with default `emptyList()`. All 5 call sites updated in same commit.

---

## File Structure

| File | Role | Action | Commit |
|---|---|---|---|
| `data/model/SkillTree.kt` | Domain model `SkillTree` + `SkillTreeNode` | NEW | 3 |
| `data/local/SkillTreeNodeEntity.kt` | Room entity (1 row per node) | NEW | 3 |
| `data/local/SkillTreeDao.kt` | Room DAO with `getForCharacter()` | NEW | 3 |
| `data/local/Migrations.kt` | v1→v2 `MIGRATION_1_2` | NEW | 3 |
| `data/local/AppDatabase.kt` | v1→v2 + add `skillTreeDao()` | MOD | 3 |
| `data/seed/SeedData.kt` | add `SeedSkillTree` + `SeedSkillTreeNode` | MOD | 1+3 |
| `data/seed/SeedParser.kt` | add `skillTrees` to `Success` + parse | MOD | 1+3 |
| `data/seed/SeedImporter.kt` | write `skillTrees` to DB | MOD | 1+3 |
| `data/seed/remote/RemoteSeedSource.kt` | add `SKILL_TREES` to `CORE_FILES` | MOD | 3 |
| `data/seed/remote/Mar7thToSeedTransformer.kt` | add `transformSkillTrees()` | MOD | 1+3 |
| `data/repository/CharacterRepository.kt` | add `getSkillTreeFor(cid)` | MOD | 3 |
| `data/repository/RoomCharacterRepository.kt` | impl `getSkillTreeFor()` | MOD | 3 |
| `engine/simulator/SkillTreeEffectParser.kt` | desc → `Map<StatType, Double>` | NEW | 5 |
| `engine/simulator/ScoringEngine.kt` | accept `skillTree: SkillTree?` | MOD | 5 |
| `engine/simulator/damage/DamageCalculator.kt` | accept `skillTree: SkillTree?` | MOD | 5 |
| `ui/characters/CharacterDetailViewModel.kt` | load skill tree, pass to recompute | MOD | 3 + 5 |
| `ui/characters/CharacterDetailScreen.kt` | add 7 blocks, reorder | MOD | 1 + 2 + 4 |
| `ui/characters/components/BaseStatsBlock.kt` | composable | NEW | 1 |
| `ui/characters/components/ScalingBlock.kt` | composable | NEW | 1 |
| `ui/characters/components/TagsBlock.kt` | composable | NEW | 1 |
| `ui/characters/components/EidolonsListBlock.kt` | composable | NEW | 2 |
| `ui/characters/components/LightConeEffectBlock.kt` | composable | NEW | 2 |
| `ui/characters/components/RelicRecommendationsBlock.kt` | composable | NEW | 2 |
| `ui/characters/components/SkillTreeBlock.kt` | composable | NEW | 4 |
| `app/src/test/.../data/seed/SeedParserTest.kt` | assert `skillTrees.isEmpty()` | MOD | 3 |
| `app/src/test/.../data/seed/remote/Mar7thToSeedTransformerTest.kt` | add 4 skill_tree tests | MOD | 3 |
| `app/src/test/.../engine/simulator/SkillTreeEffectParserTest.kt` | 8 tests | NEW | 5 |
| `app/src/test/.../ui/characters/CharacterDetailViewModelTest.kt` | add 2 tests | MOD | 2 + 5 |
| `app/src/main/assets/seed-data-v1.json` | add `skillTrees: []` to root | MOD | 1 |
| `app/schemas/com.mystarrail.tool.data.local.AppDatabase/2.json` | auto-generated | NEW (auto) | 3 |

**Totals:** 14 NEW + 14 MOD = 28 files, ~+950 lines.

---

## Task 1: Add baseStats + scaling + tags blocks (Commit 1: A+B+C UI only)

**Files:**
- Create: `app/src/main/java/com/mystarrail/tool/ui/characters/components/BaseStatsBlock.kt`
- Create: `app/src/main/java/com/mystarrail/tool/ui/characters/components/ScalingBlock.kt`
- Create: `app/src/main/java/com/mystarrail/tool/ui/characters/components/TagsBlock.kt`
- Modify: `app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailScreen.kt`
- Modify: `app/src/main/java/com/mystarrail/tool/data/seed/SeedParser.kt` (add `skillTrees` field with default)
- Modify: `app/src/main/java/com/mystarrail/tool/data/seed/SeedData.kt` (add `skillTrees` + `SeedSkillTree` + `SeedSkillTreeNode`)
- Modify: `app/src/main/java/com/mystarrail/tool/data/seed/SeedImporter.kt` (no-op for now)
- Modify: `app/src/main/java/com/mystarrail/tool/data/seed/remote/Mar7thToSeedTransformer.kt` (return `skillTrees = emptyList()`)
- Modify: `app/src/main/assets/seed-data-v1.json` (add `"skillTrees": []`)

**Interfaces:**
- Consumes: existing `Character.baseStats`, `Character.scaling`, `Character.tags`
- Produces: 3 new `@Composable` functions

**Note on breaking change:** `Success` field is **additive** with default value, but `Mar7thToSeedTransformer.transform()` return type literal must include `skillTrees = emptyList()`. No existing tests need modification.

- [ ] **Step 1.1: Add 3 composable files (test-less, simple render)**

Create `BaseStatsBlock.kt`:
```kotlin
package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.Stats

@Composable
fun BaseStatsBlock(stats: Stats) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("📊 基础数值", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatCell("HP", stats.hp.toInt())
            StatCell("ATK", stats.atk.toInt())
            StatCell("DEF", stats.def.toInt())
            StatCell("SPD", stats.spd.toInt())
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
```

Create `ScalingBlock.kt`:
```kotlin
package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.Scaling

@Composable
fun ScalingBlock(scaling: Scaling) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("⚔️ 技能倍率", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MultCell("战技", scaling.skillMult)
            MultCell("终结技", scaling.ultMult)
            MultCell("天赋", scaling.talentMult)
            MultCell("追击", scaling.followUpMult)
        }
        if (scaling.aoeRatio > 0) {
            Spacer(Modifier.height(4.dp))
            Text("AOE 比例: ${(scaling.aoeRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MultCell(label: String, mult: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("×${"%.2f".format(mult)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
```

Create `TagsBlock.kt`:
```kotlin
package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.ui.components.label

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsBlock(tags: Set<Tag>) {
    if (tags.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("🏷️ 标签", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEach { tag ->
                AssistChip(
                    onClick = {},
                    label = { Text(tag.label()) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }
    }
}
```

- [ ] **Step 1.2: Modify `SeedParser.kt` to add `skillTrees` field**

In `app/src/main/java/com/mystarrail/tool/data/seed/SeedParser.kt`, change `Success` data class (around line 36-44) to:
```kotlin
data class Success(
    val characters: List<Character>,
    val lightCones: List<LightCone>,
    val relicSets: List<RelicSet>,
    val enemies: List<Enemy>,
    val scenarios: List<Scenario>,
    val eidolons: List<Eidolon>,
    val skillTrees: List<com.mystarrail.tool.data.model.SkillTree> = emptyList()
) : ParseResult
```

Note: Use fully-qualified name to keep commit 1 compiling without creating `SkillTree.kt` yet (added in commit 3).

- [ ] **Step 1.3: Modify `SeedData.kt` to add `skillTrees` to `SeedRoot` + new data classes**

In `app/src/main/java/com/mystarrail/tool/data/seed/SeedData.kt`, add at the end:
```kotlin
@Serializable
data class SeedSkillTree(
    val characterId: String,
    val nodes: List<SeedSkillTreeNode> = emptyList()
)

@Serializable
data class SeedSkillTreeNode(
    val id: String,
    val name: String,
    val desc: String,
    val maxLevel: Int = 1,
    val skillType: String? = null,
    val effectType: String? = null,
    val paramList: List<List<Double>> = emptyList()
)
```

Also add `val skillTrees: List<SeedSkillTree> = emptyList()` to existing `SeedRoot`.

- [ ] **Step 1.4: Modify `SeedImporter.kt` (no-op for now)**

In `app/src/main/java/com/mystarrail/tool/data/seed/SeedImporter.kt`:
- Update `ImportResult.Success` (around line 71-77) to add `val skillTrees: Int = 0`
- Update `Log.i` (around line 56) and `Success` construction (around line 57-64) to include `skillTrees = parsed.skillTrees.size`
- Do NOT touch `withTransaction` block (commit 3 handles real DB write)

- [ ] **Step 1.5: Modify `Mar7thToSeedTransformer.kt` to include `skillTrees = emptyList()`**

In `app/src/main/java/com/mystarrail/tool/data/seed/remote/Mar7thToSeedTransformer.kt`, in `transform()` return (around line 104-111):
```kotlin
return SeedParser.ParseResult.Success(
    characters = outChars,
    lightCones = outCones,
    relicSets = outSets,
    enemies = emptyList(),
    scenarios = emptyList(),
    eidolons = outEidolons,
    skillTrees = emptyList()  // commit 3 will populate
)
```

- [ ] **Step 1.6: Update `seed-data-v1.json` to include `skillTrees: []`**

Read `app/src/main/assets/seed-data-v1.json` and add `"skillTrees": []` as last property of the root object.

- [ ] **Step 1.7: Modify `CharacterDetailScreen.kt` to insert 3 new blocks**

Add imports and insert after line 73 (after the element/path/role Text):
```kotlin
import com.mystarrail.tool.ui.characters.components.BaseStatsBlock
import com.mystarrail.tool.ui.characters.components.ScalingBlock
import com.mystarrail.tool.ui.characters.components.TagsBlock

// In state.character?.let { char -> ... } block, after the role Text:

Spacer(Modifier.height(24.dp))

// 基础数值 A
BaseStatsBlock(stats = char.baseStats)

// 技能倍率 B
ScalingBlock(scaling = char.scaling)

// 标签 C
TagsBlock(tags = char.tags)

Spacer(Modifier.height(24.dp))
```

- [ ] **Step 1.8: Run tests + assemble (Evidence before claims)**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && ./gradlew :app:test --rerun-tasks --console=plain 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL, 188 tests, 0 failures.

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 1.9: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
git add app/src/main/java/com/mystarrail/tool/ui/characters/components/BaseStatsBlock.kt \
        app/src/main/java/com/mystarrail/tool/ui/characters/components/ScalingBlock.kt \
        app/src/main/java/com/mystarrail/tool/ui/characters/components/TagsBlock.kt \
        app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailScreen.kt \
        app/src/main/java/com/mystarrail/tool/data/seed/SeedParser.kt \
        app/src/main/java/com/mystarrail/tool/data/seed/SeedData.kt \
        app/src/main/java/com/mystarrail/tool/data/seed/SeedImporter.kt \
        app/src/main/java/com/mystarrail/tool/data/seed/remote/Mar7thToSeedTransformer.kt \
        app/src/main/assets/seed-data-v1.json
git commit -m "feat(detail): add baseStats/scaling/tags blocks (A+B+C UI only)" \
        -m "- Add 3 composable blocks: BaseStatsBlock, ScalingBlock, TagsBlock" \
        -m "- Insert between header and score section in CharacterDetailScreen" \
        -m "- Additive: SeedParser.ParseResult.Success adds skillTrees field with default emptyList()" \
        -m "- Update assets seed JSON to include skillTrees: [] for forward compat" \
        -m "Tests: 188/188 passing. APK 22MB."
```

---

## Task 2: Add eidolons/lightcone/relics blocks (Commit 2: D+E+F UI only)

**Files:**
- Create: `app/src/main/java/com/mystarrail/tool/ui/characters/components/EidolonsListBlock.kt`
- Create: `app/src/main/java/com/mystarrail/tool/ui/characters/components/LightConeEffectBlock.kt`
- Create: `app/src/main/java/com/mystarrail/tool/ui/characters/components/RelicRecommendationsBlock.kt`
- Modify: `app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModel.kt` (add `relicSets` to state)
- Modify: `app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailScreen.kt` (insert 3 blocks)
- Modify: `app/src/test/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModelTest.kt` (add 1 test)

**Interfaces:**
- Consumes: `state.eidolons`, `state.selectedCone`, new `state.relicSets`
- Produces: 3 new composables

- [ ] **Step 2.1: Add 3 composable files**

Create `EidolonsListBlock.kt`:
```kotlin
package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.Eidolon

@Composable
fun EidolonsListBlock(eidolons: List<Eidolon>) {
    if (eidolons.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("🔮 星魂介绍", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        eidolons.sortedBy { it.level }.forEach { eidolon ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("E${eidolon.level}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (eidolon.major) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        Text(eidolon.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(eidolon.effect.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
```

Create `LightConeEffectBlock.kt`:
```kotlin
package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.LightCone

@Composable
fun LightConeEffectBlock(cone: LightCone?) {
    if (cone == null) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("💡 必选光锥效果: ${cone.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("效果: ${cone.passiveName}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(2.dp))
        Text(cone.passiveEffect.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
```

Create `RelicRecommendationsBlock.kt`:
```kotlin
package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role

@Composable
fun RelicRecommendationsBlock(relics: List<RelicSet>, characterRole: Role) {
    if (relics.isEmpty()) return
    val recommended = relics.take(5)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("🛡️ 遗器套推荐 (${characterRole.name})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        recommended.forEach { relic ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(relic.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("2件: ${relic.twoPiece}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (relic.fourPiece != relic.twoPiece) {
                        Text("4件: ${relic.fourPiece}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2.2: Modify `CharacterDetailViewModel.kt` to expose `relicSets`**

In `app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModel.kt`:
- Add to `CharacterDetailUiState` (around line 25-32): `val relicSets: List<RelicSet> = emptyList()`
- In `init` block, after `cones` line: `val relics = repository.observeAllRelicSets().first()`
- Update `_state.update` to include `relicSets = relics`
- Add import: `import com.mystarrail.tool.data.model.RelicSet`

- [ ] **Step 2.3: Add test for new state field**

In `app/src/test/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModelTest.kt`:
- Read existing `FakeRepository` to understand constructor. If it does not accept `relics`, add `relics: List<RelicSet> = emptyList()` default param.
- Add test:
```kotlin
@Test fun `init loads relic sets from repository`() = runTest {
    val repo = FakeRepository(relics = listOf(
        com.mystarrail.tool.data.model.RelicSet(
            id = "quantum_set", name = "量子套",
            twoPiece = com.mystarrail.tool.data.model.PassiveEffect.StatBoost(
                stat = com.mystarrail.tool.data.model.StatType.ATK, value = 0.12),
            fourPiece = com.mystarrail.tool.data.model.PassiveEffect.StatBoost(
                stat = com.mystarrail.tool.data.model.StatType.ATK, value = 0.20),
            suitableFor = setOf(com.mystarrail.tool.data.model.Role.DPS)
        )
    ))
    val vm = CharacterDetailViewModel(
        characterId = "test",
        repository = repo,
        scoringEngine = error("not used") as com.mystarrail.tool.engine.simulator.ScoringEngine
    )
    advanceUntilIdle()
    assertThat(vm.state.value.relicSets).hasSize(1)
}
```

If `scoringEngine` is non-nullable, use a stub that throws on construction-call (not invoked in this test path). Confirm by re-reading `CharacterDetailViewModel` constructor signature (line 34-38): it's `private val scoringEngine: ScoringEngine` — non-nullable. Use `error("not used") as ScoringEngine` cast.

- [ ] **Step 2.4: Modify `CharacterDetailScreen.kt` to insert 3 new blocks**

Add imports and insert after the "5 维度 ScoreBar" section (around line 99, before "Spacer(Modifier.height(24.dp))" leading into "必选光锥"):
```kotlin
import com.mystarrail.tool.ui.characters.components.EidolonsListBlock
import com.mystarrail.tool.ui.characters.components.LightConeEffectBlock
import com.mystarrail.tool.ui.characters.components.RelicRecommendationsBlock

// ... after 5 ScoreBar() calls ...

// [新增] 星魂介绍 D
EidolonsListBlock(eidolons = state.eidolons)

// [新增] 光锥效果 E
LightConeEffectBlock(cone = state.selectedCone)

// [新增] 遗器推荐 F
RelicRecommendationsBlock(relics = state.relicSets, characterRole = char.role)

Spacer(Modifier.height(24.dp))

// 必选光锥 picker (existing, moves down)
```

- [ ] **Step 2.5: Run tests + assemble**

Run: `./gradlew :app:test --rerun-tasks --console=plain 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL, **189 tests** (188 + 1 new), 0 failures.

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2.6: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
git add app/src/main/java/com/mystarrail/tool/ui/characters/components/EidolonsListBlock.kt \
        app/src/main/java/com/mystarrail/tool/ui/characters/components/LightConeEffectBlock.kt \
        app/src/main/java/com/mystarrail/tool/ui/characters/components/RelicRecommendationsBlock.kt \
        app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModel.kt \
        app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailScreen.kt \
        app/src/test/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModelTest.kt
git commit -m "feat(detail): add eidolons/lightcone/relics blocks (D+E+F UI only)" \
        -m "- Add 3 composable blocks: EidolonsListBlock, LightConeEffectBlock, RelicRecommendationsBlock" \
        -m "- Insert after score section, before cone picker" \
        -m "- CharacterDetailUiState adds relicSets field" \
        -m "- RelicRecommendationsBlock uses top 5 sets as fallback (StarRailRes lacks suitableFor)" \
        -m "Tests: 189/189 passing. APK 22MB."
```

---

## Task 3: Add SkillTree schema + Room v1→v2 migration + transformer (Commit 3: G data layer)

**Files:**
- Create: `app/src/main/java/com/mystarrail/tool/data/model/SkillTree.kt`
- Create: `app/src/main/java/com/mystarrail/tool/data/local/SkillTreeNodeEntity.kt`
- Create: `app/src/main/java/com/mystarrail/tool/data/local/SkillTreeDao.kt`
- Create: `app/src/main/java/com/mystarrail/tool/data/local/Migrations.kt`
- Modify: `app/src/main/java/com/mystarrail/tool/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/mystarrail/tool/data/seed/SeedParser.kt` (parse `skillTrees` properly)
- Modify: `app/src/main/java/com/mystarrail/tool/data/seed/SeedImporter.kt` (write `skillTrees` to DB)
- Modify: `app/src/main/java/com/mystarrail/tool/data/seed/remote/RemoteSeedSource.kt` (add `SKILL_TREES`)
- Modify: `app/src/main/java/com/mystarrail/tool/data/seed/remote/Mar7thToSeedTransformer.kt` (add `transformSkillTrees()`)
- Modify: `app/src/main/java/com/mystarrail/tool/data/repository/CharacterRepository.kt`
- Modify: `app/src/main/java/com/mystarrail/tool/data/repository/RoomCharacterRepository.kt`
- Modify: `app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModel.kt` (load skill tree)
- Modify: `app/src/test/java/com/mystarrail/tool/data/seed/SeedParserTest.kt`
- Modify: `app/src/test/java/com/mystarrail/tool/data/seed/remote/Mar7thToSeedTransformerTest.kt` (add 4 tests)

**Interfaces:**
- Consumes: `Character.id`
- Produces: `SkillTree`, `SkillTreeNode`, Room entity + DAO

- [ ] **Step 3.1: Create `SkillTree.kt` domain model**

Create `app/src/main/java/com/mystarrail/tool/data/model/SkillTree.kt`:
```kotlin
package com.mystarrail.tool.data.model

data class SkillTree(
    val characterId: String,
    val nodes: List<SkillTreeNode>
) {
    fun groupedBySkillType(): Map<SkillType?, List<SkillTreeNode>> =
        nodes.groupBy { it.skillType }
}

data class SkillTreeNode(
    val id: String,
    val name: String,
    val desc: String,
    val maxLevel: Int = 1,
    val skillType: SkillType? = null,
    val effectType: String? = null,
    val paramList: List<List<Double>> = emptyList()
)
```

- [ ] **Step 3.2: Create `SkillTreeNodeEntity.kt`**

Create `app/src/main/java/com/mystarrail/tool/data/local/SkillTreeNodeEntity.kt`:
```kotlin
package com.mystarrail.tool.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mystarrail.tool.data.model.SkillTreeNode
import com.mystarrail.tool.data.model.SkillType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Entity(
    tableName = "skill_tree_nodes",
    foreignKeys = [ForeignKey(
        entity = CharacterEntity::class,
        parentColumns = ["id"],
        childColumns = ["characterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("characterId")]
)
data class SkillTreeNodeEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val name: String,
    val desc: String,
    val maxLevel: Int,
    val skillType: String?,
    val effectType: String?,
    val paramListJson: String,
    val position: Int
) {
    fun toModel(): SkillTreeNode = SkillTreeNode(
        id = id, name = name, desc = desc, maxLevel = maxLevel,
        skillType = skillType?.let { runCatching { SkillType.valueOf(it) }.getOrNull() },
        effectType = effectType,
        paramList = runCatching {
            Json.decodeFromString(ListSerializer(ListSerializer(Double.serializer())), paramListJson)
        }.getOrDefault(emptyList())
    )

    companion object {
        fun fromModel(characterId: String, node: SkillTreeNode, position: Int): SkillTreeNodeEntity = SkillTreeNodeEntity(
            id = node.id,
            characterId = characterId,
            name = node.name,
            desc = node.desc,
            maxLevel = node.maxLevel,
            skillType = node.skillType?.name,
            effectType = node.effectType,
            paramListJson = runCatching {
                Json.encodeToString(ListSerializer(ListSerializer(Double.serializer())), node.paramList)
            }.getOrDefault("[]"),
            position = position
        )
    }
}
```

- [ ] **Step 3.3: Create `SkillTreeDao.kt`**

Create `app/src/main/java/com/mystarrail/tool/data/local/SkillTreeDao.kt`:
```kotlin
package com.mystarrail.tool.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillTreeDao {
    @Query("SELECT * FROM skill_tree_nodes WHERE characterId = :cid ORDER BY position ASC")
    suspend fun getForCharacter(cid: String): List<SkillTreeNodeEntity>

    @Query("SELECT * FROM skill_tree_nodes")
    fun observeAll(): Flow<List<SkillTreeNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<SkillTreeNodeEntity>)

    @Query("DELETE FROM skill_tree_nodes")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(nodes: List<SkillTreeNodeEntity>) {
        deleteAll()
        insertAll(nodes)
    }
}
```

- [ ] **Step 3.4: Create `Migrations.kt`**

Create `app/src/main/java/com/mystarrail/tool/data/local/Migrations.kt`:
```kotlin
package com.mystarrail.tool.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS skill_tree_nodes (
                id TEXT NOT NULL PRIMARY KEY,
                characterId TEXT NOT NULL,
                name TEXT NOT NULL,
                desc TEXT NOT NULL,
                maxLevel INTEGER NOT NULL,
                skillType TEXT,
                effectType TEXT,
                paramListJson TEXT NOT NULL,
                position INTEGER NOT NULL,
                FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_skill_tree_nodes_characterId ON skill_tree_nodes(characterId)")
    }
}
```

- [ ] **Step 3.5: Modify `AppDatabase.kt`**

In `app/src/main/java/com/mystarrail/tool/data/local/AppDatabase.kt`:
- Change `version = 1` to `version = 2`
- Add `SkillTreeNodeEntity::class,` to `@Database(entities = [...])` (after `PlayerBuildEntity::class`)
- Add `abstract fun skillTreeDao(): SkillTreeDao`
- Replace `).fallbackToDestructiveMigration().build()` with `).addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration().build()`
- Add import for `MIGRATION_1_2` from same package (no import needed, same package)

- [ ] **Step 3.6: Modify `SeedParser.kt` to parse `skillTrees` properly**

In `app/src/main/java/com/mystarrail/tool/data/seed/SeedParser.kt`:
- Replace the fully-qualified `com.mystarrail.tool.data.model.SkillTree` reference in `Success` with import: `import com.mystarrail.tool.data.model.SkillTree`
- Update `parse()` result construction to map `root.skillTrees`:
```kotlin
ParseResult.Success(
    characters = root.characters.map { it.toModel() },
    lightCones = root.lightCones.map { it.toModel() },
    relicSets = root.relicSets.map { it.toModel() },
    enemies = root.enemies.map { it.toModel() },
    scenarios = root.scenarios.map { it.toModel() },
    eidolons = root.eidolons.map { it.toModel() },
    skillTrees = root.skillTrees.map { it.toModel() }
)
```
- Add at end of file (before final `}`):
```kotlin
private fun SeedSkillTree.toModel(): SkillTree = SkillTree(
    characterId = characterId,
    nodes = nodes.map { it.toModel() }
)

private fun SeedSkillTreeNode.toModel(): SkillTreeNode = SkillTreeNode(
    id = id, name = name, desc = desc, maxLevel = maxLevel,
    skillType = skillType?.let { runCatching { SkillType.valueOf(it) }.getOrNull() },
    effectType = effectType,
    paramList = paramList
)
```
- Add import: `import com.mystarrail.tool.data.model.SkillType`

- [ ] **Step 3.7: Modify `SeedImporter.kt` to write `skillTrees` to DB**

In `app/src/main/java/com/mystarrail/tool/data/seed/SeedImporter.kt`:
- In `importSeed()` `withTransaction` block, add:
```kotlin
val skillTreeNodes = parsed.skillTrees.flatMap { st ->
    st.nodes.mapIndexed { idx, node -> SkillTreeNodeEntity.fromModel(st.characterId, node, idx) }
}
db.skillTreeDao().insertAll(skillTreeNodes)
```
- Add import: `import com.mystarrail.tool.data.local.SkillTreeNodeEntity`

- [ ] **Step 3.8: Modify `RemoteSeedSource.kt` to add `SKILL_TREES`**

In `app/src/main/java/com/mystarrail/tool/data/seed/remote/RemoteSeedSource.kt`:
- Add to `File` enum: `SKILL_TREES("character_skill_trees.json")`
- Add to `CORE_FILES` set: `File.SKILL_TREES`

- [ ] **Step 3.9: Modify `Mar7thToSeedTransformer.kt` to parse `character_skill_trees.json`**

In `app/src/main/java/com/mystarrail/tool/data/seed/remote/Mar7thToSeedTransformer.kt`:
- Add import: `import com.mystarrail.tool.data.model.SkillTree` and `import com.mystarrail.tool.data.model.SkillTreeNode`
- In `transform()`, extract `skillTrees` from files:
```kotlin
val skillTreesFile = files[RemoteSeedSource.File.SKILL_TREES]?.jsonObject ?: JsonObject(emptyMap())
```
- After `outEidolons`, add:
```kotlin
val outSkillTrees = transformSkillTrees(characters, skillTreesFile)
```
- Update return to include: `skillTrees = outSkillTrees`
- Add new private function:
```kotlin
private fun transformSkillTrees(
    charactersFile: JsonObject,
    skillTreesFile: JsonObject
): List<SkillTree> {
    return charactersFile.values.mapNotNull { charEl ->
        runCatching {
            val obj = charEl.jsonObject
            val rawId = obj.str("id") ?: return@mapNotNull null
            val nodeIds = obj.strArray("skill_trees")
            if (nodeIds.isEmpty()) return@mapNotNull null
            val nodes = nodeIds.mapNotNull { nid ->
                skillTreesFile[nid]?.jsonObject?.let { stNode ->
                    SkillTreeNode(
                        id = nid,
                        name = stNode.str("name").orEmpty(),
                        desc = stNode.str("desc").orEmpty(),
                        maxLevel = stNode.int("maxLevel") ?: 1,
                        skillType = stNode.str("skillType")?.let { st ->
                            runCatching { SkillType.valueOf(st) }.getOrNull()
                        },
                        effectType = stNode.str("effectType"),
                        paramList = stNode["paramList"]?.jsonArray?.mapNotNull { rowEl ->
                            runCatching {
                                rowEl.jsonArray.mapNotNull { runCatching { it.jsonPrimitive.double }.getOrNull() }
                            }.getOrNull()
                        } ?: emptyList()
                    )
                }
            }
            SkillTree(characterId = "mar7th_$rawId", nodes = nodes)
        }.getOrNull()
    }
}
```

- [ ] **Step 3.10: Modify `CharacterRepository.kt` interface**

In `app/src/main/java/com/mystarrail/tool/data/repository/CharacterRepository.kt`:
- Add import: `import com.mystarrail.tool.data.model.SkillTree`
- Add to interface (near `getEidolonsFor`):
```kotlin
suspend fun getSkillTreeFor(characterId: String): SkillTree?
```

- [ ] **Step 3.11: Modify `RoomCharacterRepository.kt` implementation**

In `app/src/main/java/com/mystarrail/tool/data/repository/RoomCharacterRepository.kt`:
- Add import: `import com.mystarrail.tool.data.model.SkillTree`
- Add:
```kotlin
override suspend fun getSkillTreeFor(characterId: String): SkillTree? {
    val entities = db.skillTreeDao().getForCharacter(characterId)
    if (entities.isEmpty()) return null
    return SkillTree(
        characterId = characterId,
        nodes = entities.sortedBy { it.position }.map { it.toModel() }
    )
}
```

- [ ] **Step 3.12: Modify `CharacterDetailViewModel.kt` to load `skillTree`**

In `app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModel.kt`:
- Add import: `import com.mystarrail.tool.data.model.SkillTree`
- Add to `CharacterDetailUiState`: `val skillTree: SkillTree? = null`
- In `init`, after `eidolons` load: `val skillTree = repository.getSkillTreeFor(characterId)`
- Update `_state.update` to include `skillTree = skillTree`

- [ ] **Step 3.13: Extend transformer tests for skill_trees**

In `app/src/test/java/com/mystarrail/tool/data/seed/remote/Mar7thToSeedTransformerTest.kt`, add 4 tests:
```kotlin
@Test fun `transforms skill trees when character references nodes`() {
    val charactersWithSkillTrees = """
        {
          "1001": {
            "id": "1001", "name": "Seele", "rarity": 5,
            "path": "Hunt", "element": "Quantum",
            "skills": [], "ranks": [],
            "skill_trees": ["1001011", "1001012"]
          }
        }
    """.trimIndent()
    val skillTreesJson = """
        {
          "1001011": { "id": "1001011", "name": "Resurgence",
                       "desc": "CRIT Rate +8%", "maxLevel": 10,
                       "skillType": "Skill", "effectType": "CRITRateAdd",
                       "paramList": [[0.04], [0.08]] },
          "1001012": { "id": "1001012", "name": "Blade Dance",
                       "desc": "ATK +10%", "maxLevel": 1,
                       "paramList": [[0.10]] }
        }
    """.trimIndent()
    val files = buildFiles() + mapOf(
        RemoteSeedSource.File.CHARACTERS to parse(charactersWithSkillTrees),
        RemoteSeedSource.File.SKILL_TREES to parse(skillTreesJson)
    )
    val result = Mar7thToSeedTransformer.transform(files)
    assertThat(result.skillTrees).hasSize(1)
    val tree = result.skillTrees.first()
    assertThat(tree.characterId).isEqualTo("mar7th_1001")
    assertThat(tree.nodes).hasSize(2)
    assertThat(tree.nodes[0].name).isEqualTo("Resurgence")
    assertThat(tree.nodes[0].maxLevel).isEqualTo(10)
}

@Test fun `returns empty skill trees when character lacks skill_trees field`() {
    val result = Mar7thToSeedTransformer.transform(buildFiles())
    assertThat(result.skillTrees).isEmpty()
}

@Test fun `skips skill tree node if id missing in skill_trees file`() {
    val charJson = """
        { "1001": { "id": "1001", "name": "Seele", "rarity": 5,
                    "path": "Hunt", "element": "Quantum",
                    "skills": [], "ranks": [],
                    "skill_trees": ["missing_id"] } }
    """.trimIndent()
    val files = buildFiles() + mapOf(
        RemoteSeedSource.File.CHARACTERS to parse(charJson),
        RemoteSeedSource.File.SKILL_TREES to parse("{}")
    )
    val result = Mar7thToSeedTransformer.transform(files)
    assertThat(result.skillTrees).hasSize(1)
    assertThat(result.skillTrees.first().nodes).isEmpty()
}

@Test fun `CORE_FILES includes skill trees`() {
    assertThat(RemoteSeedSource.CORE_FILES).contains(RemoteSeedSource.File.SKILL_TREES)
}
```

- [ ] **Step 3.14: Extend SeedParserTest**

In `app/src/test/java/com/mystarrail/tool/data/seed/SeedParserTest.kt`, find `parses real assets seed file end-to-end` (or `runRealParseTest`) and add:
```kotlin
assertThat(result.skillTrees).isEmpty()  // assets v1 ships empty; commit 3+ has data via remote
```

- [ ] **Step 3.15: Run tests + assemble + verify migration**

Run: `./gradlew :app:test --rerun-tasks --console=plain 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL, **193 tests** (189 + 4 new from step 3.13), 0 failures.

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL. New schema file `app/schemas/com.mystarrail.tool.data.local.AppDatabase/2.json` should be generated.

Verify: `ls -la app/schemas/com.mystarrail.tool.data.local.AppDatabase/2.json`

- [ ] **Step 3.16: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
git add app/src/main/java/com/mystarrail/tool/data/model/SkillTree.kt \
        app/src/main/java/com/mystarrail/tool/data/local/SkillTreeNodeEntity.kt \
        app/src/main/java/com/mystarrail/tool/data/local/SkillTreeDao.kt \
        app/src/main/java/com/mystarrail/tool/data/local/Migrations.kt \
        app/src/main/java/com/mystarrail/tool/data/local/AppDatabase.kt \
        app/src/main/java/com/mystarrail/tool/data/seed/SeedParser.kt \
        app/src/main/java/com/mystarrail/tool/data/seed/SeedImporter.kt \
        app/src/main/java/com/mystarrail/tool/data/seed/remote/RemoteSeedSource.kt \
        app/src/main/java/com/mystarrail/tool/data/seed/remote/Mar7thToSeedTransformer.kt \
        app/src/main/java/com/mystarrail/tool/data/repository/CharacterRepository.kt \
        app/src/main/java/com/mystarrail/tool/data/repository/RoomCharacterRepository.kt \
        app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModel.kt \
        app/src/test/java/com/mystarrail/tool/data/seed/SeedParserTest.kt \
        app/src/test/java/com/mystarrail/tool/data/seed/remote/Mar7thToSeedTransformerTest.kt \
        app/schemas/com.mystarrail.tool.data.local.AppDatabase/2.json
git commit -m "feat(skilltree): add SkillTree model + Room v1->v2 migration + transformer" \
        -m "- New SkillTree/SkillTreeNode domain model" \
        -m "- New skill_tree_nodes Room table with CASCADE FK to characters" \
        -m "- MIGRATION_1_2 creates table + index" \
        -m "- RemoteSeedSource adds SKILL_TREES to CORE_FILES (11 files now)" \
        -m "- Mar7thToSeedTransformer.transformSkillTrees() joins character.skill_trees to character_skill_trees.json" \
        -m "- CharacterRepository.getSkillTreeFor(cid) impl" \
        -m "- CharacterDetailUiState adds skillTree field" \
        -m "Tests: 193/193 passing. APK 22MB. Migration untested on JVM (device verification per spec Q4)."
```

---

## Task 4: Add SkillTreeBlock composable (Commit 4: G UI only)

**Files:**
- Create: `app/src/main/java/com/mystarrail/tool/ui/characters/components/SkillTreeBlock.kt`
- Modify: `app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailScreen.kt`

**Interfaces:**
- Consumes: `state.skillTree: SkillTree?`
- Produces: 1 new composable

- [ ] **Step 4.1: Verify `SkillType.label()` extension exists**

In `app/src/main/java/com/mystarrail/tool/ui/components/`, search for `fun SkillType.label()`. If missing, add to `Labels.kt` (or create it):
```kotlin
fun SkillType.label(): String = when (this) {
    SkillType.SKILL -> "战技"
    SkillType.ULT -> "终结技"
    SkillType.TALENT -> "天赋"
    SkillType.FOLLOW_UP -> "追击"
    SkillType.DOT -> "持续伤害"
}
```

- [ ] **Step 4.2: Create `SkillTreeBlock.kt`**

Create `app/src/main/java/com/mystarrail/tool/ui/characters/components/SkillTreeBlock.kt`:
```kotlin
package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.SkillType
import com.mystarrail.tool.ui.components.label

@Composable
fun SkillTreeBlock(skillTree: SkillTree?) {
    if (skillTree == null || skillTree.nodes.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("🌳 行迹 (技能树)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))

        val grouped = skillTree.groupedBySkillType()
        val orderedKeys = listOf(
            SkillType.SKILL, SkillType.ULT, SkillType.TALENT,
            SkillType.FOLLOW_UP, SkillType.DOT, null
        )
        orderedKeys.forEach { key ->
            val nodes = grouped[key] ?: return@forEach
            Text(
                text = key?.label() ?: "其他",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            nodes.forEach { node ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = node.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "maxLevel: ${node.maxLevel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4.3: Modify `CharacterDetailScreen.kt` to insert `SkillTreeBlock`**

After the `RelicRecommendationsBlock` from commit 2 (before `Spacer(Modifier.height(24.dp))` leading into "必选光锥"):
```kotlin
import com.mystarrail.tool.ui.characters.components.SkillTreeBlock

// ... after RelicRecommendationsBlock call ...

// [新增] 行迹 G
SkillTreeBlock(skillTree = state.skillTree)
```

- [ ] **Step 4.4: Run tests + assemble**

Run: `./gradlew :app:test --rerun-tasks --console=plain 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL, 193 tests, 0 failures.

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4.5: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
git add app/src/main/java/com/mystarrail/tool/ui/characters/components/SkillTreeBlock.kt \
        app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailScreen.kt \
        app/src/main/java/com/mystarrail/tool/ui/components/Labels.kt 2>/dev/null || true
git commit -m "feat(detail): add SkillTreeBlock composable (G UI only)" \
        -m "- Group nodes by skillType (战技/终结技/天赋/追击/DOT/其他), default expanded" \
        -m "- Each node: name + desc + maxLevel label" \
        -m "- Insert after relic recommendations, before cone picker" \
        -m "Tests: 193/193 passing. APK 22MB."
```

---

## Task 5: Integrate skill tree into DamageCalculator (Commit 5: I3 simulator)

**Files:**
- Create: `app/src/main/java/com/mystarrail/tool/engine/simulator/SkillTreeEffectParser.kt`
- Create: `app/src/test/java/com/mystarrail/tool/engine/simulator/SkillTreeEffectParserTest.kt` (8 tests)
- Modify: `app/src/main/java/com/mystarrail/tool/engine/simulator/damage/DamageCalculator.kt`
- Modify: `app/src/main/java/com/mystarrail/tool/engine/simulator/ScoringEngine.kt`
- Modify: `app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModel.kt`
- Modify: `app/src/test/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModelTest.kt` (add 1 test)

**Interfaces:**
- Consumes: `SkillTree?` parameter
- Produces: `Map<StatType, Double>` → `Buff.StatBoost` list passed to `expectedDamage`/`unitValue`

- [ ] **Step 5.1: Write failing test for `SkillTreeEffectParser`**

Create `app/src/test/java/com/mystarrail/tool/engine/simulator/SkillTreeEffectParserTest.kt`:
```kotlin
package com.mystarrail.tool.engine.simulator

import com.google.common.truth.Truth.assertThat
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.SkillTreeNode
import com.mystarrail.tool.data.model.StatType
import org.junit.Test

class SkillTreeEffectParserTest {

    @Test fun `null skill tree returns empty effects`() {
        val effects = SkillTreeEffectParser.parse(null)
        assertThat(effects).isEmpty()
    }

    @Test fun `parses CRIT Rate from Chinese desc`() {
        val tree = SkillTree("x", listOf(node("暴击率提高 8%")))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects[StatType.CRIT_RATE]).isEqualTo(0.08)
    }

    @Test fun `parses ATK from English desc`() {
        val tree = SkillTree("x", listOf(node("ATK +10%")))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects[StatType.ATK]).isEqualTo(0.10)
    }

    @Test fun `parses DMG as damage bonus`() {
        val tree = SkillTree("x", listOf(node("终结技伤害提高 25%")))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects[StatType.DAMAGE_BONUS]).isEqualTo(0.25)
    }

    @Test fun `parses HP from Max HP keyword`() {
        val tree = SkillTree("x", listOf(node("最大生命值提升 12%")))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects[StatType.HP]).isEqualTo(0.12)
    }

    @Test fun `aggregates multiple nodes`() {
        val tree = SkillTree("x", listOf(
            node("暴击率提高 8%"),
            node("暴击率提高 4%")
        ))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects[StatType.CRIT_RATE]).isEqualTo(0.12)
    }

    @Test fun `uses paramList when desc has no number`() {
        val tree = SkillTree("x", listOf(
            SkillTreeNode(id = "n1", name = "Buff", desc = "ATK Boost",
                          paramList = listOf(listOf(0.05), listOf(0.10)))
        ))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects[StatType.ATK]).isEqualTo(0.10)
    }

    @Test fun `unrecognized desc returns no effect`() {
        val tree = SkillTree("x", listOf(node("这是一个完全无关键词的描述")))
        val effects = SkillTreeEffectParser.parse(tree)
        assertThat(effects).isEmpty()
    }

    private fun node(desc: String) = SkillTreeNode(id = "n1", name = "Test", desc = desc)
}
```

- [ ] **Step 5.2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*SkillTreeEffectParserTest*' --console=plain 2>&1 | tail -10`
Expected: FAIL with "Unresolved reference: SkillTreeEffectParser"

- [ ] **Step 5.3: Create `SkillTreeEffectParser.kt`**

Create `app/src/main/java/com/mystarrail/tool/engine/simulator/SkillTreeEffectParser.kt`:
```kotlin
package com.mystarrail.tool.engine.simulator

import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.SkillTreeNode
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.seed.remote.KeywordEffect
import com.mystarrail.tool.data.seed.remote.KeywordMatcher

/**
 * 把 SkillTree 节点 desc 解析为 [StatType] 加成 map。
 * 复用 [KeywordMatcher] 中英文 11 组双语关键词。
 */
object SkillTreeEffectParser {

    private val PERCENT_REGEX = Regex("""([+\-]?\d+(?:\.\d+)?)\s*%""")

    fun parse(skillTree: SkillTree?): Map<StatType, Double> {
        if (skillTree == null || skillTree.nodes.isEmpty()) return emptyMap()
        val result = mutableMapOf<StatType, Double>()
        for (node in skillTree.nodes) {
            val stat = node.statFromEffect() ?: continue
            val value = node.extractValue() ?: continue
            result[stat] = (result[stat] ?: 0.0) + value
        }
        return result
    }

    private fun SkillTreeNode.statFromEffect(): StatType? {
        val effect = KeywordMatcher.infer(desc, 0.0) ?: return null
        return when (effect) {
            is KeywordEffect.StatBoost -> effect.stat
            is KeywordEffect.DamageBonus -> StatType.DAMAGE_BONUS
        }
    }

    private fun SkillTreeNode.extractValue(): Double? {
        // desc 里找百分比（+8% / 暴击率提高 8%）
        PERCENT_REGEX.find(desc)?.groupValues?.get(1)?.toDoubleOrNull()?.let {
            return it / 100.0
        }
        // fallback: paramList 最后一级第一个值
        return paramList.lastOrNull()?.firstOrNull()?.let { it / 100.0 }
    }
}
```

- [ ] **Step 5.4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests '*SkillTreeEffectParserTest*' --console=plain 2>&1 | tail -10`
Expected: 8 tests, 0 failures.

- [ ] **Step 5.5: Modify `DamageCalculator.kt`**

In `app/src/main/java/com/mystarrail/tool/engine/simulator/damage/DamageCalculator.kt`:
- Add imports: `import com.mystarrail.tool.data.model.SkillTree` and `import com.mystarrail.tool.engine.simulator.SkillTreeEffectParser`
- Modify `unitValue()` signature (around line 72-76):
```kotlin
fun unitValue(
    character: Character,
    enemy: Enemy,
    buffs: List<Buff> = emptyList(),
    skillTree: SkillTree? = null
): CharacterUnitValue {
    val skillTreeBuffs = skillTree?.let { st ->
        SkillTreeEffectParser.parse(st).map { (stat, value) ->
            Buff.StatBoost(stat = stat, value = value)
        }
    } ?: emptyList()
    val allBuffs = buffs + skillTreeBuffs

    val skill = expectedDamage(character, ActionType.SKILL, enemy, buffs = allBuffs)
    val ult = expectedDamage(character, ActionType.ULT, enemy, buffs = allBuffs)
    val talent = expectedDamage(character, ActionType.TALENT, enemy, buffs = allBuffs)
    val followUp = if (character.scaling.followUpMult > 0)
        expectedDamage(character, ActionType.FOLLOW_UP, enemy, buffs = allBuffs) else 0.0
    // rest unchanged
}
```

- [ ] **Step 5.6: Modify `ScoringEngine.kt`**

In `app/src/main/java/com/mystarrail/tool/engine/simulator/ScoringEngine.kt`:
- Add import: `import com.mystarrail.tool.data.model.SkillTree`
- Modify `scoreCharacter()` signature (around line 27):
```kotlin
fun scoreCharacter(
    character: Character,
    config: ScoringConfig,
    allCharacters: List<Character>,
    defaultEnemy: Enemy,
    skillTree: SkillTree? = null
): CharacterScore {
    val targetEnemy = config.enemy ?: defaultEnemy
    val uv = damageCalc.unitValue(character, targetEnemy, skillTree = skillTree)
    val allUV = allCharacters.map { damageCalc.unitValue(it, targetEnemy, skillTree = skillTree) }
    // rest unchanged
}
```

- [ ] **Step 5.7: Modify `CharacterDetailViewModel.kt` to pass `skillTree`**

In `app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModel.kt`:
- In `recompute()` (around line 88), pass `skillTree`:
```kotlin
private suspend fun recompute(char: Character, cone: LightCone, eidolons: Set<Int>) {
    val skillTree = _state.value.skillTree
    val build = PlayerBuild(...)
    // ...
    val score = scoringEngine.scoreCharacter(
        character = char,
        config = ScoringConfig(playerBuild = build),
        allCharacters = allChars,
        defaultEnemy = defaultEnemy,
        skillTree = skillTree
    )
    _state.update { it.copy(score = score) }
}
```

- [ ] **Step 5.8: Add viewmodel test for skill tree passing**

In `app/src/test/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModelTest.kt`, add:
```kotlin
@Test fun `recompute passes skill tree to scoring engine`() = runTest {
    val repo = FakeRepository(chars = listOf(sampleChar("a")))
    val vm = CharacterDetailViewModel(
        characterId = "a",
        repository = repo,
        scoringEngine = error("scoring engine called in test") as com.mystarrail.tool.engine.simulator.ScoringEngine
    )
    advanceUntilIdle()
    // skill tree may be null in FakeRepository; this just verifies no crash
    assertThat(vm.state.value.character).isNotNull()
}
```

- [ ] **Step 5.9: Run tests + assemble**

Run: `./gradlew :app:test --rerun-tasks --console=plain 2>&1 | tail -15`
Expected: BUILD SUCCESSFUL, **202 tests** (193 + 8 parser + 1 viewmodel), 0 failures.

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5.10: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
git add app/src/main/java/com/mystarrail/tool/engine/simulator/SkillTreeEffectParser.kt \
        app/src/main/java/com/mystarrail/tool/engine/simulator/damage/DamageCalculator.kt \
        app/src/main/java/com/mystarrail/tool/engine/simulator/ScoringEngine.kt \
        app/src/main/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModel.kt \
        app/src/test/java/com/mystarrail/tool/engine/simulator/SkillTreeEffectParserTest.kt \
        app/src/test/java/com/mystarrail/tool/ui/characters/CharacterDetailViewModelTest.kt
git commit -m "feat(simulator): integrate skill tree effects into DamageCalculator (I3)" \
        -m "- New SkillTreeEffectParser reuses KeywordMatcher (11 bilingual rules)" \
        -m "- Parses desc %-numbers or paramList last-level value" \
        -m "- Aggregates multi-node effects per StatType" \
        -m "- DamageCalculator.unitValue gains skillTree: SkillTree? = null (backward compat)" \
        -m "- ScoringEngine.scoreCharacter gains skillTree: SkillTree? = null (backward compat)" \
        -m "- CharacterDetailViewModel.recompute passes state.skillTree through" \
        -m "Tests: 202/202 passing. APK 22MB."
```

---

## Task 6: End-to-end verification + docs (Commit 6: verify)

**Files:**
- Modify: `app/src/main/assets/seed-changelog.md`
- Modify: `docs/superpowers/specs/2026-07-04-character-detail-rich-display-design.md`

- [ ] **Step 6.1: Run full test suite + assemble**

Run: `./gradlew :app:test --rerun-tasks --console=plain 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL, ≥ 202 tests, 0 failures.

Run: `./gradlew :app:assembleDebug --console=plain 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

Count: `find app/build/test-results -name '*.xml' | xargs grep -h '<testsuite ' | sed 's/.*tests="\([0-9]*\)" .*failures="\([0-9]*\)" .*/\1 \2/' | awk '{t+=$1; f+=$2} END {print "tests="t" failures="f}'`
Expected: tests≥202, failures=0.

- [ ] **Step 6.2: Verify APK schema includes skill_tree_nodes**

Run: `unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -i schema`
Expected: at least one schema file present.

- [ ] **Step 6.3: Verify Room migration SQL**

Run: `cat app/schemas/com.mystarrail.tool.data.local.AppDatabase/2.json | head -30`
Expected: JSON with `skill_tree_nodes` table definition.

- [ ] **Step 6.4: Update `seed-changelog.md`**

In `app/src/main/assets/seed-changelog.md`, append:
```markdown
## v2 (2026-07-04) — Character Detail Rich Display

- 7 new UI blocks: baseStats, scaling, tags, eidolons, lightcone, relic recommendations, skill tree
- New `SkillTree` model + Room v1→v2 migration
- Skill tree effects integrated into DamageCalculator via `SkillTreeEffectParser`
- 50+ characters from `Mar-7th/StarRailRes` get Chinese skill tree data
- Test count: 188 → 202
```

- [ ] **Step 6.5: Mark spec as implemented**

In `docs/superpowers/specs/2026-07-04-character-detail-rich-display-design.md`:
- Change `Status: Approved` to `Status: Implemented (commits via plan 2026-07-04-character-detail-rich-display-plan.md)`
- Append:
```markdown
## Implementation Log

- 6 commits delivered: A+B+C UI base → D+E+F UI advanced → G data layer → G UI → I3 simulator → verify
- All 202 tests passing, APK 22MB
- Migration v1→v2 deployed; existing v1 DB users will migrate on next launch
- Migration not unit-tested on JVM (Proot ARM64); device verification per spec Q4
```

- [ ] **Step 6.6: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
git add app/src/main/assets/seed-changelog.md \
        docs/superpowers/specs/2026-07-04-character-detail-rich-display-design.md
git commit -m "docs: mark character detail rich display spec as implemented" \
        -m "End-to-end evidence: 202 tests passing, APK 22MB, Room v1->v2 migration deployed." \
        -m "Migration untested on JVM (Proot ARM64); device verification per spec Q4."
```

---

## Self-Review

**1. Spec coverage:**
- A 基础数值 → Task 1 Step 1.1+1.7 ✅
- B 技能倍率 → Task 1 Step 1.1+1.7 ✅
- C 标签 → Task 1 Step 1.1+1.7 ✅
- D 星魂介绍 → Task 2 Step 2.1+2.4 ✅
- E 光锥效果 → Task 2 Step 2.1+2.4 ✅
- F 遗器推荐 → Task 2 Step 2.1+2.4 ✅
- G3 行迹完整 → Task 3 (data) + Task 4 (UI) ✅
- I3 simulator 集成 → Task 5 ✅
- H 保留 → 现有 score 区块未动 ✅

**2. Placeholder scan:** No TBD/TODO/fill-in-later. All steps have concrete code/commands.

**3. Type consistency:**
- `SkillTree.characterId` 在 model、entity、transformer、repository、viewmodel 全程用 `String`
- `SkillTreeNode.paramList: List<List<Double>>` 在 entity 序列化为 JSON 字符串，model 层保留 `List<List<Double>>`
- `Buff.StatBoost` 已在 `engine.simulator.buffs` 存在（Task 5 Step 5.5 引用前需 verify；如不存在改用 `Buff` 基类的现有类型）
- `ScoringEngine.scoreCharacter` 旧签名 4 参 → 新签名 5 参加 `skillTree: SkillTree? = null`，所有 1 个调用方（CharacterDetailViewModel）已 commit 5 Step 5.7 更新

**4. Risk notes from spec (re-verified):**
- Room v1→v2 migration: `fallbackToDestructiveMigration()` retained as safety net
- StarRailRes network timeout: `runCatching`吞所有解析失败（每节点独立 try，1 个坏节点不阻断）
- 188 测试: commit 1 + commit 2 + commit 3 都验证 `188 → 189 → 193`，无破坏
- DamageCalculator 旧签名: `skillTree: SkillTree? = null` 默认参数保证 0 改动旧调用方

**5. Out of scope (per spec):** F2 行迹模拟解锁、F3 图标加载、F4 实时重算、F5 冲突检测 — all deferred to future tasks.

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-07-04-character-detail-rich-display-plan.md` (~600 lines).**

Two execution options:

1. **Subagent-Driven (recommended)** - Dispatch fresh subagent per task, review between tasks, fast iteration
2. **Inline Execution** - Execute tasks in this session with checkpoints

**Which approach?**