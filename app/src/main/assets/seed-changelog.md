# Seed Data Changelog

## v2 (2026-07-04) — Character Detail Rich Display

- 7 个新 UI 区块：baseStats、scaling、tags、eidolons、lightcone、relic recommendations、skill tree
- 新 `SkillTree` model + Room v1→v2 migration
- Skill tree 效果通过 `SkillTreeEffectParser` 集成进 DamageCalculator
- 50+ 角色（来自 `Mar-7th/StarRailRes`）获得中文行迹数据
- 测试数：188 → 216 (+28)

## v1 (2026-07-03) — Initial Seed

## 内容
- 5 个角色：希儿、姬子、景元、卡芙卡、布洛妮娅
- 5 把光锥：拂晓之前、沿用追迹、胜利时刻、银河铁道之夜、致明日
- 3 套遗器：熔岩锻造者的火套、街头出身的拳套、量子套
- 10 个敌人：含 BOSS/精英/小怪/召唤物
- 3 个场景：混沌回忆 1/2、虚构叙事 1
- 5×6=30 条星魂

## 数据来源
- 数值参考公开 wiki 整理，**非官方**，仅作示例
- 后续版本会通过 App 内 wiki 抓取器增量更新

## 使用
App 首次启动时自动导入 Room（`SeedImporter`）。已存在的数据不会重复导入（用 `OnConflictStrategy.REPLACE`）。