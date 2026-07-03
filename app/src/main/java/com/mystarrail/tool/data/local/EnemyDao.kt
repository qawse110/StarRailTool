package com.mystarrail.tool.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface EnemyDao {
    @Query("SELECT * FROM enemies")
    fun observeAll(): Flow<List<EnemyEntity>>

    @Query("SELECT * FROM enemies")
    suspend fun getAll(): List<EnemyEntity>

    @Query("SELECT * FROM enemies WHERE id = :id")
    suspend fun getById(id: String): EnemyEntity?

    @Query("SELECT COUNT(*) FROM enemies")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(enemies: List<EnemyEntity>)

    @Query("DELETE FROM enemies")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(enemies: List<EnemyEntity>) {
        deleteAll()
        insertAll(enemies)
    }
}