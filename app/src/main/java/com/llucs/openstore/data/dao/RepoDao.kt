package com.llucs.openstore.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.llucs.openstore.data.entity.RepoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoDao {

    @Query("SELECT * FROM repos ORDER BY name COLLATE NOCASE")
    fun observeRepos(): Flow<List<RepoEntity>>

    @Query("SELECT * FROM repos WHERE enabled = 1 ORDER BY name COLLATE NOCASE")
    suspend fun getEnabledRepos(): List<RepoEntity>

    @Query("SELECT * FROM repos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RepoEntity?

    @Query("SELECT * FROM repos WHERE baseUrl = :baseUrl LIMIT 1")
    suspend fun getByBaseUrl(baseUrl: String): RepoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(repo: RepoEntity): Long

    @Update
    suspend fun update(repo: RepoEntity)

    @Query("UPDATE repos SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM repos WHERE id = :id")
    suspend fun delete(id: Long)
}
