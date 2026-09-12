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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory(videoId, app.videoRepository))
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
    
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    
    var showSettingsSheet by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Hide controls after delay
    LaunchedEffect(showControls, isLocked, showSettingsSheet) {
        if (showControls && !showSettingsSheet && !isLocked) {
            delay(4000)
            showControls = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
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
            val mediaItem = MediaItem.fromUri(video.url)
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
                                // Volume (Right side)
                                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                val delta = -(dragAmount.y / height * maxVolume).toInt()
                                val nextVolume = (currentVolume + delta).coerceIn(0, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVolume, 0)
                                gestureText = "Volume: ${(nextVolume.toFloat() / maxVolume * 100).toInt()}%"
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
                isVisible = showControls,
                onSettingsClick = { showSettingsSheet = true },
                onResizeModeChange = {
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
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
                        modifier = Modifier.padding(24.dp)
                    ) {
                        gestureIcon?.let { icon ->
                            Icon(icon, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            text = text,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }

            if (showSettingsSheet) {
                PlaybackSettingsBottomSheet(
                    player = exoPlayer,
                    currentSpeed = playbackSpeed,
                    currentResizeMode = resizeMode,
                    onSpeedChange = { playbackSpeed = it },
                    onResizeModeChange = { resizeMode = it },
                    onDismiss = { showSettingsSheet = false }
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
    isVisible: Boolean,
    onSettingsClick: () -> Unit,
    onResizeModeChange: () -> Unit
) {
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
                    IconButton(onClick = onResizeModeChange) {
                        Icon(Icons.Rounded.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                    }
                    IconButton(onClick = onPipClick) {
                        Icon(Icons.Rounded.PictureInPicture, contentDescription = "PiP", tint = Color.White)
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Color.White)
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
                        .height(150.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
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
                        
                        // Playback Speed Button (Quick access)
                        Button(
                            onClick = onSettingsClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = ElectricGreen
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${"%.2f".format(player.playbackParameters.speed)}x",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp)) // Space for lock button
                }
            }

            // Lock Button
            IconButton(
                onClick = onLockToggle,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsBottomSheet(
    player: Player,
    currentSpeed: Float,
    currentResizeMode: Int,
    onSpeedChange: (Float) -> Unit,
    onResizeModeChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = ElectricGreen,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ElectricGreen
                        )
                    }
                },
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Speed", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Aspect", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("Audio", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                    Text("Subs", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                0 -> SpeedSettings(currentSpeed, onSpeedChange)
                1 -> AspectSettings(currentResizeMode, onResizeModeChange)
                2 -> TrackSettings(player, C.TRACK_TYPE_AUDIO)
                3 -> TrackSettings(player, C.TRACK_TYPE_TEXT)
            }
        }
    }
}

@Composable
fun SpeedSettings(currentSpeed: Float, onSpeedChange: (Float) -> Unit) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    LazyColumn {
        items(speeds) { speed ->
            SettingsItem(
                text = "${speed}x",
                isSelected = currentSpeed == speed,
                onClick = { onSpeedChange(speed) }
            )
        }
    }
}

@Composable
fun AspectSettings(currentMode: Int, onResizeModeChange: (Int) -> Unit) {
    val modes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop",
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
    val tracks = player.currentTracks
    val trackGroups = mutableListOf<Tracks.Group>()
    
    for (group in tracks.groups) {
        if (group.type == trackType) {
            trackGroups.add(group)
        }
    }

    if (trackGroups.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No tracks available", color = Color.Gray)
        }
    } else {
        LazyColumn {
            item {
                SettingsItem(
                    text = if (trackType == C.TRACK_TYPE_TEXT) "Off" else "Default",
                    isSelected = !hasSelectionOverride(player, trackType),
                    onClick = {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(trackType)
                            .build()
                    }
                )
            }
            items(trackGroups) { group ->
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = format.language ?: format.label ?: "Track ${i + 1}"
                    SettingsItem(
                        text = label,
                        isSelected = group.isTrackSelected(i),
                        onClick = {
                            player.trackSelectionParameters = player.trackSelectionParameters
                                .buildUpon()
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

private fun hasSelectionOverride(player: Player, trackType: Int): Boolean {
    val overrides = player.trackSelectionParameters.overrides
    for (group in player.currentTracks.groups) {
        if (group.type == trackType && overrides.containsKey(group.mediaTrackGroup)) {
            return true
        }
    }
    return false
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
