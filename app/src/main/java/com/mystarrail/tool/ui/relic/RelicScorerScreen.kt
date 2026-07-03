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
import com.mystarrail.tool.data.model.Character
import com.mystarrail.tool.data.model.StatType
import com.mystarrail.tool.data.model.SubStat

@Composable
fun RelicScorerScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: RelicScorerViewModel = viewModel(
        factory = RelicScorerViewModel.factory(app.services.repository)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("遗器评估", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "基于角色定位的主词条推荐 + 副词条权重评分",
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
            modifier = Modifier.heightIn(max = 150.dp),
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