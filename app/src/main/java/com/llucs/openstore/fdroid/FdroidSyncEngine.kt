package com.llucs.openstore.fdroid

import android.content.Context
import androidx.room.withTransaction
import com.llucs.openstore.data.AppDatabase
import com.llucs.openstore.data.entity.RepoEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.jar.JarFile

class FdroidSyncEngine(
    private val context: Context,
    private val db: AppDatabase
) {
    private val downloader = HttpDownloader()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    data class SyncResult(
        val changed: Boolean,
        val appsUpserted: Int
    )

    suspend fun syncRepo(repo: RepoEntity, force: Boolean): SyncResult {
        val tmpJar = File.createTempFile("openstore_repo_", ".jar", context.cacheDir)
        try {
            val url = FdroidConstants.indexV1JarUrl(repo.baseUrl)
            val dl = downloader.downloadToFile(
                url = url,
                target = tmpJar,
                etag = if (force) "" else repo.etag,
                lastModified = if (force) "" else repo.lastModified
            )
            if (!dl.changed) {
                return SyncResult(changed = false, appsUpserted = 0)
            }

            val fingerprint = try {
                JarSignatureVerifier.fingerprintSha256ForEntry(tmpJar, FdroidConstants.indexV1JsonName())
            } catch (e: Exception) {
                throw IllegalStateException("Falha ao validar assinatura do repositório: ${e.message}")
            }

            if (repo.trusted && repo.fingerprintSha256.isNotBlank()
                && !repo.fingerprintSha256.equals(fingerprint, ignoreCase = true)
            ) {
                throw IllegalStateException("Fingerprint do repo não confere. Esperado: ${repo.fingerprintSha256} / Recebido: $fingerprint")
            }
            if (!repo.trusted) {
                throw IllegalStateException("Repo não confiável. Fingerprint detectado: $fingerprint")
            }

            val index = extractAndParseIndexV1(tmpJar)
            val mapped = IndexV1Mapper.map(repo.id, index)

            db.withTransaction {
                db.appDao().deleteByRepo(repo.id)
                db.versionDao().deleteByRepo(repo.id)
                db.appDao().upsertAll(mapped.apps)
                db.versionDao().upsertAll(mapped.versions)
                db.repoDao().update(
                    repo.copy(
                        etag = dl.etag,
                        lastModified = dl.lastModified,
                        lastSyncEpochMs = System.currentTimeMillis()
                    )
                )
            }

            return SyncResult(changed = true, appsUpserted = mapped.apps.size)
        } finally {
            tmpJar.delete()
        }
    }

    fun probeFingerprint(baseUrl: String): FingerprintResult {
        val normalized = FdroidConstants.normalizeBaseUrl(baseUrl)
        val tmpJar = File.createTempFile("openstore_probe_", ".jar", context.cacheDir)
        try {
            val url = FdroidConstants.indexV1JarUrl(normalized)
            downloader.downloadToFile(url, tmpJar, "", "")
            val fingerprint = JarSignatureVerifier.fingerprintSha256ForEntry(tmpJar, FdroidConstants.indexV1JsonName())
            val index = extractAndParseIndexV1(tmpJar)
            val name = index.repo.name.ifBlank { "Repositório" }
            return FingerprintResult(
                baseUrl = normalized,
                fingerprintSha256 = fingerprint,
                repoNameGuess = name
            )
        } finally {
            tmpJar.delete()
        }
    }

    private fun extractAndParseIndexV1(jar: File): IndexV1 {
        JarFile(jar, true).use { jf ->
            val entry = jf.getJarEntry(FdroidConstants.indexV1JsonName())
                ?: throw IllegalStateException("index-v1.json não encontrado no JAR.")
            jf.getInputStream(entry).use { input ->
                return json.decodeFromStream(IndexV1.serializer(), input)
            }
        }
    }
}
