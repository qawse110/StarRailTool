package com.mystarrail.tool.ui.nav

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
