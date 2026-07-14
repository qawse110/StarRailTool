package com.mystarrail.tool.ui.assessment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mystarrail.tool.data.model.CharacterScore
import com.mystarrail.tool.ui.components.TierBadge
import com.mystarrail.tool.ui.components.label
import com.mystarrail.tool.ui.components.shortLabel

@Composable
fun AssessmentScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: AssessmentViewModel = viewModel(
        factory = AssessmentViewModel.factory(
            app.services.repository,
            app.services.scoringEngine
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "角色强度榜（100 分制）",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "按 ScoringEngine 评分排序 · 默认对量子弱点 Boss",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.useSavedBuilds,
                onClick = { viewModel.setUseSavedBuilds(true) },
                label = { Text("已存面板") }
            )
            FilterChip(
                selected = !state.useSavedBuilds,
                onClick = { viewModel.setUseSavedBuilds(false) },
                label = { Text("模板配装") }
            )
            TextButton(onClick = viewModel::refresh) { Text("刷新") }
        }
        Spacer(Modifier.height(12.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.rows, key = { it.characterId }) { score ->
                    val char = state.charMap[score.characterId]
                    AssessmentCard(score = score, charName = char?.name ?: score.characterId,
                        charPath = char?.path, charElement = char?.element)
                }
            }
        }
    }
}

@Composable
private fun AssessmentCard(
    score: CharacterScore,
    charName: String,
    charPath: com.mystarrail.tool.data.model.Path?,
    charElement: com.mystarrail.tool.data.model.Element?
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 摘要行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        charName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        listOfNotNull(
                            charPath?.label(),
                            charElement?.shortLabel()
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TierBadge(tier = score.tier)
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "%.1f".format(score.total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "/ 100",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 展开按钮
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (expanded) "收起详情" else "查看评分明细",
                    style = MaterialTheme.typography.labelSmall)
            }

            // 明细
            if (expanded) {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                ScoreDimension("单位价值", score.unitValueScore, 25.0)
                ScoreDimension("循环期望", score.cycleScore, 5.0)
                ScoreDimension("配队协同", score.teamSynergyScore, 40.0)
                ScoreDimension("场景适配", score.scenarioScore, 20.0)
                ScoreDimension("机制完整度", score.mechanicCoverage, 10.0)
                if (score.utilityScore > 0) {
                    ScoreDimension("治疗/护盾", score.utilityScore, 10.0)
                }
            }
        }
    }
}

@Composable
private fun ScoreDimension(label: String, value: Double, max: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        // 进度条
        LinearProgressIndicator(
            progress = { (value / max).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(6.dp),
        )
        Text(
            "%.1f / %.0f".format(value, max),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}