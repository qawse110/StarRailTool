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
    AppScaffold(navController) { modifier ->
        NavHost(
            navController = navController,
            startDestination = Route.Characters.path,
            modifier = modifier
        ) {
            composable(Route.Assessment.path) { AssessmentScreen() }
            composable(Route.TeamBuilder.path) { TeamBuilderScreen() }
            composable(Route.Characters.path) {
                CharactersScreen(onCharacterClick = { id ->
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
            ) { entry ->
                val id = entry.arguments?.getString(Route.CharacterDetail.ARG) ?: ""
                CharacterDetailScreen(characterId = id, onBack = { navController.popBackStack() })
            }
        }
    }
}
