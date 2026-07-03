# 崩坏：星穹铁道 角色/配队强度量化工具 — 设计文档

- **日期**：2026-07-03
- **状态**：待用户审阅
- **范围**：第一版（M1~M14）

---

## 1. 目标与原则

### 1.1 项目目标
为崩坏：星穹铁道玩家提供一款**专业、实用、可扩展**的 Android 工具，覆盖：
- 角色强度量化（基于真实战斗模拟）
- 配队协同评分
- 敌方场景最优配队推荐
- 玩家面板编辑
- 遗器刷取评估
- 社区/官方预设方案库

### 1.2 设计原则
- **数据驱动**：所有评分来自战斗模拟器输出，**不依赖人工权重**。
- **可追溯**：每个分数都能展开看到"模拟日志"和"乘区明细"。
- **可扩展**：新角色/光锥/遗器只追加 JSON，App 端无需改代码。
- **离线优先**：内置种子数据，无网络可完整使用。
- **优雅降级**：更新失败、wiki 抓取失败均不阻断核心功能。

### 1.3 非目标（YAGNI）
- 不做账号绑定 / 云存档同步
- 不做社交分享 / 排行榜
- 不做游戏内自动化（违反 ToS）
- 不做 OCR 截图识别（成本过高）
- v1 不做抽卡概率模型

---

## 2. 技术栈

| 层级 | 选型 | 理由 |
|---|---|---|
| UI | Jetpack Compose + Material 3 | 模板已配置 |
| 导航 | Navigation Compose | 单 Activity 标配 |
| 状态 | ViewModel + StateFlow | 主流、简单 |
| 异步 | Kotlin Coroutines + Flow | 必选 |
| 本地数据库 | Room | 角色/配队/玩家面板缓存 |
| 网络 | Retrofit + OkHttp + Moshi | 拉取远端 JSON |
| 序列化 | kotlinx.serialization | 体积小、官方 |
| 图片 | Coil 3 (Compose) | 角色立绘/光锥图标 |
| 周期任务 | WorkManager | 定时拉取/抓取 |
| HTML 解析 | jsoup | wiki 抓取 |
| 单元测试 | JUnit4 + Turbine + Truth | 引擎单测 |

---

## 3. 模块拆分（按包，不分 Gradle module）

```
com.java.myapplication/
├── StarRailApp.kt                # Application
├── MainActivity.kt               # 单 Activity
├── ui/
│   ├── nav/                      # NavGraph + 路由
│   ├── characters/               # 角色库
│   ├── assessment/               # 强度评估
│   ├── teambuilder/              # 配队模拟
│   ├── scenario/                 # 场景推荐
│   ├── build/                    # 玩家面板编辑器
│   ├── battle/                   # 战斗日志调试器
│   ├── scraper/                  # 抓取 UI
│   ├── relic/                    # 遗器评估
│   ├── components/               # 通用 Composable
│   └── theme/                    # Color/Theme/Type
├── data/
│   ├── model/                    # 领域模型
│   ├── local/                    # Room
│   ├── remote/                   # Retrofit
│   ├── scraper/                  # WikiScraper
│   ├── repository/               # Repository
│   └── sync/                     # UpdateManager + Worker
├── engine/
│   ├── simulator/                # 战斗模拟器
│   │   ├── BattleSimulator
│   │   ├── DiscreteEventSimulator
│   │   ├── DamageCalculator
│   │   ├── MechanicEngine
│   │   └── rules/                # 各机制规则
│   ├── team/                     # TeamBuilder
│   ├── match/                    # MatchupRecommender
│   ├── relic/                    # RelicScorer
│   └── lightcone/                # PassiveEvaluator
└── util/
```

**理念**：UI 只调 Repository / ViewModel；引擎层不依赖 Android API，可纯 JVM 单测。

---

## 4. 领域数据模型

### 4.1 角色 Character

