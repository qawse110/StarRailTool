package com.mystarrail.tool.ui.characters.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.Stats

@Composable
fun BaseStatsBlock(stats: Stats) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("📊 基础数值", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatCell("HP", stats.hp.toInt())
            StatCell("ATK", stats.atk.toInt())
            StatCell("DEF", stats.def.toInt())
            StatCell("SPD", stats.spd.toInt())
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
