package com.mystarrail.tool.util

import android.content.Context
import com.mystarrail.tool.data.local.AppDatabase
import com.mystarrail.tool.data.repository.CharacterRepository
import com.mystarrail.tool.data.repository.RoomCharacterRepository
import com.mystarrail.tool.data.seed.SeedImporter
import com.mystarrail.tool.data.seed.remote.RemoteSeedSource
import com.mystarrail.tool.engine.relic.RelicOptimizer
import com.mystarrail.tool.engine.simulator.ScoringEngine
import com.mystarrail.tool.engine.simulator.damage.DamageCalculator
import com.mystarrail.tool.engine.simulator.sim.DiscreteEventSimulator
import com.mystarrail.tool.engine.simulator.sim.SimulationResult
import com.mystarrail.tool.engine.simulator.tables.FormulaTables
import com.mystarrail.tool.engine.team.TeamOptimizer

class ServiceLocator(appContext: Context) {
    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val seedImporter: SeedImporter by lazy { SeedImporter(appContext, database) }
    val remoteSeedSource: RemoteSeedSource by lazy { RemoteSeedSource() }
    val repository: CharacterRepository by lazy { RoomCharacterRepository(database) }

    val damageCalc: DamageCalculator by lazy { DamageCalculator(FormulaTables()) }
    val simulator: DiscreteEventSimulator by lazy { DiscreteEventSimulator(damageCalc) }
    val scoringEngine: ScoringEngine by lazy { ScoringEngine(damageCalc, simulator) }
    val teamOptimizer: TeamOptimizer by lazy { TeamOptimizer(scoringEngine) }
    val relicOptimizer: RelicOptimizer by lazy { RelicOptimizer(damageCalc) }

    /**
     * M11 配队 → 战斗日志的桥接：TeamBuilder 跑完模拟后写入，
     * BattleLogScreen 启动时读取。
     *
     * YAGNI 简化：不引入 SharedViewModel / SavedStateHandle。
     */
    var lastSimulationResult: SimulationResult? = null
}
