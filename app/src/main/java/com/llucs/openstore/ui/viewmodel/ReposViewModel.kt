package com.llucs.openstore.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.llucs.openstore.OpenStoreApp
import com.llucs.openstore.data.entity.RepoEntity
import com.llucs.openstore.fdroid.FingerprintResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ProbeState {
    data object Idle : ProbeState()
    data object Loading : ProbeState()
    data class Ready(val result: FingerprintResult) : ProbeState()
    data class Error(val message: String) : ProbeState()
}

class ReposViewModel(app: Application) : AndroidViewModel(app) {

    private val reposRepo = (app as OpenStoreApp).repos

    val repos: StateFlow<List<RepoEntity>> =
        reposRepo.observeRepos()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val probeState = MutableStateFlow<ProbeState>(ProbeState.Idle)
    val syncState = MutableStateFlow<SyncState>(SyncState.Idle)

    init {
        viewModelScope.launch { reposRepo.ensureDefaults() }
    }

    fun toggleRepo(id: Long, enabled: Boolean) {
        viewModelScope.launch { reposRepo.setRepoEnabled(id, enabled) }
    }

    fun syncNow(force: Boolean = false) {
        viewModelScope.launch {
            syncState.value = SyncState.Running
            val result = reposRepo.syncEnabledRepos(force)
            syncState.value = SyncState.Done(result.reposUpdated, result.appsProcessed, result.errors)
        }
    }

    fun probeRepo(url: String) {
        viewModelScope.launch {
            probeState.value = ProbeState.Loading
            try {
                val res = reposRepo.probeRepoFingerprint(url)
                probeState.value = ProbeState.Ready(res)
            } catch (e: Exception) {
                probeState.value = ProbeState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun acceptRepo(result: FingerprintResult) {
        viewModelScope.launch {
            reposRepo.addRepo(
                baseUrl = result.baseUrl,
                name = result.repoNameGuess,
                fingerprintSha256 = result.fingerprintSha256,
                trusted = true
            )
            probeState.value = ProbeState.Idle
            syncNow(force = true)
        }
    }

    fun dismissProbe() {
        probeState.value = ProbeState.Idle
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Running : SyncState()
    data class Done(val reposUpdated: Int, val appsProcessed: Int, val errors: List<String>) : SyncState()
}
