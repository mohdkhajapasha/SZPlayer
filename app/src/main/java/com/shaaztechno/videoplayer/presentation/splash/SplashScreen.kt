package com.shaaztechno.videoplayer.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shaaztechno.videoplayer.R
import com.shaaztechno.videoplayer.ui.theme.ElectricGreen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "alpha"
    )
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        // Using a CubicBezier that mimics an overshoot effect
        animationSpec = tween(durationMillis = 1000, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)),
        label = "scale"
    )

    val progressAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1600, easing = LinearEasing),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000) // Fast, smooth cinematic transition duration
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // 1. Logo Section
            Image(
                painter = painterResource(id = R.drawable.sz_player_icon),
                contentDescription = "SZ Player Logo",
                modifier = Modifier
                    .size(130.dp)
                    .scale(scaleAnim)
                    .alpha(alphaAnim)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. App Name Section
            Row(
                modifier = Modifier.alpha(alphaAnim),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SZ ",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp,
                        color = ElectricGreen
                    )
                )
                Text(
                    text = "Player",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Tagline Section
            Row(
                modifier = Modifier.alpha(alphaAnim),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ElectricGreen,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Watch",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ElectricGreen,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Enjoy",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        // 4. Progress Loading Bar near the lower portion
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Custom progress bar implementation to avoid Material 3 artifacts like the green dot
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(4.dp)
                    .alpha(alphaAnim)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.1f)) // Subtle track
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnim)
                        .background(ElectricGreen)
                )
            }
        }
    }
}
