package com.java.myapplication.ui.scenario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.java.myapplication.StarRailApp
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Scenario
import com.java.myapplication.ui.components.shortLabel

@Composable
fun ScenarioScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: ScenarioViewModel = viewModel(
        factory = ScenarioViewModel.factory(app.services.repository)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("场景推荐", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "根据敌人弱点 + 选中队伍元素匹配度排序",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // 队伍选择（多选）
            Text("队伍（点选加成匹配）", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(state.allChars, key = { it.id }) { c ->
                    CharToggleRow(
                        c = c,
                        isSelected = c.id in state.selectedIds,
                        onClick = { viewModel.toggleChar(c.id) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("场景列表（按适配度排序）", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.scenarios, key = { it.scenario.id }) { ss ->
                    ScenarioRow(ss)
                }
            }
        }
    }
}

@Composable
private fun CharToggleRow(c: Character, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isSelected, onCheckedChange = null)
        Text(
            "${c.name} (${c.element.shortLabel()})",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ScenarioRow(ss: ScenarioScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ss.isBestMatch) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ss.scenario.name, fontWeight = FontWeight.Bold)
                    if (ss.isBestMatch) {
                        Spacer(Modifier.width(4.dp))
                        Text("🏆", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    "难度 ${ss.scenario.difficulty} · 弱点: ${ss.coveredElements.joinToString { it.shortLabel() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${ss.scenario.enemies.size} 敌人",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                "%.0f".format(ss.fitScore),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}