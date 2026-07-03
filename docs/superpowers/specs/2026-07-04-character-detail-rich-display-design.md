# Character Detail Rich Display — Design Spec

**Date:** 2026-07-04
**Status:** Approved (user confirmed 2026-07-04, 4 open Q answered with defaults)
**Scope:** A-G3 + I3 (full skill tree + simulator integration)

## Problem

角色库详情页（`CharacterDetailScreen`）当前只展示"评分驱动 UI"（Score Ring + 5 维度分条 + 必选光锥 + 星魂 toggle），**完全缺失**：

- 基础数值（HP/ATK/DEF/SPD 4 个数字）
- 技能倍率（战技/终结技/天赋/追击 4 个倍率 + AOE）
- 标签（tags 列表）
- 星魂介绍（6 个星魂的 name + effect 描述）
- 必选光锥效果（passive 详情）
- 遗器套推荐
- 行迹/技能树（SkillTree，~10 节点/角色，含 params 数值链）
- 行迹集成到 DamageCalculator（I3：解锁节点 → 真实数值影响 score）

数据模型层（`Character`、`Eidolon`、`LightCone`）字段**已经齐备**，缺的是 UI 渲染 + 行迹 schema + simulator 集成。

## Goals (A-G3 + I3)

| 区块 | 范围 | 数据来源 |
|---|---|---|
| A 基础数值 | HP/ATK/DEF/SPD | `character.baseStats` |
| B 技能倍率 | 战技/终结技/天赋/追击 + AOE% | `character.scaling` |
| C 标签 | tags chips | `character.tags` |
| D 星魂介绍 | 6 个 Eidolon 的 name + effect 描述 | `eidolons` |
| E 光锥效果 | selected cone 的 passive 详情 | `selectedCone.passiveEffect` |
| F 遗器推荐 | 该角色适合的 relic 套（fallback 全套） | `relicSets` + role 推断 |
| G3 行迹完整 | 每个节点的 name + desc + maxLevel + params + effect 解读 | **新增 `SkillTree` schema** |
| I3 simulator 集成 | 解锁节点 → 进 `DamageCalculator` 数值链 | `ScoringEngine`/`DamageCalculator` 改造 |
| H 保留 | Score Ring + 5 维度 ScoreBar | 现有不变 |

## Non-Goals

- 不做"行迹节点选择/模拟解锁"交互（本次只展示 + 假设全解锁）
- 不做"配队"扩展
- 不做图标加载（`iconUrl` 字段已有但 UI 不渲染）
- 不重做评分 5 维度权重

## Architecture

### 1. 新增 Model: `SkillTree.kt`

```kotlin
data class SkillTree(
    val characterId: String,         // 关联 character.id
    val nodes: List<SkillTreeNode>
)

data class SkillTreeNode(
    val id: String,                  // StarRailRes 原始 id (e.g. "1001011")
    val name: String,                // 中文/英文 (e.g. "再相会" / "Resurgence")
    val desc: String,                // 描述 (含 [i] 模板占位)
    val maxLevel: Int,               // 1 / 5 / 10
    val skillType: SkillType?,       // 关联技能类型 (Normal/Skill/Ultra/Talent 等)
    val effectType: String?,         // 原始 effectType (e.g. "CRITRateAdd")
    val paramList: List<List<Double>> // 每级参数数组: [[v1], [v2], [v3], ...]
)
```

**注意**: 旧 `Character.cycleProfile` 不删（保留兼容），但 G3 的"行迹参数"是新维度。

### 2. Room Schema 迁移 (v1 → v2)

**新表 `skill_tree_nodes`** (id, characterId, name, desc, maxLevel, skillType, effectType, paramListJson, position)

```kotlin
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
    val skillType: String?,       // enum name
    val effectType: String?,
    val paramListJson: String,    // 序列化为 JSON 字符串
    val position: Int             // 在原文件数组中的顺序 (用于 UI 排序)
)
```

