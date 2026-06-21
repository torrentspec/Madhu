package com.foss.madhu

import android.content.Context
import com.foss.madhu.audio.AudioEffectManager
import com.foss.madhu.audio.ExoPlaybackEngine
import com.foss.madhu.data.JioSaavnRepository
import com.foss.madhu.download.FavoriteDownloadManager
import com.foss.madhu.prefs.UserPreferences
import com.foss.madhu.scrobble.LastFmScrobbler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual DI container — avoids Hilt (≈ 1.5 MB) to stay sub-5 MB.
 * Created once in [MadhuApp] and referenced throughout the app.
 */
class AppComponent(context: Context) {

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addNetworkInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept-Encoding", "gzip")
                .build()
            chain.proceed(request)
        }
        .build()

    val prefs         = UserPreferences(context)
    val repository    = JioSaavnRepository(context, okHttpClient)
    val audioEffectManager = AudioEffectManager()
    val scrobbler     = LastFmScrobbler(okHttpClient)
    val downloadManager = FavoriteDownloadManager(context)

    val engine = ExoPlaybackEngine(
        context          = context,
        repository       = repository,
        prefs            = prefs,
        audioEffectManager = audioEffectManager,
        scrobbler        = scrobbler,
        okHttpClient     = okHttpClient
    )

    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Hydrate scrobbler credentials from DataStore on startup.
        initScope.launch {
            scrobbler.apiKey    = prefs.lastFmApiKey.first()
            scrobbler.apiSecret = prefs.lastFmApiSecret.first()
            scrobbler.sessionKey = prefs.lastFmSessionKey.first()
            scrobbler.enabled   = prefs.scrobblingEnabled.first()
        }
    }
}
