package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.SkillTree
import com.mystarrail.tool.data.model.SkillType
import com.mystarrail.tool.ui.components.label

/**
 * 行迹（技能树）区块。按 skillType 分组，默认全部展开。
 *
 * 数据来源：[SkillTree]（由 Room SkillTreeDao 提供）。如果 [skillTree] 为 null
 * 或节点列表为空，整个区块不渲染（早返回）。
 */
@Composable
fun SkillTreeBlock(skillTree: SkillTree?) {
    if (skillTree == null || skillTree.nodes.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            "\uD83C\uDF33 行迹 (技能树)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))

        val grouped = skillTree.groupedBySkillType()
        // 固定渲染顺序：战技 → 终结技 → 天赋 → 追击 → DOT → 通用 → 未分类
        val orderedKeys = listOf(
            SkillType.SKILL, SkillType.ULT, SkillType.TALENT,
            SkillType.FOLLOW_UP, SkillType.DOT, SkillType.ALL, null
        )
        orderedKeys.forEach { key ->
            val nodes = grouped[key] ?: return@forEach
            Text(
                text = key?.label() ?: "其他",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            nodes.forEach { node ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = node.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "maxLevel: ${node.maxLevel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
