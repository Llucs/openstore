package com.llucs.openstore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.llucs.openstore.fdroid.ApkDownloadState
import com.llucs.openstore.fdroid.FdroidConstants
import com.llucs.openstore.install.Installer
import com.llucs.openstore.ui.viewmodel.DetailsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(packageName: String, navController: NavController) {
    val vm: DetailsViewModel = viewModel()
    val uiStateFlow = remember(vm, packageName) { vm.uiState(packageName) }
    val uiState by uiStateFlow.collectAsState()
    val downloadState by vm.downloadState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(packageName) {
        vm.resetDownloadState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    prettyDisplayName(uiState.app?.name, packageName),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Voltar")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors()
        )

        val app = uiState.app
        val version = uiState.version
        val repoBaseUrl = uiState.repoBaseUrl

        if (app == null || version == null || repoBaseUrl.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Carregando…", style = MaterialTheme.typography.bodyLarge)
            }
            return
        }

        val displayName = remember(app.name, app.packageName) { prettyDisplayName(app.name, app.packageName) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RepoIconLarge(
                        repoBaseUrl = repoBaseUrl,
                        iconPath = app.icon,
                        label = displayName,
                        modifier = Modifier.size(84.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (app.summary.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                app.summary,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Informações", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    InfoRow("Versão", "v${version.versionName} (${version.versionCode})")
                    InfoRow("minSdk", version.minSdk.toString())
                    InfoRow("Tamanho", formatSize(version.sizeBytes))
                    InfoRow("SHA-256", version.sha256)
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Instalação", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))

                    when (val st = downloadState) {
                        null -> {
                            Button(
                                onClick = { vm.downloadApk(repoBaseUrl, version) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.Outlined.Download, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text("Baixar e instalar")
                            }
                        }

                        is ApkDownloadState.Progress -> {
                            val total = st.totalBytes
                            val progress = if (total > 0) st.readBytes.toFloat() / total.toFloat() else null
                            Text("Baixando…", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            if (progress != null) {
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${formatSize(st.readBytes)} / ${if (total > 0) formatSize(total) else "?"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is ApkDownloadState.Done -> {
                            var autoOpened by rememberSaveable(st.file.absolutePath) { mutableStateOf(false) }
                            LaunchedEffect(st.file.absolutePath, autoOpened) {
                                if (!autoOpened) {
                                    autoOpened = true
                                    Installer.installApk(context, st.file)
                                }
                            }
                            OutlinedButton(
                                onClick = { Installer.installApk(context, st.file) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text("Abrir instalador novamente")
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { vm.resetDownloadState() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text("Pronto")
                            }
                        }

                        is ApkDownloadState.Error -> {
                            Text(
                                "Erro: ${st.message}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { vm.resetDownloadState() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text("Tentar de novo")
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = app.webSite.isNotBlank() || app.sourceCode.isNotBlank() || app.issueTracker.isNotBlank()) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Links", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(10.dp))
                        if (app.webSite.isNotBlank()) LinkRow("Website", app.webSite)
                        if (app.sourceCode.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            LinkRow("Código-fonte", app.sourceCode)
                        }
                        if (app.issueTracker.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            LinkRow("Issues", app.issueTracker)
                        }
                    }
                }
            }

            if (app.description.isNotBlank()) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Descrição", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(10.dp))
                        Text(app.description, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun RepoIconLarge(
    repoBaseUrl: String,
    iconPath: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val candidates = remember(repoBaseUrl, iconPath) { FdroidConstants.iconUrlCandidates(repoBaseUrl, iconPath) }
    var candidateIndex by remember(candidates) { mutableStateOf(0) }
    val currentModel = candidates.getOrNull(candidateIndex)

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        FallbackIconText(label)

        if (currentModel != null) {
            SubcomposeAsyncImage(
                model = currentModel,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                success = { SubcomposeAsyncImageContent() },
                error = {
                    if (candidateIndex < candidates.lastIndex) {
                        LaunchedEffect(candidateIndex) {
                            candidateIndex += 1
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FallbackIconText(label: String) {
    val initial = remember(label) {
        label.trim().takeIf { it.isNotBlank() }
            ?.split(' ', '-', '_', '.')
            ?.firstOrNull { it.isNotBlank() }
            ?.take(1)
            ?.uppercase(Locale.ROOT)
            ?: "A"
    }
    Text(
        text = initial,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(url, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.OpenInNew, contentDescription = null)
    }
}

private fun prettyDisplayName(rawName: String?, packageName: String): String {
    val name = rawName.orEmpty().trim()
    if (name.isNotBlank() && !name.equals(packageName, ignoreCase = true)) return name

    val tail = packageName.substringAfterLast('.').replace('_', ' ').replace('-', ' ').trim()
    if (tail.isBlank()) return packageName

    return tail.split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            val lower = part.lowercase(Locale.ROOT)
            if (lower.length == 1) lower.uppercase(Locale.ROOT)
            else lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.2f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}
