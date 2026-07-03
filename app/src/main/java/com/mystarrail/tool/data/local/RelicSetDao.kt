package com.mystarrail.tool.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RelicSetDao {
    @Query("SELECT * FROM relic_sets")
    fun observeAll(): Flow<List<RelicSetEntity>>

    @Query("SELECT * FROM relic_sets")
    suspend fun getAll(): List<RelicSetEntity>

    @Query("SELECT * FROM relic_sets WHERE id = :id")
    suspend fun getById(id: String): RelicSetEntity?

    @Query("SELECT COUNT(*) FROM relic_sets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<RelicSetEntity>)

    @Query("DELETE FROM relic_sets")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(sets: List<RelicSetEntity>) {
        deleteAll()
        insertAll(sets)
    }
}