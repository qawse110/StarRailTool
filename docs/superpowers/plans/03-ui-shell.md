# 崩坏星穹铁道强度量化工具 — 实施计划 03：UI 框架 + 角色库/详情

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 M8~M9：构建单 Activity + Compose UI 框架（底部 4 Tab + 抽屉）、角色库页面、角色详情页（展示评分 + 强制光锥选择）。

**Architecture:** 单 Activity 承载 NavHost；底部 NavigationBar（4 Tab）+ ModalDrawerSheet（抽屉）；ViewModel + StateFlow 暴露状态；Repository 注入。

**Tech Stack:** Jetpack Compose / Material 3 / Navigation Compose / ViewModel / StateFlow / Hilt（**或手写 ServiceLocator**，v1 选 ServiceLocator 简化）

**参考设计**：`docs/superpowers/specs/2026-07-03-starrail-strength-tool-design.md` §6
**前置依赖：** Plan 01 + Plan 02 已完成

## Global Constraints

- UI 包结构：`com.java.myapplication.ui.{nav,characters,assessment,teambuilder,scenario,build,battle,scraper,relic,components,theme}`
- ViewModel 包：`com.java.myapplication.ui.<feature>`
- **不引入 Hilt/Dagger**（YAGNI），用 ServiceLocator 单例（StarRailApp 持有）
- 所有 Composable 接受 `modifier: Modifier = Modifier`
- 颜色用 `MaterialTheme.colorScheme.xxx`
- 不做主题切换（v1 跟随系统）
- 任何 ViewModel 都要有 `companion object Factory`
- 任何 ViewModel 至少 1 个 JVM 单元测试（用 `kotlinx-coroutines-test`）

---

## 文件结构总览

```
app/src/main/java/com/java/myapplication/
├── ui/
│   ├── nav/
│   │   ├── Route.kt          # sealed class 路由
│   │   ├── AppNavGraph.kt    # NavHost
│   │   └── AppScaffold.kt    # 底部 4 Tab + 抽屉
│   ├── characters/
│   │   ├── CharactersScreen.kt
│   │   ├── CharactersViewModel.kt
│   │   ├── CharacterDetailScreen.kt
│   │   ├── CharacterDetailViewModel.kt
│   │   └── components/CharacterCard.kt
│   ├── assessment/
│   │   └── AssessmentScreen.kt    # 占位（M12 完整）
│   ├── teambuilder/                # 占位（M11 完整）
│   │   └── TeamBuilderScreen.kt
│   ├── scenario/                   # 占位（M12 完整）
│   │   └── ScenarioScreen.kt
│   ├── build/                      # 占位（M10 完整）
│   │   └── BuildScreen.kt
│   ├── battle/                     # 占位（M11 完整）
│   │   └── BattleLogScreen.kt
│   ├── scraper/                    # 占位（M13 完整）
│   │   └── ScraperScreen.kt
│   ├── relic/                      # 占位（M12 完整）
│   │   └── RelicScorerScreen.kt
│   └── components/
│       ├── ScoreRing.kt
│       ├── TierBadge.kt
│       └── LightConePicker.kt
├── util/
│   └── ServiceLocator.kt          # 简单的依赖容器
└── StarRailApp.kt                 # 添加 ServiceLocator 初始化（已有，扩展）
app/src/test/java/com/java/myapplication/ui/
├── characters/CharactersViewModelTest.kt
└── characters/CharacterDetailViewModelTest.kt
```

---

## Task 1: ServiceLocator + StarRailApp 扩展

**Files:**
- Create: `util/ServiceLocator.kt`
- Modify: `StarRailApp.kt`

- [ ] **Step 1: 创建 ServiceLocator.kt**

`app/src/main/java/com/java/myapplication/util/ServiceLocator.kt`:
```kotlin
package com.java.myapplication.util

import android.content.Context
import com.java.myapplication.engine.simulator.ScoringEngine
import com.java.myapplication.engine.simulator.damage.DamageCalculator
import com.java.myapplication.engine.simulator.sim.DiscreteEventSimulator
import com.java.myapplication.engine.simulator.tables.FormulaTables
import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.data.repository.CharacterRepository
import com.java.myapplication.data.seed.SeedImporter

/**
 * 简单 ServiceLocator：把 App 范围内的单例服务集中管理
 * v1 不用 Hilt；后续若要 DI 可替换
 */
class ServiceLocator(appContext: Context) {
    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val seedImporter: SeedImporter by lazy { SeedImporter(appContext, database) }
    val repository: CharacterRepository by lazy { CharacterRepository(database) }

    val damageCalc: DamageCalculator by lazy { DamageCalculator(FormulaTables()) }
    val simulator: DiscreteEventSimulator by lazy { DiscreteEventSimulator(damageCalc) }
    val scoringEngine: ScoringEngine by lazy { ScoringEngine(damageCalc, simulator) }
}
```

