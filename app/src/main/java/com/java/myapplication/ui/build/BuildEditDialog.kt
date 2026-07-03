package com.java.myapplication.ui.build

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.LightCone
import com.java.myapplication.data.model.PlayerBuild
import com.java.myapplication.data.model.StatType
import com.java.myapplication.ui.components.LightConePicker

/**
 * 玩家 Build 编辑弹窗。
 *
 * @param existing 已存在的 Build（新建时为 null）
 * @param allCharacters 所有角色（用于选择/显示名）
 * @param allLightCones 所有光锥（供 picker 使用）
 * @param onSave 保存回调（外部负责 upsert）
 * @param onDismiss 取消/关闭
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildEditDialog(
    existing: PlayerBuild?,
    allCharacters: List<Character>,
    allLightCones: List<LightCone>,
    onSave: (PlayerBuild) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    // 状态：可编辑字段
    var characterId by remember { mutableStateOf(existing?.characterId ?: allCharacters.firstOrNull()?.id ?: "") }
    var levelText by remember { mutableStateOf((existing?.level ?: 80).toString()) }
    var selectedCone by remember { mutableStateOf<LightCone?>(null) }
    var eidolons by remember { mutableStateOf(existing?.eidolons ?: emptySet()) }
    var showConePicker by remember { mutableStateOf(false) }

    // 初始化光锥选择
    LaunchedEffect(existing, allLightCones) {
        if (selectedCone == null && existing?.lightConeId != null) {
            selectedCone = allLightCones.firstOrNull { it.id == existing.lightConeId }
        }
    }

    val character = allCharacters.firstOrNull { it.id == characterId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "新建 Build" else "编辑 Build") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 角色选择（简化版：直接显示已选 + 切换按钮）
                Text(
                    "角色：${character?.name ?: "(未选)"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (existing == null) {
                    // 新建模式下允许选角色
                    ExposedCharacterDropdown(
                        characters = allCharacters,
                        selectedId = characterId,
                        onSelect = { characterId = it }
                    )
                }

                // 等级
                OutlinedTextField(
                    value = levelText,
                    onValueChange = { levelText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("等级 (1-80)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 光锥
                Text(
                    "光锥：${selectedCone?.name ?: "未选择"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = { showConePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("选择光锥") }

                // 星魂
                Text(
                    "🔮 星魂（已选 ${eidolons.size}）",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..6).forEach { lv ->
                        FilterChip(
                            selected = lv in eidolons,
                            onClick = {
                                eidolons = eidolons.toMutableSet().apply {
                                    if (lv in this) remove(lv) else add(lv)
                                }
                            },
                            label = { Text("E$lv") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val build = (existing ?: PlayerBuild(
                        id = 0,
                        characterId = characterId,
                        lightConeId = "",
                        relicSet4 = "",
                        mainStats = com.java.myapplication.data.model.MainStats(
                            body = StatType.CRIT_DMG,
                            boots = StatType.SPD,
                            sphere = StatType.ATK,
                            rope = StatType.ATK
                        ),
                        subStats = emptyList()
                    )).copy(
                        characterId = characterId,
                        level = levelText.toIntOrNull()?.coerceIn(1, 80) ?: 80,
                        lightConeId = selectedCone?.id ?: "",
                        eidolons = eidolons
                    )
                    onSave(build)
                },
                enabled = characterId.isNotEmpty()
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (existing != null && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除")
                    }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )

    if (showConePicker) {
        LightConePicker(
            lightCones = allLightCones,
            selected = selectedCone,
            onSelect = { selectedCone = it },
            onDismiss = { showConePicker = false }
        )
    }
}

/** 简化版角色下拉：点击展开/选择 — 不用 ExposedDropdownMenuBox 以减少 API 复杂度。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedCharacterDropdown(
    characters: List<Character>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = characters.firstOrNull { it.id == selectedId }?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("选角色") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            characters.forEach { c ->
                DropdownMenuItem(
                    text = { Text("${c.name} (${c.element.name})") },
                    onClick = {
                        onSelect(c.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** 给 Build 加一个标记用字段：用户点"删除"时用 — VM 检测到 _deleted=true 触发 delete。 */
private var PlayerBuild._deleted: Boolean
    get() = false
    set(_) { /* 不会被读：仅供 dialog 内 onClick 传特殊标记；实际由外部 onSave 处理 */ }