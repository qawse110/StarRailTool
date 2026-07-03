package com.java.myapplication.util

import android.content.Context
import com.java.myapplication.data.local.AppDatabase
import com.java.myapplication.data.repository.CharacterRepository
import com.java.myapplication.data.repository.RoomCharacterRepository
import com.java.myapplication.data.seed.SeedImporter
import com.java.myapplication.engine.simulator.ScoringEngine
import com.java.myapplication.engine.simulator.damage.DamageCalculator
import com.java.myapplication.engine.simulator.sim.DiscreteEventSimulator
import com.java.myapplication.engine.simulator.sim.SimulationResult
import com.java.myapplication.engine.simulator.tables.FormulaTables

class ServiceLocator(appContext: Context) {
    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val seedImporter: SeedImporter by lazy { SeedImporter(appContext, database) }
    val repository: CharacterRepository by lazy { RoomCharacterRepository(database) }

    val damageCalc: DamageCalculator by lazy { DamageCalculator(FormulaTables()) }
    val simulator: DiscreteEventSimulator by lazy { DiscreteEventSimulator(damageCalc) }
    val scoringEngine: ScoringEngine by lazy { ScoringEngine(damageCalc, simulator) }

    /**
     * M11 配队 → 战斗日志的桥接：TeamBuilder 跑完模拟后写入，
     * BattleLogScreen 启动时读取。
     *
     * YAGNI 简化：不引入 SharedViewModel / SavedStateHandle。
     */
    var lastSimulationResult: SimulationResult? = null
}