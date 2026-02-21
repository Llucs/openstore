package com.llucs.openstore.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.llucs.openstore.OpenStoreApp
import com.llucs.openstore.data.entity.AppEntity
import com.llucs.openstore.data.entity.VersionEntity
import com.llucs.openstore.fdroid.ApkDownloadManager
import com.llucs.openstore.fdroid.ApkDownloadState
import com.llucs.openstore.fdroid.FdroidConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailsUiState(
    val app: AppEntity? = null,
    val version: VersionEntity? = null,
    val repoBaseUrl: String = ""
)

class DetailsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as OpenStoreApp).db
    private val downloadManager = ApkDownloadManager(app)

    val downloadState = MutableStateFlow<ApkDownloadState?>(null)

    fun uiState(packageName: String): StateFlow<DetailsUiState> =
        combine(
            db.appDao().observeApp(packageName),
            db.versionDao().observeVersion(packageName)
        ) { appEntity, version ->
            val repo = appEntity?.repoId?.let { db.repoDao().getById(it) }
            DetailsUiState(
                app = appEntity,
                version = version,
                repoBaseUrl = repo?.baseUrl.orEmpty()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState())

    fun downloadApk(repoBaseUrl: String, version: VersionEntity) {
        if (downloadState.value is ApkDownloadState.Progress) return
        val url = FdroidConstants.apkUrl(repoBaseUrl, version.apkName)
        viewModelScope.launch {
            downloadManager.downloadApk(
                url = url,
                expectedSha256Hex = version.sha256,
                targetName = version.apkName
            ).collect { st ->
                downloadState.value = st
            }
        }
    }

    fun resetDownloadState() {
        downloadState.value = null
    }
}
