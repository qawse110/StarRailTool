package com.mystarrail.tool.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mystarrail.tool.ui.assessment.AssessmentScreen
import com.mystarrail.tool.ui.battle.BattleLogScreen
import com.mystarrail.tool.ui.build.BuildScreen
import com.mystarrail.tool.ui.characters.CharacterDetailScreen
import com.mystarrail.tool.ui.characters.CharactersScreen
import com.mystarrail.tool.ui.relic.RelicScorerScreen
import com.mystarrail.tool.ui.scenario.ScenarioScreen
import com.mystarrail.tool.ui.scraper.ScraperScreen
import com.mystarrail.tool.ui.teambuilder.TeamBuilderScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    AppScaffold(navController) { modifier ->
        NavHost(
            navController = navController,
            startDestination = Route.Characters.path,
            modifier = modifier
        ) {
            composable(Route.Assessment.path) { AssessmentScreen() }
            composable(Route.TeamBuilder.path) {
                TeamBuilderScreen(
                    onShowBattleLog = { navController.navigate(Route.BattleLog.path) }
                )
            }
            composable(Route.Characters.path) {
                CharactersScreen(onCharacterClick = { id ->
                    navController.navigate(Route.CharacterDetail(id).path)
                })
            }
            composable(Route.Scenario.path) { ScenarioScreen() }
            composable(Route.Build.path) { BuildScreen() }
            composable(Route.BattleLog.path) {
                BattleLogScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.RelicScorer.path) { RelicScorerScreen() }
            composable(Route.Scraper.path) { ScraperScreen() }
            composable(
                Route.CharacterDetail.PATTERN,
                arguments = listOf(navArgument(Route.CharacterDetail.ARG) {
                    type = NavType.StringType
                })
            ) { entry ->
                val id = entry.arguments?.getString(Route.CharacterDetail.ARG) ?: ""
                CharacterDetailScreen(characterId = id, onBack = { navController.popBackStack() })
            }
        }
    }
}
