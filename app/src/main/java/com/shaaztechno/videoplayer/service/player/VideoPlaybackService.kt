package com.shaaztechno.videoplayer.service.player

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.shaaztechno.videoplayer.SZPlayerApplication

@OptIn(UnstableApi::class)
class VideoPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        
        val app = applicationContext as SZPlayerApplication
        val cacheDataSourceFactory = app.szDownloadManager.cacheDataSourceFactory

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                handlePlaybackError(error)
            }
        })

        mediaSession = MediaSession.Builder(this, player)
            .build()
    }

    private fun handlePlaybackError(error: PlaybackException) {
        if (isAudioPlaybackError(error)) {
            val isAudioDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_AUDIO)
            if (!isAudioDisabled) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
                player.prepare()
                player.play()

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        applicationContext,
                        "Audio format (E-AC-3/Dolby) is not supported on this device. Playing video only.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun isAudioPlaybackError(error: PlaybackException): Boolean {
        val cause = error.cause
        val message = error.message ?: ""
        val causeMessage = cause?.message ?: ""

        if (cause is MediaCodecRenderer.DecoderInitializationException) {
            val mimeType = cause.mimeType ?: ""
            if (mimeType.contains("audio", ignoreCase = true) || mimeType.contains("eac3", ignoreCase = true) || mimeType.contains("ac3", ignoreCase = true)) {
                return true
            }
        }

        if (error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED
        ) {
            return true
        }

        if (message.contains("MediaCodecAudioRenderer", ignoreCase = true) ||
            message.contains("audio/eac3", ignoreCase = true) ||
            message.contains("audio/ac3", ignoreCase = true) ||
            causeMessage.contains("audio/eac3", ignoreCase = true) ||
            causeMessage.contains("audio/ac3", ignoreCase = true)
        ) {
            return true
        }

        return false
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.playWhenReady == false || player?.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

