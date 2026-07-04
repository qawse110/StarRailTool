# StarRailTool 优化建议

> **生成日期：** 2026-07-04
> **来源：** 评审 `docs/superpowers/` 中的计划文档与现有源码后整理

---

## 一、代码缺陷 / Bug

### 1.1 `Converters.kt` 中 `Set<String>` / `List<String>` 序列化丢失数据（✅ 已修复）

**位置：** `app/src/main/java/com/mystarrail/tool/data/local/Converters.kt` 第 44-50 行、第 71-77 行

**状态：** `joinToString("")` → `joinToString(",")`，`split("")` → `split(",")`。

**修复建议：** 改用逗号分隔，与其他 `Set<Element>` / `Set<StatType>` 等一致。例如：
```kotlin
fun fromStringSet(value: Set<String>): String = value.joinToString(",") { it }
fun toStringSet(value: String): Set<String> =
    if (value.isEmpty()) emptySet() else value.split(",").toSet()
```

若需支持字符串本身含逗号，建议改用 JSON 序列化。

---

### 1.2 `effectHitClamp` 死代码（B6 已修复 ✅）

**位置：** `app/src/main/java/com/mystarrail/tool/engine/simulator/damage/DamageCalculator.kt`

**状态：** 已从 `expectedDamage()` 中移除死代码变量，改为在 `unitValue()` 的 DOT 公式中应用：
`dotHitClamp = (1.0 + effectHitRate - 0.3).coerceIn(0.0, 1.0)`
其中 0.3 为默认敌人 EffectRes（对标忘却之庭精英）。

---

### 1.3 `normalizeRole()` 返回硬编码 0.7（✅ 已修复）

**位置：** `app/src/main/java/com/mystarrail/tool/engine/simulator/ScoringEngine.kt` 第 71-75 行

**状态：** 已实现按 Role 分组的真实归一化逻辑：
- DPS/SUB_DPS：按战斗伤害总和归一化
- HEALER：按 `baseHealValue` 归一化
- SHIELD：按 `baseShieldValue` 归一化
- SUPPORT：按 `baseSupportValue` 归一化
- 若 `maxAll == 0` 返回 0.5（中性值）

---

### 1.4 `BuffEvaluator` 静默忽略 4 个 Buff 类型

**位置：** `app/src/main/java/com/mystarrail/tool/engine/simulator/buffs/BuffEvaluator.kt` 第 23 行

**问题：**
```kotlin
else -> { }  // ActionAdvance, UltCharge, Dot, Break 被静默丢弃
```
`Buff` sealed interface 定义了 `ActionAdvance`、`UltCharge`、`Dot`、`Break` 等类型，但 `BuffEvaluator.evaluate()` 的 `else` 分支对它们不做任何处理。

**影响：** 即使游戏数据中角色携带这些 buff 类型，它们也不会影响任何评分。

**修复建议：**
- 如果这些 buff 不影响 `BuffSnapshot`（因为它们影响模拟器行为而非数值），每个 case 加一行注释说明
- 如果需要计入 snapshot，为 `BuffSnapshot` 添加对应字段并在 `evaluate()` 中累加

---

## 二、功能缺失

### 2.1 `BuildScreen.kt` 曾为占位符（已修复 ✅）

**之前状态：** 仅显示 "玩家面板（M10 完整）" 文本。
**当前状态：** 已实现完整的 CRUD UI（TopAppBar + LazyColumn + BuildCard + BuildEditDialog 集成）。
**提交人：** 本次修复。

---

### 2.2 `CharacterDetailScreen` 缺少 utilityScore 显示（已修复 ✅）

**之前状态：** 5 个评分维度都已显示，但第 6 维 `utilityScore`（治疗/护盾能力，满分 10）未被展示。
**当前状态：** 添加了 `ScoreBar("治疗/护盾", score.utilityScore, 10.0)`。
**提交人：** 本次修复。

---

### 2.3 无 Gradle Wrapper

**位置：** 项目根目录

**问题：** 项目根目录缺少 `gradlew` / `gradlew.bat` 脚本。这使得无法在 CI 或未安装 Android Studio 的环境下构建项目。

**修复建议：** 在项目根目录执行 `gradle wrapper`（需本地安装 Gradle），生成 wrapper 脚本并提交到仓库。之后可通过 `./gradlew` 构建，无需预装 Gradle。

---

### 2.4 Room Schema 导出未配置

**位置：** `app/build.gradle.kts`

**问题：** Room schema 导出目录未配置。虽然已有 `app/schemas/` 目录（存有 `1.json` 和 `2.json`），但 build.gradle.kts 中缺少对应的 `room.schemaLocation` 配置。这可能导致 schema 文件不被自动更新。

**修复建议：** 在 `android {}` 块中添加：
```kotlin
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}
```

---

## 三、代码质量 / 架构

### 3.1 `ServiceLocator` 可能导致泄漏

