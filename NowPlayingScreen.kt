package com.foss.madhu.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.foss.madhu.data.model.*
import com.foss.madhu.ui.theme.LocalAlbumPalette
import com.foss.madhu.ui.viewmodel.PlayerViewModel
import kotlin.math.abs

// ──────────────────────────────────────────────────────────────────────────────
// NowPlayingScreen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState  by viewModel.playbackState.collectAsStateWithLifecycle()
    val bitrateLabel   by viewModel.bitrateLabel.collectAsStateWithLifecycle()
    val lyrics         by viewModel.lyrics.collectAsStateWithLifecycle()
    val favoriteIds    by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val eqPreset       by viewModel.eqPreset.collectAsStateWithLifecycle()
    val customEqBands  by viewModel.customEqBands.collectAsStateWithLifecycle()
    val albumPalette   = LocalAlbumPalette.current
    val isAmoled       by viewModel.isAmoledTheme.collectAsStateWithLifecycle()

    val song           = playbackState.currentSong
    val isFavorite     = song?.let { favoriteIds.contains(it.id) } ?: false

    var showLyrics     by remember { mutableStateOf(false) }
    var showQueue      by remember { mutableStateOf(false) }

    // Animated accent colour from album art palette
    val accentColor by animateColorAsState(
        targetValue = albumPalette.vibrant,
        animationSpec = tween(durationMillis = 800, easing = EaseInOutCubic),
        label = "accentColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isAmoled) Color.Black
                      else albumPalette.darkVibrant.copy(alpha = 0.85f),
        animationSpec = tween(800, easing = EaseInOutCubic),
        label = "bgColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ── Full-bleed album art with gradient overlay ─────────────────────
        if (song != null) {
            AsyncImage(
                model          = song.highResArtUrl(),
                contentDescription = null,
                contentScale   = ContentScale.Crop,
                modifier       = Modifier
                    .fillMaxSize()
                    .alpha(if (isAmoled) 0.12f else 0.25f)
            )
        }

        // Gradient — dark at top and bottom for legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to bgColor.copy(alpha = 0.92f),
                        0.35f to Color.Transparent,
                        0.65f to Color.Transparent,
                        1.0f to bgColor.copy(alpha = 0.98f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // ── Top bar ───────────────────────────────────────────────────
            NowPlayingTopBar(
                onNavigateBack = onNavigateBack,
                onQueueClick   = { showQueue = !showQueue },
                accentColor    = accentColor
            )

            // ── Album art ─────────────────────────────────────────────────
            Spacer(Modifier.weight(1f))

            AnimatedContent(
                targetState = song?.albumArtUrl,
                transitionSpec = {
                    fadeIn(tween(400)) togetherWith fadeOut(tween(300))
                },
                label = "albumArtTransition",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 32.dp)
            ) { artUrl ->
                val artModifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp)
                    )
                if (artUrl != null) {
                    AsyncImage(
                        model              = song?.highResArtUrl(),
                        contentDescription = "Album art",
                        contentScale       = ContentScale.Crop,
                        modifier           = artModifier
                    )
                } else {
                    Box(
                        modifier = artModifier.background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint               = accentColor,
                            modifier           = Modifier.size(80.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Metadata + Bitrate badge + Favorite ───────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = song?.title ?: "Not Playing",
                        style      = MaterialTheme.typography.titleLarge,
                        color      = Color.White,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = song?.artist ?: "",
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = Color.White.copy(alpha = 0.70f),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                }

                // Favorite heart toggle
                IconButton(
                    onClick = { song?.let { viewModel.toggleFavorite(it) } }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite
                                      else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites"
                                             else "Add to favorites",
                        tint   = if (isFavorite) Color(0xFFFF5B7A) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Bitrate badge ("320 kbps • AAC") ─────────────────────────
            if (bitrateLabel.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .height(22.dp),
                    shape  = RoundedCornerShape(11.dp),
                    color  = accentColor.copy(alpha = 0.18f),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text     = bitrateLabel,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Progress slider ───────────────────────────────────────────
            SeekBar(
                progress     = playbackState.progressFraction,
                positionMs   = playbackState.positionMs,
                durationMs   = playbackState.durationMs,
                accentColor  = accentColor,
                onSeek       = { frac ->
                    viewModel.seekTo((frac * playbackState.durationMs).toLong())
                },
                modifier     = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Playback controls ─────────────────────────────────────────
            PlaybackControls(
                isPlaying    = playbackState.isPlaying,
                isBuffering  = playbackState.isBuffering,
                accentColor  = accentColor,
                onPlayPause  = {
                    if (playbackState.isPlaying) viewModel.pause() else viewModel.play()
                },
                onSkipNext   = { viewModel.skipToNext() },
                onSkipPrev   = { viewModel.skipToPrev() }
            )

            Spacer(Modifier.height(16.dp))

            // ── EQ preset chip row ────────────────────────────────────────
            EqPresetRow(
                activePreset    = eqPreset,
                accentColor     = accentColor,
                onPresetSelected = { viewModel.applyEqPreset(it) }
            )

            Spacer(Modifier.height(8.dp))

            // ── Lyrics / Queue toggle buttons ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showLyrics = !showLyrics },
                    modifier = Modifier.weight(1f),
                    border   = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (showLyrics) accentColor else Color.White.copy(0.7f)
                    )
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Lyrics,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Lyrics", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = { showQueue = !showQueue },
                    modifier = Modifier.weight(1f),
                    border   = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (showQueue) accentColor else Color.White.copy(0.7f)
                    )
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.QueueMusic,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Up Next", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // ── Lyrics bottom sheet ───────────────────────────────────────────────
        if (showLyrics) {
            LyricsBottomSheet(
                lyrics      = lyrics,
                positionMs  = playbackState.positionMs,
                accentColor = accentColor,
                isAmoled    = isAmoled,
                onDismiss   = { showLyrics = false }
            )
        }

        // ── Queue sheet ───────────────────────────────────────────────────────
        if (showQueue) {
            QueueBottomSheet(
                queue       = playbackState.queue,
                currentSong = playbackState.currentSong,
                accentColor = accentColor,
                isAmoled    = isAmoled,
                onSongClick = { idx -> viewModel.playQueue(playbackState.queue, idx) },
                onDismiss   = { showQueue = false }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Top bar
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NowPlayingTopBar(
    onNavigateBack: () -> Unit,
    onQueueClick: () -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.Filled.KeyboardArrowDown, "Back",
                tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Text(
            text  = "NOW PLAYING",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            color = Color.White.copy(alpha = 0.55f)
        )
        IconButton(onClick = onQueueClick) {
            Icon(Icons.Outlined.QueueMusic, "Queue",
                tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Seek bar
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SeekBar(
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    accentColor: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragValue  by remember { mutableStateOf(progress) }
    val displayProg = if (isDragging) dragValue else progress

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value         = displayProg,
            onValueChange = { v -> isDragging = true; dragValue = v },
            onValueChangeFinished = { onSeek(dragValue); isDragging = false },
            colors        = SliderDefaults.colors(
                thumbColor         = accentColor,
                activeTrackColor   = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.20f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text  = formatMs(if (isDragging) (dragValue * durationMs).toLong() else positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text  = formatMs(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1_000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

// ──────────────────────────────────────────────────────────────────────────────
// Playback controls
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // Skip previous
        IconButton(onClick = onSkipPrev, modifier = Modifier.size(52.dp)) {
            Icon(
                imageVector        = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint               = Color.White.copy(alpha = 0.85f),
                modifier           = Modifier.size(32.dp)
            )
        }

        // Play / Pause with circular accent background
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(accentColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick    = onPlayPause
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(28.dp),
                    color     = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                val onAccent = if (accentColor.luminance() > 0.4f) Color.Black else Color.White
                Icon(
                    imageVector        = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint               = onAccent,
                    modifier           = Modifier.size(36.dp)
                )
            }
        }

        // Skip next
        IconButton(onClick = onSkipNext, modifier = Modifier.size(52.dp)) {
            Icon(
                imageVector        = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint               = Color.White.copy(alpha = 0.85f),
                modifier           = Modifier.size(32.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// EQ preset chip row
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun EqPresetRow(
    activePreset: EqPreset,
    accentColor: Color,
    onPresetSelected: (EqPreset) -> Unit
) {
    val presets = EqPreset.values().filter { it != EqPreset.CUSTOM }

    LazyRow(
        modifier           = Modifier.fillMaxWidth(),
        contentPadding     = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presets) { preset ->
            val isSelected = preset == activePreset
            FilterChip(
                selected = isSelected,
                onClick  = { onPresetSelected(preset) },
                label    = {
                    Text(
                        text  = preset.displayName,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor,
                    selectedLabelColor     = if (accentColor.luminance() > 0.4f) Color.Black else Color.White,
                    containerColor         = Color.White.copy(alpha = 0.08f),
                    labelColor             = Color.White.copy(alpha = 0.7f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled          = true,
                    selected         = isSelected,
                    borderColor      = Color.White.copy(alpha = 0.15f),
                    selectedBorderColor = Color.Transparent
                )
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Lyrics bottom sheet
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsBottomSheet(
    lyrics: com.foss.madhu.data.model.Lyrics?,
    positionMs: Long,
    accentColor: Color,
    isAmoled: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val listState  = androidx.compose.foundation.lazy.rememberLazyListState()

    // Auto-scroll to the currently active line when time-synced.
    val activeLineIndex = remember(lyrics, positionMs) {
        if (lyrics == null || !lyrics.isTimeSynced) return@remember -1
        var idx = 0
        for (i in lyrics.lines.indices) {
            if (lyrics.lines[i].timestampMs <= positionMs) idx = i else break
        }
        idx
    }

    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0) {
            listState.animateScrollToItem(activeLineIndex.coerceAtLeast(0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = if (isAmoled) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.surface,
        dragHandle       = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Text(
            text     = "Lyrics",
            style    = MaterialTheme.typography.titleMedium,
            color    = Color.White,
            modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
        )

        when {
            lyrics == null -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "No lyrics available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
            lyrics.lines.isEmpty() -> {
                Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Lyrics not found", color = Color.White.copy(0.4f))
                }
            }
            else -> {
                LazyColumn(
                    state          = listState,
                    modifier       = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(lyrics.lines) { idx, line ->
                        val isActive = idx == activeLineIndex
                        Text(
                            text  = line.text,
                            style = if (isActive) MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ) else MaterialTheme.typography.bodyLarge,
                            color = when {
                                isActive              -> accentColor
                                lyrics.isTimeSynced && idx < activeLineIndex -> Color.White.copy(0.4f)
                                else                  -> Color.White.copy(0.7f)
                            },
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Queue bottom sheet
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueBottomSheet(
    queue: List<Song>,
    currentSong: Song?,
    accentColor: Color,
    isAmoled: Boolean,
    onSongClick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = if (isAmoled) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.surface,
        dragHandle       = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Text(
            text     = "Up Next",
            style    = MaterialTheme.typography.titleMedium,
            color    = Color.White,
            modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
        )
        LazyColumn(
            modifier       = Modifier.heightIn(max = 480.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            itemsIndexed(queue) { idx, song ->
                val isNowPlaying = song.id == currentSong?.id
                QueueItem(
                    song        = song,
                    isActive    = isNowPlaying,
                    accentColor = accentColor,
                    onClick     = { onSongClick(idx) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QueueItem(
    song: Song,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model              = song.albumArtUrl,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text     = song.title,
                style    = MaterialTheme.typography.bodyMedium,
                color    = if (isActive) accentColor else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text     = song.artist,
                style    = MaterialTheme.typography.labelSmall,
                color    = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isActive) {
            Icon(
                imageVector        = Icons.Filled.EqualizerOutlined,
                contentDescription = "Now Playing",
                tint               = accentColor,
                modifier           = Modifier.size(18.dp)
            )
        } else {
            Text(
                text  = song.formattedDuration(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

// Workaround — Material icons doesn't export EqualizerOutlined as a constant;
// reuse the filled variant via a local alias.
private val Icons.Filled.EqualizerOutlined get() = Icons.Filled.Equalizer
