package com.shaaztechno.videoplayer.presentation.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shaaztechno.videoplayer.data.local.entity.PlaylistEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel,
    onPlaylistClick: (PlaylistEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Playlist")
            }
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
            } else if (uiState.playlists.isEmpty()) {
                Text("No playlists created yet.", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onBackground)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.playlists) { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name, color = MaterialTheme.colorScheme.onSurface) },
                            leadingContent = { Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deletePlaylist(playlist) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Playlist",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier.clickable { onPlaylistClick(playlist) }
                        )
                        Divider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("New Playlist") },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistName)
                                newPlaylistName = ""
                                showDialog = false
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