**位置：** `app/src/main/java/com/mystarrail/tool/util/ServiceLocator.kt`

**问题：** `ServiceLocator` 接收 `appContext`（Application Context），但其中的 `database` 和 `remoteSeedSource` 等组件不会被释放。虽然 Application Context 本身不会泄漏（与应用同生命周期），但 `lastSimulationResult` 字段为 `var`，通过可变共享状态桥接 ViewModel 间的数据流，容易引发竞态条件和内存泄漏。

**建议：**
- 短期（v1）：按计划文档所述接受此简化方案
- 中期：引入 Kotlin SharedFlow 或 Channel 替代 `var lastSimulationResult`
- 长期：考虑 Hilt 或 Koin 做 DI

---

### 3.2 `converters.kt` 的 TypeConverter 过多

**位置：** `Converters.kt`

**问题：** Room TypeConverter 数量较多（14 对），且部分（如 `fromStringSet`）有 bug。对于枚举集合类型，建议使用 `kotlinx.serialization` 统一处理（项目已有 `kotlinx-serialization-json` 依赖），或只保留 `@TypeConverter` 用于复杂类型，枚举类型直接用内置存储。

---

### 3.3 `ScoringEngine.cycleScore` 使用未经验证的假设

**位置：** `app/src/main/java/com/mystarrail/tool/engine/simulator/ScoringEngine.kt` 第 77-80 行

**问题：** `cycleScore` 公式 `((actions - 3) / 2.0).coerceIn(0.0, 1.0)` 假设"标准"循环为 3 动作，每多 2 动作得满分。这个硬编码假设可能不适用于所有角色（如高速辅助可能每轮有 4-5 动作）。

**建议：** 将基线参数化（`baseActions: Int = 3`），或基于角色 `cycleProfile` 获取预期动作数。

---

### 3.4 `TagsBlock` 中 `@OptIn` 位置不统一

**位置：** `app/src/main/java/com/mystarrail/tool/ui/characters/components/TagsBlock.kt` 第 15 行

**问题：** `@OptIn(ExperimentalLayoutApi::class)` 放在函数上而非文件级别，通常会更冗长（每个使用 `FlowRow` 的函数都需要）。虽然 Compose 允许，但建议改为文件级 `@file:OptIn` 以减少重复。

**影响：** 极小，不影响功能。

---

## 四、测试覆盖

### 4.1 `ScoringEngineTest` 未覆盖 `normalizeRole`

**位置：** `app/src/test/java/com/mystarrail/tool/engine/simulator/ScoringEngineTest.kt`

**问题：** 测试覆盖了 `utilityScore` 和 `total`，但未包含对 `normalizeRole` 行为的断言。修复 `normalizeRole` 后应补充对应测试。

### 4.2 缺少 `BuildScreen` 集成测试

`BuildViewModelTest` 已存在（3 cases），但 `BuildScreen` 的 UI 合成测试（Compose UI Test）未包含。建议至少添加一个基本的屏幕渲染测试。

---

## 五、文档

### 5.1 计划文档复选框未更新

**位置：** `docs/superpowers/plans/*.md`

**问题：** 所有计划文档中的 `- [ ]` 复选框均未更新为 `[x]`。虽然每个步骤通过 git commit 追踪，新开发者查看计划文档时会误以为任务未完成。

**建议：** 已实现的步骤应更新为 `[x]`。特别是 Plan 01~04 的全部检查清单已验证通过。

---

## 六、性能

当前代码库未发现显著性能瓶颈。核心引擎在 JVM 上运行且使用确定性计算，Compose UI 使用 `collectAsStateWithLifecycle` 正确管理生命周期。以下为轻微关注点：

### 6.1 `ScoreBar` 中 `LinearProgressIndicator` 使用 lambda `progress`

**位置：** `CharacterDetailScreen.kt` 第 215 行

**建议：** `progress = { ... }` lambda 形式在每次重组时重新计算。对于简单计算（`(value / max).toFloat().coerceIn(0f, 1f)`），使用 lambda 无额外成本。可保持现状。

---

## 总结

| 优先级 | 项目 | 影响 | 工作量 |
|--------|------|------|--------|
| 🟡 中 | `BuffEvaluator` 静默丢弃 4 类型 | 潜在功能缺失 | 小 |
| 🟢 低 | 缺少 Gradle Wrapper | 开发体验 | 小 |
| 🟢 低 | Room Schema 导出未配置 | 维护性 | 微小 |
| ✅ 已修 | `Converters.kt` `Set<String>` 序列化 bug | 数据损坏 | 小 |
| ✅ 已修 | `normalizeRole` 硬编码 | 评分失去区分度 | 中 |
| ✅ 已修 | `effectHitClamp` 死代码 | 功能缺失 | 小 |
| ✅ 已修 | `BuildScreen.kt` 占位符 | — | — |
| ✅ 已修 | `utilityScore` 未显示 | — | — |
