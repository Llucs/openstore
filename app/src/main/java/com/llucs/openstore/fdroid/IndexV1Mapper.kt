package com.llucs.openstore.fdroid

import com.llucs.openstore.data.entity.AppEntity
import com.llucs.openstore.data.entity.VersionEntity

object IndexV1Mapper {

    data class Mapped(
        val apps: List<AppEntity>,
        val versions: List<VersionEntity>
    )

    fun map(repoId: Long, index: IndexV1): Mapped {
        val appByPkg = index.apps.associateBy { it.packageName }
        val appsOut = ArrayList<AppEntity>(index.packages.size)
        val versionsOut = ArrayList<VersionEntity>(index.packages.size)

        index.packages.forEach { (pkg, versions) ->
            val latest = versions.maxByOrNull { it.versionCode } ?: return@forEach
            val app = appByPkg[pkg]
            val name = (app?.name ?: pkg).trim()
            val summary = (app?.summary ?: "").trim()
            val description = (app?.description ?: "").trim()
            val icon = (app?.icon ?: "").trim()
            val webSite = (app?.webSite ?: "").trim()
            val source = (app?.sourceCode ?: "").trim()
            val issues = (app?.issueTracker ?: "").trim()
            val added = app?.added ?: latest.added ?: 0L
            val updated = app?.lastUpdated ?: latest.added ?: 0L

            appsOut += AppEntity(
                packageName = pkg,
                repoId = repoId,
                name = name,
                summary = summary,
                description = description,
                icon = icon,
                webSite = webSite,
                sourceCode = source,
                issueTracker = issues,
                addedEpochMs = added,
                lastUpdatedEpochMs = updated
            )
            versionsOut += VersionEntity(
                packageName = pkg,
                repoId = repoId,
                versionCode = latest.versionCode,
                versionName = latest.versionName,
                apkName = latest.apkName,
                sha256 = if (latest.hashType.equals("sha256", ignoreCase = true)) latest.hash else latest.hash,
                sizeBytes = latest.size,
                minSdk = latest.minSdkVersion,
                addedEpochMs = latest.added ?: 0L
            )
        }

        return Mapped(appsOut, versionsOut)
    }
}
