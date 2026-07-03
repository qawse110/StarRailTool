package com.java.myapplication.ui.battle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.java.myapplication.engine.simulator.sim.RoundEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleLogScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: BattleLogViewModel = viewModel(
        factory = BattleLogViewModel.factory(
            com.java.myapplication.util.ServiceLocatorResultStore(app.services)
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
            // 汇总
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("回合 ${state.totalRounds}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "总伤害 ${"%.0f".format(state.totalDmg)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("治疗 ${"%.0f".format(state.totalHealing)}", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (state.events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("还没有战斗数据 — 去配队页跑一次模拟")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.events) { event ->
                        EventRow(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: RoundEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "R${event.round} · ${event.actorId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    event.action.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (event.targets.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    event.targets.joinToString { t ->
                        "${t.targetId}:${"%.0f".format(t.damage)}${if (t.isCrit) "💥" else ""}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (event.buffsApplied.isNotEmpty()) {
                Text(
                    "+buff: ${event.buffsApplied.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (event.damageDealt > 0) {
                Text(
                    "→ ${"%.0f".format(event.damageDealt)} dmg",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}