package com.elysium.guild.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ElysiumGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    statusColor: Color = Color.Transparent,
    glowColor: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "ScaleAnimation"
    )

    val animatedGlowColor by animateColorAsState(
        targetValue = glowColor,
        animationSpec = tween(durationMillis = 500),
        label = "GlowColorAnimation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (animatedGlowColor != Color.Transparent) {
                        listOf(
                            animatedGlowColor.copy(alpha = 0.6f),
                            animatedGlowColor.copy(alpha = 0.1f),
                            animatedGlowColor.copy(alpha = 0.6f)
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.2f else 0.15f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.05f else 0.02f)
                        )
                    }
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isDark) 4.dp else 2.dp,
        shadowElevation = if (isDark) 0.dp else 3.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Status Highlight Gradient
            if (statusColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = if (isDark) 0.12f else 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Outer Glow Effect for special items
            if (animatedGlowColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    animatedGlowColor.copy(alpha = if (isDark) 0.08f else 0.05f),
                                    Color.Transparent
                                ),
                                radius = 2000f // Large radius for a subtle glow
                            )
                        )
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}
