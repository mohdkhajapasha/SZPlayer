package com.shaaztechno.videoplayer.presentation.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.shaaztechno.videoplayer.SZPlayerApplication
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.ui.theme.ElectricGreen
import com.shaaztechno.videoplayer.ui.theme.Gray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoId: String,
    onBack: () -> Unit,
    onPipClick: () -> Unit,
    onShareClick: (Video) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as SZPlayerApplication
    val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory(videoId, app.videoRepository, app.szDownloadManager))
    val uiState by viewModel.uiState.collectAsState()

    val exoPlayer = remember {
        val cacheDataSourceFactory = app.szDownloadManager.cacheDataSourceFactory
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cacheDataSourceFactory)
            )
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    
    var isLocked by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var gestureText by remember { mutableStateOf<String?>(null) }
    var gestureIcon by remember { mutableStateOf<ImageVector?>(null) }
    // Float accumulator for smooth volume gesture (mirrors brightness approach)
    var volumeAccumulator by remember { mutableFloatStateOf(-1f) }

    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isFullscreen by remember { mutableStateOf(true) }
    var isPortrait by remember { mutableStateOf(true) }

    // Buffering state & automatic track initialization
    var isBuffering by remember { mutableStateOf(false) }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
            override fun onTracksChanged(tracks: Tracks) {
                selectFirstCompatibleAudioTrack(exoPlayer, tracks)
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }
    
    var activeDetailType by remember { mutableStateOf<PlayerDetailType?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Hide controls after delay
    LaunchedEffect(showControls, isLocked, activeDetailType, isMenuOpen, showDeleteDialog) {
        if (showControls && activeDetailType == null && !isLocked && !isMenuOpen && !showDeleteDialog) {
            delay(4000)
            showControls = false
        }
    }

    // Tracks whether the player was playing before a lifecycle pause,
    // so we can restore the exact state (play/paused) on resume
    // instead of always forcing playback.
    var wasPlayingBeforeLifecyclePause by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    wasPlayingBeforeLifecyclePause = exoPlayer.isPlaying
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Restore previous state: only resume if it was playing before
                    if (wasPlayingBeforeLifecyclePause) exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.updatePlaybackHistory(exoPlayer.currentPosition, exoPlayer.duration)
            exoPlayer.release()
            lifecycleOwner.lifecycle.removeObserver(observer)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(uiState.video) {
        uiState.video?.let { video ->
            // For local/downloaded videos prefer localUri (content:// from MediaStore).
            // Provide a video/* MIME hint so ExoPlayer can detect format even without
            // a file extension in the URI path.
            val playUri = (video.localUri ?: video.url).let { android.net.Uri.parse(it) }
            val mediaItem = if (video.type == com.shaaztechno.videoplayer.domain.model.VideoType.LOCAL ||
                video.type == com.shaaztechno.videoplayer.domain.model.VideoType.DOWNLOADED) {
                MediaItem.Builder()
                    .setUri(playUri)
                    .setMimeType("video/*")
                    .build()
            } else {
                MediaItem.fromUri(playUri)
            }
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            if (uiState.initialPosition > 0) {
                exoPlayer.seekTo(uiState.initialPosition)
            }
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }
    
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (isLocked) return@detectTapGestures
                        val width = size.width
                        if (offset.x < width / 2) {
                            exoPlayer.seekBack()
                            gestureText = "-10s"
                            gestureIcon = Icons.Rounded.Replay10
                        } else {
                            exoPlayer.seekForward()
                            gestureText = "+10s"
                            gestureIcon = Icons.Rounded.Forward10
                        }
                        scope.launch {
                            delay(1000)
                            gestureText = null
                            gestureIcon = null
                        }
                    },
                    onLongPress = {
                        if (isLocked) return@detectTapGestures
                        playbackSpeed = 2.0f
                        gestureText = "2x Speed"
                        gestureIcon = Icons.Rounded.FastForward
                    },
                    onPress = {
                        if (isLocked) return@detectTapGestures
                        tryAwaitRelease()
                        if (playbackSpeed == 2.0f) {
                            playbackSpeed = 1.0f
                            gestureText = null
                            gestureIcon = null
                        }
                    }
                )
            }
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        val width = size.width
                        val height = size.height
                        
                        if (abs(dragAmount.y) > abs(dragAmount.x)) {
                            // Vertical drag
                            if (change.position.x < width / 2) {
                                // Brightness (Left side)
                                activity?.window?.let { window ->
                                    val lp = window.attributes
                                    val currentBrightness = if (lp.screenBrightness < 0) 0.5f else lp.screenBrightness
                                    lp.screenBrightness = (currentBrightness - dragAmount.y / height).coerceIn(0f, 1f)
                                    window.attributes = lp
                                    gestureText = "Brightness: ${(lp.screenBrightness * 100).toInt()}%"
                                    gestureIcon = when {
                                        lp.screenBrightness > 0.7f -> Icons.Rounded.BrightnessHigh
                                        lp.screenBrightness > 0.3f -> Icons.Rounded.BrightnessMedium
                                        else -> Icons.Rounded.BrightnessLow
                                    }
                                }
                            } else {
                                // Volume (Right side) — use float accumulator for smoothness
                                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                // Seed accumulator from actual system volume on first touch
                                if (volumeAccumulator < 0f) {
                                    volumeAccumulator = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                                }
                                volumeAccumulator = (volumeAccumulator - dragAmount.y / height * maxVolume).coerceIn(0f, maxVolume.toFloat())
                                val nextVolume = volumeAccumulator.toInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVolume, 0)
                                gestureText = "Volume: ${(volumeAccumulator / maxVolume * 100).toInt()}%"
                                gestureIcon = if (nextVolume == 0) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp
                            }
                        } else {
                            // Horizontal drag - Seeking
                            val dragProgress = dragAmount.x / width
                            val seekDelta = (dragProgress * 60000).toLong() // Seek 1 min per full width drag
                            exoPlayer.seekTo((exoPlayer.currentPosition + seekDelta).coerceIn(0, exoPlayer.duration))
                            gestureText = formatTime(exoPlayer.currentPosition)
                            gestureIcon = if (dragAmount.x > 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind
                        }
                    },
                    onDragEnd = {
                        volumeAccumulator = -1f  // reset so next drag seeds from real system volume
                        scope.launch {
                            delay(1000)
                            gestureText = null
                            gestureIcon = null
                        }
                    }
                )
            }
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ElectricGreen)
        } else if (uiState.error != null) {
            Text(uiState.error!!, color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false
                        this.resizeMode = resizeMode
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = {
                    it.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )

            PlayerControls(
                player = exoPlayer,
                videoTitle = uiState.video?.title ?: "",
                isLocked = isLocked,
                onLockToggle = { 
                    isLocked = !isLocked
                    showControls = true
                },
                onBack = onBack,
                onPipClick = onPipClick,
                onShareClick = { uiState.video?.let(onShareClick) },
                onDeleteClick = { showDeleteDialog = true },
                onDownloadClick = { /* TODO: trigger download */ },
                onCatalogueClick = { /* TODO: open online catalogue */ },
                onMenuExpandedChange = { isMenuOpen = it },
                isVisible = showControls,
                onSpeedClick = { activeDetailType = PlayerDetailType.SPEED },
                onAspectClick = { activeDetailType = PlayerDetailType.ASPECT },
                onAudioClick = { activeDetailType = PlayerDetailType.AUDIO },
                onSubtitlesClick = { activeDetailType = PlayerDetailType.SUBTITLES },
                isFullscreen = isFullscreen,
                onFullscreenClick = {
                    isFullscreen = !isFullscreen
                    activity?.let { act ->
                        val window = act.window
                        val decorView = window.decorView
                        if (!isFullscreen) {
                            // Exit fullscreen: restore system UI
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } else {
                            // Enter fullscreen: hide system UI
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        }
                    }
                },
                isPortrait = isPortrait,
                onRotationClick= {
                    isPortrait = !isPortrait
                    activity?.let { act ->
                        val window = act.window
                        val decorView = window.decorView
                        if (!isPortrait) {
                            // Exit fullscreen: restore system UI
                            @Suppress("DEPRECATION")
                            decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                            act.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        } else {
                            // Enter fullscreen: hide system UI
                            @Suppress("DEPRECATION")
                            decorView.systemUiVisibility = (
                                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    )
                            act.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    }
                }
            )

            gestureText?.let { text ->
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(18.dp)
                    ) {
                        gestureIcon?.let { icon ->
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            text = text,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Buffering indicator — shown for online videos during initial load and rebuffering
            if (isBuffering && uiState.video?.type == com.shaaztechno.videoplayer.domain.model.VideoType.ONLINE) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = ElectricGreen,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Buffering…",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            activeDetailType?.let { detailType ->
                PlaybackDetailDialog(
                    type = detailType,
                    player = exoPlayer,
                    currentSpeed = playbackSpeed,
                    currentResizeMode = resizeMode,
                    onSpeedChange = { playbackSpeed = it },
                    onResizeModeChange = { resizeMode = it },
                    onDismiss = { activeDetailType = null }
                )
            }

            // Delete Confirmation Dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Video", color = Color.White) },
                    text = { Text("Are you sure you want to delete this video?", color = Color.Gray) },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteVideo {
                                onBack()
                            }
                            showDeleteDialog = false
                        }) {
                            Text("Delete", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF1E1E1E)
                )
            }
        }
    }
}

