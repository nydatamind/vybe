package com.example.vybe.ui.screens.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.vybe.R
import com.example.vybe.core.designsystem.VybeCyan
import com.example.vybe.core.designsystem.VybeEmerald
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isReady: Boolean = true,
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Smooth Spotify-grade energetic zoom easing
    val zoomEasing = remember { CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.45f,
        animationSpec = tween(durationMillis = 30, easing = zoomEasing),
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 30),
        label = "logoAlpha"
    )

    val glowScale by animateFloatAsState(
        targetValue = if (startAnimation) 1.15f else 0.3f,
        animationSpec = tween(durationMillis = 30, easing = zoomEasing),
        label = "glowScale"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 0.3f else 0f,
        animationSpec = tween(durationMillis = 30),
        label = "glowAlpha"
    )

    LaunchedEffect(isReady) {
        startAnimation = true
        if (isReady) {
            onSplashFinished()
        }
    }

    // Dynamic background matching selected theme (Pure & minimal like Spotify)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Soft subtle glowing ambient light behind logo
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(glowScale)
                .alpha(glowAlpha)
                .blur(40.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9D00FF).copy(alpha = 0.5f),
                            Color(0xFF00C6FF).copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Only the centered App Logo (No text, clean minimal look)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(logoScale)
                .alpha(logoAlpha)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(165.dp)
            )
        }
    }
}