```kotlin
data class Character(
    val id: String, val name: String, val rarity: Int,
    val path: Path, val element: Element, val role: Role,
    val tags: Set<Tag>, val baseStats: Stats,
    val scaling: Scaling, val cycleProfile: CycleProfile?,
    val iconUrl: String, val version: Int
)

enum class Path { WARRIOR, ROGUE, MAGE, SHAMAN, WARLOCK, HUNT, PRIEST }
enum class Element { PHYSICAL, FIRE, ICE, LIGHTNING, WIND, QUANTUM, IMAGINARY }
enum class Role { DPS, SUB_DPS, SUPPORT, HEALER, SHIELD }
enum class Tag {
    DOT, ULT_CHARGE, ACTION_ADVANCE, SPEED_BOOST, ATK_BOOST, CRIT_BOOST,
    DEBUFF, SHIELD, HEAL, CLEANSE, BREAK_EFFECT, FOLLOW_UP,
    ULT_DMG_BONUS, ENERGY_REGEN, SINGLE_TARGET, AOE, IMPULSE, SUMMON
}

data class Stats(val hp: Double, val atk: Double, val def: Double, val spd: Double)
data class Scaling(
    val skillMult: Double, val ultMult: Double, val talentMult: Double,
    val followUpMult: Double = 0.0, val aoeRatio: Double = 0.0
)
data class CycleProfile(
    val cycleActions: Int, val spdBreakpoints: List<Double>,
    val isFollowUp: Boolean = false, val isDot: Boolean = false
)
```

### 4.2 光锥 LightCone

```kotlin
data class LightCone(
    val id: String, val name: String, val path: Path, val rarity: Int,
    val passiveName: String, val passiveEffect: PassiveEffect,
    val s5Multiplier: Double = 1.0
)

sealed interface PassiveEffect {
    data class StatBoost(val stat: StatType, val value: Double, val target: Target = Target.SELF) : PassiveEffect
    data class DamageBonus(val multiplier: Double, val condition: DmgCondition) : PassiveEffect
    data class SkillBoost(val type: SkillType, val multiplier: Double) : PassiveEffect
    data class EnergyRegen(val perTurn: Double) : PassiveEffect
    data class Composite(val effects: List<PassiveEffect>) : PassiveEffect
}

enum class StatType { ATK, HP, DEF, SPD, CRIT_RATE, CRIT_DMG, EHR, BRK_EFF, EFFECT_RES }
enum class Target { SELF, ALLY, TEAM, ENEMY, ALLIES_WITH_PATH }
enum class DmgCondition { ALWAYS, ULT_ACTIVE, FOLLOW_UP, DOT, BREAK, ULT_AFTER_SKILL, AFTER_EAT_SP }
enum class SkillType { SKILL, ULT, TALENT, FOLLOW_UP, DOT, ALL }
```

### 4.3 遗器 Relic

```kotlin
data class RelicSet(
    val id: String, val name: String,
    val twoPiece: PassiveEffect, val fourPiece: PassiveEffect,
    val suitableFor: Set<Role>
)

data class RelicBuild(
    val set4: String, val set2: String? = null,
    val mainStats: MainStats,
    val targetSubs: Set<StatType>, val notes: String = ""
)

data class MainStats(
    val body: StatType, val boots: StatType,
    val sphere: StatType, val rope: StatType
)
```

### 4.4 星魂 Eidolon

```kotlin
data class Eidolon(
    val id: String, val characterId: String, val level: Int,
    val name: String, val effect: EidolonEffect,
    val major: Boolean = false       // 质变级
)

sealed interface EidolonEffect {
    data class StatBoost(val stat: StatType, val value: Double, val target: Target = Target.SELF) : EidolonEffect
    data class NewMechanic(val mechanic: Tag, val param: Double = 1.0, val note: String = "") : EidolonEffect
    data class DamageBonus(val multiplier: Double, val condition: DmgCondition) : EidolonEffect
    data class EnemyDebuff(val stat: StatType, val value: Double) : EidolonEffect
    data class Composite(val effects: List<EidolonEffect>) : EidolonEffect
}
```

### 4.5 敌人 & 场景

```kotlin
data class Enemy(
    val id: String, val name: String, val count: Int,
    val weaknesses: Set<Element>, val type: EnemyType,
    val hp: Double, val toughness: Double = 0.0,
    val mechanics: Set<String>
)
enum class EnemyType { BOSS, ELITE, MOB, DOOM, SUMMON }

data class Scenario(
    val id: String, val name: String,
    val enemies: List<Enemy>, val difficulty: Int, val notes: String = ""
)
```

### 4.6 玩家面板 PlayerBuild

