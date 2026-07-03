package com.mystarrail.tool.engine.simulator.tables

data class FormulaTables(
    val level: LevelTable = LevelTable(),
    val element: ElementTable = ElementTable(),
    val weakness: WeaknessTable = WeaknessTable(),
    val breakT: BreakTable = BreakTable(),
    val actionValue: ActionValueTable = ActionValueTable()
)