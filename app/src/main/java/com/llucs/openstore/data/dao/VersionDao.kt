package com.llucs.openstore.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.llucs.openstore.data.entity.VersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VersionDao {

    @Query("SELECT * FROM versions WHERE packageName = :packageName LIMIT 1")
    fun observeVersion(packageName: String): Flow<VersionEntity?>

    @Query("SELECT * FROM versions WHERE packageName = :packageName LIMIT 1")
    suspend fun getVersion(packageName: String): VersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(versions: List<VersionEntity>)

    @Query("DELETE FROM versions WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: Long)
}
