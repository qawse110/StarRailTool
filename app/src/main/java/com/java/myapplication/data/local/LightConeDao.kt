package com.java.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LightConeDao {
    @Query("SELECT * FROM light_cones")
    fun observeAll(): Flow<List<LightConeEntity>>

    @Query("SELECT * FROM light_cones")
    suspend fun getAll(): List<LightConeEntity>

    @Query("SELECT * FROM light_cones WHERE id = :id")
    suspend fun getById(id: String): LightConeEntity?

    @Query("SELECT COUNT(*) FROM light_cones")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cones: List<LightConeEntity>)

    @Query("DELETE FROM light_cones")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(cones: List<LightConeEntity>) {
        deleteAll()
        insertAll(cones)
    }
}