package com.java.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioDao {
    @Query("SELECT * FROM scenarios")
    fun observeAll(): Flow<List<ScenarioEntity>>

    @Query("SELECT * FROM scenarios")
    suspend fun getAll(): List<ScenarioEntity>

    @Query("SELECT * FROM scenarios WHERE id = :id")
    suspend fun getById(id: String): ScenarioEntity?

    @Query("SELECT COUNT(*) FROM scenarios")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scenarios: List<ScenarioEntity>)

    @Query("DELETE FROM scenarios")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(scenarios: List<ScenarioEntity>) {
        deleteAll()
        insertAll(scenarios)
    }
}