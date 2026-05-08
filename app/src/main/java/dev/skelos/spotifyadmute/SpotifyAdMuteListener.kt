package dev.skelos.spotifyadmute

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log

/**
 * Listens to Spotify's MediaSession via NotificationListenerService and mutes
 * STREAM_MUSIC for the duration of every ad. Original volume is restored when
 * the ad ends.
 *
 * Detection uses both signals Spotify exposes:
 *  - METADATA_KEY_ADVERTISEMENT == 1
 *  - METADATA_KEY_MEDIA_ID containing "spotify:ad"
 */
class SpotifyAdMuteListener : NotificationListenerService() {

    companion object {
        private const val TAG = "SpotifyAdMute"
        private const val SPOTIFY_PKG = "com.spotify.music"
        // MediaMetadata.METADATA_KEY_ADVERTISEMENT is @hide in the SDK; the
        // underlying string key is public-facing once you know it.
        private const val META_KEY_AD = "android.media.metadata.ADVERTISEMENT"

        // Spotify localizes the title shown during ads. Match case-insensitively.
        private val AD_TITLE_KEYWORDS = listOf(
            "advertisement",
            "annonce",         // fr
            "anuncio",         // es / pt
            "werbung",         // de
            "pubblicità",      // it
            "reklam",          // tr / sv
            "reklama",         // pl
            "广告",             // zh
            "広告",             // ja
            "광고",             // ko
            "spotify ad",
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var sessionManager: MediaSessionManager
    private lateinit var audioManager: AudioManager
    private lateinit var componentName: ComponentName

    private val controllers = mutableMapOf<MediaController, MediaController.Callback>()
    private var isAd = false
    private var savedVolume = -1
    private var didMute = false

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list ->
            rebindControllers(list ?: emptyList())
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        componentName = ComponentName(this, SpotifyAdMuteListener::class.java)

        sessionManager.addOnActiveSessionsChangedListener(
            sessionsChangedListener, componentName, mainHandler
        )
        rebindControllers(sessionManager.getActiveSessions(componentName))
        Log.i(TAG, "listener connected")
    }

    override fun onListenerDisconnected() {
        try {
            sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (_: Exception) {
        }
        clearControllers()
        if (isAd) restoreVolume()
        isAd = false
        super.onListenerDisconnected()
    }

    private fun rebindControllers(sessions: List<MediaController>) {
        val spotify = sessions.filter { it.packageName == SPOTIFY_PKG }
        val current = spotify.toSet()

        val gone = controllers.keys - current
        for (c in gone) {
            controllers.remove(c)?.let { c.unregisterCallback(it) }
        }

        for (c in spotify) {
            if (c in controllers) continue
            val cb = createCallback(c)
            controllers[c] = cb
            c.registerCallback(cb, mainHandler)
            handleMetadata(c.metadata)
        }
    }

    private fun clearControllers() {
        for ((c, cb) in controllers) {
            try { c.unregisterCallback(cb) } catch (_: Exception) {}
        }
        controllers.clear()
    }

    private fun createCallback(controller: MediaController) = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            handleMetadata(metadata)
        }

        override fun onSessionDestroyed() {
            controllers.remove(controller)?.let {
                try { controller.unregisterCallback(it) } catch (_: Exception) {}
            }
            if (isAd && controllers.isEmpty()) {
                restoreVolume()
                isAd = false
            }
        }
    }

    private fun handleMetadata(metadata: MediaMetadata?) {
        if (metadata == null) return

        val advFlag = metadata.getLong(META_KEY_AD) == 1L
        val mediaId = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID).orEmpty()
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val displayTitle = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE).orEmpty()
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()

        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "metadata: title='$title' displayTitle='$displayTitle' artist='$artist' " +
                        "album='$album' mediaId='$mediaId' advFlag=$advFlag"
            )
        }

        val ad = advFlag
                || mediaId.contains("spotify:ad", ignoreCase = true)
                || titleLooksLikeAd(title)
                || titleLooksLikeAd(displayTitle)

        if (ad && !isAd) {
            isAd = true
            saveAndMute()
            Log.i(TAG, "ad detected (title='$title' displayTitle='$displayTitle') -> mute")
        } else if (!ad && isAd) {
            restoreVolume()
            isAd = false
            Log.i(TAG, "ad ended (title='$title') -> unmute")
        }
    }

    private fun titleLooksLikeAd(s: String): Boolean {
        if (s.isBlank()) return false
        val lower = s.lowercase()
        return AD_TITLE_KEYWORDS.any { lower.contains(it) }
    }

    private fun saveAndMute() {
        if (savedVolume < 0) {
            savedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
        // Try the session-targeted route first (only mutes Spotify, not the
        // whole STREAM_MUSIC). Fall back to AudioManager.adjustStreamVolume
        // with ADJUST_MUTE if that fails for any reason.
        val sessionMuted = controllers.keys.any { c ->
            try {
                c.setVolumeTo(0, 0)
                Log.i(TAG, "muted via MediaController.setVolumeTo")
                true
            } catch (e: Exception) {
                Log.w(TAG, "controller.setVolumeTo failed: $e")
                false
            }
        }
        if (!sessionMuted) {
            try {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0
                )
                Log.i(TAG, "muted via adjustStreamVolume(ADJUST_MUTE)")
            } catch (e: Exception) {
                Log.e(TAG, "adjustStreamVolume failed: $e")
            }
        }
        didMute = true
    }

    private fun restoreVolume() {
        if (!didMute) return
        // Restore by reversing whatever path we used.
        var sessionRestored = false
        for (c in controllers.keys) {
            try {
                c.setVolumeTo(if (savedVolume >= 0) savedVolume else c.playbackInfo?.maxVolume ?: 15, 0)
                sessionRestored = true
            } catch (e: Exception) {
                Log.w(TAG, "controller.setVolumeTo restore failed: $e")
            }
        }
        if (!sessionRestored) {
            try {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0
                )
            } catch (e: Exception) {
                Log.e(TAG, "adjustStreamVolume unmute failed: $e")
            }
            // Defensive: also restore the absolute level in case unmute alone
            // came back to a different value.
            if (savedVolume >= 0) {
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0)
                } catch (_: Exception) { }
            }
        }
        savedVolume = -1
        didMute = false
    }
}