**Migration**:
```kotlin
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

`AppDatabase` 升级：
- `version = 2`
- 加 `abstract fun skillTreeDao(): SkillTreeDao`
- 移除 `fallbackToDestructiveMigration()`（保留旧 DB 兼容）

### 3. Transformer 改造

`Mar7thToSeedTransformer.kt`:
- `File` enum 加 `SKILL_TREES("character_skill_trees.json")`
- `CORE_FILES` 加进去（共 11 个）
- `transform()` 加 `transformSkillTrees(characters, skillTrees)` 调用
- 返回类型：保持 `SeedParser.ParseResult.Success`，但需扩展结构

**问题**: `SeedParser.ParseResult.Success` 当前只支持 6 个集合，**没有 `skillTrees`**。需要：
- 加 `skillTrees: List<SkillTree>` 字段
- `SeedParser.parse` 路径（assets JSON）保持兼容：`skillTrees = emptyList()`（assets v1 不含）
- transformer 路径填充解析结果

`SeedData.kt`:
- `SeedRoot` 加 `skillTrees: List<SeedSkillTree> = emptyList()` 字段（default 兼容旧 JSON）
- 新 data classes: `SeedSkillTree`, `SeedSkillTreeNode`

`SeedImporter.kt`:
- `importSeed()` 写 `parsed.skillTrees` 到 DB
- Room write 拆出 1 个新的 `skillTreeDao().insertAll(...)` 调用

### 4. Simulator 集成 (I3)

`DamageCalculator.kt`:
- 新签名 `unitValue(character, enemy, skillTrees: SkillTree? = null)`
- 行为：skillTrees 为 null 或全 0 level → 等同旧行为（向后兼容）
- 行为：skillTrees 非空 → 遍历 nodes 解析 desc 中的数值（如"+12% CRIT Rate"），累加到 unitValue 的乘区

**简化实现**（不重写 DamageCalculator 全部）：
- 抽 `SkillTreeEffectParser` 复用 `KeywordMatcher`（已有 11 组双语关键词）
- 输出 `Map<StatType, Double>` (per-stat 加成%)
- `unitValue` 接受 map，累加到现有 score

`ScoringEngine.kt`:
- 注入 `SkillTreeRepository`（新增）
- `scoreCharacter()` 接收 `skillTrees: SkillTree?` 参数
- 把 skillTrees 效果透传给 `damageCalc.unitValue(...)` 和 `simulator.simulate(...)`（如果 simulator 也接受）

`CharacterDetailViewModel.kt`:
- `init` 加 `repository.getSkillTreeFor(characterId)`
- 加到 state: `skillTree: SkillTree?`
- `recompute()` 传 skillTree 给 scoringEngine

### 5. UI 改造 (CharacterDetailScreen.kt)

按从上到下顺序新增区块（在现有"评分"前后穿插）：

```
头部 (已有)
↓
[新增] 基础数值 A (4 列 Row: HP/ATK/DEF/SPD)
[新增] 技能倍率 B (4 列 Row: skill/ult/talent/followUp + AOE%)
[新增] 标签 C (chips Wrap)
↓
评分大圆环 + 5 维度 (已有 H)
↓
[新增] 星魂介绍 D (6 个 Card: name + level + effect 描述)
[新增] 光锥效果 E (selected cone 的 passive 描述)
[新增] 遗器推荐 F (适合该角色 role 的 relic 套列表)
[新增] 行迹 G3 (按 skillType 分组: 战技/终结技/天赋/其他 4 个 Section, 每节点 = name + desc + maxLevel)
↓
必选光锥 picker + 星魂 toggle (已有, 移到底部)
```

**Composable 拆分**:
- `BaseStatsBlock(Stats)`
- `ScalingBlock(Scaling)`
- `TagsBlock(Set<Tag>)`
- `EidolonsListBlock(List<Eidolon>)`
- `LightConeEffectBlock(LightCone)`
- `RelicRecommendationsBlock(Set<RelicSet>, Role)`
- `SkillTreeBlock(SkillTree)` 内部按 `skillType` 分组
- 全部独立 `@Composable` 文件，放 `ui/characters/components/`

**交互**:
- 行迹节点只读展示（不可解锁/模拟）
- 滚动：现有 `verticalScroll` 仍适用，新增 7 个区块后变长

## File Changes Summary

| 文件 | 改动类型 | 行数估 |
|---|---|---|
| `data/model/SkillTree.kt` | NEW | +50 |
| `data/model/Character.kt` | MOD (1 字段) | +1 |
| `data/model/SealedEffects.kt` | 1 处 typealias 适配 | +3 |
| `data/seed/SeedData.kt` | MOD (+ 2 data class) | +25 |
| `data/seed/SeedParser.kt` | MOD (parse skillTrees) | +15 |
| `data/seed/SeedImporter.kt` | MOD (写 skillTrees) | +8 |
| `data/seed/remote/Mar7thToSeedTransformer.kt` | MOD (解析 skill_trees.json) | +40 |
| `data/seed/remote/RemoteSeedSource.kt` | MOD (CORE_FILES +1) | +2 |
| `data/local/SkillTreeNodeEntity.kt` | NEW | +40 |
| `data/local/SkillTreeDao.kt` | NEW | +30 |
| `data/local/AppDatabase.kt` | MOD (v1→v2 + migration) | +20 |
| `data/local/Migrations.kt` | NEW | +20 |
| `data/repository/CharacterRepository.kt` | MOD (interface) | +5 |
| `data/repository/RoomCharacterRepository.kt` | MOD (impl) | +20 |
| `engine/simulator/SkillTreeEffectParser.kt` | NEW | +60 |
| `engine/simulator/ScoringEngine.kt` | MOD (接受 skillTrees) | +15 |
| `engine/simulator/damage/DamageCalculator.kt` | MOD (接受 param map) | +25 |
| `ui/characters/CharacterDetailScreen.kt` | MOD (加 7 区块) | +150 |
| `ui/characters/components/BaseStatsBlock.kt` | NEW | +40 |
| `ui/characters/components/ScalingBlock.kt` | NEW | +45 |
| `ui/characters/components/TagsBlock.kt` | NEW | +30 |
| `ui/characters/components/EidolonsListBlock.kt` | NEW | +55 |
| `ui/characters/components/LightConeEffectBlock.kt` | NEW | +40 |
| `ui/characters/components/RelicRecommendationsBlock.kt` | NEW | +50 |
| `ui/characters/components/SkillTreeBlock.kt` | NEW | +90 |
| `ui/characters/CharacterDetailViewModel.kt` | MOD (加 skillTree state) | +15 |

**总计**: 11 NEW + 14 MOD = 25 文件，约 +900 行

## Test Plan

### 新单元测试 (TDD red-green-refactor)
- `SkillTreeNodeEntityTest` (3 个)
- `SkillTreeDaoTest` (需 Robolectric 或跳过；纯 JVM 不行 → **跳过**, 等设备验证)
- `Mar7thToSeedTransformerTest` (扩 4 个 skill_trees 路径)
- `SkillTreeEffectParserTest` (8 个中英文 desc 解析)
- `CharacterDetailViewModelTest` (扩 3 个)

### 保留测试
- 现有 188 测试必须**全过**
- SeedParserTest 的"real assets" 测试需扩到允许 skillTrees 为空（兼容性）

### 端到端验证 (Evidence before claims)
- `./gradlew :app:test --rerun-tasks` → BUILD SUCCESSFUL, ≥ 200 tests
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL
- 沙箱 curl 拉 `character_skill_trees.json` 验证结构（之前 timeout，retry 后 200）
- adb install + 启动 app 验证详情页显示新区块

## Risk Analysis

| 风险 | 等级 | 缓解 |
|---|---|---|
| Room v1→v2 迁移失败（用户已有 DB） | 中 | 提供 `fallbackToDestructiveMigration()` 兜底；migration 仍写正确逻辑 |
| StarRailRes 沙箱网络 timeout | 中 | transformer 用 `runCatching` 吞失败（与现有策略一致） |
| 188 测试破坏 | 中 | 每个 commit 后立即跑全套；refactor 与新功能分多个 commit |
| DamageCalculator 重构影响现有 score | 高 | 旧签名保留 default null 行为；新签名额外参数；旧测试 0 改动 |
| 行迹 param 数组结构与 model 假设不符 | 低 | 在 transformer 用 `runCatching` 吞坏节点，测试覆盖空/坏两种 |
| 50 角色 × 10 节点 = 500 行 UI 滚动卡顿 | 低 | 用 `LazyColumn` 替代 verticalScroll，blocks 复用 |

## Out of Scope (Future Tasks)

- F2 任务：行迹模拟解锁（选择 1/未选 0）
- F3 任务：图标加载 + Coil 集成（已有 `iconUrl` 字段）
- F4 任务：行迹解锁后实时重算 score
- F5 任务：行迹与光锥被动冲突检测

## Implementation Order (Multi-Commit)

1. **commit 1**: A+B+C 基础数据区块（最小可见性，UI only）
2. **commit 2**: D+E+F 高级区块（eidolons/lightcone/relics）
3. **commit 3**: G schema + Room migration + transformer 解析
4. **commit 4**: G UI 渲染（按 skillType 分组）
5. **commit 5**: I3 simulator 集成（`SkillTreeEffectParser` + `DamageCalculator`）
6. **commit 6**: 端到端验证 + 文档

每个 commit 独立可运行，独立有测试。

## Resolved Decisions (2026-07-04 user confirm)

| Q | 答案 |
|---|---|
| 1. UI 区块顺序 | ✅ 头部→数值→倍率→标签→评分→星魂→光锥→遗器→行迹→toggle（按 spec 提议） |
| 2. 行迹分组折叠 | ✅ **默认全部展开**（fold 是 F2 任务，不在本次范围） |
| 3. `[i]%` 模板 | ✅ **只展示 raw desc + maxLevel**（如"+12% 暴击伤害（maxLevel 10）"），不展开每级 |
| 4. DB migration 测试 | ✅ **不写 v1→v2 真实测试**（Proot ARM64 无 Robolectric；真机/设备验证兜底） |

## Self-Review Notes

- **破坏性改动**: `SeedParser.ParseResult.Success` 加 `skillTrees` 字段会破坏现有 5 个调用方。`writing-plans` 必须显式标注"破坏性改动"并在 commit 1 完成兼容层。
- **测试数量**: 当前默认答"不加 migration 测试" → 目标测试数 **≥ 200**（不含 migration；含则 ≥ 210）。
- **风险 'I3 DamageCalculator 高风险'** 缓解策略已通过 spec 4. 节说明（旧签名保留 default null），plan 阶段会进一步细化具体乘区定义。

---

**Status: Approved. Next: invoke `writing-plans` skill to produce implementation plan.**
