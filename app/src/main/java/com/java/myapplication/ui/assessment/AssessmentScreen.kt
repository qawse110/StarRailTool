package com.java.myapplication.ui.assessment

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
import com.java.myapplication.data.model.CharacterScore
import com.java.myapplication.ui.components.TierBadge
import com.java.myapplication.ui.components.label
import com.java.myapplication.ui.components.shortLabel

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
        Spacer(Modifier.height(12.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.rows, key = { it.characterId }) { score ->
                    val char = state.charMap[score.characterId]
                    AssessmentRow(score = score, charName = char?.name ?: score.characterId,
                        charPath = char?.path, charElement = char?.element)
                }
            }
        }
    }
}

@Composable
private fun AssessmentRow(
    score: CharacterScore,
    charName: String,
    charPath: com.java.myapplication.data.model.Path?,
    charElement: com.java.myapplication.data.model.Element?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
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
            Text(
                "%.1f".format(score.total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}