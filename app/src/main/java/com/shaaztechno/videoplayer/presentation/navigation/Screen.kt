package com.shaaztechno.videoplayer.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Downloads : Screen("downloads")
    object Playlists : Screen("playlists")
    object PlaylistDetail : Screen("playlist/{playlistId}/{playlistName}") {
        fun createRoute(playlistId: Long, playlistName: String) = "playlist/$playlistId/$playlistName"
    }
    object Settings : Screen("settings")
    object AddVideo : Screen("add_video")
    object VideoUrl : Screen("video_url")
    object Search : Screen("search")
    object Player : Screen("player/{videoId}") {
        fun createRoute(videoId: String) = "player/$videoId"
    }
}