```kotlin
data class PlayerBuild(
    val id: Long = 0, val characterId: String,
    val level: Int = 80, val ascension: Int = 6,
    val lightConeId: String, val lightConeLevel: Int = 80,
    val lightConeSuperimposition: Int = 1,
    val relicSet4: String, val relicSet2: String? = null,
    val mainStats: MainStats,
    val subStats: List<SubStat>,
    val eidolons: Set<Int> = emptySet(),
    val notes: String = ""
)
data class SubStat(val type: StatType, val value: Double, val rolls: Int)

data class BuildPreset(
    val id: String, val name: String, val characterId: String,
    val source: PresetSource, val build: PlayerBuild
)
enum class PresetSource { COMMUNITY, OFFICIAL, USER }
```

### 4.7 评分结果

```kotlin
data class CharacterScore(
    val characterId: String, val config: ScoringConfig,
    val unitValueScore: Double,    // 0-25
    val cycleScore: Double,         // 0-5
    val teamSynergyScore: Double,   // 0-40
    val scenarioScore: Double,      // 0-20
    val mechanicCoverage: Double,   // 0-10
    val total: Double,              // 0-100
    val tier: Tier
)
enum class Tier { S, A, B, C }

data class ScoringConfig(
    val playerBuild: PlayerBuild,
    val enemy: Enemy? = null
)
```kotlin
data class TeamScore(
    val totalDamage: Double, val totalHealing: Double,
    val totalShielding: Double, val roundsToKill: Int?,
    val ultsCast: Map<String, Int>, val buffUptime: Map<String, Double>,
    val breakdown: Map<String, Double>,
    val score: Double
)
```

---

## 5. 评分引擎（核心：战斗模拟驱动）

### 5.1 评分总分结构（100 分制）

| 维度 | 满分 | 引擎 | 输入 |
|---|---|---|---|
| **单位价值** | 25 | DamageCalculator (G1) | 角色+光锥+遗器模板 |
| **循环期望** | 5 | DiscreteEventSimulator (G2) | 5 回合行动数+终结技频率 |
| **配队协同** | 40 | DiscreteEventSimulator (G2) | 4 人 5 回合模拟 |
| **场景适配** | 20 | 弱点表 + G1 | 敌人属性+机制 |
| **机制完整度** | 10 | 标签覆盖分析 | 队伍标签覆盖 |
| **合计** | **100** | — | — |

> **彻底抛弃人工权重**。所有分数来自模拟器输出指标的归一化。

### 5.2 引擎 1：DamageCalculator（G1 单体期望）

不模拟回合，用概率公式算"理论单次行动伤害期望"。

```kotlin
class DamageCalculator(
    private val levelTable: LevelTable,
    private val elementTable: ElementTable,
    private val weaknessTable: WeaknessTable,
    private val buffEvaluator: BuffEvaluator
) {
    fun expectedDamage(
        character: Character, action: ActionType,
        enemy: Enemy, buffs: BuffSnapshot
    ): Double
}
```

**核心公式**：
```
伤害 = 攻击 × 技能倍率 × 暴击期望 × 增伤乘区 × 易伤乘区 × 属性抗性 × 等级压制 × 防御

  攻击 = 基础攻击 × (1 + 攻击%) + 固定值
  暴击期望 = 1 + 暴击率 × 暴伤
  增伤乘区 = ∏(1 + 各类增伤%)
  易伤 = ∏(1 + 易伤%)
  属性抗性 = 弱点倍率(1.0 / 0.5) × (1 - 抗性穿透)
  等级压制 = 1.0 ± (等级差 × 系数)
  防御 = 受方防御 / (受方防御 + 攻击方攻击 × 10 + 200)
```

**角色单位价值**：
```kotlin
data class CharacterUnitValue(
    val expectedSkillDmg: Double, val expectedUltDmg: Double,
    val expectedTalentDmg: Double, val expectedFollowUpDmg: Double,
    val dotDps: Double, val effectiveActionValue: Double,
    val ultChargeRate: Double,
    val baseSupportValue: Double, val baseHealValue: Double,
    val baseShieldValue: Double
)
```

**归一化为分数**（不靠人设权重，用分位数）：
```kotlin
fun unitScore(uv: CharacterUnitValue, role: Role,
              allUnitValues: List<CharacterUnitValue>): Double {
    val sameRole = allUnitValues.filter { it.role == role }
    val percentile = percentileRank(uv, sameRole)        // 0-1
    return percentile * 25.0
}
```

### 5.3 引擎 2：DiscreteEventSimulator（G2 离散事件模拟）

模拟 N=5 回合的行动序列，输出"配队协同分"。

```kotlin
class DiscreteEventSimulator(
    private val damageCalc: DamageCalculator,
    private val mechanicEngine: MechanicEngine
) {
    fun simulate(team: List<Combatant>, enemies: List<Enemy>,
                 rounds: Int = 5): SimulationResult
}

