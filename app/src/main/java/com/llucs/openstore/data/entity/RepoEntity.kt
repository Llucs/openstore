package com.llucs.openstore.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "repos",
    indices = [Index(value = ["baseUrl"], unique = true)]
)
data class RepoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val fingerprintSha256: String,
    val trusted: Boolean,
    val enabled: Boolean,
    val etag: String,
    val lastModified: String,
    val lastSyncEpochMs: Long
)
