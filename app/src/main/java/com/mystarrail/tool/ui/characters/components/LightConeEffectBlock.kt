package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.LightCone

@Composable
fun LightConeEffectBlock(cone: LightCone?) {
    if (cone == null) return
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            "\uD83D\uDCA1 必选光锥效果: ${cone.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text("\u6548\u679C: ${cone.passiveName}", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(2.dp))
        Text(
            cone.passiveEffect.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}