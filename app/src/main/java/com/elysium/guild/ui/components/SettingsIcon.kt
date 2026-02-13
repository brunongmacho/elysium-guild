package com.elysium.guild.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SettingsIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier.size(24.dp)
) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            
            // Stylized Gear/Tune icon
            val radius = size.width * 0.25f
            
            if (isSelected) {
                drawCircle(
                    color = color.copy(alpha = 0.15f),
                    radius = radius,
                    center = center,
                    style = Fill
                )
            }
            
            drawCircle(
                color = color,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            
            // Sliders/Prongs
            val prongLength = 4.dp.toPx()
            for (i in 0 until 3) {
                val y = centerY + (i - 1) * 6.dp.toPx()
                // Line
                drawLine(
                    color = color,
                    start = Offset(centerX - size.width * 0.35f, y),
                    end = Offset(centerX + size.width * 0.35f, y),
                    strokeWidth = strokeWidth * 0.5f,
                    cap = StrokeCap.Round
                )
                
                // Small indicator box on the line
                val indicatorX = when(i) {
                    0 -> centerX - 4.dp.toPx()
                    1 -> centerX + 4.dp.toPx()
                    else -> centerX - 1.dp.toPx()
                }
                
                drawRect(
                    color = color,
                    topLeft = Offset(indicatorX - 1.5.dp.toPx(), y - 2.5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(3.dp.toPx(), 5.dp.toPx()),
                    style = if (isSelected) Fill else Stroke(width = strokeWidth * 0.5f)
                )
            }
        }
    }
}
