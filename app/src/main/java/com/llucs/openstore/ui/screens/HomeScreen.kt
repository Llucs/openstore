package com.llucs.openstore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.llucs.openstore.data.model.AppWithVersion
import com.llucs.openstore.fdroid.FdroidConstants
import com.llucs.openstore.ui.viewmodel.HomeViewModel
import java.util.Locale

private enum class HomeFeed(val label: String) {
    TRENDING("Em alta"),
    ALL("Todos"),
    GAMES("Jogos"),
    TOOLS("Ferramentas"),
    MEDIA("Mídia"),
    INTERNET("Internet"),
    DEV("Dev")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val vm: HomeViewModel = viewModel()
    val apps by vm.apps.collectAsState()
    val query by vm.query.collectAsState()
    var selectedFeed by rememberSaveable { mutableStateOf(HomeFeed.TRENDING.name) }

    val selectedFeedEnum = remember(selectedFeed) {
        HomeFeed.entries.firstOrNull { it.name == selectedFeed } ?: HomeFeed.TRENDING
    }

    val visibleApps = remember(apps, selectedFeedEnum) {
        apps.filteredFor(selectedFeedEnum)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        CenterAlignedTopAppBar(
            title = { Text("OpenStore", fontWeight = FontWeight.SemiBold) },
            actions = {
                IconButton(onClick = { navController.navigate("repos") }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Repos")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (selectedFeedEnum == HomeFeed.TRENDING) "Apps em alta" else selectedFeedEnum.label,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (apps.isEmpty()) {
                            "Sincronize um repositório para começar"
                        } else {
                            "${visibleApps.size} apps visíveis • ${apps.size} no total"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { vm.query.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Buscar apps, pacote ou descrição") },
                shape = MaterialTheme.shapes.large
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeFeed.entries.forEach { feed ->
                    FilterChip(
                        selected = selectedFeedEnum == feed,
                        onClick = { selectedFeed = feed.name },
                        label = { Text(feed.label) },
                        shape = MaterialTheme.shapes.large,
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (apps.isEmpty()) {
            EmptyState(
                title = "Nada por aqui ainda",
                subtitle = "Abra a aba Repos e toque em Sincronizar para baixar a lista do F-Droid."
            )
        } else if (visibleApps.isEmpty()) {
            EmptyState(
                title = "Nenhum app nessa seção",
                subtitle = "Tente outro filtro ou pesquise com outro nome."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                items(visibleApps, key = { it.packageName }) { item ->
                    AppCard(item = item) {
                        navController.navigate("details/${item.packageName}")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCard(item: AppWithVersion, onClick: () -> Unit) {
    val displayName = remember(item.name, item.packageName) { prettyAppName(item.name, item.packageName) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RepoIcon(
                repoBaseUrl = item.repoBaseUrl,
                iconPath = item.icon,
                label = displayName,
                modifier = Modifier.size(56.dp)
            )

            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.summary.isNotBlank()) {
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val version = item.versionName?.takeIf { it.isNotBlank() }?.let { "v$it" }.orEmpty()
                    AnimatedVisibility(visible = version.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = version,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RepoIcon(
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
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        IconFallbackLabel(label = label, compact = true)

        if (currentModel != null) {
            SubcomposeAsyncImage(
                model = currentModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
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
private fun IconFallbackLabel(label: String, compact: Boolean) {
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
        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun List<AppWithVersion>.filteredFor(feed: HomeFeed): List<AppWithVersion> {
    if (isEmpty()) return this
    return when (feed) {
        HomeFeed.ALL -> sortedByDisplayName()
        HomeFeed.TRENDING -> sortedWith(
            compareByDescending<AppWithVersion> { it.lastUpdatedEpochMs }
                .thenByDescending { it.addedEpochMs }
                .thenByDescending { it.versionCode ?: 0L }
                .thenBy { prettyAppName(it.name, it.packageName).lowercase(Locale.ROOT) }
        )
        HomeFeed.GAMES -> filter { it.matchesCategory("games") }.sortedByDisplayName()
        HomeFeed.TOOLS -> filter { it.matchesCategory("tools") }.sortedByDisplayName()
        HomeFeed.MEDIA -> filter { it.matchesCategory("media") }.sortedByDisplayName()
        HomeFeed.INTERNET -> filter { it.matchesCategory("internet") }.sortedByDisplayName()
        HomeFeed.DEV -> filter { it.matchesCategory("dev") }.sortedByDisplayName()
    }
}

private fun List<AppWithVersion>.sortedByDisplayName(): List<AppWithVersion> =
    sortedBy { prettyAppName(it.name, it.packageName).lowercase(Locale.ROOT) }

private fun AppWithVersion.matchesCategory(category: String): Boolean {
    val hay = buildString {
        append(name)
        append(' ')
        append(summary)
        append(' ')
        append(packageName)
    }.lowercase(Locale.ROOT)

    return when (category) {
        "games" -> listOf("game", "jogo", "minecraft", "chess", "puzzle", "emulator").any(hay::contains)
        "tools" -> listOf("tool", "util", "file", "manager", "backup", "clock", "terminal", "archive", "security", "calc").any(hay::contains)
        "media" -> listOf("music", "audio", "video", "camera", "gallery", "photo", "podcast", "player").any(hay::contains)
        "internet" -> listOf("browser", "web", "mail", "chat", "messag", "social", "rss", "download", "torrent").any(hay::contains)
        "dev" -> listOf("git", "code", "ide", "terminal", "ssh", "api", "json", "network", "debug").any(hay::contains)
        else -> false
    }
}

private fun prettyAppName(rawName: String, packageName: String): String {
    val normalized = rawName.trim()
    if (normalized.isNotBlank() && !normalized.equals(packageName, ignoreCase = true)) return normalized

    val base = packageName.substringAfterLast('.').replace('_', ' ').replace('-', ' ').trim()
    if (base.isBlank()) return packageName

    return base.split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            val lower = token.lowercase(Locale.ROOT)
            if (lower.length == 1) lower.uppercase(Locale.ROOT)
            else lower.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
}
