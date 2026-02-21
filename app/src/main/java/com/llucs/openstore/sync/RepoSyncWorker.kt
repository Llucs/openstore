package com.llucs.openstore.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.llucs.openstore.OpenStoreApp

class RepoSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as OpenStoreApp
        return try {
            app.repos.syncEnabledRepos(force = false)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "openstore_repo_sync"
        const val TAG = "openstore_repo_sync_tag"
    }
}
