# StarRailTool

崩坏：星穹铁道 角色配置与战斗模拟 Android 工具 — Kotlin / Jetpack Compose / Room。

## 概述

为崩铁玩家提供一个**离线优先**的角色面板管理 + 配装评分工具：

- 角色基础信息（命途 / 元素 / 标签 / 技能倍率 / 行迹）
- 玩家自定义面板（光锥 / 遗器 / 主属性 / 副词条 / 星魂 / 备注）
- **6 维战斗评分**（单位价值 + 循环 + 配队 + 场景 + 机制覆盖 + **治疗护盾 utilityScore**）
- 战斗模拟器：DOT 持续伤害 / Buff 累积 / 暴击期望 / 等级压制 / 弱点易伤

## 技术栈

| 类别 | 选型 |
|---|---|
| 语言 | Kotlin 2.2.0 |
| UI | Jetpack Compose (BOM 2026.01.01) + Material 3 |
| 架构 | MVVM + StateFlow + Repository |
| 数据库 | Room 2.7.0（v2 schema 含 skill_tree_nodes） |
| 网络 | OkHttp 4.12.0 + Conscrypt（Mar-7th/StarRailRes 远程数据） |
| 解析 | Moshi 1.15.1 + Jsoup 1.18.1（Wiki 抓取） |
| 图片 | Coil 3.0.4 |
| 导航 | Navigation Compose 2.8.5 |
| 异步 | Kotlin Coroutines 1.9.0 |
| 构建 | Gradle 8.x + Version Catalog |

**配置**：minSdk 24 / targetSdk 35 / compileSdk 35 / Java 17

## 模块结构

```
app/src/main/java/com/mystarrail/tool/
├── StarRailApp.kt              # Application 入口
├── MainActivity.kt             # 单 Activity 容器
├── data/                       # 数据层
│   ├── model/                  # 领域模型（Character/Enemy/Buff/Scaling/...）
│   ├── local/                  # Room DB + DAO + Converters
│   ├── repository/             # Repository 接口 + 实现
│   ├── seed/                   # 本地 seed (assets) + 远程 (Mar-7th)
│   └── remote/                 # KeywordMatcher 关键字 → Buff 规则
├── engine/
│   └── simulator/              # 战斗模拟器
│       ├── buffs/              # Buff sealed interface + BuffSnapshot + Evaluator
│       ├── damage/             # DamageCalculator + CharacterUnitValue
│       ├── rules/              # MechanicEngine + DotRule / BreakRule / ...
│       ├── sim/                # DiscreteEventSimulator
│       ├── tables/             # FormulaTables（弱点/抗性/等级/行动值）
│       ├── ScoringEngine.kt    # 6 维评分
│       └── SkillTreeEffectParser.kt
├── ui/
│   ├── nav/                    # NavGraph
│   ├── theme/                  # Material 3 主题
│   ├── components/             # 复用 Composable
│   ├── characters/             # 角色详情（含 SkillTreeBlock）
│   ├── build/                  # 配装编辑（CRUD）
│   ├── teambuilder/            # 队伍编辑器
│   ├── battle/                 # 战斗模拟
│   ├── assessment/             # 全角色评估
│   ├── scenario/               # 场景适配
│   ├── relic/                  # 遗器浏览
│   └── scraper/                # Wiki 抓取 UI
└── util/                       # 工具函数
```

## 核心数据模型

- **`Character`**: 角色（id, 命途, 元素, 角色定位, 标签, 基础属性, 技能倍率, 行迹, 循环配置）
- **`Buff`** (sealed): 9 种类型 — `StatBoost` / `DamageBonus` / `EasyDmg` / `SpeedMod` / `ActionAdvance` / `UltCharge` / `Dot` / `HealingBoost` / `ShieldBoost`
- **`BuffSnapshot`**: 评估后的扁平字段（15 字段：atkBoost, critRateBoost, effectHitRate, healingBoost, ...）
- **`CharacterUnitValue`**: 角色单位价值（expectedSkill/Ult/Talent/FollowUpDmg, dotDps, baseHealValue, baseShieldValue, ...）
- **`CharacterScore`**: 6 维评分（unitValue/cycle/teamSynergy/scenario/mechanicCoverage + **utilityScore 0-10**）

## 战斗公式

```
expectedDamage = atk * mult * critExpect * (1+增伤) * (1+易伤)
                 * 弱点倍率 * (1-抗性) * 等级压制 * 防御减免

critRate = min(1.0, 0.5 + critRateBoost)  // B7 暴击率上限 100%
critExpect = 1 + critRate * (1 + critDmgBoost)

dotDps = atk * dotMult * critExpect * (1+增伤) * 0.6  // B1
baseHealValue = baseHeal * (1 + healingBoost) * 0.5  // B3
baseShieldValue = baseShield * (1 + shieldBoost)      // B4
effectHitClamp = (EHR - EffectRes).coerceIn(0, 1)     // B6 预计算
```

## 评分体系

满分 100，5 维原始 + 1 维治疗护盾：

| 维度 | 权重 | 来源 |
|---|---|---|
| unitValueScore | 25 | normalizeRole(uv) * 25 |
| cycleScore | 5 | (cycleActions - 3) / 2 * 5 |
| teamSynergyScore | 40 | simulate(单人) * 40 |
| scenarioScore | 20 | 弱点匹配 0.9, 否则 0.5 * 20 |
| mechanicCoverage | 10 | min(tags/5, 1) * 10 |
| **utilityScore** | **0-10**（内嵌于 100） | min((heal+shield)/2000, 1) * 10 |

## 构建

```bash
# 测试
./gradlew :app:test

# 构建 Debug APK
./gradlew :app:assembleDebug

# APK 位置
app/build/outputs/apk/debug/app-debug.apk
```

## 测试

- **250 单元测试**，0 failures / 0 errors
- 测试框架: JUnit 4.13.2 + Truth 1.4.4
- 覆盖: Buff/Damage/Scoring/SkillTree/Character Detail/Build/Converter/Repository

```bash
./gradlew :app:test
# 或
./gradlew :app:testDebugUnitTest
```

## 数据源

- **远程**: Mar-7th/StarRailRes（GitHub raw）
- **Wiki 抓取**: jsoup
- **本地**: assets/seed/*.json（构建时打包）
- 远程拉取通过 WorkManager 周期更新，失败回退本地

## 设计文档

- [`docs/superpowers/specs/2026-07-04-simulator-enhancement-design.md`](docs/superpowers/specs/) — 战斗模拟器 V3 增强（B1-B8）
- [`docs/superpowers/plans/2026-07-04-simulator-enhancement-plan.md`](docs/superpowers/plans/) — 实施计划
- [`docs/superpowers/optimization-suggestions.md`](docs/superpowers/) — 后续优化建议

## 开发

```bash
# 拉取最新代码
git pull origin master

# 推送新代码
git push origin master

# Android Studio 打开项目
# 推荐使用 Hedgehog (2023.1.1) 或更新版本
```

## 协议

仅供学习与非商业用途。游戏数据归 miHoYo / HoYoverse 所有。
