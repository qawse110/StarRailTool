package com.mystarrail.tool.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mystarrail.tool.data.model.Enemy
import com.mystarrail.tool.data.model.Scenario

@Entity(tableName = "scenarios")
data class ScenarioEntity(
    @PrimaryKey val id: String,
    val name: String,
    val enemyIds: List<String>,
    val difficulty: Int,
    val notes: String
) {
    fun toModel(enemies: List<Enemy>): Scenario = Scenario(
        id = id, name = name, enemies = enemies,
        difficulty = difficulty, notes = notes
    )

    companion object {
        fun fromModel(s: Scenario): ScenarioEntity = ScenarioEntity(
            id = s.id, name = s.name,
            enemyIds = s.enemies.map { it.id },
            difficulty = s.difficulty, notes = s.notes
        )
    }
}