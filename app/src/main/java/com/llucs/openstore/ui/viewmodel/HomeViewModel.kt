package com.llucs.openstore.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.llucs.openstore.OpenStoreApp
import com.llucs.openstore.data.model.AppWithVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as OpenStoreApp).db
    private val repos = (app as OpenStoreApp).repos

    val query = MutableStateFlow("")

    val apps: StateFlow<List<AppWithVersion>> =
        query.flatMapLatest { q -> db.appDao().observeApps(query = q.trim()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repos.ensureDefaults() }
    }
}