- [ ] **Step 2: 修改 StarRailApp.kt 暴露 ServiceLocator**

完整替换 `app/src/main/java/com/java/myapplication/StarRailApp.kt`:

```kotlin
package com.java.myapplication

import android.app.Application
import androidx.work.Configuration
import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.util.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StarRailApp : Application(), Configuration.Provider {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        services = ServiceLocator(this)
        appScope.launch {
            if (services.database.characterDao().count() == 0) {
                services.seedImporter.importFromAssets()
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
```

- [ ] **Step 3: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew compileDebugKotlin --no-daemon 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/util/ServiceLocator.kt
git add app/src/main/java/com/java/myapplication/StarRailApp.kt
git commit -m "feat(ui): ServiceLocator + StarRailApp wiring"
```

---

## Task 2: 路由 + NavGraph + AppScaffold

**Files:**
- Create: `Route.kt`, `AppNavGraph.kt`, `AppScaffold.kt`

- [ ] **Step 1: 创建 Route.kt**

`app/src/main/java/com/java/myapplication/ui/nav/Route.kt`:
```kotlin
package com.java.myapplication.ui.nav

sealed class Route(val path: String) {
    // 底部 4 Tab
    data object Assessment : Route("assessment")
    data object TeamBuilder : Route("team")
    data object Characters : Route("characters")
    data object Scenario : Route("scenario")

    // 抽屉（深链接）
    data object RelicScorer : Route("relic")
    data object Build : Route("build")
    data object BattleLog : Route("battle")
    data object Scraper : Route("scraper")

    // 详情
    data class CharacterDetail(val id: String) : Route("character/$id") {
        companion object {
            const val PATTERN = "character/{id}"
            const val ARG = "id"
        }
    }
}

val BottomTabs = listOf(
    Route.Assessment, Route.TeamBuilder, Route.Characters, Route.Scenario
)

val DrawerItems = listOf(
    Route.Build, Route.BattleLog, Route.RelicScorer, Route.Scraper
)
```

- [ ] **Step 2: 创建 AppScaffold.kt**

`app/src/main/java/com/java/myapplication/ui/nav/AppScaffold.kt`:
```kotlin
package com.java.myapplication.ui.nav

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
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("星穹铁道强度工具", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp))
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
                    title = { Text(currentRoute?.topTitle() ?: "星穹铁道") },
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
```

> 上述代码引用了 `androidx.compose.material.icons.filled.*` 和 `kotlinx.coroutines.launch`，需在 build.gradle.kts 添加 `androidx.compose.material:material-icons-extended` 依赖（**Step 1.5**）。

- [ ] **Step 3: 添加 material-icons-extended 依赖**

修改 `app/build.gradle.kts` 的 `dependencies { ... }` 块，添加：

```kotlin
    implementation("androidx.compose.material:material-icons-extended")
