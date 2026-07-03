package com.mystarrail.tool.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface EidolonDao {
    @Query("SELECT * FROM eidolons")
    fun observeAll(): Flow<List<EidolonEntity>>

    @Query("SELECT * FROM eidolons")
    suspend fun getAll(): List<EidolonEntity>

    @Query("SELECT * FROM eidolons WHERE characterId = :cid ORDER BY level ASC")
    suspend fun getForCharacter(cid: String): List<EidolonEntity>

    @Query("SELECT COUNT(*) FROM eidolons")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(eidolons: List<EidolonEntity>)

    @Query("DELETE FROM eidolons")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(eidolons: List<EidolonEntity>) {
        deleteAll()
        insertAll(eidolons)
    }
}