package com.example.ui.components.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import com.example.model.ApiProvider
import com.example.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ColorFilter

@Composable
fun ProviderIcon(
    provider: ApiProvider,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    Box(
        modifier = modifier
            .background(Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (provider) {
            ApiProvider.GOOGLE -> GeminiIcon(modifier = Modifier.matchParentSize())
            ApiProvider.OPENAI -> OpenAiIcon(modifier = Modifier.matchParentSize(), color = color ?: Color(0xFF10A37F))
            ApiProvider.ANTHROPIC -> ClaudeIcon(modifier = Modifier.matchParentSize(), color = color ?: Color(0xFFD97753))
            ApiProvider.NABIH -> NabihIcon(modifier = Modifier.matchParentSize())
        }
    }
}

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
    Image(
        painter = painterResource(id = R.drawable.ic_openai),
        contentDescription = "OpenAI",
        modifier = modifier,
        colorFilter = ColorFilter.tint(color)
    )
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
                        quadraticTo(50f - w, 50f - length * 0.4f, 50f - w * 0.3f, 50f - length)
                        quadraticTo(50f, 50f - length - 4f, 50f + w * 0.3f, 50f - length)
                        quadraticTo(50f + w, 50f - length * 0.4f, 50f + w/2f, 50f)
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
                radius = 16f,
                center = Offset(50f, 50f)
            )
        }
    }
}

@Composable
fun NabihIcon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "Nabih AI",
        modifier = modifier
    )
}
