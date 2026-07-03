package com.java.myapplication.ui.characters

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Task 2 占位；Task 4 会替换为完整角色库列表。 */
@Composable
fun CharactersScreen(onCharacterClick: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("角色库（M9 完整）")
    }
}