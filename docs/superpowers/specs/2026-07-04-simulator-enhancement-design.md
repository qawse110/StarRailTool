# Simulator Enhancement — Design Spec

**Date:** 2026-07-04
**Status:** Awaiting user approval (brainstorm Q1-Q6 settled, V3 完整方案)
**Scope:** 扩展战斗模拟器（V3 完整方案）

## Problem

当前 `DamageCalculator` 实现的战斗公式是**初版（M1）**，存在 5 个已知缺口：

1. **Buff 静默丢弃**：`BuffEvaluator.applyStat` 对 `EHR` / `BRK_EFF` / `EFFECT_RES` 走 `else -> snap`，角色持有这些 stat 时 buff 完全无效
2. **暴击率无上限**：`critRate = 0.5 + attackerBuffs.critRateBoost`，理论可超过 100%
3. **Buff 已定义但未接线**：`Buff.Dot` / `Buff.Break` / `Buff.ActionAdvance` / `Buff.UltCharge` 在 sealed interface 里有，但 `BuffEvaluator` 不处理（`else -> { }` 静默）
4. **DOT 未完整接线**：`CharacterUnitValue.dotDps` 字段已存在但 `DamageCalculator.unitValue` 写死 0.0；`Scaling` 无 `dotMult` 字段
5. **治疗/护盾无加成字段**：`Buff.HealingBoost` / `Buff.ShieldBoost` 不存在；`baseHealValue` / `baseShieldValue` 写死 0.0；评分 5 维不含治疗/护盾能力

**用户原始诉求**："加新能力"（B 选项，V3 完整方案，2026-07-04 brainstorming 确认）

## Solution Overview

8 项增量改动 + 9 个新单元测试 + 1 个集成测试。**架构保持稳定**，所有改动向后兼容（默认参数 / 新字段不破坏现有调用方）。

### Design Decisions（已确认）

| 决策点 | 选择 | 理由 |
|---|---|---|
| BuffSnapshot 粒度 | **X1 统一扩展**（6 新字段） | 简单，KISS |
| DOT 行为 | **Y1 完整 tick**（DotRule 已实现） | 真实持续伤害 |
| 治疗/护盾评分 | **Z2 第 6 维 utilityScore (10 分)** | 多维评分 |
| Buff target | **W2 新 Buff 加 `target: BuffTarget = SELF` 默认** | 1 人队伍下 target 不影响结果 |
| 工作范围 | **V3 完整** | 用户选 "全做" |

## Scope

### 8 项能力（按依赖顺序）

| # | 能力 | 改动 |
|---|---|---|
| **B2** | BuffSnapshot 补全 | `BuffSnapshot` +3 字段（effectHitRate/effectRes/breakEffect）；`applyStat` 补 3 个 StatType |
| **B7** | 暴击率上限 100% | `DamageCalculator` 1 行：`critRate = min(1.0, 0.5 + boost)` |
| **B5** | EasyDmg 走真公式 | `expectedDamage` 读 `easyDmgTaken`（已读，无需改） |
| **B3** | 治疗 buff | `Buff.HealingBoost` data class + `BuffSnapshot.healingBoost` + `expectedHealing()` |
| **B4** | 护盾 buff | `Buff.ShieldBoost` data class + `BuffSnapshot.shieldBoost` + `expectedShield()` |
| **B1** | DOT 联通 | `Scaling.dotMult` 字段 + `CharacterUnitValue.dotDps` 公式 + KeywordMatcher "Heal Rate" |
| **B6** | EHR/EffectRes 命中 | `expectedDamage` 加 `effectHitClamp`（命中率 = `effectHitRate` vs `effectRes`，无随机） |
| **B8** | 第 6 维 utilityScore | `CharacterScore.utilityScore` + `ScoringEngine` 公式（10 分） |

### Out of Scope（YAGNI）

- 多目标 buff 实际扩散（W2 留字段，逻辑全 SELF）
- 暴击随机数（继续确定性 `critExpect` 公式）
- DOT tick 在 DiscreteEventSimulator 重写（`DotRule` 已存在自动 tick）
- 真机校准（数据驱动优化）

## Architecture

### 模块边界

```
KeywordMatcher (Task 5) → SkillTreeEffectParser → DamageCalculator.unitValue
                                                  ├─→ BuffSnapshot (新 6 字段)
                                                  ├─→ expectedHealing() → baseHealValue
                                                  ├─→ expectedShield() → baseShieldValue
                                                  └─→ dotDps 公式
                              → ScoringEngine.scoreCharacter
                                  └─→ utilityScore 第 6 维 → CharacterScore.utilityScore
```

**单一职责**：
- `BuffSnapshot` 集中所有 buff 字段评估
- `expectedHealing/Shield/Dot` 各自单一公式函数
- `ScoringEngine.utilityScore` 独立计算

### 数据模型变更

