package com.foss.madhu.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.foss.madhu.MadhuApp
import com.foss.madhu.audio.AudioEffectManager
import com.foss.madhu.data.JioSaavnRepository
import com.foss.madhu.data.model.*
import com.foss.madhu.download.FavoriteDownloadManager
import com.foss.madhu.prefs.UserPreferences
import com.foss.madhu.ui.theme.AlbumPalette
import com.foss.madhu.ui.theme.extractAlbumPalette
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * PlayerViewModel
 *
 * Single ViewModel for both the NowPlaying and Queue screens.
 * Bridges [ExoPlaybackEngine] → Compose UI via StateFlows.
 *
 * Also owns:
 *  • Album-art Palette extraction (bitmap → [AlbumPalette])
 *  • Favorite toggling (preference write + download trigger)
 *  • Search state for the search screen
 *  • EQ preset apply forwarding
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val appComponent   = (app as MadhuApp).appComponent
    private val engine         = appComponent.engine
    private val repository     = appComponent.repository
    private val prefs          = appComponent.prefs
    private val downloadManager = appComponent.downloadManager
    private val audioEffectMgr = appComponent.audioEffectManager

    // ── Player state (pass-through from engine) ───────────────────────────────

    val playbackState: StateFlow<PlaybackState> = engine.playbackState

    val bitrateLabel: StateFlow<String> = engine.currentBitrateLabel

    val lyrics: StateFlow<Lyrics?> = engine.lyrics

    // ── Preferences ───────────────────────────────────────────────────────────

    val streamQuality: StateFlow<StreamQuality> = prefs.streamQuality
        .stateIn(viewModelScope, SharingStarted.Eagerly, StreamQuality.HI_FIDELITY)

    val eqPreset: StateFlow<EqPreset> = prefs.eqPreset
        .stateIn(viewModelScope, SharingStarted.Eagerly, EqPreset.OFF)

    val customEqBands: StateFlow<ShortArray> = prefs.customEqBands
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShortArray(5) { 0 })

    val isAmoledTheme: StateFlow<Boolean> = prefs.isAmoledTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isDynamicColor: StateFlow<Boolean> = prefs.isDynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val crossfadeDurationMs: StateFlow<Int> = prefs.crossfadeDurationMs
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences.DEFAULT_CROSSFADE_MS)

    val replayGainFactor: StateFlow<Float> = prefs.replayGainFactor
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences.DEFAULT_REPLAY_GAIN)

    val favoriteIds: StateFlow<Set<String>> = prefs.favoriteIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val scrobblingEnabled: StateFlow<Boolean> = prefs.scrobblingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val lastFmUsername: StateFlow<String> = prefs.lastFmUsername
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val downloadProgress: StateFlow<Map<String, Int>> = downloadManager.progressMap
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // ── Album art palette (dynamic colour) ────────────────────────────────────

    private val _albumPalette = MutableStateFlow(AlbumPalette())
    val albumPalette: StateFlow<AlbumPalette> = _albumPalette.asStateFlow()

    // ── Search state ──────────────────────────────────────────────────────────

    private val _searchResults  = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _isSearching    = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        // When the current song changes, extract palette from the new album art.
        viewModelScope.launch {
            playbackState
                .map { it.currentSong?.highResArtUrl() }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { url -> extractPaletteFromUrl(url) }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Playback controls (delegated to engine)
    // ──────────────────────────────────────────────────────────────────────────

    fun play()                          = engine.play()
    fun pause()                         = engine.pause()
    fun skipToNext()                    = engine.skipToNext()
    fun skipToPrev()                    = engine.skipToPrev()
    fun seekTo(posMs: Long)             = engine.seekTo(posMs)
    fun playQueue(songs: List<Song>, startIndex: Int = 0) =
        engine.playQueue(songs, startIndex)

    // ── Search ────────────────────────────────────────────────────────────────

    fun search(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = runCatching {
                repository.search(query)
            }.getOrDefault(emptyList())
            _isSearching.value = false
        }
    }

    // ── Favourites & Downloads ────────────────────────────────────────────────

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isNowFav = prefs.toggleFavorite(song.id)
            if (isNowFav) {
                // Kick off silent background download
                val url = runCatching {
                    repository.getStreamUrl(song, StreamQuality.HI_FIDELITY)
                }.getOrDefault("")
                downloadManager.enqueueDownload(song, url)
            } else {
                downloadManager.deleteDownload(song)
            }
        }
    }

    fun isFavorite(songId: String): Boolean = favoriteIds.value.contains(songId)

    // ── Stream quality ────────────────────────────────────────────────────────

    fun setStreamQuality(quality: StreamQuality) {
        viewModelScope.launch {
            prefs.setStreamQuality(quality)
            engine.setStreamQuality(quality)
        }
    }

    // ── EQ ───────────────────────────────────────────────────────────────────

    fun applyEqPreset(preset: EqPreset) {
        viewModelScope.launch {
            prefs.setEqPreset(preset)
            audioEffectMgr.applyPreset(preset)
        }
    }

    fun setCustomBand(band: Int, gainMb: Short) {
        viewModelScope.launch {
            audioEffectMgr.setCustomBandGain(band, gainMb)
            val updated = customEqBands.value.clone()
            if (band in updated.indices) updated[band] = gainMb
            prefs.setCustomEqBands(updated)
        }
    }

    // ── Playback preferences ──────────────────────────────────────────────────

    fun setCrossfadeDuration(ms: Int) {
        viewModelScope.launch {
            prefs.setCrossfadeDuration(ms)
            engine.setCrossfadeDuration(ms)
        }
    }

    fun setReplayGainFactor(factor: Float) {
        viewModelScope.launch {
            prefs.setReplayGainFactor(factor)
            engine.setReplayGainFactor(factor)
        }
    }

    // ── Theme preferences ─────────────────────────────────────────────────────

    fun setAmoledTheme(enabled: Boolean) =
        viewModelScope.launch { prefs.setAmoledTheme(enabled) }

    fun setDynamicColor(enabled: Boolean) =
        viewModelScope.launch { prefs.setDynamicColor(enabled) }

    // ── Last.fm ───────────────────────────────────────────────────────────────

    fun saveLastFmCredentials(apiKey: String, apiSecret: String) =
        viewModelScope.launch { prefs.setLastFmCredentials(apiKey, apiSecret) }

    fun authenticateLastFm(username: String, password: String) {
        viewModelScope.launch {
            val scrobbler = appComponent.scrobbler
            scrobbler.apiKey    = prefs.lastFmApiKey.first()
            scrobbler.apiSecret = prefs.lastFmApiSecret.first()
            val sessionKey = scrobbler.authenticate(username, password)
            if (sessionKey != null) {
                prefs.setLastFmSession(sessionKey, username)
                scrobbler.sessionKey = sessionKey
            }
        }
    }

    fun setScrobblingEnabled(enabled: Boolean) =
        viewModelScope.launch {
            prefs.setScrobblingEnabled(enabled)
            appComponent.scrobbler.enabled = enabled
        }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun extractPaletteFromUrl(url: String) {
        viewModelScope.launch {
            try {
                val loader  = ImageLoader(getApplication())
                val request = ImageRequest.Builder(getApplication())
                    .data(url)
                    .allowHardware(false) // Palette needs software bitmap
                    .build()
                val result = loader.execute(request)
                val bitmap = (result as? SuccessResult)?.drawable
                    ?.let { (it as? android.graphics.drawable.BitmapDrawable)?.bitmap }
                    ?: return@launch
                _albumPalette.value = extractAlbumPalette(bitmap)
            } catch (e: Exception) {
                // Silently ignore; keep previous palette
            }
        }
    }
}
