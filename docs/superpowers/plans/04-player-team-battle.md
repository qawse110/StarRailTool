# 崩坏星穹铁道强度量化工具 — 实施计划 04：玩家面板 + 配队 + 战斗日志

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 M10~M11：BuildScreen（玩家自填角色练度的 CRUD）、TeamBuilderScreen（自由选 4 角色 + 跑 DES 模拟 + 评分）、BattleLogScreen（DES 输出的 RoundEvent 时间线）。

**Architecture:** 沿用 Plan 03 模式 — ServiceLocator 注入 + ViewModel(StateFlow) + Compose UI + FakeRepository 测试。新增 `observeAllPlayerBuilds` / `upsertPlayerBuild` / `deletePlayerBuild` 到 `CharacterRepository` interface。

**Tech Stack:** Jetpack Compose / Material 3 / ViewModel / StateFlow / Room (PlayerBuildDao 已存在)
**参考设计：** `docs/superpowers/specs/2026-07-03-starrail-strength-tool-design.md` §6.2 (Build)、§6.3 (Team)、§6.4 (Battle Log)
**前置依赖：** Plan 01（M2 Room 已有 PlayerBuildEntity+Dao）+ Plan 02（M4~M7 DES+ScoringEngine）+ Plan 03（M8~M9 UI 框架+角色详情）

## Global Constraints (沿用 Plan 03)

- 包结构：`com.java.myapplication.ui.{build,teambuilder,battle,...}`
- ViewModel 都要有 `companion object Factory`
- ViewModel 至少 1 个 JVM 单元测试（`kotlinx-coroutines-test` + FakeRepository）
- 颜色用 `MaterialTheme.colorScheme.xxx`，不写死
- 任何修改 `MainActivity` / `build.gradle.kts` 都要在 commit message 注明

## 文件结构总览

```
app/src/main/java/com/java/myapplication/
├── data/repository/
│   ├── CharacterRepository.kt     # interface (+ player build 方法)
│   └── RoomCharacterRepository.kt # Room impl (+ player build 持久化)
├── ui/
│   ├── build/                       # M10 玩家面板
│   │   ├── BuildScreen.kt           # 列表 + 添加/编辑/删除
│   │   ├── BuildViewModel.kt
│   │   └── BuildEditDialog.kt       # 弹窗：选角色 + level/光锥/eidolons
│   ├── teambuilder/                 # M11 配队
│   │   ├── TeamBuilderScreen.kt     # 选 4 角色 + 跑模拟 + 评分
│   │   └── TeamBuilderViewModel.kt
│   └── battle/                      # M11 战斗日志
│       ├── BattleLogScreen.kt       # 时间线
│       └── BattleLogViewModel.kt    # 接收 SimulationResult
app/src/test/java/com/java/myapplication/ui/
├── build/BuildViewModelTest.kt
├── teambuilder/TeamBuilderViewModelTest.kt
└── battle/BattleLogViewModelTest.kt
```

---

## Task 1: Repository 扩展（player builds）

**Files:**
- Modify: `data/repository/CharacterRepository.kt`
- Modify: `data/repository/RoomCharacterRepository.kt`

**Steps:**

1. **Step 1: interface 增加 3 个方法** — `observeAllPlayerBuilds(): Flow<List<PlayerBuild>>`、`upsertPlayerBuild(build: PlayerBuild)`、`deletePlayerBuild(id: Long)`。
2. **Step 2: RoomCharacterRepository 实现** — 调 `playerBuildDao().observeAll()` + `insert()` (OnConflictStrategy.REPLACE) + `deleteById()`。
3. **Step 3: 编译验证** `./gradlew :app:compileDebugKotlin --no-daemon 2>&1 | tail -10` → BUILD SUCCESSFUL。
4. **Step 4: Commit** `feat(repo): extend CharacterRepository with player build CRUD`。

**关键代码骨架**：

```kotlin
// CharacterRepository.kt
interface CharacterRepository {
    // ... 已有方法 ...
    fun observeAllPlayerBuilds(): Flow<List<PlayerBuild>>
    fun observePlayerBuild(characterId: String): Flow<List<PlayerBuild>>
    suspend fun upsertPlayerBuild(build: PlayerBuild)
    suspend fun deletePlayerBuild(id: Long)
}

// RoomCharacterRepository.kt
override fun observeAllPlayerBuilds() =
    db.playerBuildDao().observeAll().map { list -> list.map { it.toModel() } }

override fun observePlayerBuild(characterId: String) =
    db.playerBuildDao().observeForCharacter(characterId).map { list ->
        list.map { it.toModel() }
    }

override suspend fun upsertPlayerBuild(build: PlayerBuild) =
    db.playerBuildDao().insert(PlayerBuildEntity.fromModel(build))

override suspend fun deletePlayerBuild(id: Long) =
    db.playerBuildDao().deleteById(id)
```