@Composable
fun PlayerControls(
    player: Player,
    videoTitle: String,
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onBack: () -> Unit,
    onPipClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onCatalogueClick: () -> Unit = {},
    onMenuExpandedChange: (Boolean) -> Unit = {},
    isVisible: Boolean,
    onSpeedClick: () -> Unit,
    onAspectClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    isFullscreen: Boolean = true,
    onFullscreenClick: () -> Unit = {},
    onRotationClick: () -> Unit = {},
    isPortrait: Boolean = true
) {
    var currentSpeed by remember { mutableFloatStateOf(player.playbackParameters.speed) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                currentSpeed = playbackParameters.speed
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isVisible) {
            if (!isLocked) {
                // Top Bar Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                )

                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = videoTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    IconButton(onClick = onRotationClick) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector =  Icons.Rounded.ScreenRotation,
                            contentDescription = "Screen Rotation",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = onFullscreenClick) {
                        Icon(modifier = Modifier.size(28.dp),
                            imageVector = if (isFullscreen){
                                Icons.Rounded.Fullscreen

                            } else {
                                Icons.Rounded.FullscreenExit
                           },
                            contentDescription = if (isFullscreen) "Fullscreen" else "Exit Fullscreen",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onPipClick) {
                        Icon(
                            Icons.Rounded.PictureInPicture, contentDescription = "PiP", tint = Color.White,modifier = Modifier.size(24.dp))
                    }
                    Text(
                        text = if (currentSpeed == 1f) "1.0x" else "${"%.2f".format(currentSpeed).trimEnd('0').trimEnd('.')}x",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clickable { onSpeedClick() }
                    )
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = {
                            menuExpanded = true
                            onMenuExpandedChange(true)
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {
                                menuExpanded = false
                                onMenuExpandedChange(false)
                            },
                            modifier = Modifier.background(Color(0xFF1E1E1E))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onMenuExpandedChange(false)
                                    onShareClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Download", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onMenuExpandedChange(false)
                                    onDownloadClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Online Catalogue", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = Color.White)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onMenuExpandedChange(false)
                                    onCatalogueClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onMenuExpandedChange(false)
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }

                // Center Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(48.dp)
                ) {
                    IconButton(onClick = { player.seekBack() }) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(48.dp))
                    }

                    var isPlaying by remember { mutableStateOf(player.isPlaying) }
                    DisposableEffect(player) {
                        val listener = object : Player.Listener {
                            override fun onIsPlayingChanged(playing: Boolean) {
                                isPlaying = playing
                            }
                        }
                        player.addListener(listener)
                        onDispose { player.removeListener(listener) }
                    }

                    IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                        Icon(
                            if (isPlaying) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled,
                            contentDescription = "Play/Pause",
                            tint = ElectricGreen,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    IconButton(onClick = { player.seekForward() }) {
                        Icon(Icons.Rounded.Forward10, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }

                // Bottom Bar Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Bottom Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    var position by remember { mutableLongStateOf(player.currentPosition) }
                    var duration by remember { mutableLongStateOf(player.duration) }
                    var bufferedPosition by remember { mutableLongStateOf(player.bufferedPosition) }

                    LaunchedEffect(player) {
                        while (true) {
                            position = player.currentPosition
                            duration = player.duration
                            bufferedPosition = player.bufferedPosition
                            delay(500)
                        }
                    }

                    // Seekbar with buffering
                    Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
                        if (duration > 0) {
                            LinearProgressIndicator(
                                progress = (bufferedPosition.toFloat() / duration).coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .padding(horizontal = 12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                trackColor = Color.Transparent
                            )
                        }
                        
                        Slider(
                            value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                            onValueChange = { player.seekTo((it * duration).toLong()) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricGreen,
                                activeTrackColor = ElectricGreen,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Time Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(position)} / ${formatTime(duration)}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Individual Functionality Buttons with Icons & Labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aspect Label with Icon
                        PlayerActionButton(
                            icon = Icons.Rounded.AspectRatio,
                            label = "Aspect",
                            onClick = onAspectClick
                        )

                        // Audio Label with Icon
                        PlayerActionButton(
                            icon = Icons.Rounded.Audiotrack,
                            label = "Audio",
                            onClick = onAudioClick
                        )

                        // Subtitles Label with Icon
                        PlayerActionButton(
                            icon = Icons.Rounded.Subtitles,
                            label = "Subtitles",
                            onClick = onSubtitlesClick
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Lock Button
            IconButton(
                onClick = onLockToggle,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                Icon(
                    if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = "Lock",
                    tint = if (isLocked) ElectricGreen else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // Mini progress bar when controls are hidden
            var position by remember { mutableLongStateOf(player.currentPosition) }
            var duration by remember { mutableLongStateOf(player.duration) }
            LaunchedEffect(player) {
                while (true) {
                    position = player.currentPosition
                    duration = player.duration
                    delay(1000)
                }
            }
            if (duration > 0 && !isLocked) {
                LinearProgressIndicator(
                    progress = (position.toFloat() / duration).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(2.dp),
                    color = ElectricGreen,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = Color.White.copy(alpha = 0.12f),
        contentColor = Color.White,
        modifier = modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ElectricGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

enum class PlayerDetailType {
    SPEED,
    ASPECT,
    AUDIO,
    SUBTITLES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackDetailBottomSheet(
    type: PlayerDetailType,
    player: Player,
    currentSpeed: Float,
    currentResizeMode: Int,
    onSpeedChange: (Float) -> Unit,
    onResizeModeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161616),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with Icon, Title, and Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = when (type) {
                            PlayerDetailType.SPEED -> Icons.Rounded.Speed
                            PlayerDetailType.ASPECT -> Icons.Rounded.AspectRatio
                            PlayerDetailType.AUDIO -> Icons.Rounded.Audiotrack
                            PlayerDetailType.SUBTITLES -> Icons.Rounded.Subtitles
                        },
                        contentDescription = null,
                        tint = ElectricGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = when (type) {
                            PlayerDetailType.SPEED -> "Playback Speed"
                            PlayerDetailType.ASPECT -> "Aspect Ratio"
                            PlayerDetailType.AUDIO -> "Audio Track"
                            PlayerDetailType.SUBTITLES -> "Subtitles"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Divider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White.copy(alpha = 0.1f)
            )

            // Content according to the label clicked
            when (type) {
                PlayerDetailType.SPEED -> SpeedSettings(currentSpeed, onSpeedChange)
                PlayerDetailType.ASPECT -> AspectSettings(currentResizeMode, onResizeModeChange)
                PlayerDetailType.AUDIO -> TrackSettings(player, C.TRACK_TYPE_AUDIO)
                PlayerDetailType.SUBTITLES -> TrackSettings(player, C.TRACK_TYPE_TEXT)
            }
        }
    }
}

@Composable
fun PlaybackDetailDialog(
    type: PlayerDetailType,
    player: Player,
    currentSpeed: Float,
    currentResizeMode: Int,
    onSpeedChange: (Float) -> Unit,
    onResizeModeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (type) {
                                PlayerDetailType.SPEED -> Icons.Rounded.Speed
                                PlayerDetailType.ASPECT -> Icons.Rounded.AspectRatio
                                PlayerDetailType.AUDIO -> Icons.Rounded.Audiotrack
                                PlayerDetailType.SUBTITLES -> Icons.Rounded.Subtitles
                            },
                            contentDescription = null,
                            tint = ElectricGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = when (type) {
                                PlayerDetailType.SPEED -> "Playback Speed"
                                PlayerDetailType.ASPECT -> "Aspect Ratio"
                                PlayerDetailType.AUDIO -> "Audio Track"
                                PlayerDetailType.SUBTITLES -> "Subtitles"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Content
                when (type) {
                    PlayerDetailType.SPEED -> SpeedSettings(currentSpeed, onSpeedChange)
                    PlayerDetailType.ASPECT -> AspectSettings(currentResizeMode, onResizeModeChange)
                    PlayerDetailType.AUDIO -> TrackSettings(player, C.TRACK_TYPE_AUDIO)
                    PlayerDetailType.SUBTITLES -> TrackSettings(player, C.TRACK_TYPE_TEXT)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SpeedSettings(currentSpeed: Float, onSpeedChange: (Float) -> Unit) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    LazyColumn {
        items(speeds) { speed ->
            val label = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x"
            SettingsItem(
                text = label,
                isSelected = currentSpeed == speed,
                onClick = { onSpeedChange(speed) }
            )
        }
    }
}

@Composable
fun AspectSettings(currentMode: Int, onResizeModeChange: (Int) -> Unit) {
    val modes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit (Original Aspect)",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch (Fill Screen)",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop (Zoom to Fill)",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH to "Fixed Width",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT to "Fixed Height"
    )
    LazyColumn {
        items(modes) { (mode, label) ->
            SettingsItem(
                text = label,
                isSelected = currentMode == mode,
                onClick = { onResizeModeChange(mode) }
            )
        }
    }
}

@Composable
fun TrackSettings(player: Player, trackType: Int) {
    var currentTracks by remember { mutableStateOf(player.currentTracks) }
    var trackParameters by remember { mutableStateOf(player.trackSelectionParameters) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }
            override fun onTrackSelectionParametersChanged(parameters: androidx.media3.common.TrackSelectionParameters) {
                trackParameters = parameters
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(currentTracks, trackType) {
        if (trackType == C.TRACK_TYPE_AUDIO && !hasSelectionOverride(player, C.TRACK_TYPE_AUDIO)) {
            selectFirstCompatibleAudioTrack(player, currentTracks)
        }
    }

    val trackGroups = remember(currentTracks, trackType) {
        val list = mutableListOf<Tracks.Group>()
        for (group in currentTracks.groups) {
            if (group.type == trackType) {
                list.add(group)
            }
        }
        list
    }

    if (trackGroups.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (trackType == C.TRACK_TYPE_AUDIO) "No audio tracks available" else "No subtitles available",
                color = Color.Gray
            )
        }
    } else {
        val isTextTrack = trackType == C.TRACK_TYPE_TEXT
        val isSubtitlesDisabled = trackParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        val firstCompatibleTrack = remember(currentTracks, trackType) {
            if (trackType == C.TRACK_TYPE_AUDIO) findFirstCompatibleAudioTrack(currentTracks) else null
        }
        val hasAnyAudioSelected = remember(trackGroups, trackParameters) {
            if (trackType == C.TRACK_TYPE_AUDIO) {
                trackGroups.any { g ->
                    (0 until g.length).any { idx ->
                        g.isTrackSelected(idx) || isTrackOverridden(trackParameters, g, idx)
                    }
                }
            } else false
        }

        LazyColumn {
            if (isTextTrack) {
                item {
                    SettingsItem(
                        text = "Off",
                        isSelected = isSubtitlesDisabled,
                        onClick = {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                .build()
                        }
                    )
                }
            }
            items(trackGroups) { group ->
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = formatTrackLabel(format, i)
                    val isSelected = if (isTextTrack) {
                        !isSubtitlesDisabled && (group.isTrackSelected(i) || isTrackOverridden(trackParameters, group, i))
                    } else {
                        val isDirectlySelected = group.isTrackSelected(i) || isTrackOverridden(trackParameters, group, i)
                        if (isDirectlySelected) {
                            true
                        } else if (!hasAnyAudioSelected && firstCompatibleTrack != null) {
                            group == firstCompatibleTrack.first && i == firstCompatibleTrack.second
                        } else {
                            false
                        }
                    }
                    SettingsItem(
                        text = label,
                        isSelected = isSelected,
                        onClick = {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
                                .setTrackTypeDisabled(trackType, false)
                                .setOverrideForType(
                                    androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i)
                                )
                                .build()
                        }
                    )
                }
            }
        }
    }
}

private fun formatTrackLabel(format: androidx.media3.common.Format, index: Int): String {
    val lang = format.language
    val displayLanguage = if (!lang.isNullOrBlank()) {
        try {
            java.util.Locale.forLanguageTag(lang).getDisplayLanguage(java.util.Locale.getDefault()).takeIf { it.isNotBlank() } ?: lang
        } catch (_: Exception) {
            lang
        }
    } else null

    val label = format.label
    return when {
        !label.isNullOrBlank() && !displayLanguage.isNullOrBlank() && !label.equals(displayLanguage, ignoreCase = true) -> "$displayLanguage ($label)"
        !displayLanguage.isNullOrBlank() -> displayLanguage
        !label.isNullOrBlank() -> label
        else -> "Track ${index + 1}"
    }
}

private fun findFirstCompatibleAudioTrack(tracks: Tracks): Pair<Tracks.Group, Int>? {
    val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    // First priority: fully supported track (FORMAT_HANDLED)
    for (group in audioGroups) {
        for (i in 0 until group.length) {
            if (group.isTrackSupported(i, false)) {
                return Pair(group, i)
            }
        }
    }
    // Fallback: track exceeding capabilities but supported codec
    for (group in audioGroups) {
        for (i in 0 until group.length) {
            if (group.isTrackSupported(i, true)) {
                return Pair(group, i)
            }
        }
    }
    return null
}

private fun selectFirstCompatibleAudioTrack(player: Player, tracks: Tracks): Boolean {
    if (hasSelectionOverride(player, C.TRACK_TYPE_AUDIO)) {
        return true
    }
    val firstCompatible = findFirstCompatibleAudioTrack(tracks) ?: return false
    player.trackSelectionParameters = player.trackSelectionParameters
        .buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .setOverrideForType(
            androidx.media3.common.TrackSelectionOverride(firstCompatible.first.mediaTrackGroup, firstCompatible.second)
        )
        .build()
    return true
}

private fun hasSelectionOverride(player: Player, trackType: Int): Boolean {
    val overrides = player.trackSelectionParameters.overrides
    if (overrides.keys.any { it.type == trackType }) {
        return true
    }
    for (group in player.currentTracks.groups) {
        if (group.type == trackType && overrides.containsKey(group.mediaTrackGroup)) {
            return true
        }
    }
    return false
}

private fun isTrackOverridden(
    parameters: androidx.media3.common.TrackSelectionParameters,
    group: Tracks.Group,
    trackIndex: Int
): Boolean {
    val override = parameters.overrides[group.mediaTrackGroup]
    return override != null && override.trackIndices.contains(trackIndex)
}

@Composable
fun SettingsItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (isSelected) ElectricGreen else Color.White,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = ElectricGreen)
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
