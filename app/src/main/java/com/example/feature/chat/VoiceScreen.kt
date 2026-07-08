package com.example.feature.chat

import com.example.core.model.AppLanguage
import com.example.feature.settings.SettingsViewModel

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
import kotlinx.coroutines.delay
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    settingsViewModel: SettingsViewModel,
    chatViewModel: com.example.feature.chat.ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.language == AppLanguage.ARABIC

    var isListening by remember { mutableStateOf(true) }
    var voiceStateText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            voiceStateText = if (isArabic) "مطلوب إذن الميكروفون" else "Microphone permission required"
            isListening = false
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

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

    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val isGenerating by chatViewModel.isGenerating.collectAsStateWithLifecycle()
    val streamingResponse by chatViewModel.currentStreamingResponse.collectAsStateWithLifecycle()
    
    // Setup Speech Recognizer
    LaunchedEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    voiceStateText = if (isArabic) "تحدث الآن..." else "Speak now..."
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    voiceStateText = if (isArabic) "جاري المعالجة..." else "Processing..."
                }
                override fun onError(error: Int) {
                    voiceStateText = if (isArabic) "خطأ في التعرف. انقر للمحاولة مرة أخرى." else "Recognition error. Tap mic to retry."
                    isListening = false
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognizedText = matches?.firstOrNull()
                    if (!recognizedText.isNullOrBlank()) {
                        voiceStateText = if (isArabic) "جاري التفكير..." else "Thinking..."
                        chatViewModel.sendMessage(recognizedText)
                    } else {
                        isListening = false
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer = recognizer
        } else {
            voiceStateText = if (isArabic) "التعرف على الصوت غير متاح" else "Speech recognition not available"
            isListening = false
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    // Handle Mic toggle
    LaunchedEffect(isListening) {
        if (isListening && speechRecognizer != null && !isGenerating) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isArabic) "ar" else "en-US")
            }
            ttsEngine?.stop()
            speechRecognizer?.startListening(intent)
            voiceStateText = if (isArabic) "جاري الاستماع إليك..." else "Listening to you..."
        } else if (!isListening) {
            speechRecognizer?.stopListening()
            if (!isGenerating) {
                voiceStateText = if (isArabic) "تم كتم الميكروفون" else "Microphone muted"
            }
        }
    }

    // Handle Generation and TTS
    LaunchedEffect(isGenerating) {
        if (!isGenerating && streamingResponse.isBlank()) {
            // When generation finishes, TTS should speak the latest model message
            val lastMsg = (chatState as? ChatUiState.Success)?.messages?.lastOrNull { it.role == "model" }
            if (lastMsg != null) {
                voiceStateText = if (isArabic) "يتحدث الآن..." else "Speaking..."
                ttsEngine?.speak(lastMsg.content, TextToSpeech.QUEUE_FLUSH, null, null)
                // Resume listening after a delay
                delay((lastMsg.content.length * 50).toLong() + 2000)
                if (isListening) {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isArabic) "ar" else "en-US")
                    }
                    speechRecognizer?.startListening(intent)
                }
            }
        } else if (isGenerating) {
            voiceStateText = if (isArabic) "جاري التفكير..." else "Thinking..."
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