data class Combatant(
    val character: Character, val stats: Stats,
    val lightCone: LightCone, val relic: RelicSet,
    val eidolons: Map<Int, Eidolon>,
    var hp: Double, var sp: Double = 3.0, var ultCharge: Double = 0.0,
    var actionValue: Double = 0.0,
    val buffs: MutableList<Buff> = mutableListOf(),
    val debuffs: MutableList<Debuff> = mutableListOf()
)
```

**主循环**：
```kotlin
fun simulate(...): SimulationResult {
    val log = mutableListOf<RoundEvent>()
    repeat(rounds) { round ->
        val order = team.sortedBy { it.actionValue }
        order.forEach { c ->
            // 1. 行动值归零/推进
            c.actionValue = (c.actionValue - 10000.0).coerceAtMost(0.0) +
                            (10000.0 * speedFactor(c.stats.spd))
            // 2. 机制触发（拉条/充能/DOT结算）
            mechanicEngine.onActionStart(c, team, log)
            // 3. AI 决策技能
            val action = decideAction(c, team, enemies)
            executeAction(c, action, enemies, log)
            // 4. 追击判定
            mechanicEngine.checkFollowUps(c, action, team, enemies, log)
            // 5. 行动结束结算
            mechanicEngine.endOfAction(c, team, enemies, log)
        }
        mechanicEngine.endOfRound(team, enemies, log)
    }
    return SimulationResult(log, aggregate(log, team, enemies))
}
```

**配队协同分**（40 分）：
```kotlin
fun teamScore(result: SimulationResult, enemies: List<Enemy>): TeamScore {
    val dmgScore = normalize(result.totalDamage.values.sum()) * 15.0
    val cycleScore = result.ultsCast.values.average() * 5.0
    val buffScore = result.totalBuffUptime.values.average() * 10.0
    val killScore = (result.roundsToKill?.let { 5.0 - it } ?: 0.0) * 2.0
    val surviveScore = 8.0
    return TeamScore(score = dmgScore + cycleScore + buffScore + killScore + surviveScore)
}
```

### 5.4 引擎 3：MechanicEngine（机制规则）

把每个 Tag 抽象成**触发器 + 效果**：

```kotlin
interface MechanicRule {
    val tag: Tag
    fun onActionStart(c: Combatant, team: List<Combatant>, log: LogBuilder)
    fun onActionEnd(c: Combatant, team: List<Combatant>, log: LogBuilder)
    fun onFollowUpCheck(...): Boolean
    fun onRoundEnd(...)
}
```

**关键机制实现**：

| 机制 | 实现 |
|---|---|
| DOT | `applyDot` 每回合 tick，受易伤/增伤乘区影响 |
| 追击 | 触发后插入一次 Action，伤害 = 角色天赋倍率 |
| 拉条 | `actionValue -= 10000.0 × 拉条%` |
| 终结技 | 充能满释放，立即行动 + 增伤 |
| 解控 | 移除自己 debuffs 中"控制类" |
| 充能 | 击杀+30/命中+5/受击+10 |
| 击破 | 韧性条归零 → 1 回合击破 → DOT |
| 易伤 | `target.debuffs += Debuff(EASY_DMG, 0.3, 2回合)` |
| 召唤 | 生成新 Combatant |

### 5.5 引擎 4：场景适配

```kotlin
data class ScenarioScore(
    val weaknessCoverage: Double, val elementAdvantage: Double,
    val aoeMatch: Double, val mechanicCounter: Double, val score: Double
)

