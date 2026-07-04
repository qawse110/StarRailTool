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
import com.mystarrail.tool.data.model.RelicSet
import com.mystarrail.tool.data.model.Role

@Composable
fun RelicRecommendationsBlock(relics: List<RelicSet>, characterRole: Role) {
    if (relics.isEmpty()) return
    val recommended = relics.take(5)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            "\uD83D\uDEE1\uFE0F 遗器套推荐 (${characterRole.name})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        recommended.forEach { relic ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(relic.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "2\u4EF6: ${relic.twoPiece}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (relic.fourPiece != relic.twoPiece) {
                        Text(
                            "4\u4EF6: ${relic.fourPiece}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}