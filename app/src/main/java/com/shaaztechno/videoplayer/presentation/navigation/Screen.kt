package com.shaaztechno.videoplayer.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Library : Screen("library")
    object Downloads : Screen("downloads")
    object Playlists : Screen("playlists")
    object PlaylistDetail : Screen("playlist/{playlistId}/{playlistName}") {
        fun createRoute(playlistId: Long, playlistName: String) = 
            "playlist/$playlistId/${Uri.encode(playlistName)}"
    }
    object Settings : Screen("settings")
    object AddVideo : Screen("add_video")
    object VideoUrl : Screen("video_url")
    object Instagram : Screen("instagram")
    object Search : Screen("search")
    object ContinueWatching : Screen("continue_watching")
    object Player : Screen("player/{videoId}") {
        fun createRoute(videoId: String) = "player/${Uri.encode(videoId)}"
    }
    object FolderVideos : Screen("folder_videos/{folderName}") {
        fun createRoute(folderName: String) = "folder_videos/${Uri.encode(folderName)}"
    }
}
