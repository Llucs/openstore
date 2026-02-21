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

    fun iconUrl(baseUrl: String, icon: String): String {
        val b = normalizeBaseUrl(baseUrl)
        return if (icon.contains("/")) b + icon.trimStart('/') else b + "icons/" + icon
    }
}
