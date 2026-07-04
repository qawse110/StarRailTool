package com.mystarrail.tool.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mystarrail.tool.StarRailApp
import com.mystarrail.tool.engine.simulator.sim.ActionType
import com.mystarrail.tool.engine.simulator.sim.RoundEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleLogScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: BattleLogViewModel = viewModel(
        factory = BattleLogViewModel.factory(
            com.mystarrail.tool.util.ServiceLocatorResultStore(app.services)
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("战斗日志") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 汇总面板
            SummaryPanel(state)

            if (state.events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "暂无战斗数据",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "前往「配队模拟」页面运行一次 4 人战斗模拟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 伤害构成
                DamageBreakdownPanel(state)
                Spacer(Modifier.height(4.dp))

                // 角色贡献面板
                CharacterContributionPanel(state)

                Spacer(Modifier.height(4.dp))
                HorizontalDivider()

                // 回合事件列表
                Text(
                    "行动日志",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.events, key = { "${it.round}_${it.actorId}_${it.action}_${it.actionValueBefore}" }) { event ->
                        EventCard(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPanel(state: BattleLogUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("回合", state.totalRounds.toString())
                StatItem("总伤害", "${"%.0f".format(state.totalDmg)}")
                StatItem("击杀", "${state.enemyKills} 只")
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("行动", "${state.actionsByChar.values.sum()} 次")
                StatItem("终结技", "${state.ultsByChar.values.sum()} 次")
                StatItem("击杀回合", state.roundsToKill?.toString() ?: "未击杀")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun DamageBreakdownPanel(state: BattleLogUiState) {
    val bd = state.damageBreakdown
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "伤害构成",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DmgTypeItem("战技", bd.skillDmg, bd.total)
                DmgTypeItem("终结技", bd.ultDmg, bd.total)
                DmgTypeItem("追击", bd.followUpDmg, bd.total)
                DmgTypeItem("DOT", bd.dotDmg, bd.total)
                DmgTypeItem("击破", bd.breakDmg, bd.total)
            }
        }
    }
}

@Composable
private fun DmgTypeItem(label: String, value: Double, total: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "${"%.0f".format(value)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (total > 0) {
            Text(
                "${(value / total * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CharacterContributionPanel(state: BattleLogUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "角色贡献",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            val totalDmg = state.totalDmg
            state.dmgByChar.entries.sortedByDescending { it.value }.forEach { (charId, dmg) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        charId,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${"%.0f".format(dmg)} (${if (totalDmg > 0) (dmg / totalDmg * 100).toInt() else 0}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "行动: ${state.actionsByChar[charId] ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "终结技: ${state.ultsByChar[charId] ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: RoundEvent) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // 摘要行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 回合标签
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (event.action) {
                            ActionType.SKILL -> MaterialTheme.colorScheme.primary
                            ActionType.ULT -> MaterialTheme.colorScheme.tertiary
                            ActionType.FOLLOW_UP -> MaterialTheme.colorScheme.secondary
                            ActionType.DOT -> MaterialTheme.colorScheme.error
                            ActionType.TALENT -> MaterialTheme.colorScheme.primary
                            ActionType.PASS -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            "R${event.round}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        event.actorId,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionBadge(event.action)
                    if (event.damageDealt > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${"%.0f".format(event.damageDealt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 伤害目标行
            if (event.targets.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                event.targets.forEach { t ->
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "→ ${t.targetId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            " ${"%.0f".format(t.damage)}${if (t.isCrit) " 💥暴击" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (t.isCrit) FontWeight.Bold else FontWeight.Normal,
                            color = if (t.isCrit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 展开详情
            if (expanded) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                // 行动值变化
                Row {
                    Text(
                        "行动值: ${"%.0f".format(event.actionValueBefore)} → ${"%.0f".format(event.actionValueAfter)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 充能变化
                Row {
                    Text(
                        "充能: ${"%.0f".format(event.ultChargeBefore)} → ${"%.0f".format(event.ultChargeAfter)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // 治疗
                if (event.healingDone > 0) {
                    Row {
                        Text(
                            "治疗: ${"%.0f".format(event.healingDone)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                // Buffs
                if (event.buffsApplied.isNotEmpty()) {
                    Row {
                        Text(
                            "Buff: ${event.buffsApplied.joinToString()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                // 机制触发
                if (event.mechanicsTriggered.isNotEmpty()) {
                    Row {
                        Text(
                            "机制: ${event.mechanicsTriggered.joinToString { "${it.type}(${"%.0f".format(it.param)})" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionBadge(action: ActionType) {
    val (label, containerColor) = when (action) {
        ActionType.SKILL -> "战技" to MaterialTheme.colorScheme.primary
        ActionType.ULT -> "终结技" to MaterialTheme.colorScheme.tertiary
        ActionType.FOLLOW_UP -> "追击" to MaterialTheme.colorScheme.secondary
        ActionType.DOT -> "DOT" to MaterialTheme.colorScheme.error
        ActionType.TALENT -> "天赋" to MaterialTheme.colorScheme.primary
        ActionType.PASS -> "普攻" to MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor.copy(alpha = 0.15f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = containerColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}