fun scenarioScore(team: List<Combatant>, scenario: Scenario): ScenarioScore {
    val coverage = team.count {
        it.character.element in scenario.enemies.flatMap { e -> e.weaknesses }
    } / 4.0
    val advantage = team.map {
        damageCalc.elementAdvantage(it, scenario.enemies[0])
    }.average()
    return ScenarioScore(score = (coverage * 8 + advantage * 12))
}
```

### 5.6 关键数据表

| 表 | 字段 | 来源 |
|---|---|---|
| `level_suppress` | 等级差, 系数 | wiki 抓取 |
| `element_resist` | 元素, 抗性, 倍率 | wiki 抓取 |
| `weakness_bonus` | 弱点元素, 倍率 | 0.5 / 1.0 |
| `break_effect` | 韧性表, 击破伤害 | wiki 抓取 |
| `action_value` | 10000 / 速度 | 游戏公式 |

### 5.7 模拟结果事件日志

```kotlin
data class RoundEvent(
    val round: Int, val actorId: String, val action: ActionType,
    val targets: List<TargetHit>, val damageDealt: Double,
    val healingDone: Double, val buffsApplied: List<BuffRef>,
    val mechanicsTriggered: List<MechanicEvent>,
    val actionValueBefore: Double, val actionValueAfter: Double,
    val ultChargeBefore: Double, val ultChargeAfter: Double
)

data class MechanicEvent(
    val type: String,                       // ACTION_ADVANCE / FOLLOW_UP / DOT_TICK / BREAK
    val source: String, val target: String?, val param: Double
)
```

### 5.8 模拟器校验（自检）

为避免公式错误导致评分离谱，加入**已知基准回归测试**：

```kotlin
@Test fun `Seele expected damage matches wiki value within 5%`()
@Test fun `Kafka DOT cycle damage matches 5-round simulation`()
@Test fun `Bronya 100% action advance applies correctly`()
@Test fun `5 rounds always returns 5 rounds of log`()
@Test fun `dead enemy receives no damage`()
@Test fun `action value never goes below 0`()
@Test fun `total score clamped 0-100 with max buffs`()
```

---

## 6. UI 信息架构

### 6.1 整体入口

```
底部 4 Tab（高频）：强度评估 / 配队 / 角色库 / 场景
侧边抽屉（低频/工具）：遗器评估器 / 玩家面板 / 数据更新 / 设置 / 主题 / 关于
```

### 6.2 关键页面

| 页面 | 内容 |
|---|---|
| 角色库 | 网格/列表切换 + 多维筛选 + 搜索 |
| 角色详情 | 评分大数字 + 分维度条 + 光锥列表（强制） + 遗器推荐 + 星魂 E0~E6 对比 |
| 强度评估（首屏） | 快速评估入口 + 版本强度榜 |
| 配队模拟器 | 4 槽位 + 队伍评分 + 队伍信息 |
| 战斗日志 | 5 回合逐回合展开 + 汇总 + 可展开单回合 |
| 场景推荐 | Top 5 配队 + 理由 + 缺失 |
| 玩家面板编辑器 | 完整面板 + 预设库 |
| 遗器评估器 | 单件 + 批量 + 套装诊断 |
| 数据更新 | 抓取进度 + 错误日志 + 回滚 |

### 6.3 主题
- 主色：星穹铁道蓝紫调（`#3D5AFE` / `#FF6E40`）
- Tier 色：S=金 / A=紫 / B=蓝 / C=灰
- 支持深色模式（跟随系统）

### 6.4 强制光锥 UX
- 角色详情默认显示"未选光锥（基线评分）"
- 光锥快速选择面板：按契合度分级（专属/推荐/可用/3★下位）
- 配队每个槽位必填光锥，未完成时"生成评估"按钮禁用

### 6.5 战斗日志调试器

```
┌─────────────────────────────┐
│  ← 战斗日志                 │
├─────────────────────────────┤
│  模拟设置:                  │
│  队伍: 希儿+布洛+停云+佩拉│
│  敌人: 1× BOSS (弱点:风/量)│
│  回合数: [5]                │
│  [▶ 重新模拟]              │
├─────────────────────────────┤
│  📊 汇总:                   │
│  总伤害: 245,678           │
│  终结技: 7 次              │
│  击杀回合: 4               │
│  队伍评分: 92.5            │
├─────────────────────────────┤
│  📜 回合 1                 │
│  ├─ 布洛妮娅 行动 (AV: 8945)│
│  │  ├─ 战技 → 希儿         │
│  │  │  └─ 拉条 50% (+5000 AV)│
│  │  └─ 充能 30 → 30       │
│  ├─ 希儿 行动 (AV: 9340)   │
│  │  ├─ 战技 → BOSS         │
│  │  │  └─ 伤害 12,345 (暴击)│
│  │  └─ 触发追击 (概率 50%) │
│  │     └─ 伤害 8,901       │
│  ├─ 停云 行动 ...           │
│  └─ 佩拉 行动 ...           │
│  📜 回合 2                 │
│  ...                       │
└─────────────────────────────┘
```

