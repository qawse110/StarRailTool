package com.mystarrail.tool.ui.build

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.mystarrail.tool.data.model.LightCone
import com.mystarrail.tool.data.model.PlayerBuild

/**
 * 玩家面板（Build CRUD）界面。
 *
 * 列表展示所有已保存的 PlayerBuild，支持新建/编辑/删除。
 * 编辑/新建使用 [BuildEditDialog]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: BuildViewModel = viewModel(
        factory = BuildViewModel.factory(app.services.repository)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingBuild by remember { mutableStateOf<PlayerBuild?>(null) }

    // 加载所有角色和光锥（供 dialog 使用）
    val allCharacters by app.services.repository.observeAllCharacters()
        .collectAsState(initial = emptyList())
    val allLightCones by app.services.repository.observeAllLightCones()
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("玩家面板") },
                actions = {
                    IconButton(onClick = {
                        editingBuild = null
                        showDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "新建 Build")
                    }
                }
            )
        }
    ) { padding ->
        if (state.builds.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "暂无 Build",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击右上角 + 开始创建",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.builds, key = { it.id }) { build ->
                    val character = state.charMap[build.characterId]
                    val coneName = allLightCones.firstOrNull { it.id == build.lightConeId }?.name
                    BuildCard(
                        build = build,
                        characterName = character?.name ?: build.characterId,
                        lightConeName = coneName,
                        onEdit = { editingBuild = build; showDialog = true },
                        onDelete = { viewModel.delete(build.id) }
                    )
                }
            }
        }
    }

    // 编辑/新建弹窗
    if (showDialog) {
        BuildEditDialog(
            existing = editingBuild,
            allCharacters = allCharacters,
            allLightCones = allLightCones,
            onSave = { build ->
                viewModel.upsert(build)
                showDialog = false
                editingBuild = null
            },
            onDelete = if (editingBuild != null) ({
                viewModel.delete(editingBuild!!.id)
                showDialog = false
                editingBuild = null
            }) else null,
            onDismiss = {
                showDialog = false
                editingBuild = null
            }
        )
    }
}

@Composable
private fun BuildCard(
    build: PlayerBuild,
    characterName: String,
    lightConeName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 角色名 + 等级
                Text(
                    characterName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Lv.${build.level}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "星魂 ${build.eidolons.size}/6",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (lightConeName != null) {
                    Text(
                        "光锥：$lightConeName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 操作按钮
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
