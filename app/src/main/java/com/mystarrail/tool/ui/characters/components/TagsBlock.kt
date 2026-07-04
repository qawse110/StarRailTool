package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.Tag
import com.mystarrail.tool.ui.components.label

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsBlock(tags: Set<Tag>) {
    if (tags.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("🏷️ 标签", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEach { tag ->
                AssistChip(
                    onClick = {},
                    label = { Text(tag.label()) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        }
    }
}
