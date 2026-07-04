package com.mystarrail.tool.data.model

/**
 * 角色行迹（技能树）。一组节点（每个节点 = 一个可解锁的能力）。
 *
 * 数据来源：Mar-7th/StarRailRes `character_skill_trees.json`。
 */
data class SkillTree(
    val characterId: String,
    val nodes: List<SkillTreeNode>
) {
    /** 按 skillType 分组（用于 UI 渲染）。未分类节点放在 null 桶。 */
    fun groupedBySkillType(): Map<SkillType?, List<SkillTreeNode>> =
        nodes.groupBy { it.skillType }
}

/** 单个行迹节点：name + desc（中文/英文）+ maxLevel + params（每级数值表）。 */
data class SkillTreeNode(
    val id: String,
    val name: String,
    val desc: String,
    val maxLevel: Int = 1,
    val skillType: SkillType? = null,
    val effectType: String? = null,
    val paramList: List<List<Double>> = emptyList()
)