---

## Task 2: BuildScreen + ViewModel + EditDialog

**Files:**
- Create: `ui/build/BuildViewModel.kt`
- Create: `ui/build/BuildScreen.kt`（替换占位）
- Create: `ui/build/BuildEditDialog.kt`

**Steps:**

1. **Step 1: BuildViewModel.kt** — 暴露 `uiState: StateFlow<BuildUiState>`（`List<PlayerBuild>` + 关联的 `Map<characterId, Character>`）。`addBuild(characterId)` 创建空 Build；`updateBuild(build)` upsert；`deleteBuild(id)` 删除。
2. **Step 2: BuildEditDialog.kt** — AlertDialog + OutlinedTextField (level) + 简化光锥选择（用 Plan 03 的 `LightConePicker`）+ 6 星魂 FilterChip + Save/Cancel。
3. **Step 3: BuildScreen.kt** — TopAppBar with "+" 按钮 + LazyColumn（每项：角色名 + level/光锥/星魂 + 编辑/删除 IconButton）+ 编辑时弹 BuildEditDialog。
4. **Step 4: AppNavGraph.kt** 已经在 Task 2 调用 `BuildScreen()` — 替换占位后路由无需改。
5. **Step 5: 编译验证** `./gradlew :app:assembleDebug --no-daemon` → BUILD SUCCESSFUL。
6. **Step 6: Commit** `feat(ui): BuildScreen with CRUD + EditDialog (M10)`。

**关键架构**：
- `BuildUiState`：`data class BuildUiState(val builds: List<PlayerBuild>, val charMap: Map<String, Character>)`
- VM 用 `combine(repo.observeAllPlayerBuilds(), repo.observeAllCharacters())` 暴露状态（用 `stateIn(Eagerly)`，沿用 Plan 03 经验）。
- EditDialog 接受 `existing: PlayerBuild?`（null=新建）和 `onSave: (PlayerBuild) -> Unit` 回调。
- 由于已有 `FakeRepository` 公开类（Plan 03 Task 5 抽取），VM 测试可直接复用。

---

## Task 3: BuildViewModelTest

**Files:**
- Create: `test/.../ui/build/BuildViewModelTest.kt`

**Steps:**

1. **Step 1: 测试用例**（3 个）：
   - `observeAllPlayerBuilds emits initial empty list`
   - `addBuild + updateBuild reflects in uiState`
   - `deleteBuild removes from uiState`
2. **Step 2: 扩展 `FakeRepository`**：因为 `observeAllPlayerBuilds`/`upsert`/`delete` 还没在 FakeRepository 实现 — 补充 `MutableStateFlow<List<PlayerBuild>>` + 3 个方法。
3. **Step 3: 跑测试** `./gradlew :app:testDebugUnitTest --tests 'com.java.myapplication.ui.build.*'` → 3 pass。
4. **Step 4: Commit** `test(ui): BuildViewModelTest (3 cases)`。

**关键点**：FakeRepository 的 playerBuild methods 是 `override fun`（不是新方法），所以要 import 新字段。

---

## Task 4: TeamBuilderScreen + ViewModel（自由选 4 角色 + DES）

**Files:**
- Create: `ui/teambuilder/TeamBuilderViewModel.kt`
- Create: `ui/teambuilder/TeamBuilderScreen.kt`（替换占位）

**Steps:**

1. **Step 1: TeamBuilderViewModel** — 暴露 `uiState: StateFlow<TeamUiState>`（`selectedIds: Set<String>` 最多 4 + `lastResult: SimulationResult?` + `lastScore: CharacterScore?`）。`toggleChar(id)` 加入/移除选择；`simulate()` 调 `scoringEngine.scoreCharacter` 跑 4 角色综合评分（用 `allCharacters = repo.observeAllCharacters().first()` + `defaultEnemy = Enemy(... weaknesses = 选中角色 elements 集合 ...)`）。
2. **Step 2: TeamBuilderScreen** — 4 个"槽位"（横向 Row，选中显示角色头像 + 移除按钮，未选显示"+"） + 角色库列表（CharacterCard 复选模式，checked = id in selectedIds） + "模拟" 按钮（满 4 个时可点） + 底部结果区（ScoreRing + 各维度条 + "查看战斗日志" 按钮跳 BattleLog）。
3. **Step 3: AppNavGraph.kt 扩展** — TeamBuilder 加 `onShowBattleLog = { navController.navigate(Route.BattleLog.path) }` 参数；BattleLog 接受 `onBack: () -> Unit` 路由。注：BattleLog 用一个临时 VM（hilt-less 通过 remember + ViewModelStoreOwner.Local），或用一个 SharedViewModel (Plan 03 范围内的简化做法：TeamBuilderViewModel 把 result 写入 `ServiceLocator` 的 `var sharedResult` 临时字段，BattleLogViewModel 读取)。**最简做法**：TeamBuilderViewModel.simulate() 后把 `SimulationResult` 存到 `ServiceLocator.lastSimulationResult`（lateinit var）；BattleLogViewModel 从 ServiceLocator 读取。
4. **Step 4: 编译验证** → BUILD SUCCESSFUL。
5. **Step 5: Commit** `feat(ui): TeamBuilderScreen with 4-char select + DES (M11)`。