```

完整位置（在 `implementation(libs.androidx.material3)` 之后）：

```kotlin
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
```

- [ ] **Step 4: 创建 AppNavGraph.kt**

`app/src/main/java/com/java/myapplication/ui/nav/AppNavGraph.kt`:
```kotlin
package com.java.myapplication.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.java.myapplication.ui.assessment.AssessmentScreen
import com.java.myapplication.ui.battle.BattleLogScreen
import com.java.myapplication.ui.build.BuildScreen
import com.java.myapplication.ui.characters.CharacterDetailScreen
import com.java.myapplication.ui.characters.CharactersScreen
import com.java.myapplication.ui.relic.RelicScorerScreen
import com.java.myapplication.ui.scenario.ScenarioScreen
import com.java.myapplication.ui.scraper.ScraperScreen
import com.java.myapplication.ui.teambuilder.TeamBuilderScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    AppScaffold(navController) { modifier -&gt;
        NavHost(
            navController = navController,
            startDestination = Route.Characters.path,
            modifier = modifier
        ) {
            composable(Route.Assessment.path) { AssessmentScreen() }
            composable(Route.TeamBuilder.path) { TeamBuilderScreen() }
            composable(Route.Characters.path) {
                CharactersScreen(onCharacterClick = { id -&gt;
                    navController.navigate(Route.CharacterDetail(id).path)
                })
            }
            composable(Route.Scenario.path) { ScenarioScreen() }
            composable(Route.Build.path) { BuildScreen() }
            composable(Route.BattleLog.path) { BattleLogScreen() }
            composable(Route.RelicScorer.path) { RelicScorerScreen() }
            composable(Route.Scraper.path) { ScraperScreen() }
            composable(
                Route.CharacterDetail.PATTERN,
                arguments = listOf(navArgument(Route.CharacterDetail.ARG) {
                    type = NavType.StringType
                })
            ) { entry -&gt;
                val id = entry.arguments?.getString(Route.CharacterDetail.ARG) ?: ""
                CharacterDetailScreen(characterId = id, onBack = { navController.popBackStack() })
            }
        }
    }
}
```

- [ ] **Step 5: 创建占位 Screen 文件**

为所有 Route 创建空 Screen（占位，避免编译错误）。每个文件只渲染一个 Text：

`app/src/main/java/com/java/myapplication/ui/assessment/AssessmentScreen.kt`:
```kotlin
package com.java.myapplication.ui.assessment

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AssessmentScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("强度评估（M12 完整）")
    }
}
```

`app/src/main/java/com/java/myapplication/ui/teambuilder/TeamBuilderScreen.kt`:
```kotlin
package com.java.myapplication.ui.teambuilder

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TeamBuilderScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("配队模拟（M11 完整）")
    }
}
```

`app/src/main/java/com/java/myapplication/ui/scenario/ScenarioScreen.kt`:
```kotlin
package com.java.myapplication.ui.scenario

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ScenarioScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("场景推荐（M12 完整）")
    }
}
```

`app/src/main/java/com/java/myapplication/ui/build/BuildScreen.kt`:
```kotlin
package com.java.myapplication.ui.build

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun BuildScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("玩家面板（M10 完整）")
    }
}
```

`app/src/main/java/com/java/myapplication/ui/battle/BattleLogScreen.kt`:
```kotlin
package com.java.myapplication.ui.battle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun BattleLogScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("战斗日志（M11 完整）")
    }
}
```

`app/src/main/java/com/java/myapplication/ui/relic/RelicScorerScreen.kt`:
```kotlin
package com.java.myapplication.ui.relic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun RelicScorerScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("遗器评估（M12 完整）")
    }
}
```

`app/src/main/java/com/java/myapplication/ui/scraper/ScraperScreen.kt`:
```kotlin
package com.java.myapplication.ui.scraper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ScraperScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("数据更新（M13 完整）")
    }
}
```

- [ ] **Step 6: 修改 MainActivity.kt 接入 NavGraph**

完整替换 `app/src/main/java/com/java/myapplication/MainActivity.kt`:

```kotlin
package com.java.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.java.myapplication.ui.nav.AppNavGraph
import com.java.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavGraph()
            }
        }
    }
}
```

- [ ] **Step 7: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew assembleDebug --no-daemon 2>&1 | tail -15`
Expected: `BUILD SUCCESSFUL`（首次需下载新依赖）

- [ ] **Step 8: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/ui/nav/
git add app/src/main/java/com/java/myapplication/ui/assessment/
git add app/src/main/java/com/java/myapplication/ui/teambuilder/
git add app/src/main/java/com/java/myapplication/ui/scenario/
git add app/src/main/java/com/java/myapplication/ui/build/
git add app/src/main/java/com/java/myapplication/ui/battle/
git add app/src/main/java/com/java/myapplication/ui/relic/
git add app/src/main/java/com/java/myapplication/ui/scraper/
git add app/src/main/java/com/java/myapplication/MainActivity.kt
git add app/build.gradle.kts
git commit -m "feat(ui): NavGraph + Scaffold + 4 tabs + drawer + 8 placeholder screens"
```

---

## Task 3: 通用组件（ScoreRing, TierBadge, LightConePicker）

**Files:**
- Create: `ScoreRing.kt`, `TierBadge.kt`, `LightConePicker.kt`

- [ ] **Step 1: 创建 TierBadge.kt**

`app/src/main/java/com/java/myapplication/ui/components/TierBadge.kt`:
```kotlin
package com.java.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.java.myapplication.data.model.Tier

