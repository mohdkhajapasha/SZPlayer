package com.shaaztechno.videoplayer.presentation.home

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shaaztechno.videoplayer.R
import com.shaaztechno.videoplayer.data.local.entity.HistoryEntity
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.presentation.components.BannerAd
import com.shaaztechno.videoplayer.ui.theme.*

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
    onNavigateToVideoUrl: () -> Unit = {},
    onNavigateToInstagram: () -> Unit = {},
    onFolderClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                            contentDescription = stringResource(R.string.sz_player_logo_desc),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.sz_player_upper),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 19.sp
                                )
                            )
                            Text(
                                text = stringResource(R.string.tagline),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp,
                                    color = MutedGray,
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
                            contentDescription = stringResource(R.string.search),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddVideoClick,
                containerColor = ElectricGreen,
                contentColor = Black,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_video_desc),
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
            // 0. Hero Banner Carousel
            item {
                BannerCarousel(
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 1. Continue Watching Section (only visible when videos exist to continue watching)
            val activeHistory = uiState.recentlyPlayed.firstOrNull()
            if (activeHistory != null) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = stringResource(R.string.continue_watching),
                        showViewAll = false,
                        onViewAllClick = onNavigateToContinueWatching
                    )
                    ContinueWatchingCard(
                        history = activeHistory,
                        onClick = { onVideoClick(activeHistory.toVideo()) }
                    )
                }
            }

            // 2. Recently Played Section (only visible when there are recently played items to display)
            val historyList = if (uiState.recentlyPlayed.size > 1) {
                uiState.recentlyPlayed.drop(1)
            } else {
                emptyList()
            }

            if (historyList.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = stringResource(R.string.recently_played),
                        showViewAll = true,
                        onViewAllClick = onNavigateToContinueWatching
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyList, key = { it.videoId }) { history ->
                            val progress = if (history.duration > 0) history.lastPosition.toFloat() / history.duration else 0f
                            RecentVideoCard(
                                title = history.title,
                                durationText = formatDuration(history.duration),
                                dateText = formatRelativeDate(context, history.timestamp),
                                thumbnailUrl = history.thumbnailUrl ?: history.url,
                                progress = progress,
                                onClick = { onVideoClick(history.toVideo()) }
                            )
                        }
                    }
                }
            }

            // Ad below recent
            item {
                BannerAd(modifier = Modifier.padding(top = 16.dp))
            }

            // 3. My Playlists Section
            if (uiState.playlists.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader(
                        title = stringResource(R.string.my_playlists),
                        showViewAll = true,
                        onViewAllClick = onNavigateToPlaylists
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.playlists, key = { it.id }) { playlist ->
                            PlaylistHeroCard(
                                name = playlist.name,
                                videoCount = stringResource(R.string.playlist_label),
                                icon = Icons.Default.PlaylistPlay,
                                backdropUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500",
                                onClick = { onPlaylistClick(playlist.id, playlist.name) }
                            )
                        }
                    }
                }
            }

            // 3. Online Catalog Section
            if (uiState.onlineVideos.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ElectricGreen)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = stringResource(R.string.online_catalog),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }

                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            uiState.onlineVideos,
                            key = { it.id }
                        ) { video ->

                            val progress = uiState.historyMap[video.id] ?: 0f

                            OnlineCatalogCard(
                                video = video,
                                progress = progress,
                                onClick = { onVideoClick(video) },
                                optionsContent = {
                                    var showMenu by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = null,
                                            tint = MutedGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Play") },
                                            onClick = {
                                                onVideoClick(video)
                                                showMenu = false
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Share") },
                                            onClick = {
                                                onShareClick(video)
                                                showMenu = false
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            onClick = {
                                                viewModel.deleteVideo(video)
                                                showMenu = false
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
            // Ad above Local Videos
            item {
                BannerAd(modifier = Modifier.padding(top = 16.dp))
            }

        // 4. Local Videos
            val folderList = uiState.folders.entries.toList()

            if (folderList.isNotEmpty()) {

                item {
                    Spacer(modifier = Modifier.height(20.dp))

                    SectionHeader(
                        title = stringResource(R.string.local_videos),
                        showViewAll = folderList.size > 6,
                        onViewAllClick = onNavigateToLibrary
                    )
                }

                // Only preview a maximum of 6 folders on Home.
                // All folders remain available through View All.
                val homeFolders = folderList.take(6)

                items(
                    items = homeFolders,
                    key = { it.key }
                ) { (folderName, videos) ->

                    FolderCard(
                        folderName = folderName,
                        videoCount = videos.size,
                        onClick = {
                            onFolderClick(folderName)
                        },
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 10.dp
                        )
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.no_folders_found),
                        color = MutedGray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 8.dp
                        )
                    )
                }
            }
            // Ad above Local Videos
            item {
                BannerAd(modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

data class BannerItem(
    val id: String,
    val title: String,
    val subtitle: String,
    @androidx.annotation.DrawableRes val imageRes: Int,
    val tag: String,
    val video: Video? = null
)

@Composable
fun BannerCarousel(
    modifier: Modifier = Modifier
) {
    val bannerItems = remember {
        listOf(
            BannerItem(
                id = "instagram_reel",
                title = "Download Instagram Reel",
                subtitle = "Save your favorite reels in just a few simple steps!",
                imageRes = R.drawable.insta_reel_download_banner,
                tag = "Instagram Reel",
                video = null
            ),
            BannerItem(
                id = "video_url",
                title = "Video URL Download or Play Online",
                subtitle = "Just paste the link, check it and enjoy your video!",
                imageRes = R.drawable.video_url_download_banner,
                tag = "Video URL",
                video = null
            )
        )
    }

    val pagerState = rememberPagerState(
        pageCount = { bannerItems.size }
    )

    // Auto-scroll
    LaunchedEffect(Unit) {
        while (true) {
            delay(4500L)

            if (!pagerState.isScrollInProgress) {
                val nextPage =
                    (pagerState.currentPage + 1) % bannerItems.size

                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(120.dp)
    ) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp
        ) { page ->

            BannerCard(
                item = bannerItems[page]
            )
        }

        // ---------------------------------------------------------
        // BANNER INDICATORS
        // Bottom-right, matching the reference design
        // ---------------------------------------------------------
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 18.dp,
                    bottom = 16.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            repeat(bannerItems.size) { index ->

                val isSelected =
                    pagerState.currentPage == index

                Box(
                    modifier = Modifier
                        .size(
                            if (isSelected) {
                                10.dp
                            } else {
                                8.dp
                            }
                        )
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                ElectricGreen
                            } else {
                                InactiveIndicator
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) {
                                ElectricGreen.copy(alpha = 0.35f)
                            } else {
                                Color.White.copy(alpha = 0.12f)
                            },
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun BannerCard(
    item: BannerItem
) {
    Card(
        modifier = Modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            width = 1.dp,
            color = BannerBorder
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            // -----------------------------------------------------
            // LOCAL DRAWABLE IMAGE
            // -----------------------------------------------------
            Image(
                painter = painterResource(
                    id = item.imageRes
                ),
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale =  ContentScale.FillBounds
            )

            // -----------------------------------------------------
            // SUBTLE DARK GRADIENT
            // Keeps the left side darker like the reference
            // -----------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.30f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.05f)
                            ),
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY
                        )
                    )
            )
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
                    text = stringResource(R.string.view_all),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ElectricGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.view_all),
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
    if (history == null) return

    val displayTitle = history.title
    val displayImage = history.thumbnailUrl ?: history.url

    val progress = if (history.duration > 0) {
        (history.lastPosition.toFloat() / history.duration)
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    val progressPercent = (progress * 100).toInt()
    val durationText = formatDuration(history.duration)

    val isOnline = history.type == VideoType.ONLINE.name
    val sourceText = if (isOnline) stringResource(R.string.online_video) else stringResource(R.string.local_videos)
    val sourceIcon = if (isOnline) Icons.Default.Language else Icons.Default.Folder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(125.dp)
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DeepBlueGrey
        ),
        border = BorderStroke(
            width = 1.dp,
            color = BorderBlueGrey
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ---------------------------------------------------------
            // VIDEO THUMBNAIL
            // ---------------------------------------------------------
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141923))
            ) {

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(displayImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark overlay to make the play button stand out
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.12f)
                        )
                )

                // Center play button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(
                            Color.Black.copy(alpha = 0.65f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play_desc),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                        .background(
                            Color.Black.copy(alpha = 0.82f),
                            RoundedCornerShape(5.dp)
                        )
                        .padding(
                            horizontal = 6.dp,
                            vertical = 3.dp
                        )
                ) {
                    Text(
                        text = durationText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Progress indicator on thumbnail bottom
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomStart)
                            .background(
                                Color.White.copy(alpha = 0.20f)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(ElectricGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // ---------------------------------------------------------
            // VIDEO INFORMATION
            // ---------------------------------------------------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {

                // Video title
                Text(
                    text = displayTitle,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Folder/source row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = sourceIcon,
                        contentDescription = null,
                        tint = MutedGray,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = sourceText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MutedGray,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress + percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                ProgressTrackDark
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(ElectricGreen)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "$progressPercent%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ---------------------------------------------------------
            // LARGE PLAY BUTTON
            // ---------------------------------------------------------
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(ElectricGreen)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.play_desc),
                    tint = Black,
                    modifier = Modifier.size(18.dp)
                )
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
                .background(Color(0xFF141923))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
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
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp
                )
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
fun OnlineCatalogCard(
    video: Video,
    progress: Float = 0f,
    onClick: () -> Unit,
    optionsContent: (@Composable BoxScope.() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .width(190.dp)
            .clickable { onClick() }
    ) {

        // Video Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141923))
        ) {

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnailUrl ?: video.url)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Subtle bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )

            // Duration
            if (video.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                        .background(
                            Color.Black.copy(alpha = 0.80f),
                            RoundedCornerShape(5.dp)
                        )
                        .padding(
                            horizontal = 6.dp,
                            vertical = 3.dp
                        )
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Continue-watching progress
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = ElectricGreen,
                    trackColor = Color.Transparent
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Video title and options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = video.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            
            if (optionsContent != null) {
                Box(modifier = Modifier.padding(top = 0.dp)) {
                    optionsContent()
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Metadata
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = video.category
                    ?: video.type.name
                        .lowercase()
                        .replaceFirstChar { it.titlecase() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MutedGray,
                    fontSize = 11.sp
                )
            )

            if (video.duration > 0) {
                Text(
                    text = " • ",
                    color = MutedGray,
                    fontSize = 10.sp
                )

                Text(
                    text = formatDuration(video.duration),
                    maxLines = 1,
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
                            .background(PillGreen.copy(alpha = 0.85f)),
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
                            color = MutedGray,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}



@Composable
fun FolderCard(
    folderName: String,
    videoCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.06f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Folder icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Color.White.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = ElectricGreen,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Folder details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = folderName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (videoCount == 1) {
                        "1 video"
                    } else {
                        "$videoCount videos"
                    },
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = MutedGray
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(22.dp)
            )
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
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141923))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnailUrl ?: video.url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
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
            val typeText = when(video.type) {
                VideoType.ONLINE -> stringResource(R.string.online_video)
                VideoType.LOCAL -> stringResource(R.string.local_video)
                VideoType.DOWNLOADED -> "Downloaded"
            }
            Text(
                text = typeText +
                        if (video.duration > 0) " • ${formatDuration(video.duration)}" else "",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
        }
        // Trailing options button — wrapped in a Box so callers can anchor
        // a DropdownMenu directly to this button (positioned like a leaf).
        Box {
            IconButton(onClick = onShare) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.options_desc), tint = Color.Gray)
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

private fun formatRelativeDate(context: Context, timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = System.currentTimeMillis()
    val timeMs = if (timestamp < 10000000000L) timestamp * 1000 else timestamp
    val diff = now - timeMs
    if (diff < 0) return context.getString(R.string.just_now)
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        minutes < 60 -> if (minutes <= 1) context.getString(R.string.just_now) else context.getString(R.string.min_ago, minutes)
        hours < 24 -> context.getString(R.string.hours_ago, hours)
        days == 1L -> context.getString(R.string.yesterday)
        days < 30 -> context.getString(R.string.days_ago, days)
        days < 365 -> context.getString(R.string.months_ago, days / 30)
        else -> context.getString(R.string.years_ago, days / 365)
    }
}
