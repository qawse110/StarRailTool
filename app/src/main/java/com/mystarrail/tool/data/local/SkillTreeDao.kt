package com.mystarrail.tool.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillTreeDao {
    @Query("SELECT * FROM skill_tree_nodes WHERE characterId = :cid ORDER BY position ASC")
    suspend fun getForCharacter(cid: String): List<SkillTreeNodeEntity>

    @Query("SELECT * FROM skill_tree_nodes")
    fun observeAll(): Flow<List<SkillTreeNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<SkillTreeNodeEntity>)

    @Query("DELETE FROM skill_tree_nodes")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(nodes: List<SkillTreeNodeEntity>) {
        deleteAll()
        insertAll(nodes)
    }
}