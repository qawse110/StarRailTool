package com.mystarrail.tool.ui.teambuilder

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
import com.mystarrail.tool.StarRailApp
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.ui.components.label
import com.mystarrail.tool.ui.components.shortLabel

@Composable
fun TeamBuilderScreen(
    onShowBattleLog: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: TeamBuilderViewModel = viewModel(
        factory = TeamBuilderViewModel.factory(
            app.services.repository,
            app.services.scoringEngine,
            com.mystarrail.tool.util.ServiceLocatorResultStore(app.services)
        )
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("配队模拟", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // 4 槽位
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { idx ->
                val char = state.allChars.firstOrNull { it.id in state.selectedIds && idx == state.selectedIds.toList().indexOf(it.id) }
                SlotBox(
                    label = "槽 ${idx + 1}",
                    char = char,
                    onRemove = { char?.let { viewModel.toggleChar(it.id) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 模拟按钮
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = viewModel::simulate,
                enabled = state.canSimulate
            ) {
                if (state.isSimulating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("模拟 (${state.selectedIds.size}/4)")
                }
            }
            OutlinedButton(onClick = viewModel::clearSelection) {
                Text("清空")
            }
        }

        Spacer(Modifier.height(12.dp))

        // 上次结果（增强版）
        if (state.lastResult != null) {
            val result = state.lastResult!!
            val totalDmg = result.damageBreakdown.total

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 汇总
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem2("总伤害", "${"%.0f".format(totalDmg)}")
                        StatItem2("行动", "${result.actions.values.sum()}")
                        StatItem2("终结技", "${result.ultsCast.values.sum()}")
                        StatItem2("击杀回合", result.roundsToKill?.toString() ?: "未击杀")
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    // 伤害构成
                    Text("伤害构成", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DmgLabel("战技", result.damageBreakdown.skillDmg, totalDmg)
                        DmgLabel("终结技", result.damageBreakdown.ultDmg, totalDmg)
                        DmgLabel("追击", result.damageBreakdown.followUpDmg, totalDmg)
                        DmgLabel("DOT", result.damageBreakdown.dotDmg, totalDmg)
                    }

                    Spacer(Modifier.height(8.dp))

                    // 角色贡献
                    Text("角色贡献", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    result.totalDamage.entries.sortedByDescending { it.value }.forEach { (charId, dmg) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(charId, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(
                                "${"%.0f".format(dmg)} (${if (totalDmg > 0) (dmg / totalDmg * 100).toInt() else 0}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "行动:${result.actions[charId] ?: 0} 大招:${result.ultsCast[charId] ?: 0}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onShowBattleLog) {
                        Text("📜 查看详细战斗日志")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 角色库列表（可点选）
        Text("角色库（点选 4 个）", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.allChars, key = { it.id }) { c ->
                CharSelectRow(
                    c = c,
                    isSelected = c.id in state.selectedIds,
                    enabled = c.id in state.selectedIds || state.selectedIds.size < 4,
                    onClick = { viewModel.toggleChar(c.id) }
                )
            }
        }
    }
}

@Composable
private fun StatItem2(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DmgLabel(label: String, value: Double, total: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${"%.0f".format(value)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (total > 0) {
            Text("${(value / total * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SlotBox(
    label: String,
    char: Character?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable(enabled = char != null, onClick = onRemove),
        shape = MaterialTheme.shapes.medium,
        color = if (char != null) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (char == null) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(char.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${char.rarity}★ ${char.path.label()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CharSelectRow(
    c: Character,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = null, enabled = enabled)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("${c.name}  ${c.rarity}★", fontWeight = FontWeight.Bold)
                Text(
                    "${c.path.label()} · ${c.element.shortLabel()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}