package com.shaaztechno.videoplayer.presentation.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onVideoClick: (Video) -> Unit,
    onShareClick: (Video) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    
    var showRenameDialog by remember { mutableStateOf<Video?>(null) }
    var renameTitleText by remember { mutableStateOf("") }
    
    var showDeleteDialog by remember { mutableStateOf<Video?>(null) }
    var showPlaylistDialog by remember { mutableStateOf<Video?>(null) }
    var showCreatePlaylistInDialog by remember { mutableStateOf(false) }
    var newPlaylistNameText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "LIBRARY", 
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = ElectricGreen
                        )
                    ) 
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort Videos", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Name", color = if (uiState.sortOrder == SortOrder.NAME) ElectricGreen else MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.NAME)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Date", color = if (uiState.sortOrder == SortOrder.DATE) ElectricGreen else MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.DATE)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Size", color = if (uiState.sortOrder == SortOrder.SIZE) ElectricGreen else MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.SIZE)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            if (uiState.isFolderView) Icons.Default.ViewList else Icons.Default.Folder,
                            contentDescription = "Toggle View",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onBackground)
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
                    Text("No local videos found", color = Color.Gray)
                    Button(
                        onClick = { viewModel.refresh() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen, contentColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Scan Device")
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isFolderView) {
                        uiState.folders.forEach { (folderName, videos) ->
                            item {
                                FolderHeader(folderName, videos.size)
                            }
                            items(videos) { video ->
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
                                                text = { Text("Rename", color = MaterialTheme.colorScheme.onSurface) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    menuExpanded = false
                                                    showRenameDialog = video
                                                    renameTitleText = video.title
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
                                                    showDeleteDialog = video
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        items(uiState.videos) { video ->
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
                                                text = { Text("Rename", color = MaterialTheme.colorScheme.onSurface) },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    menuExpanded = false
                                                    showRenameDialog = video
                                                    renameTitleText = video.title
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
                                                    showDeleteDialog = video
                                                }
                                            )
                                        }
                                    }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }


            // Rename Dialog
            showRenameDialog?.let { video ->
                AlertDialog(
                    onDismissRequest = { showRenameDialog = null },
                    title = { Text("Rename Video", color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        OutlinedTextField(
                            value = renameTitleText,
                            onValueChange = { renameTitleText = it },
                            label = { Text("Title") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = ElectricGreen
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (renameTitleText.isNotBlank()) {
                                    viewModel.renameVideo(video, renameTitleText)
                                    showRenameDialog = null
                                }
                            }
                        ) {
                            Text("Save", color = ElectricGreen)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameDialog = null }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            // Delete Confirmation Dialog
            showDeleteDialog?.let { video ->
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = null },
                    title = { Text("Delete Video", color = MaterialTheme.colorScheme.onSurface) },
                    text = { Text("Are you sure you want to remove this video from the library?", color = Color.Gray) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteVideo(video)
                                showDeleteDialog = null
                            }
                        ) {
                            Text("Delete", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = null }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
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

@Composable
fun FolderHeader(name: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground)
            )
        }
    }
}