玩家可展开每一行看具体数值，可"质疑"任何分。

### 6.6 玩家面板编辑器

```
┌─────────────────────────────┐
│  ← 玩家面板 - 姬子          │
├─────────────────────────────┤
│  📋 基础:                   │
│  等级: [80]   突破: [6]     │
│  星魂: [E0] [E1] [E2] ...  │
├─────────────────────────────┤
│  🔦 光锥:                   │
│  [拂晓之前 ✦5]  Lv 80 [5叠]│
├─────────────────────────────┤
│  🎒 遗器:                   │
│  4件套: [熔岩锻造者▼]      │
│  躯干: 暴伤 [32.2%]         │
│  鞋子: 速度 [12.3]          │
│  位面球: 火伤 [38.8%]       │
│  连结绳: 攻击 [34.6%]       │
│  副词条:                    │
│  暴击 [8.7%] (3 rolls)    │
│  暴伤 [10.5%] (2 rolls)   │
│  速度 [5] (2 rolls)        │
│  攻击 [5.4%] (1 roll)     │
├─────────────────────────────┤
│  💾 [保存面板]              │
│  📋 [从预设载入]            │
│  🧪 [用此面板模拟]          │
└─────────────────────────────┘
```

### 6.7 数据更新页

```
┌─────────────────────────────┐
│  📡 数据更新                │
├─────────────────────────────┤
│  数据源: PRTS Wiki          │
│  [切换源]                   │
├─────────────────────────────┤
│  当前数据版本: v20250120     │
│  本地最后更新: 2 小时前     │
├─────────────────────────────┤
│  📦 抓取项:                 │
│  ✅ 角色 (30/30)  2 分钟前  │
│  ✅ 光锥 (20/20)  2 分钟前  │
│  ✅ 遗器 (15/15)  1 分钟前  │
│  ⚠️ 敌人 (48/50) 2 个失败  │
├─────────────────────────────┤
│  [🔄 立即抓取]             │
│  [📋 查看错误日志]          │
│  [↩️ 回滚到 v20250115]      │
└─────────────────────────────┘
```

---

## 7. 数据更新机制

### 7.1 数据源（多重）
- **远端 JSON**（自托管 GitHub Pages / CDN）
- **App 内 wiki 抓取器**（jsoup 抓 PRTS Wiki / SR Wiki）
- **本地种子数据**（assets 内置 v1，开箱即用）
- **玩家面板**：本地 Room 表

### 7.2 远端 manifest 协议
```json
{
  "version": 20250120,
  "publishedAt": "2025-01-20T10:00:00Z",
  "minAppVersion": 1,
  "files": {
    "characters": { "url": "characters-v20250120.json", "sha256": "abc..." },
    "lightCones": { "url": "light-cones-v20250120.json", "sha256": "def..." },
    "relics":     { "url": "relics-v20250120.json",     "sha256": "ghi..." },
    "enemies":    { "url": "enemies-v20250120.json",    "sha256": "jkl..." },
    "scenarios":  { "url": "scenarios-v20250120.json",  "sha256": "mno..." },
    "eidolons":   { "url": "eidolons-v20250120.json",   "sha256": "pqr..." }
  },
  "changelog": "新增花火、忘归人；调整卡芙卡 E2 系数"
}
```

### 7.3 更新策略
- 启动检查：本地 version < 远端 version → 触发同步
- WorkManager 周期任务：24h 一次，要求网络
- 用户手动触发：抽屉"立即抓取"
- 失败不阻断 UI：先使用本地数据

### 7.4 离线降级
- 首次启动：`assets/seed-data-v1.json`
- 无网络：本地数据库，UI 标识"离线"
- 抓取/更新失败：回滚到上一版本（Room 事务化）

