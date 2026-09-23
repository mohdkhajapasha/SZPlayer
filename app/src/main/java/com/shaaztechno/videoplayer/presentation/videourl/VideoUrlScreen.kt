package com.shaaztechno.videoplayer.presentation.videourl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaaztechno.videoplayer.domain.model.VideoMediaInfo
import com.shaaztechno.videoplayer.ui.theme.BorderSubtle
import com.shaaztechno.videoplayer.ui.theme.ElectricGreen
import com.shaaztechno.videoplayer.ui.theme.MutedGray
import com.shaaztechno.videoplayer.ui.theme.PillGreen
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoUrlScreen(
    viewModel: VideoUrlViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToDownloads: () -> Unit
) {
    val urlInput by viewModel.urlInput.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ADD VIDEO FROM URL",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // URL Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Video URL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { viewModel.onUrlChanged(it) },
                        placeholder = {
                            Text(
                                "Paste video URL (MP4, HLS, DASH, etc.)",
                                color = MutedGray,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = ElectricGreen
                            )
                        },
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onClearUrl() }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear URL",
                                        tint = MutedGray
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val clipText = clipboardManager.getText()?.text
                                        if (!clipText.isNullOrBlank()) {
                                            viewModel.onPasteUrl(clipText)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste from clipboard",
                                        tint = ElectricGreen
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricGreen,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = ElectricGreen,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        maxLines = 4
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    viewModel.onPasteUrl(clipText)
                                }
                            },
                            shape = RoundedCornerShape(25.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ElectricGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Paste", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.checkUrl() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricGreen,
                                contentColor = MaterialTheme.colorScheme.background,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContentColor = MutedGray
                            ),
                            enabled = urlInput.isNotBlank() && uiState !is VideoUrlUiState.Checking
                        ) {
                            if (uiState is VideoUrlUiState.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.background
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Checking…",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Check URL",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // State UI rendering
            when (val state = uiState) {
                is VideoUrlUiState.Idle -> {
                    // Helpful information card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = ElectricGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Supported Formats",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                "• Progressive videos: MP4, WebM, MKV, 3GP, MOV\n" +
                                "• Adaptive streams: HLS (.m3u8), DASH (.mpd)\n" +
                                "• Direct media download URLs without file extensions",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MutedGray,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }

                is VideoUrlUiState.Checking -> {
                    CheckingCard()
                }

                is VideoUrlUiState.Success -> {
                    SuccessResultCard(
                        mediaInfo = state.mediaInfo,
                        onPlay = { viewModel.playVideo(onNavigateToPlayer) },
                        onDownload = { viewModel.downloadVideo(onNavigateToDownloads) }
                    )
                }

                is VideoUrlUiState.Error -> {
                    ErrorResultCard(
                        errorMessage = state.message,
                        onTryAgain = { viewModel.checkUrl() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = ElectricGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Checking video…",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "Validating URL accessibility, server headers, and stream playability",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MutedGray
                )
            )
        }
    }
}

@Composable
private fun SuccessResultCard(
    mediaInfo: VideoMediaInfo,
    onPlay: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ElectricGreen.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Available status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PillGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = ElectricGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Video Available",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = ElectricGreen
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PillGreen
                ) {
                    Text(
                        text = mediaInfo.mediaType,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ElectricGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Video Title
            Text(
                text = mediaInfo.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Divider(color = BorderSubtle)

            // Info rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow(label = "Format", value = mediaInfo.mediaType)

                if (mediaInfo.fileSize != null && mediaInfo.fileSize > 0) {
                    InfoRow(label = "Size", value = formatFileSize(mediaInfo.fileSize))
                }

                if (mediaInfo.duration != null && mediaInfo.duration > 0) {
                    InfoRow(label = "Duration", value = formatDuration(mediaInfo.duration))
                }

                InfoRow(
                    label = "Playback",
                    value = if (mediaInfo.isPlayable) "Supported" else "Not supported",
                    isHighlight = mediaInfo.isPlayable
                )

                InfoRow(
                    label = "Download",
                    value = if (mediaInfo.isDownloadable) "Supported" else "Unavailable",
                    isHighlight = mediaInfo.isDownloadable
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play Button
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricGreen,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    enabled = mediaInfo.isPlayable
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PLAY",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // Download Button
                OutlinedButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.5.dp, if (mediaInfo.isDownloadable) ElectricGreen else BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (mediaInfo.isDownloadable) ElectricGreen else MutedGray
                    ),
                    enabled = mediaInfo.isDownloadable
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DOWNLOAD",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorResultCard(
    errorMessage: String,
    onTryAgain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Unable to access video",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MutedGray,
                    lineHeight = 20.sp
                )
            )

            OutlinedButton(
                onClick = onTryAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(23.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = ElectricGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MutedGray
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isHighlight) ElectricGreen else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1024) {
        val gb = mb / 1024
        String.format(Locale.ROOT, "%.2f GB", gb)
    } else {
        String.format(Locale.ROOT, "%.1f MB", mb)
    }
}

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes)
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    return if (hours > 0) {
        val remMinutes = minutes - TimeUnit.HOURS.toMinutes(hours)
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, remMinutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}
