package com.mystarrail.tool.engine.simulator.sim

import com.mystarrail.tool.data.model.Element

data class RoundEvent(
    val round: Int,
    val actorId: String,
    val action: ActionType,
    val targets: List<TargetHit>,
    val damageDealt: Double,
    val healingDone: Double,
    val buffsApplied: List<String>,
    val mechanicsTriggered: List<MechanicEvent>,
    val actionValueBefore: Double,
    val actionValueAfter: Double,
    val ultChargeBefore: Double,
    val ultChargeAfter: Double
)

data class TargetHit(
    val targetId: String,
    val element: Element,
    val damage: Double,
    val isCrit: Boolean
)

data class MechanicEvent(
    val type: String,
    val source: String,
    val target: String?,
    val param: Double
)