```kotlin
@Test fun `import is atomic`() = runTest {
    val corrupted = """{ broken json"""
    val before = db.characterDao().count()
    assertFailsWith<ParseException> { repo.importData(corrupted) }
    assertEquals(before, db.characterDao().count())    // 数据未变
}
```

### 7.5 抓取器架构

```kotlin
interface WikiScraper {
    suspend fun scrapeCharacter(id: String): Character
    suspend fun scrapeLightCone(id: String): LightCone
    suspend fun scrapeRelicSet(id: String): RelicSet
    suspend fun scrapeEnemy(id: String): Enemy
}

class PrtsWikiScraper(...) : WikiScraper  // 适配器 1
class StarRailWikiScraper(...) : WikiScraper  // 适配器 2

class UpdateManager(
    private val scraper: WikiScraper,
    private val repo: CharacterRepository
) {
    suspend fun runFullScrape(): ScrapeResult
    // 进度回调：ScrapeProgress.Done / Failed
    // 错误聚合：ScrapeResult(success, failed, errors)
}
```

**抓取风险缓解**：
- 适配器层 + 解析失败告警
- 礼貌延迟（避免 IP 封禁）
- 失败回滚
- 抓取项：30 角色 / 20 光锥 / 15 遗器 / 50 敌人

### 7.6 导入流程

```kotlin
suspend fun importData(file: DataFile, content: ByteArray) {
    val newData = parse(content)
    db.withTransaction {
        db.characterDao().deleteAll()
        db.characterDao().insertAll(newData.characters)
        // ... 其他表
    }
    // 失败自动回滚（事务保证）
}
```

---

## 8. 测试策略

### 8.1 测试金字塔
- **单元测试 50+**：引擎、数据、工具（JVM）
- **集成测试 5-10**：Repository + Room（Android instrumented）
- **UI 测试 1-2**：关键流程（Compose）

### 8.2 单元测试清单（核心）

**模拟器精度验证**（对照 wiki 数据回归）：
```kotlin
@Test fun `Seele expected damage matches wiki value within 5%`()
@Test fun `Kafka DOT cycle damage matches 5-round simulation`()
@Test fun `Bronya 100% action advance applies correctly`()
@Test fun `Himeko fire damage vs fire weakness enemy matches DPS chart`()
```

**模拟器不变式**：
```kotlin
@Test fun `5 rounds always returns 5 rounds of log`()
@Test fun `dead enemy receives no damage`()
@Test fun `action value never goes below 0`()
@Test fun `total score clamped 0-100 with max buffs`()
```

**数据层**：
```kotlin
@Test fun `character by id is case insensitive`()
@Test fun `filter by path returns only matching characters`()
@Test fun `failed import does not corrupt existing data`()
@Test fun `upToDate returns without network call`()
@Test fun `update from older to newer version writes new data`()
```

**配队 & 推荐**：
```kotlin
@Test fun `single element team has 0 element coverage`()
@Test fun `balanced team with DPS support healer scores higher`()
@Test fun `team covering all weaknesses ranks first`()
@Test fun `top 5 recommendations sorted by score descending`()
```

**遗器**：
```kotlin
@Test fun `god-tier relic scores > 95`()
@Test fun `trash relic scores < 40`()
@Test fun `main stat mismatch loses 30 points`()
```

### 8.3 集成测试（Android）
```kotlin
@Test fun `insert and query roundtrip preserves data`()
@Test fun `filter by path and role composes correctly`()
```

### 8.4 UI 测试（Compose）
```kotlin
@Test fun `character detail shows score`()
@Test fun `cone selection updates score`()
@Test fun `eidolon selector shows 7 levels`()
```

### 8.5 手动测试场景

| 场景 | 期望 |
|---|---|
| 首次启动 | 显示种子数据，3-4 个示例角色 |
| 离线启动 | 正常显示，UI 标识"离线" |
| 启动 + 联网 | 检查更新，可选择"立即更新" |
| 选希儿 + 5★光锥 + 满星魂 | 评分 ≥ 90 |
| 选 4★ DPS + 3★光锥 + E0 | 评分 ≤ 70 |
| 配队 4 个同属性 | 元素覆盖 = 0，队伍分降 |
| 战斗日志展开 | 看到逐回合详细日志 |

---

## 9. 部署与发布

### 9.1 构建配置

