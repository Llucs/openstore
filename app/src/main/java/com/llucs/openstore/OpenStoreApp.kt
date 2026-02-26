package com.llucs.openstore

import android.app.Application
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.llucs.openstore.data.AppDatabase
import com.llucs.openstore.data.RepoRepository
import com.llucs.openstore.sync.RepoSyncWorker
import java.util.concurrent.TimeUnit

class OpenStoreApp : Application() {

    lateinit var db: AppDatabase
        private set

    lateinit var repos: RepoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.create(this)
        repos = RepoRepository(this, db)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<RepoSyncWorker>(
            12, TimeUnit.HOURS,
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(RepoSyncWorker.TAG)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RepoSyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        val startupRequest = OneTimeWorkRequestBuilder<RepoSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(RepoSyncWorker.TAG_STARTUP)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            RepoSyncWorker.UNIQUE_STARTUP_NAME,
            ExistingWorkPolicy.KEEP,
            startupRequest
        )
    }
}
