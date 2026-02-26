package com.llucs.openstore.fdroid

object FdroidConstants {
    const val FDROID_REPO_BASE_URL = "https://f-droid.org/repo/"
    const val FDROID_FINGERPRINT_SHA256 = "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB"

    fun normalizeBaseUrl(url: String): String {
        var u = url.trim()
        if (!u.endsWith("/")) u += "/"
        return u
    }

    fun indexV1JarUrl(baseUrl: String): String = normalizeBaseUrl(baseUrl) + "index-v1.jar"
    fun indexV1JsonName(): String = "index-v1.json"
    fun apkUrl(baseUrl: String, apkName: String): String = normalizeBaseUrl(baseUrl) + apkName

    fun iconUrl(baseUrl: String, icon: String): String = iconUrlCandidates(baseUrl, icon).firstOrNull().orEmpty()

    fun iconUrlCandidates(baseUrl: String, icon: String): List<String> {
        val raw = icon.trim()
        if (raw.isBlank()) return emptyList()
        if (raw.startsWith("https://", ignoreCase = true) || raw.startsWith("http://", ignoreCase = true)) {
            return listOf(raw)
        }

        val normalizedBase = normalizeBaseUrl(baseUrl)
        val cleaned = raw.removePrefix("./").trimStart('/')

        val candidates = linkedSetOf<String>()

        if (cleaned.contains('/')) {
            candidates += normalizedBase + cleaned
            if (cleaned.startsWith("repo/")) {
                candidates += normalizedBase + cleaned.removePrefix("repo/")
            } else {
                candidates += normalizedBase + "repo/" + cleaned
            }
            return candidates.toList()
        }

        val iconDirs = listOf(
            "icons-640/",
            "icons-480/",
            "icons-320/",
            "icons-240/",
            "icons-160/",
            "icons-128/",
            "icons-96/",
            "icons-72/",
            "icons-48/",
            "icons/"
        )

        iconDirs.forEach { dir -> candidates += normalizedBase + dir + cleaned }
        candidates += normalizedBase + cleaned
        candidates += normalizedBase + "repo/" + cleaned

        return candidates.toList()
    }
}
