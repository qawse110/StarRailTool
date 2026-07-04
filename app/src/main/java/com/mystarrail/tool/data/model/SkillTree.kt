package com.mystarrail.tool.data.model

/**
 * 角色技能树领域模型。
 *
 * - characterId: 关联的 [Character.id]
 * - nodes: 该角色可学的技能/天赋节点（行迹/天赋/秘技等）
 *
 * Commit 3 完整版：取代 commit 1 的占位 stub。
 */
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