package com.llucs.openstore.data.model

data class AppWithVersion(
    val packageName: String,
    val name: String,
    val summary: String,
    val icon: String,
    val repoId: Long,
    val repoBaseUrl: String,
    val versionName: String?,
    val versionCode: Long?,
    val apkName: String?,
    val sha256: String?,
    val sizeBytes: Long?,
    val minSdk: Int?
)
