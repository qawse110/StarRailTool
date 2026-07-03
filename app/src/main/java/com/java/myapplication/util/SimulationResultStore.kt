package com.java.myapplication.util

import com.java.myapplication.engine.simulator.sim.SimulationResult

/**
 * M11 配队 → 战斗日志的桥接：用 [SimulationResultStore] 接口（不是 ServiceLocator）
 * 让 VM 测试无需 Robolectric/Context。
 */
interface SimulationResultStore {
    var lastSimulationResult: SimulationResult?
}

/** 生产实现：状态存到 ServiceLocator 字段里。 */
class ServiceLocatorResultStore(
    private val services: ServiceLocator
) : SimulationResultStore {
    override var lastSimulationResult: SimulationResult?
        get() = services.lastSimulationResult
        set(value) { services.lastSimulationResult = value }
}