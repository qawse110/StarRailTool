package com.mystarrail.tool.data.model

/**
 * 技能树占位模型 — 完整实现在 commit 3。
 *
 * 留空壳是因为 SeedParser.ParseResult.Success 已在 commit 1 增加 `skillTrees` 字段，
 * 必须有可解析的类型；commit 3 会用真实字段替换。
 */
data class SkillTree(
    val characterId: String
)