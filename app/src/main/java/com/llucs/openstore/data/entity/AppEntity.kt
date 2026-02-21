package com.llucs.openstore.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "apps",
    indices = [
        Index(value = ["repoId"]),
        Index(value = ["name"]),
        Index(value = ["summary"])
    ]
)
data class AppEntity(
    @PrimaryKey val packageName: String,
    val repoId: Long,
    val name: String,
    val summary: String,
    val description: String,
    val icon: String,
    val webSite: String,
    val sourceCode: String,
    val issueTracker: String,
    val addedEpochMs: Long,
    val lastUpdatedEpochMs: Long
)
