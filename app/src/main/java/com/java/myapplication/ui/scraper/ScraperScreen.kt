package com.java.myapplication.ui.scraper

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@Composable
fun ScraperScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: ScraperViewModel = viewModel(
        factory = ScraperViewModel.factory(
            app.services.repository,
            reimportCallback = { app.services.seedImporter.importFromAssets() }
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("数据更新", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "查看当前数据源状态 · 抓取 URL · 重新导入 seed JSON",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 数据源状态卡
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📊 数据源状态", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (state.isLoadingStatus) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    StatusRow("角色", state.characterCount)
                    StatusRow("光锥", state.lightConeCount)
                    StatusRow("遗器套", state.relicCount)
                    StatusRow("场景", state.scenarioCount)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.reimportSeed() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("🔄 重新导入 seed JSON") }
                }
            }
        }

        // URL 抓取
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("🌐 抓取 URL", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    label = { Text("URL（http/https）") },
                    placeholder = { Text("https://example.com/wiki/seele") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::fetch,
                    enabled = !state.isFetching && state.url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("抓取预览")
                    }
                }
            }
        }

        // 结果
        state.lastResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    result,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // 错误
        state.lastError?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    "❌ $err",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text("$count", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}