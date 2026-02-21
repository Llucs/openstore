package com.llucs.openstore

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
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

        val request = PeriodicWorkRequestBuilder<RepoSyncWorker>(12, TimeUnit.HOURS)
            .addTag(RepoSyncWorker.TAG)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RepoSyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
