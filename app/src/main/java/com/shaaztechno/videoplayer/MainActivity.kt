@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)
package com.shaaztechno.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shaaztechno.videoplayer.data.local.UserSettings
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.domain.model.VideoType
import com.shaaztechno.videoplayer.presentation.addvideo.AddVideoScreen
import com.shaaztechno.videoplayer.presentation.addvideo.AddVideoViewModel
import com.shaaztechno.videoplayer.presentation.downloads.DownloadsScreen
import com.shaaztechno.videoplayer.presentation.downloads.DownloadsViewModel
import com.shaaztechno.videoplayer.presentation.home.ContinueWatchingScreen
import com.shaaztechno.videoplayer.presentation.home.HomeScreen
import com.shaaztechno.videoplayer.presentation.home.HomeViewModel
import com.shaaztechno.videoplayer.presentation.instagram.InstagramScreen
import com.shaaztechno.videoplayer.presentation.instagram.InstagramViewModel
import com.shaaztechno.videoplayer.presentation.library.FolderVideosScreen
import com.shaaztechno.videoplayer.presentation.library.FolderVideosViewModel
import com.shaaztechno.videoplayer.presentation.library.LibraryScreen
import com.shaaztechno.videoplayer.presentation.library.LibraryViewModel
import com.shaaztechno.videoplayer.presentation.navigation.Screen
import com.shaaztechno.videoplayer.presentation.player.PlayerActivity
import com.shaaztechno.videoplayer.presentation.playlist.PlaylistDetailScreen
import com.shaaztechno.videoplayer.presentation.playlist.PlaylistDetailViewModel
import com.shaaztechno.videoplayer.presentation.playlist.PlaylistScreen
import com.shaaztechno.videoplayer.presentation.playlist.PlaylistViewModel
import com.shaaztechno.videoplayer.presentation.search.SearchScreen
import com.shaaztechno.videoplayer.presentation.search.SearchViewModel
import com.shaaztechno.videoplayer.presentation.settings.SettingsScreen
import com.shaaztechno.videoplayer.presentation.settings.SettingsViewModel
import com.shaaztechno.videoplayer.presentation.splash.SplashScreen
import com.shaaztechno.videoplayer.presentation.videourl.VideoUrlScreen
import com.shaaztechno.videoplayer.presentation.videourl.VideoUrlViewModel
import com.shaaztechno.videoplayer.ui.theme.SZPlayerTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private val isInPipMode = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val app = applicationContext as SZPlayerApplication

        // Handle intent on initial creation
        handleIntent(intent)

        setContent {
            val settings by app.settingsDataStore.settingsFlow.collectAsState(initial = UserSettings())
            val darkMode = settings.darkMode

            SZPlayerTheme(darkMode = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(isInPipMode.value)
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode.value = isInPictureInPictureMode
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_SEND) {
            val uri = if (intent.action == Intent.ACTION_SEND) {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            } else {
                intent.data
            } ?: return
            
            Log.d("MainActivity", "Handling Intent URI: $uri")
            val app = applicationContext as SZPlayerApplication
            lifecycleScope.launch {
                val videoId = uri.toString()
                val existingVideo = app.videoRepository.getVideoById(videoId)
                
                if (existingVideo == null) {
                    var title = "External Video"
                    try {
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1 && cursor.moveToFirst()) {
                                title = cursor.getString(nameIndex)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error resolving title", e)
                        title = uri.lastPathSegment ?: "External Video"
                    }

                    val type = if (uri.scheme?.startsWith("http") == true) VideoType.ONLINE else VideoType.LOCAL
                    val externalVideo = Video(
                        id = videoId,
                        title = title,
                        url = uri.toString(),
                        type = type,
                        localUri = if (type == VideoType.LOCAL) uri.toString() else null,
                        dateAdded = System.currentTimeMillis() / 1000
                    )
                    app.videoRepository.addVideo(externalVideo)
                }
                
                PlayerActivity.start(this@MainActivity, videoId)
            }
        }
    }

    fun shareVideo(video: Video) {
        val sendIntent: Intent = Intent().apply {
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
                            this@MainActivity,
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
}

@Composable
fun MainScreen(isInPipMode: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val activity = context as? MainActivity

    val items = listOf(
        BottomNavItem(Screen.Home, Icons.Default.Home, "Home"),
        BottomNavItem(Screen.Library, Icons.Default.Folder, "Library"),
        BottomNavItem(Screen.Playlists, Icons.Default.PlaylistPlay, "Playlists"),
        BottomNavItem(Screen.Downloads, Icons.Default.Download, "Downloads"),
        BottomNavItem(Screen.Settings, Icons.Default.GridView, "More")
    )

    val showBottomBar = !isInPipMode && itemExistsInBottomNav(currentDestination?.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route?.startsWith(item.screen.route.split("/")[0]) == true
                        } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                                )
                            },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.shaaztechno.videoplayer.ui.theme.ElectricGreen,
                                selectedTextColor = com.shaaztechno.videoplayer.ui.theme.ElectricGreen,
                                indicatorColor = com.shaaztechno.videoplayer.ui.theme.PillGreen,
                                unselectedIconColor = com.shaaztechno.videoplayer.ui.theme.MutedGray,
                                unselectedTextColor = com.shaaztechno.videoplayer.ui.theme.MutedGray
                            ),
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 4 }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(
                route = Screen.Splash.route,
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                SplashScreen(
                    onSplashComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Screen.Home.route,
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(220)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(220)) }
            ) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.videoRepository, app.database.playlistDao()))
                HomeScreen(
                    viewModel = viewModel, 
                    onVideoClick = { video ->
                        PlayerActivity.start(context, video.id)
                    },
                    onAddVideoClick = {
                        navController.navigate(Screen.AddVideo.route)
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    },
                    onNavigateToLibrary = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToPlaylists = {
                        navController.navigate(Screen.Playlists.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToContinueWatching = {
                        navController.navigate(Screen.ContinueWatching.route)
                    },
                    onPlaylistClick = { id, name ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(id, name))
                    },
                    onNavigateToVideoUrl = {
                        navController.navigate(Screen.VideoUrl.route)
                    },
                    onNavigateToInstagram = {
                        navController.navigate(Screen.Instagram.route)
                    },
                    onFolderClick = { folderName ->
                        navController.navigate(Screen.FolderVideos.createRoute(folderName))
                    }
                )
            }
            composable(
                route = Screen.FolderVideos.route,
                arguments = listOf(
                    navArgument("folderName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val folderName = backStackEntry.arguments?.getString("folderName") ?: return@composable
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: FolderVideosViewModel = viewModel(
                    factory = FolderVideosViewModel.Factory(folderName, app.videoRepository, app.database.playlistDao())
                )
                FolderVideosScreen(
                    folderName = folderName,
                    viewModel = viewModel,
                    onVideoClick = { video ->
                        PlayerActivity.start(context, video.id)
                    },
                    onBack = { navController.popBackStack() },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
            }
            composable(Screen.ContinueWatching.route) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.videoRepository, app.database.playlistDao()))
                ContinueWatchingScreen(
                    viewModel = viewModel,
                    onVideoClick = { video ->
                        PlayerActivity.start(context, video.id)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
            }
            composable(
                route = Screen.Library.route,
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(220)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(220)) }
            ) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(app.videoRepository, app.database.playlistDao()))
                LibraryScreen(
                    viewModel = viewModel, 
                    onVideoClick = { video ->
                        PlayerActivity.start(context, video.id)
                    },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    },
                    onFolderClick = { folderName ->
                        navController.navigate(Screen.FolderVideos.createRoute(folderName))
                    }
                )
            }
            composable(
                route = Screen.Downloads.route,
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(220)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(220)) }
            ) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: DownloadsViewModel = viewModel(
                    factory = DownloadsViewModel.Factory(app.videoRepository, app.szDownloadManager)
                )
                DownloadsScreen(
                    viewModel = viewModel,
                    onVideoClick = { video ->
                        PlayerActivity.start(context, video.id)
                    },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
            }
            composable(
                route = Screen.Playlists.route,
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(220)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(220)) }
            ) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: PlaylistViewModel = viewModel(factory = PlaylistViewModel.Factory(app.database.playlistDao(), app.videoRepository))
                PlaylistScreen(viewModel, onPlaylistClick = { playlist ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id, playlist.name))
                })
            }
            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(
                    navArgument("playlistId") { type = NavType.LongType },
                    navArgument("playlistName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                val playlistName = backStackEntry.arguments?.getString("playlistName") ?: ""
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: PlaylistDetailViewModel = viewModel(
                    factory = PlaylistDetailViewModel.Factory(playlistId, app.database.playlistDao())
                )
                PlaylistDetailScreen(
                    playlistName = playlistName,
                    viewModel = viewModel,
                    onVideoClick = { video ->
                        PlayerActivity.start(context, video.id)
                    },
                    onBack = { navController.popBackStack() },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
            }
            composable(
                route = Screen.Settings.route,
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(220)) },
                popEnterTransition = { fadeIn(animationSpec = tween(220)) },
                popExitTransition = { fadeOut(animationSpec = tween(220)) }
            ) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(app.settingsDataStore, app.videoRepository))
                SettingsScreen(viewModel)
            }
            composable(Screen.AddVideo.route) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: AddVideoViewModel = viewModel(factory = AddVideoViewModel.Factory(app.videoRepository))
                AddVideoScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onBrowseDeviceVideos = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onNavigateToUrlScreen = {
                        navController.navigate(Screen.VideoUrl.route)
                    },
                    onNavigateToInstagram = {
                        navController.navigate(Screen.Instagram.route)
                    }
                )
            }
            composable(Screen.VideoUrl.route) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: VideoUrlViewModel = viewModel(
                    factory = VideoUrlViewModel.Factory(
                        app.checkVideoUrlUseCase,
                        app.videoRepository,
                        app.szDownloadManager
                    )
                )
                VideoUrlScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToPlayer = { videoId ->
                        PlayerActivity.start(context, videoId)
                    },
                    onNavigateToDownloads = {
                        navController.navigate(Screen.Downloads.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Instagram.route) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: InstagramViewModel = viewModel(
                    factory = InstagramViewModel.Factory(
                        app.getInstagramReelUseCase,
                        app.szDownloadManager
                    )
                )
                InstagramScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenVideo = { videoId ->
                        PlayerActivity.start(context, videoId)
                    }
                )
            }
            composable(Screen.Search.route) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory(app.videoRepository))
                SearchScreen(
                    viewModel = viewModel,
                    onVideoClick = { video ->
                        PlayerActivity.start(context, video.id)
                    },
                    onBack = { navController.popBackStack() },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
            }
            composable(
                route = Screen.Player.route
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId")
                LaunchedEffect(videoId) {
                    if (videoId != null) {
                        PlayerActivity.start(context, videoId)
                    }
                    navController.popBackStack()
                }
            }
        }
    }
}

private fun itemExistsInBottomNav(route: String?): Boolean {
    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.Library.route,
        Screen.Playlists.route,
        Screen.Downloads.route,
        Screen.Settings.route
    )
    return bottomNavRoutes.contains(route)
}

data class BottomNavItem(
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
