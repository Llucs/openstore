package com.llucs.openstore.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.llucs.openstore.OpenStoreApp
import com.llucs.openstore.R

class RepoSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ensureChannel(applicationContext)
        setForeground(createForegroundInfo("Sincronizando repositórios…"))

        val app = applicationContext as OpenStoreApp
        return try {
            val summary = app.repos.syncEnabledRepos(force = false)
            if (summary.errors.isNotEmpty()) {
                notifyResult(
                    title = "OpenStore: sincronização concluída com erros",
                    text = "Repos: ${summary.reposUpdated} • Apps: ${summary.appsProcessed}"
                )
            } else {
                notifyResult(
                    title = "OpenStore: sincronização concluída",
                    text = "Repos: ${summary.reposUpdated} • Apps: ${summary.appsProcessed}"
                )
            }
            Result.success()
        } catch (e: Exception) {
            notifyResult(
                title = "OpenStore: falha na sincronização",
                text = e.message ?: e.javaClass.simpleName
            )
            Result.retry()
        }
    }

    private fun createForegroundInfo(message: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("OpenStore")
            .setContentText(message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(
            FOREGROUND_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun notifyResult(title: String, text: String) {
        runCatching {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            nm.notify(RESULT_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val UNIQUE_NAME = "openstore_repo_sync"
        const val UNIQUE_STARTUP_NAME = "openstore_repo_sync_startup"
        const val TAG = "openstore_repo_sync_tag"
        const val TAG_STARTUP = "openstore_repo_sync_startup_tag"

        private const val CHANNEL_ID = "openstore_sync"
        private const val FOREGROUND_NOTIFICATION_ID = 13001
        private const val RESULT_NOTIFICATION_ID = 13002

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sincronização do OpenStore",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mostra o andamento da sincronização de repositórios"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