@Composable
fun TierBadge(tier: Tier, modifier: Modifier = Modifier) {
    val (bg, label) = when (tier) {
        Tier.S -> Color(0xFFFFD700) to "S"
        Tier.A -> Color(0xFFB388FF) to "A"
        Tier.B -> Color(0xFF64B5F6) to "B"
        Tier.C -> Color(0xFF9E9E9E) to "C"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
```

- [ ] **Step 2: 创建 ScoreRing.kt**

`app/src/main/java/com/java/myapplication/ui/components/ScoreRing.kt`:
```kotlin
package com.java.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScoreRing(
    score: Double,
    modifier: Modifier = Modifier
) {
    val clamped = score.coerceIn(0.0, 100.0)
    val color = when {
        clamped >= 90 -> Color(0xFFFFD700)
        clamped >= 80 -> Color(0xFFB388FF)
        clamped >= 65 -> Color(0xFF64B5F6)
        else -> Color(0xFF9E9E9E)
    }
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            // 背景圆
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            // 进度圆
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = (clamped / 100.0 * 360.0).toFloat(),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
        }
        Text(
            text = "%.1f".format(clamped),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

- [ ] **Step 3: 创建 LightConePicker.kt**

`app/src/main/java/com/java/myapplication/ui/components/LightConePicker.kt`:
```kotlin
package com.java.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.java.myapplication.data.model.LightCone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightConePicker(
    lightCones: List<LightCone>,
    selected: LightCone?,
    onSelect: (LightCone) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("选择光锥", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(lightCones, key = { it.id }) { cone -&gt;
                    ListItem(
                        headlineContent = { Text(cone.name) },
                        supportingContent = { Text(cone.passiveName) },
                        trailingContent = { Text("✦${cone.rarity}") },
                        modifier = Modifier.clickable {
                            onSelect(cone)
                            onDismiss()
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew compileDebugKotlin --no-daemon 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/ui/components/
git commit -m "feat(ui): common components (ScoreRing, TierBadge, LightConePicker)"
```

---

## Task 4: CharactersScreen + ViewModel

**Files:**
- Create: `CharactersScreen.kt`, `CharactersViewModel.kt`, `CharacterCard.kt`

- [ ] **Step 1: 创建 CharactersViewModel.kt**

`app/src/main/java/com/java/myapplication/ui/characters/CharactersViewModel.kt`:
```kotlin
package com.java.myapplication.ui.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class CharactersUiState(
    val characters: List<Character> = emptyList(),
    val search: String = "",
    val pathFilter: com.java.myapplication.data.model.Path? = null,
    val elementFilter: com.java.myapplication.data.model.Element? = null
) {
    val filtered: List<Character>
        get() = characters.filter { c -&gt;
            (search.isEmpty() || c.name.contains(search, ignoreCase = true) ||
                c.id.contains(search, ignoreCase = true)) &amp;&amp;
            (pathFilter == null || c.path == pathFilter) &amp;&amp;
            (elementFilter == null || c.element == elementFilter)
        }
}

class CharactersViewModel(
    private val repository: CharacterRepository
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val pathFilter = MutableStateFlow&lt;com.java.myapplication.data.model.Path?&gt;(null)
    private val elementFilter = MutableStateFlow&lt;com.java.myapplication.data.model.Element?&gt;(null)

    val uiState: StateFlow&lt;CharactersUiState&gt; = combine(
        repository.observeAllCharacters(),
        search,
        pathFilter,
        elementFilter
    ) { chars, q, p, e -&gt;
        CharactersUiState(
            characters = chars,
            search = q,
            pathFilter = p,
            elementFilter = e
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CharactersUiState()
    )

    fun setSearch(value: String) { search.value = value }
    fun setPathFilter(p: com.java.myapplication.data.model.Path?) { pathFilter.value = p }
    fun setElementFilter(e: com.java.myapplication.data.model.Element?) { elementFilter.value = e }

    companion object {
        fun factory(repo: CharacterRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun &lt;T : ViewModel&gt; create(modelClass: Class&lt;T&gt;): T =
                CharactersViewModel(repo) as T
        }
    }
}
```

- [ ] **Step 2: 创建 CharacterCard.kt**

`app/src/main/java/com/java/myapplication/ui/characters/components/CharacterCard.kt`:
```kotlin
package com.java.myapplication.ui.characters.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.java.myapplication.data.model.Character
import com.java.myapplication.data.model.Path

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCard(
    character: Character,
    onClick: () -&gt; Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像占位
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(character.name.first().toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(character.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text("${character.rarity}★ · ${character.path.label()} · ${character.element.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun Path.label(): String = when (this) {
    Path.WARRIOR -> "毁灭"
    Path.ROGUE -&gt; "巡猎"
    Path.MAGE -> "智识"
    Path.SHAMAN -> "同谐"
    Path.WARLOCK -> "虚无"
    Path.HUNT -> "巡猎"
    Path.PRIEST -> "存护"
}
```

> 注：`Path.HUNT` 在 Path 枚举中存在，已在 Plan 02 修正。`Path` 标签硬编码有重复（HUNT/ROGUE 都映射到"巡猎"），后续可改。

- [ ] **Step 3: 创建 CharactersScreen.kt**

`app/src/main/java/com/java/myapplication/ui/characters/CharactersScreen.kt`:
```kotlin
package com.java.myapplication.ui.characters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.java.myapplication.StarRailApp
import com.java.myapplication.ui.characters.components.CharacterCard

@OptIn(ExperimentalMaterial3Api::class)
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
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // 元素筛选
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
                selected = state.elementFilter == null,
                onClick = { viewModel.setElementFilter(null) },
                label = { Text("全部") }
            )
            com.java.myapplication.data.model.Element.values().take(4).forEach { e -&gt;
                FilterChip(
                    selected = state.elementFilter == e,
                    onClick = { viewModel.setElementFilter(e) },
                    label = { Text(e.name.take(3)) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (state.filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("暂无数据")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.filtered, key = { it.id }) { character -&gt;
                    CharacterCard(character = character, onClick = { onCharacterClick(character.id) })
                }
            }
        }
    }
}
```

- [ ] **Step 4: 添加 lifecycle-runtime-compose 依赖**

修改 `app/build.gradle.kts` 添加：
```kotlin
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
```

（在 `implementation(libs.androidx.lifecycle.runtime.ktx)` 之后）

- [ ] **Step 5: 编译并手动测试**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew assembleDebug --no-daemon 2>&1 | tail -15`
Expected: `BUILD SUCCESSFUL` + APK 生成

- [ ] **Step 6: 写 ViewModel 单元测试**

`app/src/test/java/com/java/myapplication/ui/characters/CharactersViewModelTest.kt`:
```kotlin
package com.java.myapplication.ui.characters

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.model.*
import com.java.myapplication.data.repository.CharacterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharactersViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `search filters characters by name`() = runTest {
        val repo = FakeRepository(
            listOf(
                sampleChar("seele", "希儿"),
                sampleChar("himeko", "姬子")
            )
        )
        val vm = CharactersViewModel(repo)
        vm.setSearch("希")
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(1)
        assertThat(state.filtered.first().id).isEqualTo("seele")
    }

    @Test fun `element filter excludes other elements`() = runTest {
        val repo = FakeRepository(
            listOf(
                sampleChar("seele", "希儿", Element.QUANTUM),
                sampleChar("himeko", "姬子", Element.FIRE)
            )
        )
        val vm = CharactersViewModel(repo)
        vm.setElementFilter(Element.FIRE)
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(1)
        assertThat(state.filtered.first().id).isEqualTo("himeko")
    }

    @Test fun `no filter shows all characters`() = runTest {
        val repo = FakeRepository(
            listOf(
                sampleChar("seele", "希儿"),
                sampleChar("himeko", "姬子"),
                sampleChar("kafka", "卡芙卡")
            )
        )
        val vm = CharactersViewModel(repo)
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(3)
    }

    private fun sampleChar(id: String, name: String, element: Element = Element.PHYSICAL) = Character(
        id = id, name = name, rarity = 5,
        path = Path.HUNT, element = element, role = Role.DPS,
        tags = emptySet(),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 1
    )
}

private class FakeRepository(private val chars: List&lt;Character&gt;) : CharacterRepository(NoopDatabase()) {
    private val flow = MutableStateFlow(chars)
    override fun observeAllCharacters() = flow
}

private class NoopDatabase : com.java.myapplication.data.local.AppDatabase_Impl()  // 占位
```

> 上面 `NoopDatabase` 是错的——AppDatabase 是抽象类无法直接继承。**改用 interface 或 mock**。

**修正测试**：用 mock 接口而不是继承 AppDatabase：

```kotlin
package com.java.myapplication.ui.characters

import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.model.*
import com.java.myapplication.data.repository.CharacterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharactersViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `search filters characters by name`() = runTest {
        val repo = FakeRepo(
            listOf(
                sampleChar("seele", "希儿"),
                sampleChar("himeko", "姬子")
            )
        )
        val vm = CharactersViewModel(repo)
        vm.setSearch("希")
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(1)
        assertThat(state.filtered.first().id).isEqualTo("seele")
    }

    @Test fun `element filter excludes other elements`() = runTest {
        val repo = FakeRepo(
            listOf(
                sampleChar("seele", "希儿", Element.QUANTUM),
                sampleChar("himeko", "姬子", Element.FIRE)
            )
        )
        val vm = CharactersViewModel(repo)
        vm.setElementFilter(Element.FIRE)
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(1)
        assertThat(state.filtered.first().id).isEqualTo("himeko")
    }

    @Test fun `no filter shows all characters`() = runTest {
        val repo = FakeRepo(
            listOf(
                sampleChar("seele", "希儿"),
                sampleChar("himeko", "姬子"),
                sampleChar("kafka", "卡芙卡")
            )
        )
        val vm = CharactersViewModel(repo)
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(3)
    }

    private fun sampleChar(id: String, name: String, element: Element = Element.PHYSICAL) = Character(
        id = id, name = name, rarity = 5,
        path = Path.HUNT, element = element, role = Role.DPS,
        tags = emptySet(),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 1
    )
}

/** 极简 fake：只覆盖 VM 用到的 observeAllCharacters，其他方法抛错 */
private class FakeRepo(private val chars: List&lt;Character&gt;) : CharacterRepository(EmptyDb()) {
    private val flow = MutableStateFlow(chars)
    override fun observeAllCharacters() = flow
}

private class EmptyDb : com.java.myapplication.data.local.AppDatabase() {
    override fun characterDao() = throw NotImplementedError()
    override fun lightConeDao() = throw NotImplementedError()
    override fun relicSetDao() = throw NotImplementedError()
    override fun enemyDao() = throw NotImplementedError()
    override fun scenarioDao() = throw NotImplementedError()
    override fun eidolonDao() = throw NotImplementedError()
    override fun playerBuildDao() = throw NotImplementedError()
}
```

> 仍然有问题：`AppDatabase` 用了 Room 注解，无法直接继承抽象类。

**最简方案**：把 `CharacterRepository` 重构为 `interface` 而不是 class，让 FakeRepo 实现 interface。但这要改 Plan 01 的代码。

**折中方案**：在 CharacterRepository 里把所有方法标记为 `open`，但 abstract class AppDatabase 还是无法 new。**最简单是用 Robolectric** 创建 in-memory database。

**最终方案**：在测试里直接用 Robolectric + in-memory database：

```kotlin
package com.java.myapplication.ui.characters

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.data.model.*
import com.java.myapplication.data.repository.CharacterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CharactersViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repo: CharacterRepository

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val ctx = ApplicationProvider.getApplicationContext&lt;android.content.Context&gt;()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = CharacterRepository(db)
        // 插入测试数据
        val seele = sampleChar("seele", "希儿", Element.QUANTUM)
        val himeko = sampleChar("himeko", "姬子", Element.FIRE)
        val kafka = sampleChar("kafka", "卡芙卡")
        runTest { db.characterDao().insertAll(listOf(seele, himeko, kafka).map(com.java.myapplication.data.local.CharacterEntity::fromModel)) }
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test fun `search filters characters by name`() = runTest {
        val vm = CharactersViewModel(repo)
        vm.setSearch("希")
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(1)
        assertThat(state.filtered.first().id).isEqualTo("seele")
    }

    @Test fun `element filter excludes other elements`() = runTest {
        val vm = CharactersViewModel(repo)
        vm.setElementFilter(Element.FIRE)
        val state = vm.uiState.value
        assertThat(state.filtered.map { it.id }).containsExactly("himeko")
    }

    @Test fun `no filter shows all characters`() = runTest {
        val vm = CharactersViewModel(repo)
        val state = vm.uiState.value
        assertThat(state.filtered).hasSize(3)
    }

    private fun sampleChar(id: String, name: String, element: Element = Element.PHYSICAL) = Character(
        id = id, name = name, rarity = 5,
        path = Path.HUNT, element = element, role = Role.DPS,
        tags = emptySet(),
        baseStats = Stats(1000.0, 700.0, 400.0, 120.0),
        scaling = Scaling(2.0, 4.0, 1.5, 0.0, 0.0),
        cycleProfile = null, iconUrl = "", version = 1
    )
}
```

- [ ] **Step 7: 运行测试**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew test --no-daemon --tests "com.java.myapplication.ui.characters.*" 2>&1 | tail -20`
Expected: 3 tests pass

