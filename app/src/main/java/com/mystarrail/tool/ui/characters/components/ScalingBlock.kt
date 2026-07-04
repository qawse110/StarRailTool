package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.Scaling

@Composable
fun ScalingBlock(scaling: Scaling) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("⚔️ 技能倍率", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MultCell("战技", scaling.skillMult)
            MultCell("终结技", scaling.ultMult)
            MultCell("天赋", scaling.talentMult)
            MultCell("追击", scaling.followUpMult)
        }
        if (scaling.aoeRatio > 0) {
            Spacer(Modifier.height(4.dp))
            Text("AOE 比例: ${(scaling.aoeRatio * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MultCell(label: String, mult: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("×${"%.2f".format(mult)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
