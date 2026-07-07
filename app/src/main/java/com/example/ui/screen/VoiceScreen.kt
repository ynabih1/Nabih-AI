package com.example.ui.screen

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.language == AppLanguage.ARABIC

    var isListening by remember { mutableStateOf(true) }
    var voiceStateText by remember { mutableStateOf("") }

    // TTS engine
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    LaunchedEffect(Unit) {
        ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine?.language = if (isArabic) Locale("ar") else Locale.US
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            ttsEngine?.stop()
            ttsEngine?.shutdown()
        }
    }

    // Speech Simulator
    LaunchedEffect(isListening) {
        if (isListening) {
            voiceStateText = if (isArabic) "جاري الاستماع إليك..." else "Listening to you..."
            delay(4000)
            voiceStateText = if (isArabic) "جاري التفكير..." else "Thinking..."
            delay(1500)
            val textToSpeak = if (isArabic) {
                "أهلاً بك يا صديقي، أنا نبيه المساعد الذكي. كيف يمكنني إجابتك صوتياً اليوم؟"
            } else {
                "Hello, I am Nabih, your voice companion. I am fully ready to discuss anything with you."
            }
            voiceStateText = if (isArabic) "يتحدث الآن..." else "Speaking..."
            ttsEngine?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            voiceStateText = if (isArabic) "تم كتم الميكروفون" else "Microphone muted"
            ttsEngine?.stop()
        }
    }

    // Wave animation scaling transition
    val infiniteTransition = rememberInfiniteTransition()
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "المحادثة الصوتية الفورية" else "Advanced Voice Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Title description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isArabic) "نبيه المساعد الصوتي" else "Nabih Voice Agent",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isArabic) "تواصل بشكل طبيعي وكامل دون الحاجة للكتابة" else "Engage in effortless spoken chat in real time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            // Glowing Waves visual canvas center
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isListening) {
                    // Pulsing Wave Ring 1
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        scale(waveScale1) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.2f),
                                        primaryColor.copy(alpha = 0.0f)
                                    )
                                ),
                                radius = size.minDimension / 4
                            )
                        }
                    }

                    // Pulsing Wave Ring 2
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        scale(waveScale2) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.1f),
                                        primaryColor.copy(alpha = 0.0f)
                                    )
                                ),
                                radius = size.minDimension / 4
                            )
                        }
                    }
                }

                // Central Mic / Volume Icon core
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (voiceStateText.contains("Speak") || voiceStateText.contains("يتحدث")) Icons.Default.VolumeUp else Icons.Default.Mic,
                        contentDescription = "Microphone State",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Real-time voice state label
            Text(
                text = voiceStateText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            // Mic Toggle control footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { isListening = !isListening },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Mute",
                        tint = if (isListening) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (isListening) {
                        if (isArabic) "اضغط للبدء في كتم الصوت" else "Tap to pause voice session"
                    } else {
                        if (isArabic) "اضغط لإلغاء كتم الصوت" else "Tap to resume voice session"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}
