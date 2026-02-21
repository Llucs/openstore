package com.llucs.openstore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.llucs.openstore.data.entity.RepoEntity
import com.llucs.openstore.ui.viewmodel.ProbeState
import com.llucs.openstore.ui.viewmodel.ReposViewModel
import com.llucs.openstore.ui.viewmodel.SyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoriesScreen() {
    val vm: ReposViewModel = viewModel()
    val repos by vm.repos.collectAsState()
    val probeState by vm.probeState.collectAsState()
    val syncState by vm.syncState.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var urlText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Repositórios") },
            actions = {
                IconButton(onClick = { vm.syncNow(force = false) }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Sincronizar")
                }
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Adicionar repo")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors()
        )

        if (syncState is SyncState.Running) {
            Text(
                "Sincronizando…",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else if (syncState is SyncState.Done) {
            val done = syncState as SyncState.Done
            val msg = if (done.errors.isEmpty()) {
                "OK • repos atualizados: ${done.reposUpdated} • apps: ${done.appsProcessed}"
            } else {
                "Com erros • repos: ${done.reposUpdated} • apps: ${done.appsProcessed}"
            }
            Text(
                msg,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = if (done.errors.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            items(repos, key = { it.id }) { repo ->
                RepoCard(repo = repo, onToggle = { enabled -> vm.toggleRepo(repo.id, enabled) })
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Adicionar repositório") },
            text = {
                Column {
                    Text(
                        "Cole a URL do repo (precisa terminar com /repo/ ou /archive/).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        vm.probeRepo(urlText)
                        showAdd = false
                    },
                    enabled = urlText.trim().isNotBlank()
                ) {
                    Text("Verificar")
                }
            },
            dismissButton = {
                Button(onClick = { showAdd = false }) { Text("Cancelar") }
            }
        )
    }

    when (probeState) {
        is ProbeState.Loading -> {
            AlertDialog(
                onDismissRequest = { vm.dismissProbe() },
                title = { Text("Verificando…") },
                text = { Text("Baixando index e validando assinatura.") },
                confirmButton = { }
            )
        }
        is ProbeState.Error -> {
            val msg = (probeState as ProbeState.Error).message
            AlertDialog(
                onDismissRequest = { vm.dismissProbe() },
                title = { Text("Falhou") },
                text = { Text(msg) },
                confirmButton = {
                    Button(onClick = { vm.dismissProbe() }) { Text("OK") }
                }
            )
        }
        is ProbeState.Ready -> {
            val res = (probeState as ProbeState.Ready).result
            AlertDialog(
                onDismissRequest = { vm.dismissProbe() },
                title = { Text("Confiar neste repo?") },
                text = {
                    Column {
                        Text("Nome: ${res.repoNameGuess}")
                        Spacer(Modifier.height(6.dp))
                        Text("URL: ${res.baseUrl}")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Fingerprint SHA-256 (assinatura):",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(res.fingerprintSha256, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    Button(onClick = { vm.acceptRepo(res) }) { Text("Confiar e adicionar") }
                },
                dismissButton = {
                    FilledTonalButton(onClick = { vm.dismissProbe() }) { Text("Cancelar") }
                }
            )
        }
        else -> Unit
    }
}

@Composable
private fun RepoCard(repo: RepoEntity, onToggle: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(repo.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(repo.baseUrl, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!repo.trusted) {
                    Spacer(Modifier.height(6.dp))
                    Text("Não confiável", color = MaterialTheme.colorScheme.error)
                }
            }
            Switch(checked = repo.enabled, onCheckedChange = onToggle)
        }
    }
}
