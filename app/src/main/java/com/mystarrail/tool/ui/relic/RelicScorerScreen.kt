package com.mystarrail.tool.ui.relic

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
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.engine.relic.RelicOptimizer

@Composable
fun RelicScorerScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: RelicScorerViewModel = viewModel(
        factory = RelicScorerViewModel.factory(
            app.services.repository,
            app.services.relicOptimizer
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("遗器评估 / 自动调优", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "主词条推荐 + 副词条权重评分 + 套装/主词条自动搜索（写入玩家面板）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // 角色选择
        Text("角色（点选）", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier.heightIn(max = 120.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(state.allChars, key = { it.id }) { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = c.id == state.selectedChar?.id,
                        onClick = { viewModel.selectChar(c) }
                    )
                    Text(c.name, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = viewModel::optimize,
            enabled = state.selectedChar != null && !state.isOptimizing
        ) {
            if (state.isOptimizing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("优化中…")
            } else {
                Text("自动调优遗器")
            }
        }

        state.applyMessage?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(12.dp))

        // 主词条推荐
        state.mainStatRec?.let { rec ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📌 主词条推荐", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("头/胸: ${rec.body.name}", style = MaterialTheme.typography.bodySmall)
                    Text("鞋: ${rec.boots.name}", style = MaterialTheme.typography.bodySmall)
                    Text("球: ${rec.sphere.name}", style = MaterialTheme.typography.bodySmall)
                    Text("绳: ${rec.rope.name}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 4 件套推荐
        state.bestSet?.let { set ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🛡 推荐遗器套", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(set.name, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 自动优化结果
        if (state.recommendations.isNotEmpty()) {
            Text("优化方案", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false).heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.recommendations) { rec ->
                    OptimizeResultCard(rec = rec, onApply = { viewModel.applyRecommendation(rec) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // 副词条权重 + 评分
        state.selectedChar?.let { c ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚖ 副词条权重（${c.name}）", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    state.subStatWeights.toSortedMap(compareByDescending<StatType> { state.subStatWeights[it] })
                        .forEach { (type, w) ->
                            Text(
                                "  ${type.name}: ${"%.1f".format(w)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "副词条评分: ${"%.1f".format(state.subStatScore)} / 100",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun OptimizeResultCard(
    rec: RelicOptimizer.Recommendation,
    onApply: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(rec.notes, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                "期望 ${"%.0f".format(rec.estimatedUnitScore)} · 相对基线 ${
                    "%+.0f".format(rec.upliftVsBaseline * 100)
                }%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "目标副词条: " + rec.relicBuild.targetSubs.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onApply) { Text("写入面板") }
        }
    }
}
