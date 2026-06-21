package com.foss.madhu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.foss.madhu.data.model.Song
import com.foss.madhu.ui.screens.NowPlayingScreen
import com.foss.madhu.ui.screens.SettingsScreen
import com.foss.madhu.ui.theme.AlbumPalette
import com.foss.madhu.ui.theme.LocalAlbumPalette
import com.foss.madhu.ui.theme.MadhuTheme
import com.foss.madhu.ui.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(this)[PlayerViewModel::class.java]

        // Start PlaybackService so audio continues in background.
        startForegroundService(android.content.Intent(this, service.PlaybackService::class.java))

        setContent {
            val isAmoled    by viewModel.isAmoledTheme.collectAsStateWithLifecycle()
            val isDynamic   by viewModel.isDynamicColor.collectAsStateWithLifecycle()
            val albumPalette by viewModel.albumPalette.collectAsStateWithLifecycle()

            MadhuTheme(
                isAmoled       = isAmoled,
                isDynamicColor = isDynamic,
                albumPalette   = albumPalette
            ) {
                MadhuNavHost(viewModel = viewModel)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Navigation host
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun MadhuNavHost(viewModel: PlayerViewModel) {
    val navController = rememberNavController()
    val albumPalette  by viewModel.albumPalette.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalAlbumPalette provides albumPalette) {
        NavHost(
            navController    = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    viewModel    = viewModel,
                    onOpenPlayer = { navController.navigate("now_playing") },
                    onOpenSettings = { navController.navigate("settings") }
                )
            }
            composable("now_playing") {
                NowPlayingScreen(
                    viewModel      = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel      = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Home / Search screen
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlayerViewModel,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching   by viewModel.isSearching.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    var query         by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value         = query,
                        onValueChange = { q -> query = q; viewModel.search(q) },
                        placeholder   = { Text("Search songs, artists, albums…") },
                        leadingIcon   = { Icon(Icons.Filled.Search, null) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = TextFieldDefaults.colors(
                            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            // Mini player bar — tap to open Now Playing
            if (playbackState.currentSong != null) {
                MiniPlayer(
                    song        = playbackState.currentSong!!,
                    isPlaying   = playbackState.isPlaying,
                    onClick     = onOpenPlayer,
                    onPlayPause = { if (playbackState.isPlaying) viewModel.pause() else viewModel.play() }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isSearching) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (query.isBlank()) {
                // Empty state
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Search for music", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Type above to explore JioSaavn's catalogue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(vertical = 8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
                ) {
                    items(searchResults, key = { it.id }) { song ->
                        SongListItem(
                            song    = song,
                            onClick = {
                                viewModel.playQueue(
                                    songs      = searchResults,
                                    startIndex = searchResults.indexOf(song)
                                )
                                onOpenPlayer()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongListItem(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .androidx.compose.foundation.clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model              = song.albumArtUrl,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(52.dp)
                .androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                .let { it }
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(song.formattedDuration(), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
    }
}

@Composable
private fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit
) {
    Surface(
        modifier     = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape        = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color        = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .androidx.compose.foundation.clickable(onClick = onClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model              = song.albumArtUrl,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(44.dp)
                    .androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    .let { it }
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
