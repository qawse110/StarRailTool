package com.mystarrail.tool.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerBuildDao {
    @Query("SELECT * FROM player_builds")
    fun observeAll(): Flow<List<PlayerBuildEntity>>

    @Query("SELECT * FROM player_builds WHERE characterId = :cid")
    fun observeForCharacter(cid: String): Flow<List<PlayerBuildEntity>>

    @Query("SELECT * FROM player_builds WHERE id = :id")
    suspend fun getById(id: Long): PlayerBuildEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(build: PlayerBuildEntity): Long

    @Update
    suspend fun update(build: PlayerBuildEntity)

    @Delete
    suspend fun delete(build: PlayerBuildEntity)
}