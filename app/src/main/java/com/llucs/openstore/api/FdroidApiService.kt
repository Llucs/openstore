package com.llucs.openstore.api

import com.llucs.openstore.fdroid.FdroidConstants
import com.llucs.openstore.fdroid.FingerprintResult
import com.llucs.openstore.fdroid.HttpDownloader
import com.llucs.openstore.fdroid.IndexV1
import com.llucs.openstore.fdroid.JarSignatureVerifier
import kotlinx.serialization.json.Json
import java.io.File
import java.util.jar.JarFile

class FdroidApiService(
    private val cacheDir: File
) {
    private val downloader = HttpDownloader()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    data class IndexDownloadResult(
        val changed: Boolean,
        val etag: String,
        val lastModified: String,
        val fingerprint: String?,
        val index: IndexV1?
    )

    data class ProbeResult(
        val fingerprintSha256: String,
        val repoName: String
    )

    fun downloadIndex(
        baseUrl: String,
        etag: String = "",
        lastModified: String = "",
        expectedFingerprint: String = ""
    ): IndexDownloadResult {
        val tmpJar = File.createTempFile("fdroid_index_", ".jar", cacheDir)
        try {
            val url = FdroidConstants.indexV1JarUrl(baseUrl)
            val dl = downloader.downloadToFile(url, tmpJar, etag, lastModified)
            if (!dl.changed) {
                return IndexDownloadResult(changed = false, etag = etag, lastModified = lastModified, fingerprint = null, index = null)
            }

            val fingerprint = JarSignatureVerifier.fingerprintSha256ForEntry(
                tmpJar, FdroidConstants.indexV1JsonName()
            )

            if (expectedFingerprint.isNotBlank() &&
                !expectedFingerprint.equals(fingerprint, ignoreCase = true)
            ) {
                throw IllegalStateException(
                    "Fingerprint mismatch. Expected: $expectedFingerprint, Got: $fingerprint"
                )
            }

            val index = extractAndParseIndexV1(tmpJar)
            return IndexDownloadResult(
                changed = true,
                etag = dl.etag,
                lastModified = dl.lastModified,
                fingerprint = fingerprint,
                index = index
            )
        } finally {
            tmpJar.delete()
        }
    }

    fun probeRepo(baseUrl: String): ProbeResult {
        val normalized = FdroidConstants.normalizeBaseUrl(baseUrl)
        val tmpJar = File.createTempFile("fdroid_probe_", ".jar", cacheDir)
        try {
            val url = FdroidConstants.indexV1JarUrl(normalized)
            downloader.downloadToFile(url, tmpJar, "", "")
            val fingerprint = JarSignatureVerifier.fingerprintSha256ForEntry(
                tmpJar, FdroidConstants.indexV1JsonName()
            )
            val index = extractAndParseIndexV1(tmpJar)
            val name = index.repo.name.ifBlank { "Repository" }
            return ProbeResult(fingerprintSha256 = fingerprint, repoName = name)
        } finally {
            tmpJar.delete()
        }
    }

    fun resolveIconUrl(baseUrl: String, iconPath: String): List<String> =
        FdroidConstants.iconUrlCandidates(baseUrl, iconPath)

    fun buildApkUrl(baseUrl: String, apkName: String): String =
        FdroidConstants.apkUrl(baseUrl, apkName)

    private fun extractAndParseIndexV1(jar: File): IndexV1 {
        JarFile(jar, true).use { jf ->
            val entry = jf.getJarEntry(FdroidConstants.indexV1JsonName())
                ?: throw IllegalStateException("index-v1.json not found in JAR.")
            jf.getInputStream(entry).use { input ->
                val text = input.readAllBytes().decodeToString()
                return json.decodeFromString(IndexV1.serializer(), text)
            }
        }
    }
}
