package com.mystarrail.tool.ui.characters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.mystarrail.tool.ui.characters.components.BaseStatsBlock
import com.mystarrail.tool.ui.characters.components.EidolonsListBlock
import com.mystarrail.tool.ui.characters.components.LightConeEffectBlock
import com.mystarrail.tool.ui.characters.components.RelicRecommendationsBlock
import com.mystarrail.tool.ui.characters.components.ScalingBlock
import com.mystarrail.tool.ui.characters.components.SkillTreeBlock
import com.mystarrail.tool.ui.characters.components.TagsBlock
import com.mystarrail.tool.ui.components.LightConePicker
import com.mystarrail.tool.ui.components.ScoreRing
import com.mystarrail.tool.ui.components.TierBadge
import com.mystarrail.tool.ui.components.label
import com.mystarrail.tool.ui.components.shortLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    characterId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: CharacterDetailViewModel = viewModel(
        key = "detail-$characterId",
        factory = CharacterDetailViewModel.factory(
            characterId,
            app.services.repository,
            app.services.scoringEngine
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showConePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.character?.name ?: "角色详情") },
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
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            state.character?.let { char ->
                // 头部
                Text(
                    "${char.rarity}★ ${char.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${char.element.shortLabel()} · ${char.path.label()} · ${char.role.label()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                // 基础数值 A
                BaseStatsBlock(stats = char.baseStats)

                // 技能倍率 B
                ScalingBlock(scaling = char.scaling)

                // 标签 C
                TagsBlock(tags = char.tags)

                Spacer(Modifier.height(24.dp))

                // 评分大圆环
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScoreRing(score = state.score?.total ?: 0.0)
                        Spacer(Modifier.height(8.dp))
                        state.score?.let { TierBadge(tier = it.tier) }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 分维度条
                state.score?.let { score ->
                    ScoreBar("单位价值", score.unitValueScore, 25.0)
                    ScoreBar("循环期望", score.cycleScore, 5.0)
                    ScoreBar("配队协同", score.teamSynergyScore, 40.0)
                    ScoreBar("场景适配", score.scenarioScore, 20.0)
                    ScoreBar("机制完整度", score.mechanicCoverage, 10.0)
                }
                Spacer(Modifier.height(24.dp))
                // [新增] 星魂介绍 D
                EidolonsListBlock(eidolons = state.eidolons)
                // [新增] 光锥效果 E
                LightConeEffectBlock(cone = state.selectedCone)
                // [新增] 遗器推荐 F
                RelicRecommendationsBlock(relics = state.relicSets, characterRole = char.role)
                // [新增] 行迹 G
                SkillTreeBlock(skillTree = state.skillTree)
                Spacer(Modifier.height(24.dp))
                // 强制光锥
                Text(
                    "📌 必选光锥",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedCard(
                    onClick = { showConePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                state.selectedCone?.name ?: "未选择（点击选择）",
                                fontWeight = FontWeight.Bold
                            )
                            state.selectedCone?.let {
                                Text(
                                    it.passiveName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "✦${state.selectedCone?.rarity ?: "-"}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 星魂
                Text(
                    "🔮 星魂 (E${if (state.selectedEidolons.isEmpty()) 0 else state.selectedEidolons.max()})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..6).forEach { level ->
                        val active = level in state.selectedEidolons
                        FilterChip(
                            selected = active,
                            onClick = { viewModel.toggleEidolon(level) },
                            label = { Text("E$level") }
                        )
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (showConePicker) {
            LightConePicker(
                lightCones = state.lightCones,
                selected = state.selectedCone,
                onSelect = { viewModel.selectCone(it) },
                onDismiss = { showConePicker = false }
            )
        }
    }
}

@Composable
private fun ScoreBar(label: String, value: Double, max: Double) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${"%.1f".format(value)} / ${"%.0f".format(max)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { (value / max).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
    }
}