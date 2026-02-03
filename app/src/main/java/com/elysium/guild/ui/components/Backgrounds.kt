package com.elysium.guild.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elysium.guild.utils.Constants

@Composable
fun DynamicElysiumBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundTransition")
    
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
            
            // Draw blurred color blobs using centralized constants
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Constants.COLOR_BLOB_PURPLE.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(canvasWidth * 0.2f, canvasHeight * 0.2f),
                    radius = 400.dp.toPx()
                ),
                radius = 400.dp.toPx(),
                center = Offset(
                    x = canvasWidth * 0.2f + (50 * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat(),
                    y = canvasHeight * 0.2f + (50 * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat()
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Constants.COLOR_BLOB_BLUE.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(canvasWidth * 0.8f, canvasHeight * 0.7f),
                    radius = 500.dp.toPx()
                ),
                radius = 500.dp.toPx(),
                center = Offset(
                    x = canvasWidth * 0.8f + (70 * kotlin.math.sin(Math.toRadians(angle.toDouble()))).toFloat(),
                    y = canvasHeight * 0.7f + (70 * kotlin.math.cos(Math.toRadians(angle.toDouble()))).toFloat()
                )
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Constants.COLOR_BLOB_ORANGE.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.4f),
                    radius = 350.dp.toPx()
                ),
                radius = 350.dp.toPx(),
                center = Offset(
                    x = canvasWidth * 0.5f + (100 * kotlin.math.cos(Math.toRadians(angle.toDouble() + 180))).toFloat(),
                    y = canvasHeight * 0.4f + (100 * kotlin.math.sin(Math.toRadians(angle.toDouble() + 180))).toFloat()
                )
            )
        }
        content()
    }
}
