package com.shaaztechno.videoplayer.presentation.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.annotation.OptIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.media3.common.util.UnstableApi
import com.shaaztechno.videoplayer.R
import com.shaaztechno.videoplayer.SZPlayerApplication
import com.shaaztechno.videoplayer.data.local.UserSettings
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.ui.theme.SZPlayerTheme
import java.io.File

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID) ?: run {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setFullscreen(true)

        val app = applicationContext as SZPlayerApplication

        setContent {
            val settings by app.settingsDataStore.settingsFlow.collectAsState(initial = UserSettings())
            val darkMode = settings.darkMode

            SZPlayerTheme(darkMode = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayerScreen(
                        videoId = videoId,
                        onBack = { finish() },
                        onPipClick = { enterPipMode() },
                        onShareClick = { video -> shareVideo(video) }
                    )
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.stay,
                R.anim.slide_out_bottom
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    fun setFullscreen(enabled: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (enabled) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    fun shareVideo(video: Video) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            if (video.type == VideoType.ONLINE) {
                putExtra(Intent.EXTRA_TEXT, "Check out this video: ${video.title}\n${video.url}")
                type = "text/plain"
            } else {
                try {
                    val uri = if (video.url.startsWith("content://")) {
                        Uri.parse(video.url)
                    } else {
                        FileProvider.getUriForFile(
                            this@PlayerActivity,
                            "${applicationContext.packageName}.fileprovider",
                            File(video.url)
                        )
                    }
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "video/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                    putExtra(Intent.EXTRA_TEXT, "Check out this video: ${video.title}\n${video.url}")
                    type = "text/plain"
                }
            }
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Video")
        startActivity(shareIntent)
    }

    companion object {
        const val EXTRA_VIDEO_ID = "extra_video_id"

        fun start(context: Context, videoId: String) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_ID, videoId)
            }
            context.startActivity(intent)
            if (context is Activity) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    context.overrideActivityTransition(
                        Activity.OVERRIDE_TRANSITION_OPEN,
                        R.anim.slide_in_bottom,
                        R.anim.stay
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay)
                }
            }
        }
    }
}