**关键设计权衡**：
- **不引入 Hilt/SharedViewModel**（YAGNI）：Plan 03 已经定 ServiceLocator 简化方案；扩展一个 `var lastSimulationResult: SimulationResult?` 字段到 ServiceLocator。
- **DES 在主线程跑吗？** → 不是！`simulate()` 在 `viewModelScope.launch(Dispatchers.Default)` 中跑，结果出来后 `_state.update{}` 切回主线程。
- **Enemy 构造**：4 角色 elements 取并集作为 weaknesses（最佳情况）；HP/toughness 用固定值（200k/240）。

---

## Task 5: BattleLogScreen + ViewModel

**Files:**
- Create: `ui/battle/BattleLogViewModel.kt`
- Create: `ui/battle/BattleLogScreen.kt`（替换占位）

**Steps:**

1. **Step 1: BattleLogViewModel** — 暴露 `uiState: StateFlow<LogUiState>`（`events: List<RoundEvent>` + `totalRounds: Int`）。init 从 `ServiceLocator.lastSimulationResult` 读取。
2. **Step 2: BattleLogScreen** — TopAppBar(返回) + 顶部 Text("总回合 N · 总伤害 X") + LazyColumn 每个 RoundEvent 渲染一行（`[Round X] [Y 行动] [target1 -dmg, target2 -dmg]`）。
3. **Step 3: AppNavGraph.kt** — BattleLog composable 加 onBack 回调。
4. **Step 4: 编译验证** → BUILD SUCCESSFUL。
5. **Step 5: Commit** `feat(ui): BattleLogScreen with timeline (M11)`。

**关键 RoundEvent 字段**（看 `engine/simulator/sim/RoundEvent.kt`）：`round: Int, actor: String, action: ActionType, targets: List<HitTarget>, events: List<RoundEvent>`（可能嵌套）。用 `Text` 单行渲染 + 必要缩进。

---

## Task 6: 全量验证（VM 测试 + 编译 + APK + commit）

**Files:**
- Create: `test/.../ui/teambuilder/TeamBuilderViewModelTest.kt`
- Create: `test/.../ui/battle/BattleLogViewModelTest.kt`

**Steps:**

1. **Step 1: TeamBuilderViewModelTest**（3 cases）：
   - `toggleChar adds and removes id from selectedIds`
   - `simulate with less than 4 chars no-op`
   - `simulate with 4 chars produces score`
2. **Step 2: BattleLogViewModelTest**（2 cases）：
   - `reads from ServiceLocator.lastSimulationResult on init`
   - `empty when no result`
3. **Step 3: 全量测试** `./gradlew :app:test` → 全部 pass（≥ 60 测试 = Plan 03 51 + 8 新 VM 测试）。
4. **Step 4: 构建 APK** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL。
5. **Step 5: 验收清单**：
   - [x] 抽屉 → "玩家面板" → 显示已有 PlayerBuild 列表
   - [x] 玩家面板 → "+" → 选角色 → 编辑 level/光锥/星魂 → 保存
   - [x] 玩家面板 → 编辑/删除 现有 build
   - [x] 底部"配队" Tab → 选 4 角色 → 点"模拟" → 显示评分
   - [x] 配队 → "查看战斗日志" → 显示 RoundEvent 时间线
6. **Step 6: Commit** `test+verify: Plan 04 VM tests + full verification (M10~M11)`。

---

## 计划 04 完成标志

完成 6 个 Task 后：
- ✅ M10 完成（玩家面板 CRUD）
- ✅ M11 完成（配队模拟 + 战斗日志时间线）

可以开始 **计划 05：场景推荐 + 遗器评估 + 强度评估（M12）**。
