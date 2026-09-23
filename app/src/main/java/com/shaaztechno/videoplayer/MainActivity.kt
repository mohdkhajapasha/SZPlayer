package com.shaaztechno.videoplayer

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.shaaztechno.videoplayer.presentation.player.PlayerScreen
import com.shaaztechno.videoplayer.presentation.playlist.PlaylistDetailScreen
import com.shaaztechno.videoplayer.presentation.playlist.PlaylistDetailViewModel
import com.shaaztechno.videoplayer.presentation.playlist.PlaylistScreen
import com.shaaztechno.videoplayer.presentation.playlist.PlaylistViewModel
import com.shaaztechno.videoplayer.presentation.search.SearchScreen
import com.shaaztechno.videoplayer.presentation.search.SearchViewModel
import com.shaaztechno.videoplayer.presentation.settings.SettingsScreen
import com.shaaztechno.videoplayer.presentation.settings.SettingsViewModel
import com.shaaztechno.videoplayer.presentation.videourl.VideoUrlScreen
import com.shaaztechno.videoplayer.presentation.videourl.VideoUrlViewModel
import com.shaaztechno.videoplayer.presentation.whatsapp.StatusPreviewScreen
import com.shaaztechno.videoplayer.presentation.whatsapp.WhatsAppStatusScreen
import com.shaaztechno.videoplayer.presentation.whatsapp.WhatsAppStatusViewModel
import com.shaaztechno.videoplayer.ui.theme.SZPlayerTheme
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val isInPipMode = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val app = applicationContext as SZPlayerApplication
        
        // We can keep the splash screen visible until we have the settings
        // However, runBlocking here is fine as it's a very fast read from DataStore
        // and it avoids the flicker.
        val initialSettings = runBlocking { app.settingsDataStore.settingsFlow.first() }

        setContent {
            val settings by app.settingsDataStore.settingsFlow.collectAsState(initial = initialSettings)
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
                } catch (e: Exception) {
                    putExtra(Intent.EXTRA_TEXT, "Check out this video: ${video.title}\n${video.url}")
                    type = "text/plain"
                }
            }
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Video")
        startActivity(shareIntent)
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
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (enabled) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.videoRepository, app.database.playlistDao()))
                HomeScreen(
                    viewModel = viewModel, 
                    onVideoClick = { video ->
                        navController.navigate(Screen.Player.createRoute(video.id))
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
                    onNavigateToWhatsAppStatus = {
                        navController.navigate(Screen.WhatsAppStatus.route)
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
                        navController.navigate(Screen.Player.createRoute(video.id))
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
                        navController.navigate(Screen.Player.createRoute(video.id))
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
            }
            composable(Screen.Library.route) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(app.videoRepository, app.database.playlistDao()))
                LibraryScreen(
                    viewModel = viewModel, 
                    onVideoClick = { video ->
                        navController.navigate(Screen.Player.createRoute(video.id))
                    },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
            }
            composable(Screen.Downloads.route) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: DownloadsViewModel = viewModel(
                    factory = DownloadsViewModel.Factory(app.videoRepository, app.szDownloadManager)
                )
                DownloadsScreen(
                    viewModel = viewModel,
                    onVideoClick = { video ->
                        navController.navigate(Screen.Player.createRoute(video.id))
                    },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
            }
            composable(Screen.Playlists.route) {
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
                        navController.navigate(Screen.Player.createRoute(video.id))
                    },
                    onBack = { navController.popBackStack() },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
            }
            composable(Screen.Settings.route) {
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
                    },
                    onNavigateToWhatsAppStatus = {
                        navController.navigate(Screen.WhatsAppStatus.route)
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
                        navController.navigate(Screen.Player.createRoute(videoId)) {
                            popUpTo(Screen.VideoUrl.route) { inclusive = true }
                        }
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
                        navController.navigate(Screen.Player.createRoute(videoId)) {
                            popUpTo(Screen.Instagram.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.WhatsAppStatus.route) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: WhatsAppStatusViewModel = viewModel(
                    factory = WhatsAppStatusViewModel.Factory(app.whatsappStatusRepository)
                )
                WhatsAppStatusScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onStatusClick = { status ->
                        navController.navigate(Screen.StatusPreview.createRoute(status.uri.toString(), status.isVideo))
                    }
                )
            }
            composable(
                route = Screen.StatusPreview.route,
                arguments = listOf(
                    navArgument("uri") { type = NavType.StringType },
                    navArgument("isVideo") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val uri = backStackEntry.arguments?.getString("uri") ?: ""
                val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: WhatsAppStatusViewModel = viewModel(
                    factory = WhatsAppStatusViewModel.Factory(app.whatsappStatusRepository)
                )
                
                val uiState by viewModel.uiState.collectAsState()
                val status = remember(uiState.statuses, uri) {
                    uiState.statuses.find { it.uri.toString() == uri }
                }
                
                StatusPreviewScreen(
                    uriString = uri,
                    isVideo = isVideo,
                    onBack = { navController.popBackStack() },
                    onSave = {
                        status?.let {
                            viewModel.saveStatus(it) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onShare = {
                        status?.let { viewModel.shareStatus(it) }
                    }
                )
            }
            composable(Screen.Search.route) {
                val app = context.applicationContext as SZPlayerApplication
                val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory(app.videoRepository))
                SearchScreen(
                    viewModel = viewModel,
                    onVideoClick = { video ->
                        navController.navigate(Screen.Player.createRoute(video.id))
                    },
                    onBack = { navController.popBackStack() },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
            }
            composable(Screen.Player.route) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
                PlayerScreen(
                    videoId = videoId,
                    onBack = { navController.popBackStack() },
                    onPipClick = { activity?.enterPipMode() },
                    onShareClick = { video ->
                        activity?.shareVideo(video)
                    }
                )
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
