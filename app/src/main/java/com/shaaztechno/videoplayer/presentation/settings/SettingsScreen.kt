package com.shaaztechno.videoplayer.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaaztechno.videoplayer.ui.theme.ElectricGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val settings by viewModel.settings.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearCatalogDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Red.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.Red
                    )
                }
            },
            title = {
                Text(
                    text = "Clear History",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear all playback history? This action cannot be undone.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showDeleteDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Playback history cleared",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Clear History", color = ElectricGreen )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showClearCatalogDialog) {
        AlertDialog(
            onDismissRequest = { showClearCatalogDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Red.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.Red
                    )
                }
            },
            title = {
                Text(
                    text = "Clear Online Catalog",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear all online catalog videos? They will be restored on next refresh.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearOnlineCatalog()
                        showClearCatalogDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Online catalog cleared",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Clear Catalog", color = ElectricGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCatalogDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(28.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "SETTINGS", 
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = ElectricGreen
                        )
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(bottom = 12.dp),
                    containerColor = Color(0xFF1A1C1E),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ElectricGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        settings?.let { userSettings ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Player Category
                Text("Player Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ElectricGreen))
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Default Orientation", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = userSettings.defaultOrientation == "auto",
                            onClick = { viewModel.updateDefaultOrientation("auto") },
                            label = { Text("Auto") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricGreen,
                                selectedLabelColor = MaterialTheme.colorScheme.background,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        FilterChip(
                            selected = userSettings.defaultOrientation == "landscape",
                            onClick = { viewModel.updateDefaultOrientation("landscape") },
                            label = { Text("Landscape") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricGreen,
                                selectedLabelColor = MaterialTheme.colorScheme.background,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        FilterChip(
                            selected = userSettings.defaultOrientation == "portrait",
                            onClick = { viewModel.updateDefaultOrientation("portrait") },
                            label = { Text("Portrait") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricGreen,
                                selectedLabelColor = MaterialTheme.colorScheme.background,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                SettingsSwitchItem(
                    title = "Brightness Gesture",
                    subtitle = "Swipe vertically on left side to change brightness",
                    checked = userSettings.brightnessGestureEnabled,
                    onCheckedChange = { viewModel.updateBrightnessGestureEnabled(it) }
                )

                SettingsSwitchItem(
                    title = "Volume Gesture",
                    subtitle = "Swipe vertically on right side to change volume",
                    checked = userSettings.volumeGestureEnabled,
                    onCheckedChange = { viewModel.updateVolumeGestureEnabled(it) }
                )

                SettingsSwitchItem(
                    title = "Seeking Gesture",
                    subtitle = "Swipe horizontally to seek backward/forward",
                    checked = userSettings.seekingGestureEnabled,
                    onCheckedChange = { viewModel.updateSeekingGestureEnabled(it) }
                )

                HorizontalDivider(color = Color.DarkGray)

                // Playback Category
                Text("Playback Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ElectricGreen))
                
                SettingsSwitchItem(
                    title = "Auto-play Next",
                    subtitle = "Automatically play the next video in the list",
                    checked = userSettings.autoPlayNext,
                    onCheckedChange = { viewModel.updateAutoPlayNext(it) }
                )

                SettingsSwitchItem(
                    title = "Background Playback",
                    subtitle = "Continue audio playback when app is minimized",
                    checked = userSettings.backgroundPlaybackEnabled,
                    onCheckedChange = { viewModel.updateBackgroundPlaybackEnabled(it) }
                )

                SettingsSwitchItem(
                    title = "Resume Playback",
                    subtitle = "Continue from where you left off",
                    checked = userSettings.resumePlayback,
                    onCheckedChange = { viewModel.updateResumePlayback(it) }
                )

                SettingsSwitchItem(
                    title = "Keep Screen Awake",
                    subtitle = "Prevent screen from turning off during playback",
                    checked = userSettings.keepScreenAwake,
                    onCheckedChange = { viewModel.updateKeepScreenAwake(it) }
                )

//                Divider(color = Color.DarkGray)
//                Text("Appearance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ElectricGreen))
//                
//                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                    Text("Dark Mode", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
//                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                        FilterChip(
//                            selected = userSettings.darkMode == "light",
//                            onClick = { viewModel.updateDarkMode("light") },
//                            label = { Text("Light") },
//                            colors = FilterChipDefaults.filterChipColors(
//                                selectedContainerColor = ElectricGreen,
//                                selectedLabelColor = MaterialTheme.colorScheme.background,
//                                containerColor = MaterialTheme.colorScheme.surface,
//                                labelColor = MaterialTheme.colorScheme.onSurface
//                            )
//                        )
//                        FilterChip(
//                            selected = userSettings.darkMode == "dark",
//                            onClick = { viewModel.updateDarkMode("dark") },
//                            label = { Text("Dark") },
//                            colors = FilterChipDefaults.filterChipColors(
//                                selectedContainerColor = ElectricGreen,
//                                selectedLabelColor = MaterialTheme.colorScheme.background,
//                                containerColor = MaterialTheme.colorScheme.surface,
//                                labelColor = MaterialTheme.colorScheme.onSurface
//                            )
//                        )
//                        FilterChip(
//                            selected = userSettings.darkMode == "system",
//                            onClick = { viewModel.updateDarkMode("system") },
//                            label = { Text("System") },
//                            colors = FilterChipDefaults.filterChipColors(
//                                selectedContainerColor = ElectricGreen,
//                                selectedLabelColor = MaterialTheme.colorScheme.background,
//                                containerColor = MaterialTheme.colorScheme.surface,
//                                labelColor = MaterialTheme.colorScheme.onSurface
//                            )
//                        )
//                    }
//                }

                HorizontalDivider(color = Color.DarkGray)
                Text("Data & History", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = ElectricGreen))
                
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red).run { androidx.compose.foundation.BorderStroke(1.dp, Color.Red) }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Playback History")
                }


                HorizontalDivider(color = Color.DarkGray)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                    Spacer(Modifier.width(12.dp))
                    Text("SZ Player v1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedTrackColor = ElectricGreen,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