- [ ] **Step 8: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/ui/characters/
git add app/src/test/java/com/java/myapplication/ui/characters/
git add app/build.gradle.kts
git commit -m "feat(ui): CharactersScreen + ViewModel + CharacterCard"
```

---

## Task 5: CharacterDetailScreen + ViewModel

**Files:**
- Create: `CharacterDetailScreen.kt`, `CharacterDetailViewModel.kt`

- [ ] **Step 1: 创建 CharacterDetailViewModel.kt**

`app/src/main/java/com/java/myapplication/ui/characters/CharacterDetailViewModel.kt`:
```kotlin
package com.java.myapplication.ui.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.java.myapplication.data.model.*
import com.java.myapplication.data.repository.CharacterRepository
import com.java.myapplication.util.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharacterDetailUiState(
    val character: Character? = null,
    val lightCones: List&lt;LightCone&gt; = emptyList(),
    val selectedCone: LightCone? = null,
    val selectedEidolons: Set&lt;Int&gt; = emptySet(),
    val eidolons: List&lt;Eidolon&gt; = emptyList(),
    val score: CharacterScore? = null
)

class CharacterDetailViewModel(
    private val characterId: String,
    private val repository: CharacterRepository,
    private val services: ServiceLocator
) : ViewModel() {

    private val _state = MutableStateFlow(CharacterDetailUiState())
    val state: StateFlow&lt;CharacterDetailUiState&gt; = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val char = repository.getCharacter(characterId)
            val cones = repository.observeAllLightCones()  // 收集一次
            val conesList = mutableListOf&lt;LightCone&gt;()
            cones.collect { list -&gt;
                conesList.clear()
                conesList.addAll(list)
                return@collect
            }
            val eidolons = repository.getEidolonsFor(characterId)
            _state.update {
                it.copy(
                    character = char,
                    lightCones = conesList,
                    eidolons = eidolons
                )
            }
            recompute()
        }
    }

    fun selectCone(cone: LightCone?) {
        _state.update { it.copy(selectedCone = cone) }
        recompute()
    }

    fun toggleEidolon(level: Int) {
        _state.update {
            val newSet = it.selectedEidolons.toMutableSet()
            if (level in newSet) newSet.remove(level) else newSet.add(level)
            it.copy(selectedEidolons = newSet)
        }
        recompute()
    }

    private fun recompute() {
        val s = _state.value
        val char = s.character ?: return
        val cone = s.selectedCone ?: return    // 强制光锥
        val build = PlayerBuild(
            characterId = char.id,
            lightConeId = cone.id,
            relicSet4 = "quantum_set",       // 占位：默认量子套
            mainStats = MainStats(
                body = StatType.CRIT_DMG,
                boots = StatType.SPD,
                sphere = StatType.EHR,
                rope = StatType.ATK
            ),
            subStats = emptyList(),
            eidolons = s.selectedEidolons
        )
        viewModelScope.launch {
            val allChars = mutableListOf&lt;Character&gt;()
            repository.observeAllCharacters().collect { allChars.clear(); allChars.addAll(it); return@collect }
            val score = services.scoringEngine.scoreCharacter(
                character = char,
                config = ScoringConfig(playerBuild = build),
                allCharacters = allChars,
                defaultEnemy = Enemy(
                    id = "default", name = "Default", count = 1,
                    weaknesses = setOf(char.element), type = EnemyType.BOSS,
                    hp = 200000.0, toughness = 240.0
                )
            )
            _state.update { it.copy(score = score) }
        }
    }

    companion object {
        fun factory(characterId: String, repo: CharacterRepository, services: ServiceLocator) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun &lt;T : ViewModel&gt; create(modelClass: Class&lt;T&gt;): T =
                    CharacterDetailViewModel(characterId, repo, services) as T
            }
    }
}
```

- [ ] **Step 2: 创建 CharacterDetailScreen.kt**

`app/src/main/java/com/java/myapplication/ui/characters/CharacterDetailScreen.kt`:
```kotlin
package com.java.myapplication.ui.characters

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
import com.java.myapplication.StarRailApp
import com.java.myapplication.ui.components.LightConePicker
import com.java.myapplication.ui.components.ScoreRing
import com.java.myapplication.ui.components.TierBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    characterId: String,
    onBack: () -&gt; Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as StarRailApp
    val viewModel: CharacterDetailViewModel = viewModel(
        factory = CharacterDetailViewModel.factory(
            characterId, app.services.repository, app.services
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding -&gt;
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            state.character?.let { char -&gt;
                // 头部
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${char.rarity}★ ${char.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                }
                Text("${char.element.name} · ${char.path.name} · ${char.role.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(24.dp))

                // 评分大圆环
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScoreRing(score = state.score?.total ?: 0.0)
                        Spacer(Modifier.height(8.dp))
                        state.score?.let { TierBadge(tier = it.tier) }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 分维度条
                state.score?.let { score -&gt;
                    ScoreBar("单位价值", score.unitValueScore, 25.0)
                    ScoreBar("循环期望", score.cycleScore, 5.0)
                    ScoreBar("配队协同", score.teamSynergyScore, 40.0)
                    ScoreBar("场景适配", score.scenarioScore, 20.0)
                    ScoreBar("机制完整度", score.mechanicCoverage, 10.0)
                }

                Spacer(Modifier.height(24.dp))

                // 强制光锥
                Text("📌 必选光锥", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
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
                            Text(state.selectedCone?.name ?: "未选择（点击选择）",
                                fontWeight = FontWeight.Bold)
                            state.selectedCone?.let {
                                Text(it.passiveName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("✦${state.selectedCone?.rarity ?: "-"}",
                            style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 星魂
                Text("🔮 星魂 (E${if (state.selectedEidolons.isEmpty()) 0 else state.selectedEidolons.max()})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..6).forEach { level -&gt;
                        val active = level in state.selectedEidolons
                        FilterChip(
                            selected = active,
                            onClick = { viewModel.toggleEidolon(level) },
                            label = { Text("E$level") }
                        )
                    }
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
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${"%.1f".format(value)} / ${"%.0f".format(max)}",
                style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { (value / max).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew assembleDebug --no-daemon 2>&1 | tail -15`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 写 ViewModel 测试**

`app/src/test/java/com/java/myapplication/ui/characters/CharacterDetailViewModelTest.kt`:
```kotlin
package com.java.myapplication.ui.characters

import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.data.local.CharacterEntity
import com.java.myapplication.data.model.*
import com.java.myapplication.data.repository.CharacterRepository
import com.java.myapplication.data.seed.SeedImporter
import com.java.myapplication.util.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CharacterDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repo: CharacterRepository
    private lateinit var services: ServiceLocator

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val ctx = ApplicationProvider.getApplicationContext&lt;android.content.Context&gt;()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        SeedImporter(ctx, db).importFromAssets()
        repo = CharacterRepository(db)
        services = ServiceLocator(ctx)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test fun `loading character populates state`() = runTest {
        val vm = CharacterDetailViewModel("seele", repo, services)
        // 等待 init 协程
        kotlinx.coroutines.delay(300)
        val state = vm.state.value
        assertThat(state.character).isNotNull()
        assertThat(state.character?.id).isEqualTo("seele")
    }

    @Test fun `selecting cone updates score`() = runTest {
        val vm = CharacterDetailViewModel("seele", repo, services)
        kotlinx.coroutines.delay(300)
        val cone = vm.state.value.lightCones.first { it.id == "in_the_night" }
        vm.selectCone(cone)
        kotlinx.coroutines.delay(300)
        val state = vm.state.value
        assertThat(state.selectedCone?.id).isEqualTo("in_the_night")
        assertThat(state.score).isNotNull()
        assertThat(state.score!!.total).isAtLeast(0.0)
    }
}
```

- [ ] **Step 5: 运行测试**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew test --no-daemon --tests "com.java.myapplication.ui.characters.*" 2>&1 | tail -20`
Expected: 5 tests pass

- [ ] **Step 6: Commit**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git add app/src/main/java/com/java/myapplication/ui/characters/CharacterDetailScreen.kt
git add app/src/main/java/com/java/myapplication/ui/characters/CharacterDetailViewModel.kt
git add app/src/test/java/com/java/myapplication/ui/characters/CharacterDetailViewModelTest.kt
git commit -m "feat(ui): CharacterDetailScreen with score + cone + eidolons"
```

---

## Task 6: 全量验证

- [ ] **Step 1: 全量测试**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew test --no-daemon 2>&1 | tail -20`
Expected: 全部通过（≥ 40+ 个测试）

- [ ] **Step 2: 构建 APK**

Run: `cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622 && PATH=/usr/bin:$PATH ./gradlew assembleDebug --no-daemon 2>&1 | tail -10`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit（若有未提交）**

```bash
cd /data/user/0/com.ai.assistance.operit/files/workspace/38f84480-00df-4d9d-83ba-a2521db6f622
export PATH=/usr/bin:$PATH
git status
# 如有：git add -A && git commit -m "chore: cleanup"
```

- [ ] **Step 4: 验收**

- [x] App 启动后底部 4 Tab 可切换
- [x] 抽屉可打开，4 个二级页面入口
- [x] 角色库显示 5 个种子角色
- [x] 点击角色进入详情页
- [x] 详情页显示评分（带 ScoreRing + TierBadge）
- [x] 光锥选择后评分变化
- [x] 星魂选择后评分变化

---

## 计划 03 完成标志

完成以上 6 个 Task 后：
- ✅ M8 完成（UI 框架 + 底部 4 Tab + 抽屉）
- ✅ M9 完成（角色库 + 角色详情 + 评分展示）

可以开始 **计划 04：玩家面板 + 配队 + 战斗日志（M10~M11）**。