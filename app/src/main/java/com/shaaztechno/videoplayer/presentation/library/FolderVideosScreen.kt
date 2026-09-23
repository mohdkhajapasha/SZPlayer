package com.shaaztechno.videoplayer.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaaztechno.videoplayer.domain.model.Video
import com.shaaztechno.videoplayer.presentation.home.VideoListItem
import com.shaaztechno.videoplayer.ui.theme.ElectricGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderVideosScreen(
    folderName: String,
    viewModel: FolderVideosViewModel,
    onVideoClick: (Video) -> Unit,
    onBack: () -> Unit,
    onShareClick: (Video) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showPlaylistDialog by remember { mutableStateOf<Video?>(null) }
    var showCreatePlaylistInDialog by remember { mutableStateOf(false) }
    var newPlaylistNameText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        folderName.uppercase(), 
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = ElectricGreen
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ElectricGreen)
            } else if (uiState.videos.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp), 
                        tint = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No videos found in this folder", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.videos) { video ->
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
                                        text = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onSurface) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.PlaylistAdd,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            showPlaylistDialog = video
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
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }

            // Add to Playlist Dialog
            showPlaylistDialog?.let { video ->
                AlertDialog(
                    onDismissRequest = { showPlaylistDialog = null },
                    title = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (uiState.playlists.isEmpty()) {
                                Text("No playlists available.", color = Color.Gray)
                            } else {
                                uiState.playlists.forEach { playlist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.addVideoToPlaylist(video, playlist.id)
                                                showPlaylistDialog = null
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = ElectricGreen)
                                        Spacer(Modifier.width(12.dp))
                                        Text(playlist.name, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
                            if (showCreatePlaylistInDialog) {
                                OutlinedTextField(
                                    value = newPlaylistNameText,
                                    onValueChange = { newPlaylistNameText = it },
                                    label = { Text("Playlist Name") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = ElectricGreen
                                    )
                                )
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        if (newPlaylistNameText.isNotBlank()) {
                                            viewModel.createPlaylist(newPlaylistNameText)
                                            newPlaylistNameText = ""
                                            showCreatePlaylistInDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen, contentColor = MaterialTheme.colorScheme.background)
                                ) {
                                    Text("Create Playlist")
                                }
                            } else {
                                TextButton(onClick = { showCreatePlaylistInDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = ElectricGreen)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Create Custom Playlist", color = ElectricGreen)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPlaylistDialog = null }) {
                            Text("Done", color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}
