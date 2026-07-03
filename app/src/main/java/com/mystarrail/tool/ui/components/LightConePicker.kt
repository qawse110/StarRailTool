package com.mystarrail.tool.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mystarrail.tool.data.model.LightCone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightConePicker(
    lightCones: List<LightCone>,
    selected: LightCone?,
    onSelect: (LightCone) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "选择光锥",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(lightCones, key = { it.id }) { cone ->
                    ListItem(
                        headlineContent = { Text(cone.name) },
                        supportingContent = { Text(cone.passiveName) },
                        trailingContent = { Text("✦${cone.rarity}") },
                        modifier = Modifier.clickable {
                            onSelect(cone)
                            onDismiss()
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}