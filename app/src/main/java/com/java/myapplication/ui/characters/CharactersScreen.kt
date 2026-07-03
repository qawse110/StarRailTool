package com.java.myapplication.ui.characters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.java.myapplication.StarRailApp
import com.java.myapplication.data.model.Element
import com.java.myapplication.ui.characters.components.CharacterCard

@Composable
fun CharactersScreen(
    onCharacterClick: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: CharactersViewModel = viewModel(
        factory = CharactersViewModel.factory(app.services.repository)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.search,
            onValueChange = viewModel::setSearch,
            label = { Text("搜索角色") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))

        // 元素筛选
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = state.elementFilter == null,
                onClick = { viewModel.setElementFilter(null) },
                label = { Text("全部") }
            )
            Element.values().forEach { e ->
                FilterChip(
                    selected = state.elementFilter == e,
                    onClick = { viewModel.setElementFilter(e) },
                    label = { Text(e.name.take(3)) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (state.filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无数据")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.filtered, key = { it.id }) { character ->
                    CharacterCard(
                        character = character,
                        onClick = { onCharacterClick(character.id) }
                    )
                }
            }
        }
    }
}