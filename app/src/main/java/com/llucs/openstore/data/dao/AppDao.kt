package com.llucs.openstore.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.llucs.openstore.data.entity.AppEntity
import com.llucs.openstore.data.model.AppWithVersion
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("""
        SELECT 
            apps.packageName AS packageName,
            apps.name AS name,
            apps.summary AS summary,
            apps.icon AS icon,
            apps.repoId AS repoId,
            repos.baseUrl AS repoBaseUrl,
            versions.versionName AS versionName,
            versions.versionCode AS versionCode,
            versions.apkName AS apkName,
            versions.sha256 AS sha256,
            versions.sizeBytes AS sizeBytes,
            versions.minSdk AS minSdk,
            apps.addedEpochMs AS addedEpochMs,
            apps.lastUpdatedEpochMs AS lastUpdatedEpochMs
        FROM apps
        JOIN repos ON repos.id = apps.repoId
        LEFT JOIN versions ON apps.packageName = versions.packageName
        WHERE repos.enabled = 1
        AND (
            :query = '' OR
            LOWER(apps.name) LIKE '%' || LOWER(:query) || '%' OR
            LOWER(apps.summary) LIKE '%' || LOWER(:query) || '%' OR
            LOWER(apps.packageName) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY CASE WHEN TRIM(apps.name) = '' THEN apps.packageName ELSE apps.name END COLLATE NOCASE
    """)
    fun observeApps(query: String): Flow<List<AppWithVersion>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    fun observeApp(packageName: String): Flow<AppEntity?>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<AppEntity>)

    @Query("DELETE FROM apps WHERE repoId = :repoId")
    suspend fun deleteByRepo(repoId: Long)
}
