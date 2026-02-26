package com.llucs.openstore.fdroid

import com.llucs.openstore.data.entity.AppEntity
import com.llucs.openstore.data.entity.VersionEntity
import java.util.Locale

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
            val localized = app?.localized.orEmpty()

            val localizedPick = pickLocalizedEntry(localized)
            val localizedEntry = localizedPick?.second
            val localizedKey = localizedPick?.first.orEmpty()

            val name = (app?.name ?: localizedEntry?.name ?: pkg).orEmpty().trim().ifBlank { pkg }
            val summary = (app?.summary ?: localizedEntry?.summary ?: "").orEmpty().trim()
            val description = (app?.description ?: localizedEntry?.description ?: "").orEmpty().trim()

            val rawIcon = (app?.icon ?: localizedEntry?.icon ?: "").orEmpty().trim()
            val icon = resolveIconPath(
                packageName = pkg,
                localizedKey = localizedKey,
                localizedIcon = localizedEntry?.icon,
                fallbackIcon = app?.icon,
                chosenRawIcon = rawIcon
            )

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
                sha256 = latest.hash,
                sizeBytes = latest.size,
                minSdk = latest.minSdkVersion,
                addedEpochMs = latest.added ?: 0L
            )
        }

        return Mapped(appsOut, versionsOut)
    }

    private fun resolveIconPath(
        packageName: String,
        localizedKey: String,
        localizedIcon: String?,
        fallbackIcon: String?,
        chosenRawIcon: String
    ): String {
        val chosen = chosenRawIcon.trim()
        if (chosen.isBlank()) return ""

        val appIcon = fallbackIcon.orEmpty().trim()
        if (appIcon.isNotBlank() && chosen == appIcon) {
            return appIcon
        }

        val locIcon = localizedIcon.orEmpty().trim()
        if (locIcon.isNotBlank() && chosen == locIcon) {
            if (locIcon.startsWith("http://", ignoreCase = true) || locIcon.startsWith("https://", ignoreCase = true)) {
                return locIcon
            }
            if (locIcon.contains('/')) return locIcon
            val safeLocale = localizedKey.trim().replace('\\', '/').trim('/').ifBlank { defaultLocaleKey() }
            return "$packageName/$safeLocale/$locIcon"
        }

        return chosen
    }

    private fun defaultLocaleKey(): String {
        val locale = Locale.getDefault()
        val lang = locale.language.orEmpty()
        val country = locale.country.orEmpty()
        return when {
            lang.isBlank() -> "en-US"
            country.isBlank() -> lang
            else -> "$lang-$country"
        }
    }

    private fun pickLocalizedEntry(localized: Map<String, AppLocalizedV1>): Pair<String, AppLocalizedV1>? {
        if (localized.isEmpty()) return null

        val default = Locale.getDefault()
        val dynamicKeys = buildList {
            val language = default.language.orEmpty()
            val country = default.country.orEmpty()
            if (language.isNotBlank() && country.isNotBlank()) {
                add("$language-$country")
                add("${language}_${country}")
            }
            if (language.isNotBlank()) add(language)
        }

        val preferredKeys = dynamicKeys + listOf("pt-BR", "pt_BR", "pt", "en-US", "en_US", "en")
        preferredKeys.forEach { key ->
            val entry = localized[key] ?: return@forEach
            if (entryHasUsefulContent(entry)) return key to entry
        }

        localized.entries.forEach { (key, entry) ->
            if (entryHasUsefulContent(entry)) return key to entry
        }

        return localized.entries.firstOrNull()?.toPair()
    }

    private fun entryHasUsefulContent(entry: AppLocalizedV1): Boolean {
        return !entry.name.isNullOrBlank()
            || !entry.summary.isNullOrBlank()
            || !entry.description.isNullOrBlank()
            || !entry.icon.isNullOrBlank()
    }
}
