package com.example.feature.chat

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.rotate

@Composable
fun GeminiIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path().apply {
            moveTo(width / 2f, 0f)
            cubicTo(width / 2f, height * 0.3f, width * 0.7f, height / 2f, width, height / 2f)
            cubicTo(width * 0.7f, height / 2f, width / 2f, height * 0.7f, width / 2f, height)
            cubicTo(width / 2f, height * 0.7f, width * 0.3f, height / 2f, 0f, height / 2f)
            cubicTo(width * 0.3f, height / 2f, width / 2f, height * 0.3f, width / 2f, 0f)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF8AB4F8), Color(0xFF4285F4), Color(0xFF9E00FF))
            )
        )
    }
}

@Composable
fun OpenAiIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF10A37F)) {
    Canvas(modifier = modifier) {
        val width = size.width
        val scaleValue = width / 100f
        
        scale(scaleValue, scaleValue, pivot = Offset.Zero) {
            val strokeWidth = 6.0f
            for (i in 0 until 6) {
                rotate(i * 60f, pivot = Offset(50f, 50f)) {
                    val path = Path().apply {
                        moveTo(50f, 42f)
                        cubicTo(50f, 28f, 62f, 22f, 72f, 30f)
                        cubicTo(82f, 38f, 80f, 54f, 68f, 62f)
                        lineTo(54f, 70f)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun ClaudeIcon(modifier: Modifier = Modifier, color: Color = Color(0xFFD97753)) {
    Canvas(modifier = modifier) {
        val width = size.width
        val scaleValue = width / 100f
        
        scale(scaleValue, scaleValue, pivot = Offset.Zero) {
            val lobeAngles = floatArrayOf(0f, 40f, 80f, 120f, 160f, 200f, 240f, 280f, 320f)
            val lobeLengths = floatArrayOf(35f, 38f, 36f, 34f, 37f, 35f, 38f, 36f, 35f)
            val lobeWidths = floatArrayOf(12f, 13f, 11f, 12f, 13f, 11f, 12f, 13f, 11f)
            
            for (i in 0 until 9) {
                rotate(lobeAngles[i], pivot = Offset(50f, 50f)) {
                    val path = Path().apply {
                        val length = lobeLengths[i]
                        val w = lobeWidths[i]
                        moveTo(50f - w/2f, 50f)
                        quadraticBezierTo(50f - w, 50f - length * 0.4f, 50f - w * 0.3f, 50f - length)
                        quadraticBezierTo(50f, 50f - length - 4f, 50f + w * 0.3f, 50f - length)
                        quadraticBezierTo(50f + w, 50f - length * 0.4f, 50f + w/2f, 50f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = color
                    )
                }
            }
            
            drawCircle(
                color = color,
                radius = 14f,
                center = Offset(50f, 50f)
            )
        }
    }
}
