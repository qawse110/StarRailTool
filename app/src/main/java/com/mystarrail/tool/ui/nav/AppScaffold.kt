package com.mystarrail.tool.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentRouteObj = currentRoute?.toRoute()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "星穹铁道强度工具",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                DrawerItems.forEach { route ->
                    NavigationDrawerItem(
                        label = { Text(route.label()) },
                        selected = currentRoute == route.path,
                        onClick = {
                            navController.navigate(route.path) {
                                popUpTo(Route.Characters.path)
                                launchSingleTop = true
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentRouteObj?.topTitle() ?: "星穹铁道") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            bottomBar = {
                if (currentRoute in BottomTabs.map { it.path }) {
                    NavigationBar {
                        BottomTabs.forEach { route ->
                            NavigationBarItem(
                                selected = currentRoute == route.path,
                                onClick = {
                                    navController.navigate(route.path) {
                                        popUpTo(Route.Characters.path)
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Icon(route.icon(), contentDescription = route.label()) },
                                label = { Text(route.label()) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            content(Modifier.padding(padding))
        }
    }
}

private fun Route.label(): String = when (this) {
    Route.Assessment -> "评估"
    Route.TeamBuilder -> "配队"
    Route.Characters -> "角色"
    Route.Scenario -> "场景"
    Route.Build -> "玩家面板"
    Route.BattleLog -> "战斗日志"
    Route.RelicScorer -> "遗器"
    Route.Scraper -> "数据更新"
    is Route.CharacterDetail -> "角色详情"
}

private fun Route.icon(): ImageVector = when (this) {
    Route.Assessment -> Icons.Default.Assessment
    Route.TeamBuilder -> Icons.Default.Group
    Route.Characters -> Icons.Default.Person
    Route.Scenario -> Icons.Default.SportsKabaddi
    else -> Icons.Default.Assessment
}

private fun Route.topTitle(): String = when (this) {
    Route.Assessment -> "强度评估"
    Route.TeamBuilder -> "配队模拟"
    Route.Characters -> "角色库"
    Route.Scenario -> "场景推荐"
    Route.Build -> "玩家面板"
    Route.BattleLog -> "战斗日志"
    Route.RelicScorer -> "遗器评估"
    Route.Scraper -> "数据更新"
    is Route.CharacterDetail -> "角色详情"
}

/** 把 path 字符串还原为 Route 对象（用于 title/label 查找）。 */
private fun String.toRoute(): Route? = when (this) {
    Route.Assessment.path -> Route.Assessment
    Route.TeamBuilder.path -> Route.TeamBuilder
    Route.Characters.path -> Route.Characters
    Route.Scenario.path -> Route.Scenario
    Route.Build.path -> Route.Build
    Route.BattleLog.path -> Route.BattleLog
    Route.RelicScorer.path -> Route.RelicScorer
    Route.Scraper.path -> Route.Scraper
    else -> if (startsWith("character/")) Route.CharacterDetail(substringAfter("character/")) else null
}