| 类型 | 旧 | 新 | 兼容性 |
|---|---|---|---|
| `Buff` | 8 data class | +2（HealingBoost/ShieldBoost）；4 个旧 Buff 加 `target: BuffTarget = SELF` | ✅ 向后 |
| `BuffSnapshot` | 9 字段 | 15 字段（B2 +3: effectHitRate/effectRes/breakEffect；B1+B3+B4 各 +1: dotDmg/healingBoost/shieldBoost） | ✅ 向后（全 default 0.0） |
| `Scaling` | 5 字段 | 6 字段（+dotMult: Double = 0.0） | ✅ 向后 |
| `CharacterScore` | 7 字段 | 8 字段（+utilityScore: Double = 0.0） | ✅ 向后 |
| `CharacterUnitValue` | 10 字段 | 10 字段（dotDps 不再硬编码 0） | ✅ 兼容 |

### 文件清单（12 个 main + 1 个 test）

| 文件 | 改动 |
|---|---|
| `data/model/Scaling.kt` | +1 字段 |
| `data/model/CharacterScore.kt` | +1 字段 |
| `engine/simulator/buffs/Buff.kt` | +2 data class；+4 target 字段 |
| `engine/simulator/buffs/BuffSnapshot.kt` | +6 字段；+operator plus 更新 |
| `engine/simulator/buffs/BuffEvaluator.kt` | +3 StatType case；+2 Buff case |
| `engine/simulator/damage/DamageCalculator.kt` | 1 行 clamp；+expectedHealing/Shield；DOT 公式 |
| `engine/simulator/ScoringEngine.kt` | +utilityScore 计算；改 total 公式 |
| `data/seed/remote/Mar7thToSeedTransformer.kt` | +"Heal Rate" KeywordMatcher 规则 |
| `engine/simulator/SkillTreeEffectParser.kt` | 极小改：damageBonus 字段保留（不动 Task 5 测试） |
| `test/.../buffs/BuffEvaluatorTest.kt` | NEW（5 测试） |
| `test/.../damage/DamageCalculatorTest.kt` | +3 测试 |
| `test/.../ScoringEngineTest.kt` | NEW（2 测试） |
| `test/.../ui/characters/CharacterDetailViewModelTest.kt` | +1 集成测试 |

## Data Flow

### DOT 数据流

```
Character.scaling.dotMult
    ↓
DamageCalculator.dotDps(character, atkBoost, critBoost, dmgBonus, debuff)
    = atk * (1+atkBoost) * dotMult * (1+critRate*critDmg) * (1+dmgBonus) * (1-抗性)
    ↓
CharacterUnitValue.dotDps
    ↓
ScoringEngine (用于 utilityScore 或直接展示)
```

### 治疗/护盾数据流

```
Buff.HealingBoost(shieldBoost) → list
    ↓
BuffSnapshot.healingBoost = sum
    ↓
DamageCalculator.expectedHealing(character, healingBoost) = baseHeal * (1 + healingBoost)
    ↓
CharacterUnitValue.baseHealValue
    ↓
ScoringEngine.utilityScore = min(10.0,
    (baseHealValue/2000.0 + baseShieldValue/2000.0).coerceAtMost(1.0) * 10.0
)
```

### EHR/EffectRes 数据流

```
StatType.EHR → Buff.StatBoost → BuffSnapshot.effectHitRate
StatType.EFFECT_RES → 应用于敌人 → enemy EffectRes snapshot
    ↓
expectedDamage 加 effectHitClamp 判定（无随机，确定性）
有效命中 = effectHitRate - effectRes 截断到 [0, 1]
```

## Error Handling

| 场景 | 行为 |
|---|---|
| 未知 StatType StatBoost | `applyStat` 返回原 snap（保持当前行为） |
| healingBoost = 0 | expectedHealing = baseHeal（不抛错） |
| dotMult = 0 | dotDps = 0（DPS 角色行为） |
| effectHitRate = 0 | effectHitClamp = 0（debuff 不生效） |
| 缺失 SkillTree | 空 map（Task 5 已处理） |

## Testing Strategy

### TDD Red-Green-Refactor

**9 单元 + 1 集成测试**：

1. `BuffEvaluatorTest` 5 测试：
   - `EHR StatBoost → snapshot.effectHitRate += value`
   - `BRK_EFF StatBoost → snapshot.breakEffect += value`
   - `EFFECT_RES StatBoost → snapshot.effectRes += value`
   - `HealingBoost buff → snapshot.healingBoost += multiplier`
   - `ShieldBoost buff → snapshot.shieldBoost += multiplier`

2. `DamageCalculatorTest` 3 测试：
   - `critRate clamps to 1.0 when 0.5+boost > 1.0`
   - `expectedHealing with healingBoost = 0.5 yields 1.5x baseHeal`
   - `dotDps formula matches manual calc`

3. `ScoringEngineTest` 2 测试：
   - `utilityScore contributes 0-10 to total`
   - `6 维 total sum equals 100 when all components maxed`

4. `CharacterDetailViewModelTest` 1 集成测试：
   - `DPS char → utilityScore = 0`
   - `HEAL char → utilityScore > 0`

## Verification

- ✅ 全量测试 216 → **225+** (新 9 单元 + 1 集成)
- ✅ `./gradlew :app:assembleDebug` BUILD SUCCESSFUL
- ✅ APK 22MB 不变（新字段小）

## Plan Deviations from Original

无（V3 与 brainstorm 决定一致）。

## Next Step

写完 spec 后 → `writing-plans` skill 写实施 plan → implement。
