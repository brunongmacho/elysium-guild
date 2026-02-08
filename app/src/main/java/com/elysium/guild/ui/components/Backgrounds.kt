package com.elysium.guild.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elysium.guild.utils.Constants
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun DynamicElysiumBackground(
    modifier: Modifier = Modifier,
    scrollOffset: Float = 0f,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundTransition")
    val isDarkTheme = isSystemInDarkTheme()
    
    // Get current time to determine day/night cycle
    val hour = remember { 
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour 
    }
    val isNight = hour >= 20 || hour < 6

    // Theme-aware color palette
    val targetBlob1 = when {
        isNight && isDarkTheme -> Color(0xFF4A148C) // Night + Dark: Deep Purple
        isNight && !isDarkTheme -> Color(0xFFB39DDB) // Night + Light: Soft Lavender
        !isNight && isDarkTheme -> Color(0xFF0D47A1) // Day + Dark: Rich Blue
        else -> Color(0xFFBBDEFB) // Day + Light: Sky Blue
    }

    val targetBlob2 = when {
        isNight && isDarkTheme -> Color(0xFF1A237E) // Night + Dark: Deep Indigo
        isNight && !isDarkTheme -> Color(0xFFC5CAE9) // Night + Light: Soft Indigo
        !isNight && isDarkTheme -> Color(0xFF01579B) // Day + Dark: Deep Cyan
        else -> Color(0xFFE1F5FE) // Day + Light: Pale Blue
    }

    val targetBlob3 = when {
        isNight && isDarkTheme -> Color(0xFF311B92) // Night + Dark: Very Deep Purple
        isNight && !isDarkTheme -> Color(0xFFD1C4E9) // Night + Light: Pale Lavender
        !isNight && isDarkTheme -> Color(0xFFE65100) // Day + Dark: Deep Orange
        else -> Color(0xFFFFF3E0) // Day + Light: Pale Orange
    }

    // Animate colors for smooth transitions
    val blob1Color by animateColorAsState(targetBlob1, tween(2000), label = "Blob1Color")
    val blob2Color by animateColorAsState(targetBlob2, tween(2000), label = "Blob2Color")
    val blob3Color by animateColorAsState(targetBlob3, tween(2000), label = "Blob3Color")

    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AngleAnimation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Parallax factor
            val parallaxY = scrollOffset * 0.1f
            
            // Adjust alpha based on theme: Light mode needs much subtler blobs to keep text readable
            val baseAlpha = if (isDarkTheme) 1f else 0.6f

            // Draw blurred color blobs
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob1Color.copy(alpha = 0.15f * baseAlpha), Color.Transparent),
                    center = Offset(canvasWidth * 0.2f, canvasHeight * 0.2f),
                    radius = 400.dp.toPx()
                ),
                radius = 400.dp.toPx(),
                center = Offset(
                    x = canvasWidth * 0.2f + (50 * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat(),
                    y = canvasHeight * 0.2f + (50 * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat() - parallaxY
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob2Color.copy(alpha = 0.12f * baseAlpha), Color.Transparent),
                    center = Offset(canvasWidth * 0.8f, canvasHeight * 0.7f),
                    radius = 500.dp.toPx()
                ),
                radius = 500.dp.toPx(),
                center = Offset(
                    x = canvasWidth * 0.8f + (70 * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat(),
                    y = canvasHeight * 0.7f + (70 * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat() - (parallaxY * 0.5f)
                )
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob3Color.copy(alpha = 0.08f * baseAlpha), Color.Transparent),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.4f),
                    radius = 350.dp.toPx()
                ),
                radius = 350.dp.toPx(),
                center = Offset(
                    x = canvasWidth * 0.5f + (100 * kotlin.math.cos(Math.toRadians(angle.toDouble() + 180))).toFloat(),
                    y = canvasHeight * 0.4f + (100 * kotlin.math.sin(Math.toRadians(angle.toDouble() + 180))).toFloat() - (parallaxY * 0.8f)
                )
            )
        }
        content()
    }
}
