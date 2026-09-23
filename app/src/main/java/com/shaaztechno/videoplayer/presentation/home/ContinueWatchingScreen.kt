package com.shaaztechno.videoplayer.presentation.home

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.ui.theme.ElectricGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinueWatchingScreen(
    viewModel: HomeViewModel,
    onVideoClick: (Video) -> Unit,
    onBackClick: () -> Unit,
    onShareClick: (Video) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recently Played",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 20.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.recentlyPlayed.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No history found", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.recentlyPlayed) { history ->
                        val progress = if (history.duration > 0) history.lastPosition.toFloat() / history.duration else 0f
                        VideoListItem(
                            video = history.toVideo(),
                            progress = progress,
                            onClick = { onVideoClick(history.toVideo()) },
                            onShare = { onShareClick(history.toVideo()) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}
