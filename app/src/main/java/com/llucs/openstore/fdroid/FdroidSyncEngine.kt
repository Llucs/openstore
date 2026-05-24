package com.llucs.openstore.fdroid

import android.content.Context
import androidx.room.withTransaction
import com.llucs.openstore.api.FdroidApiService
import com.llucs.openstore.data.AppDatabase
import com.llucs.openstore.data.entity.RepoEntity
import kotlinx.coroutines.yield

class FdroidSyncEngine(
    context: Context,
    private val db: AppDatabase,
    private val api: FdroidApiService = FdroidApiService(context.cacheDir)
) {
    data class SyncResult(
        val changed: Boolean,
        val appsUpserted: Int
    )

    suspend fun syncRepo(repo: RepoEntity, force: Boolean): SyncResult {
        val dl = api.downloadIndex(
            baseUrl = repo.baseUrl,
            etag = if (force) "" else repo.etag,
            lastModified = if (force) "" else repo.lastModified,
            expectedFingerprint = repo.fingerprintSha256
        )

        if (!dl.changed || dl.index == null) {
            return SyncResult(changed = false, appsUpserted = 0)
        }

        val mapped = IndexV1Mapper.map(repo.id, dl.index)

        db.versionDao().deleteByRepo(repo.id)
        db.appDao().deleteByRepo(repo.id)

        val total = mapped.apps.size
        val chunkSize = 200
        var inserted = 0
        while (inserted < total) {
            val end = minOf(inserted + chunkSize, total)
            db.withTransaction {
                db.appDao().upsertAll(mapped.apps.subList(inserted, end))
                db.versionDao().upsertAll(mapped.versions.subList(inserted, end))
            }
            inserted = end
            yield()
        }

        db.repoDao().update(
            repo.copy(
                etag = dl.etag,
                lastModified = dl.lastModified,
                lastSyncEpochMs = System.currentTimeMillis()
            )
        )

        return SyncResult(changed = true, appsUpserted = total)
    }

    fun probeFingerprint(baseUrl: String): FingerprintResult {
        val normalized = FdroidConstants.normalizeBaseUrl(baseUrl)
        val result = api.probeRepo(normalized)
        return FingerprintResult(
            baseUrl = normalized,
            fingerprintSha256 = result.fingerprintSha256,
            repoNameGuess = result.repoName
        )
    }
}
