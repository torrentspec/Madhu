package com.foss.madhu.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.foss.madhu.MainActivity
import com.foss.madhu.MadhuApp

/**
 * PlaybackService
 *
 * Media3 [MediaLibraryService] that wraps the [ExoPlaybackEngine]'s player.
 *
 * Handles:
 *  - Foreground service lifecycle so audio continues when the app is backgrounded
 *  - MediaSession creation (enables Bluetooth / headset / lock-screen controls)
 *  - System media notification via Media3's built-in [DefaultMediaNotificationProvider]
 *
 * The service is declared in AndroidManifest.xml:
 *  <service android:name=".service.PlaybackService"
 *           android:exported="true"
 *           android:foregroundServiceType="mediaPlayback">
 *    <intent-filter>
 *      <action android:name="androidx.media3.session.MediaLibraryService"/>
 *      <action android:name="android.media.browse.MediaBrowserService"/>
 *    </intent-filter>
 *  </service>
 */
class PlaybackService : MediaLibraryService() {

    private var mediaSession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val engine = (application as MadhuApp).appComponent.engine

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("navigate_to", "now_playing")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaLibrarySession.Builder(
            this,
            engine.player,
            object : MediaLibrarySession.Callback {}
        )
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
