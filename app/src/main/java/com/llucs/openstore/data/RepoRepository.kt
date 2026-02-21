package com.llucs.openstore.data

import android.content.Context
import com.llucs.openstore.data.entity.RepoEntity
import com.llucs.openstore.fdroid.FdroidConstants
import com.llucs.openstore.fdroid.FdroidSyncEngine
import com.llucs.openstore.fdroid.FingerprintResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RepoRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val repoDao = db.repoDao()
    private val appDao = db.appDao()
    private val versionDao = db.versionDao()
    private val engine = FdroidSyncEngine(context, db)

    suspend fun ensureDefaults() {
        withContext(Dispatchers.IO) {
            val existing = repoDao.getByBaseUrl(FdroidConstants.FDROID_REPO_BASE_URL)
            if (existing == null) {
                repoDao.upsert(
                    RepoEntity(
                        name = "F-Droid",
                        baseUrl = FdroidConstants.FDROID_REPO_BASE_URL,
                        fingerprintSha256 = FdroidConstants.FDROID_FINGERPRINT_SHA256,
                        trusted = true,
                        enabled = true,
                        etag = "",
                        lastModified = "",
                        lastSyncEpochMs = 0L
                    )
                )
            }
        }
    }

    fun observeRepos(): Flow<List<RepoEntity>> = repoDao.observeRepos()

    suspend fun setRepoEnabled(id: Long, enabled: Boolean) {
        withContext(Dispatchers.IO) { repoDao.setEnabled(id, enabled) }
    }

    suspend fun deleteRepo(id: Long) {
        withContext(Dispatchers.IO) {
            repoDao.delete(id)
            appDao.deleteByRepo(id)
            versionDao.deleteByRepo(id)
        }
    }

    suspend fun syncEnabledRepos(force: Boolean = false): SyncSummary =
        withContext(Dispatchers.IO) {
            ensureDefaults()
            val repos = repoDao.getEnabledRepos()
            var apps = 0
            var updatedRepos = 0
            val errors = mutableListOf<String>()
            repos.forEach { repo ->
                try {
                    val result = engine.syncRepo(repo, force)
                    if (result.changed) updatedRepos += 1
                    apps += result.appsUpserted
                } catch (e: Exception) {
                    errors += "${repo.name}: ${e.message ?: e.javaClass.simpleName}"
                }
            }
            SyncSummary(updatedRepos, apps, errors)
        }

    suspend fun probeRepoFingerprint(baseUrl: String): FingerprintResult =
        withContext(Dispatchers.IO) {
            engine.probeFingerprint(baseUrl)
        }

    suspend fun addRepo(baseUrl: String, name: String, fingerprintSha256: String, trusted: Boolean) {
        withContext(Dispatchers.IO) {
            val normalized = FdroidConstants.normalizeBaseUrl(baseUrl)
            repoDao.upsert(
                RepoEntity(
                    name = name,
                    baseUrl = normalized,
                    fingerprintSha256 = fingerprintSha256,
                    trusted = trusted,
                    enabled = true,
                    etag = "",
                    lastModified = "",
                    lastSyncEpochMs = 0L
                )
            )
        }
    }

    data class SyncSummary(
        val reposUpdated: Int,
        val appsProcessed: Int,
        val errors: List<String>
    )
}