```kotlin
android {
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "DATA_BASE_URL", "\"https://dev-starrail-strength.example.com/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "DATA_BASE_URL", "\"https://starrail-strength.example.com/\"")
        }
    }
}
```

### 9.2 CI 流程（GitHub Actions）

```yaml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: 17 }
      - run: ./gradlew test lint assembleDebug
      - uses: actions/upload-artifact@v4
        with: { path: app/build/outputs/apk/debug/app-debug.apk }
```

### 9.3 种子数据版本控制

```
app/src/main/assets/
├── seed-data-v1.json          # 角色 + 光锥 + 遗器 + 敌人 + 场景 + 星魂
├── seed-formulas-v1.json      # 伤害公式系数（等级压制/抗性/弱点等游戏表）
└── seed-changelog.md          # 首版说明
```

---

## 10. 实施里程碑（M1~M14）

| 阶段 | 内容 | 验证 |
|---|---|---|
| **M1** | 工程脚手架 + 依赖（Room, Retrofit, Coil, jsoup, WorkManager, Navigation） | `./gradlew build` 成功 |
| **M2** | 数据模型 + Room entities + DAOs + Repository 框架 | 编译通过 |
| **M3** | 种子数据（assets 内置 v1）+ 导入逻辑 | Room 有数据 |
| **M4** | DamageCalculator（G1）+ LevelTable/ElementTable/WeaknessTable | 单元测试 20+ |
| **M5** | DiscreteEventSimulator（G2）+ Combatant/ActionValue | 单元测试 15+ |
| **M6** | MechanicEngine：C1~C5 所有规则（基础/速度/终结技/DOT/追击/击破/召唤/解控/易伤/拉条） | 单元测试 30+ |
| **M7** | 模拟器精度验证（对照 wiki 数据回归） | 5+ 验证测试通过 |
| **M8** | UI 框架（NavGraph + 底部 4 Tab + 抽屉 + 主题） | App 启动可切换 |
| **M9** | 角色库 + 角色详情 + 评分展示（用模拟器） | 选中角色展示分 |
| **M10** | 玩家面板编辑器 + 预设库 | 保存/载入/编辑 |
| **M11** | 配队模拟器 + 战斗日志调试器 | 模拟 5 回合可看 |
| **M12** | 场景推荐 + 遗器刷取评估器 | 推荐 Top 5 + 评估 |
| **M13** | 抓取器（WikiScraper）+ 更新 UI + 离线降级 + CI | 抓取流程跑通 |
| **M14** | 打包 APK + 文档 + 发布 | `assembleRelease` 成功 |

---

## 11. 关键决策回顾

| 决策点 | 选择 | 理由 |
|---|---|---|
| 架构 | 方案 1：单 Activity + Compose + Room | 主流、模板已配、可测试 |
| 数据源 | 网络 + 离线降级 + App 内 wiki 抓取 | 既实时又可控 |
| 算法 | G3：期望(G1) + 离散事件模拟(G2) 双层 | 既可解释又真实 |
| 强制光锥 | 是，权重 12 分 | 强约束让评分有意义 |
| 导航 | 底部 4 Tab + 抽屉 | 高频/低频分离 |
| 遗器刷取 | 完整子模块 | 高频痛点 |
| 星魂 | 配置 E0~E6 | 同角色差异巨大 |
| 抓取器 | App 内 jsoup + 适配器层 | 可更新、可降级 |
| 战斗日志 | 必做 | 评分可追溯 |
| 玩家面板 | 编辑器 + 预设库 | 个性化与社区结合 |

---

## 12. 风险与缓解

| 风险 | 缓解 |
|---|---|
| wiki 结构变更 | 适配器层 + 解析失败告警 + 抓取结果回滚 |
| 频繁抓取被封 | 礼貌延迟 + 缓存 + 失败退避 |
| 模拟器与实际游戏偏差 | 已知基准回归测试 + 公开数据校验 |
| 评分公式争议 | 战斗日志可追溯 + 玩家可质疑 |
| 数据版本不兼容 | manifest.minAppVersion + 自动回滚 |
| 玩家面板填错 | 实时校验（数值范围）+ 默认值 |
| 离线无法抓取 | assets 种子数据 + UI 离线标识 |

---

**文档版本**：v1.0  
**下次更新**：用户审阅反馈后进入 writing-plans 阶段

