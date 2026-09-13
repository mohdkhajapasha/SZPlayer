package com.shaaztechno.videoplayer

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.presentation.addvideo.AddVideoScreen
import com.shaaztechno.videoplayer.presentation.addvideo.AddVideoViewModel
import com.shaaztechno.videoplayer.presentation.downloads.DownloadsScreen
import com.shaaztechno.videoplayer.presentation.downloads.DownloadsViewModel
import com.shaaztechno.videoplayer.presentation.home.ContinueWatchingScreen
import com.shaaztechno.videoplayer.presentation.home.HomeScreen
import com.shaaztechno.videoplayer.presentation.home.HomeViewModel
import com.shaaztechno.videoplayer.presentation.instagram.InstagramScreen
import com.shaaztechno.videoplayer.presentation.instagram.InstagramViewModel
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
import com.shaaztechno.videoplayer.ui.theme.SZPlayerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable drawing behind system bars for true full-screen
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        // Make status and navigation bars transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        // Hide system UI bars
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
        setContent {
            SZPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    fun shareVideo(video: Video) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out this video: ${video.title}\n${video.url}")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
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
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val activity = context as? MainActivity
    
    data class BottomNavItem(
        val screen: Screen,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val label: String
    )

    val items = listOf(
        BottomNavItem(Screen.Home, Icons.Default.Home, "Home"),
        BottomNavItem(Screen.Library, Icons.Default.Folder, "Library"),
        BottomNavItem(Screen.Playlists, Icons.Default.PlaylistPlay, "Playlists"),
        BottomNavItem(Screen.Downloads, Icons.Default.Download, "Downloads"),
        BottomNavItem(Screen.Settings, Icons.Default.GridView, "More")
    )

    val showBottomBar = currentDestination?.route != Screen.Player.route &&
                         currentDestination?.route != Screen.AddVideo.route &&
                         currentDestination?.route != Screen.VideoUrl.route &&
                         currentDestination?.route != Screen.Instagram.route &&
                         currentDestination?.route != Screen.Search.route &&
                         currentDestination?.route != Screen.ContinueWatching.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF090D0B),
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
        }
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
