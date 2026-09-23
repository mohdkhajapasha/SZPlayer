package com.shaaztechno.videoplayer.presentation.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.presentation.home.VideoListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistName: String,
    viewModel: PlaylistDetailViewModel,
    onVideoClick: (Video) -> Unit,
    onBack: () -> Unit,
    onShareClick: (Video) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.videos.isEmpty()) {
                Text("No videos in this playlist.", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onBackground)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.videos) { video ->
                        VideoListItem(
                            video = video,
                            onClick = { onVideoClick(video) },
                            onShare = { onShareClick(video) }
                        )
                    }
                }
            }
        }
    }
}
