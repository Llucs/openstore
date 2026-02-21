package com.llucs.openstore.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "versions",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["repoId"]), Index(value = ["packageName"])]
)
data class VersionEntity(
    @PrimaryKey val packageName: String,
    val repoId: Long,
    val versionCode: Long,
    val versionName: String,
    val apkName: String,
    val sha256: String,
    val sizeBytes: Long,
    val minSdk: Int,
    val addedEpochMs: Long
)
