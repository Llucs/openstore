package com.llucs.openstore.fdroid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IndexV1(
    val repo: RepoV1,
    val apps: List<AppV1> = emptyList(),
    val packages: Map<String, List<PackageVersionV1>> = emptyMap()
)

@Serializable
data class RepoV1(
    val name: String = "",
    val timestamp: Long = 0L,
    val address: String? = null
)

@Serializable
data class AppV1(
    val packageName: String,
    val name: String? = null,
    val summary: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val webSite: String? = null,
    val sourceCode: String? = null,
    val issueTracker: String? = null,
    val added: Long? = null,
    val lastUpdated: Long? = null
)

@Serializable
data class PackageVersionV1(
    val apkName: String,
    val hash: String,
    val hashType: String,
    val size: Long = 0L,
    val minSdkVersion: Int = 1,
    val versionCode: Long,
    val versionName: String,
    val added: Long? = null
)
