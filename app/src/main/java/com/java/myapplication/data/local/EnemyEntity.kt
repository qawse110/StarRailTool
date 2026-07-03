package com.java.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.java.myapplication.data.model.Element
import com.java.myapplication.data.model.Enemy
import com.java.myapplication.data.model.EnemyType

@Entity(tableName = "enemies")
data class EnemyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val count: Int,
    val weaknesses: Set<Element>,
    val type: EnemyType,
    val hp: Double,
    val toughness: Double,
    val mechanics: Set<String>
) {
    fun toModel(): Enemy = Enemy(
        id = id, name = name, count = count,
        weaknesses = weaknesses, type = type,
        hp = hp, toughness = toughness, mechanics = mechanics
    )

    companion object {
        fun fromModel(e: Enemy): EnemyEntity = EnemyEntity(
            id = e.id, name = e.name, count = e.count,
            weaknesses = e.weaknesses, type = e.type,
            hp = e.hp, toughness = e.toughness, mechanics = e.mechanics
        )
    }
}