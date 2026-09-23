package com.shaaztechno.videoplayer.presentation.downloads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaaztechno.videoplayer.data.downloader.DownloadProgressItem
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.presentation.home.VideoListItem
import com.shaaztechno.videoplayer.ui.theme.BorderSubtle
import com.shaaztechno.videoplayer.ui.theme.ElectricGreen
import com.shaaztechno.videoplayer.ui.theme.MutedGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onVideoClick: (Video) -> Unit,
    onShareClick: (Video) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DOWNLOADS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
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
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricGreen)
                }
            } else if (uiState.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = MutedGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No Downloads",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "Videos you download from URLs will appear here for offline playback.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MutedGray,
                                lineHeight = 18.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Active & Failed Downloads
                    if (uiState.activeDownloads.isNotEmpty()) {
                        item {
                            Text(
                                text = "ACTIVE DOWNLOADS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = ElectricGreen
                                )
                            )
                        }

                        items(uiState.activeDownloads, key = { it.id }) { item ->
                            ActiveDownloadCard(
                                item = item,
                                onPause = { viewModel.pauseDownload(item.id) },
                                onResume = { viewModel.resumeDownload(item.id) },
                                onCancel = { viewModel.cancelDownload(item.id) }
                            )
                        }
                    }

                    // Completed Downloads
                    if (uiState.completedVideos.isNotEmpty()) {
                        item {
                            Text(
                                text = "DOWNLOADED VIDEOS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(uiState.completedVideos, key = { it.id }) { video ->
                            var menuExpanded by remember { mutableStateOf(false) }
                            VideoListItem(
                                video = video,
                                onClick = { onVideoClick(video) },
                                onShare = { menuExpanded = true },
                                dropdownContent = {
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
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
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.deleteDownload(video)
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadCard(
    item: DownloadProgressItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${item.percentage.toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (item.isFailed) MaterialTheme.colorScheme.error else ElectricGreen,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Status label & progress bar
            val statusText = when {
                item.isFailed -> "Download failed"
                item.isPaused -> "Paused"
                item.isDownloading -> "Downloading…"
                else -> "Queued"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (item.isFailed) MaterialTheme.colorScheme.error else MutedGray,
                    fontSize = 12.sp
                )
            )

            LinearProgressIndicator(
                progress = (item.percentage / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (item.isFailed) MaterialTheme.colorScheme.error else ElectricGreen,
                trackColor = MaterialTheme.colorScheme.background
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isFailed) {
                    OutlinedButton(
                        onClick = onResume,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ElectricGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", fontSize = 12.sp)
                    }
                } else if (item.isPaused) {
                    OutlinedButton(
                        onClick = onResume,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ElectricGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onPause,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MutedGray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel", fontSize = 12.sp)
                }
            }
        }
    }
}
