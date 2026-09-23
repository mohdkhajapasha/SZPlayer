package com.shaaztechno.videoplayer.presentation.home

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shaaztechno.videoplayer.R
import com.shaaztechno.videoplayer.data.local.entity.HistoryEntity
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.ui.theme.ElectricGreen
import com.shaaztechno.videoplayer.ui.theme.MutedGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onVideoClick: (Video) -> Unit,
    onAddVideoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onShareClick: (Video) -> Unit,
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToContinueWatching: () -> Unit = {},
    onPlaylistClick: (Long, String) -> Unit = { _, _ -> },
    onNavigateToWhatsAppStatus: () -> Unit = {},
    onNavigateToVideoUrl: () -> Unit = {},
    onNavigateToInstagram: () -> Unit = {},
    onFolderClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    // Fix orientation to portrait for HomeScreen
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val videoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_VIDEO] ?: false
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }
        if (videoPermission) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.sz_player_icon),
                            contentDescription = "SZ Player Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SZ PLAYER",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 19.sp
                                )
                            )
                            Text(
                                text = "PLAY  .  ANYTHING  .  ANYWHERE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    color = Color(0xFF7E8A82),
                                    fontSize = 8.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddVideoClick,
                containerColor = ElectricGreen,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Video",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Continue Watching Section
            item {
                SectionHeader(
                    title = "Continue Watching",
                    showViewAll = false,
                    onViewAllClick = onNavigateToContinueWatching
                )
                val activeHistory = uiState.recentlyPlayed.firstOrNull()
                ContinueWatchingCard(
                    history = activeHistory,
                    onClick = {
                        if (activeHistory != null) {
                            onVideoClick(activeHistory.toVideo())
                        } else if (uiState.localVideos.isNotEmpty()) {
                            onVideoClick(uiState.localVideos.first())
                        } else {
                            onNavigateToLibrary()
                        }
                    }
                )
            }

            // 2. Recently Played Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Recently Played",
                    showViewAll = true,
                    onViewAllClick = onNavigateToContinueWatching
                )
                val historyList = if (uiState.recentlyPlayed.size > 1) {
                    uiState.recentlyPlayed.drop(1)
                } else {
                    emptyList()
                }

                if (historyList.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyList) { history ->
                            val progress = if (history.duration > 0) history.lastPosition.toFloat() / history.duration else 0f
                            RecentVideoCard(
                                title = history.title,
                                durationText = formatDuration(history.duration),
                                dateText = formatRelativeDate(history.timestamp),
                                thumbnailUrl = history.thumbnailUrl ?: history.url,
                                progress = progress,
                                onClick = { onVideoClick(history.toVideo()) }
                            )
                        }
                    }
                } else {
                    // Display mockup items matching Image 2 perfectly
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            RecentVideoCard(
                                title = "3.mp4",
                                durationText = "94:48",
                                dateText = "2 days ago",
                                thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500",
                                onClick = {
                                    uiState.localVideos.find { it.title.contains("3") }?.let(onVideoClick) ?: onNavigateToLibrary()
                                }
                            )
                        }
                        item {
                            RecentVideoCard(
                                title = "Video-55092.mp4",
                                durationText = "59:02",
                                dateText = "3 days ago",
                                thumbnailUrl = "https://images.unsplash.com/photo-1564769625905-50e93615e769?w=500",
                                onClick = {
                                    uiState.localVideos.find { it.title.contains("55092") }?.let(onVideoClick) ?: onNavigateToLibrary()
                                }
                            )
                        }
                        item {
                            RecentVideoCard(
                                title = "video.mp4",
                                durationText = "12:34",
                                dateText = "4 days ago",
                                thumbnailUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=500",
                                onClick = {
                                    uiState.localVideos.firstOrNull()?.let(onVideoClick) ?: onNavigateToLibrary()
                                }
                            )
                        }
                    }
                }
            }

            // 3. My Playlists Section
            if (uiState.playlists.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = "My Playlists",
                        showViewAll = true,
                        onViewAllClick = onNavigateToPlaylists
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.playlists) { playlist ->
                            PlaylistHeroCard(
                                name = playlist.name,
                                videoCount = "Playlist",
                                icon = Icons.Default.PlaylistPlay,
                                backdropUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500",
                                onClick = { onPlaylistClick(playlist.id, playlist.name) }
                            )
                        }
                    }
                }
            }

            // 4. Local Videos Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Local Videos",
                    showViewAll = true,
                    onViewAllClick = onNavigateToLibrary
                )
                val folderList = if (uiState.folders.isNotEmpty()) {
                    uiState.folders.entries.toList()
                } else {
                    emptyList()
                }

                if (folderList.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(folderList) { (folderName, videos) ->
                            LocalFolderCard(
                                name = folderName,
                                countText = "${videos.size} videos",
                                onClick = { onFolderClick(folderName) }
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            LocalFolderCard(
                                name = "Movies",
                                countText = "24 videos",
                                onClick = { onFolderClick("Movies") }
                            )
                        }
                        item {
                            LocalFolderCard(
                                name = "Downloads",
                                countText = "8 videos",
                                onClick = { onFolderClick("Downloads") }
                            )
                        }
                        item {
                            LocalFolderCard(
                                name = "Camera",
                                countText = "32 videos",
                                onClick = { onFolderClick("Camera") }
                            )
                        }
                    }
                }
            }

            // 6. Online Videos Section (if available)
            if (uiState.onlineVideos.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(title = "Online Catalog", showViewAll = false)
                }
                items(uiState.onlineVideos) { video ->
                    val progress = uiState.historyMap[video.id] ?: 0f
                    var menuExpanded by remember { mutableStateOf(false) }
                    VideoListItem(
                        video = video,
                        progress = progress,
                        onClick = { onVideoClick(video) },
                        onShare = { menuExpanded = true },
                        dropdownContent = {
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Play", color = MaterialTheme.colorScheme.onSurface) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onVideoClick(video)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share", color = MaterialTheme.colorScheme.onSurface) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onShareClick(video)
                                    }
                                )
                            }
                        }
                    )
                }
            }

            if (uiState.isLoading && uiState.onlineVideos.isEmpty() && uiState.localVideos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricGreen)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    showViewAll: Boolean = false,
    onViewAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Neon Green Vertical Pill Indicator
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ElectricGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
        if (showViewAll) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onViewAllClick() }
            ) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ElectricGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View All",
                    tint = ElectricGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    history: HistoryEntity?,
    onClick: () -> Unit
) {
    val displayTitle = history?.title ?: "Bigg Boss Season 20 Episode 1"
    val displaySubtitle = if (history != null) {
        "Continue Watching • ${formatDuration(history.lastPosition)}"
    } else {
        "Reality Show  •  S20 E1  •  2025"
    }
    val displayImage = history?.thumbnailUrl ?: history?.url
        ?: "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=800"

    val progress = if (history != null && history.duration > 0) {
        (history.lastPosition.toFloat() / history.duration).coerceIn(0f, 1f)
    } else {
        0.054f // 05:05 of 94:48
    }

    val timeLabel = if (history != null) {
        "${formatDuration(history.lastPosition)} / ${formatDuration(history.duration)}"
    } else {
        "05:05 / 94:48"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = displayImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Vignette gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.92f)
                            ),
                            startY = 60f
                        )
                    )
            )

            // "HD" Badge in top-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 0.8.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "HD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }

            // Bottom Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Play Button with neon green circular border
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(BorderStroke(2.dp, ElectricGreen), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = ElectricGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = displaySubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFB0B8B2),
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar and Timestamp Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = ElectricGreen,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFA0A8A2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RecentVideoCard(
    title: String,
    durationText: String,
    dateText: String,
    thumbnailUrl: String,
    progress: Float = 0f,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .height(92.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Duration Pill Overlay on bottom-right of thumbnail
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
            
            // Progress Bar at the bottom of the thumbnail
            if (progress > 0) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = ElectricGreen,
                    trackColor = Color.Transparent
                )
            }
        }

        // Title and 3-dots row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp
                )
            )
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                modifier = Modifier.size(16.dp)
            )
        }

        // Date text
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MutedGray,
                fontSize = 11.sp
            ),
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
fun PlaylistHeroCard(
    name: String,
    videoCount: String,
    icon: ImageVector,
    backdropUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(155.dp)
            .height(105.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Icon circle + Chevron
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF142B18).copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = ElectricGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Bottom Column: Name & Count
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = videoCount,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFB0B0B0),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LocalFolderCard(
    name: String,
    countText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(142.dp)
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MutedGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MutedGray,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MutedGray,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ThemedFolderCard(
    name: String,
    countText: String,
    backdropUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(82.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Content on bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFB0B0B0),
                            fontSize = 10.sp
                        )
                    )
                }
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun VideoListItem(
    video: Video,
    progress: Float = 0f,
    onClick: () -> Unit,
    onShare: () -> Unit = {},
    dropdownContent: (@Composable BoxScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            AsyncImage(
                model = video.thumbnailUrl ?: video.url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            
            if (progress > 0) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ElectricGreen,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = video.type.name.lowercase().replaceFirstChar { it.titlecase() } +
                        if (video.duration > 0) " • ${formatDuration(video.duration)}" else "",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
        }
        // Trailing options button — wrapped in a Box so callers can anchor
        // a DropdownMenu directly to this button (positioned like a leaf).
        Box {
            IconButton(onClick = onShare) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
            }
            dropdownContent?.invoke(this)
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
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

private fun formatRelativeDate(timestamp: Long): String {
    if (timestamp <= 0) return "2 days ago"
    val now = System.currentTimeMillis()
    val timeMs = if (timestamp < 10000000000L) timestamp * 1000 else timestamp
    val diff = now - timeMs
    if (diff < 0) return "Just now"
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        minutes < 60 -> if (minutes <= 1) "Just now" else "$minutes min ago"
        hours < 24 -> "$hours hours ago"
        days == 1L -> "Yesterday"
        days < 30 -> "$days days ago"
        days < 365 -> "${days / 30} months ago"
        else -> "${days / 365} years ago"
    }
}
