package com.example.feature.chat

import com.example.core.model.AppLanguage
import com.example.feature.settings.SettingsViewModel

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

private fun isTextArabic(text: String): Boolean {
    for (char in text) {
        if (Character.UnicodeBlock.of(char) == Character.UnicodeBlock.ARABIC) {
            return true
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    settingsViewModel: SettingsViewModel,
    chatViewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.language == AppLanguage.ARABIC

    var isListening by remember { mutableStateOf(true) }
    var voiceStateText by remember { mutableStateOf("") }
    
    // Real-time Waveform Amplitudes
    val waveformAmplitudes = remember { mutableStateListOf<Float>() }
    // Initialize with quiet state
    LaunchedEffect(Unit) {
        for (i in 0..24) {
            waveformAmplitudes.add(0.05f)
        }
    }

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

    // TTS engine state
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isAiSpeaking by remember { mutableStateOf(false) }

    val speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null) }
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val isGenerating by chatViewModel.isGenerating.collectAsStateWithLifecycle()
    val streamingResponse by chatViewModel.currentStreamingResponse.collectAsStateWithLifecycle()

    // Setup TextToSpeech
    LaunchedEffect(Unit) {
        ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine?.language = if (isArabic) Locale("ar") else Locale.US

                // Native event listeners to maintain conversational turns
                ttsEngine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isAiSpeaking = true
                        voiceStateText = if (isArabic) "يتحدث Nabih AI الآن..." else "Nabih AI is speaking..."
                    }

                    override fun onDone(utteranceId: String?) {
                        isAiSpeaking = false
                        // Once finished speaking, instantly open the mic for continuous conversation!
                        if (isListening) {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                val primaryLang = if (isArabic) "ar-SA" else "en-US"
                                val secondaryLang = if (isArabic) "en-US" else "ar-SA"
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLang)
                                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(secondaryLang))
                                putExtra("android.speech.extra.ENABLE_LANGUAGE_DETECTION", true)
                                putExtra("android.speech.extra.LANGUAGE_SWITCH_ENABLED", true)
                                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                            }
                            // Run on main thread safely
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                speechRecognizer?.startListening(intent)
                                voiceStateText = if (isArabic) "جاري الاستماع إليك..." else "Listening to you..."
                            }
                        } else {
                            voiceStateText = if (isArabic) "جاهز للرد" else "Ready"
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        isAiSpeaking = false
                    }
                })
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsEngine?.stop()
            ttsEngine?.shutdown()
            speechRecognizer?.destroy()
        }
    }

    // Configure Speech Recognition listeners
    LaunchedEffect(speechRecognizer, isArabic) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                voiceStateText = if (isArabic) "تحدث الآن..." else "Speak now..."
            }

            override fun onBeginningOfSpeech() {
                // Clear waveform
                waveformAmplitudes.clear()
                for (i in 0..24) {
                    waveformAmplitudes.add(0.05f)
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Map dB audio level to normalized height
                val norm = ((rmsdB + 2f) / 14f).coerceIn(0.05f, 1.0f)
                if (waveformAmplitudes.size >= 25) {
                    waveformAmplitudes.removeAt(0)
                }
                waveformAmplitudes.add(norm)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                voiceStateText = if (isArabic) "جاري التفكير..." else "Analyzing speech..."
            }

            override fun onError(error: Int) {
                // Handle different error states gracefully
                voiceStateText = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> if (isArabic) "لم اسمعك بوضوح. انقر للتحدث" else "Didn't catch that. Tap to talk."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (isArabic) "انتهى وقت التحدث. انقر للتحدث" else "Speech timeout. Tap to talk."
                    else -> if (isArabic) "خطأ في الاتصال. انقر للبدء" else "Connection paused. Tap to restart."
                }
                isListening = false
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()
                if (!recognizedText.isNullOrBlank()) {
                    voiceStateText = if (isArabic) "جاري التفكير مع Nabih AI..." else "Nabih AI is thinking..."
                    chatViewModel.sendMessage(recognizedText)
                } else {
                    isListening = false
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // Process microphone active states
    LaunchedEffect(isListening, speechRecognizer, isArabic) {
        if (isListening && speechRecognizer != null && !isGenerating && !isAiSpeaking) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val primaryLang = if (isArabic) "ar-SA" else "en-US"
                val secondaryLang = if (isArabic) "en-US" else "ar-SA"
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLang)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf(secondaryLang))
                putExtra("android.speech.extra.ENABLE_LANGUAGE_DETECTION", true)
                putExtra("android.speech.extra.LANGUAGE_SWITCH_ENABLED", true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            ttsEngine?.stop()
            speechRecognizer?.startListening(intent)
            voiceStateText = if (isArabic) "جاري الاستماع إليك..." else "Listening to you..."
        } else if (!isListening) {
            speechRecognizer?.stopListening()
            if (!isGenerating && !isAiSpeaking) {
                voiceStateText = if (isArabic) "الميكروفون صامت حالياً" else "Microphone is muted"
            }
        }
    }

    // Trigger Speech output once response is loaded
    LaunchedEffect(isGenerating) {
        if (!isGenerating && streamingResponse.isBlank()) {
            val lastMsg = (chatState as? ChatUiState.Success)?.messages?.lastOrNull { it.role == "model" }
            if (lastMsg != null) {
                val textToSpeak = lastMsg.content
                if (textToSpeak.isNotBlank()) {
                    // Dynamically set language of Text-To-Speech engine based on actual text
                    if (isTextArabic(textToSpeak)) {
                        ttsEngine?.language = Locale("ar")
                    } else {
                        ttsEngine?.language = Locale.US
                    }
                    // Speak response with clean progress tracker id
                    val params = Bundle().apply {
                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "NABIH_VOICE_UTTERANCE")
                    }
                    ttsEngine?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "NABIH_VOICE_UTTERANCE")
                }
            }
        } else if (isGenerating) {
            ttsEngine?.stop()
            isAiSpeaking = false
            voiceStateText = if (isArabic) "جاري صياغة الرد..." else "Formulating response..."
        }
    }

    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Animatable size for modern circular mic button
    val buttonSize by animateDpAsState(
        targetValue = if (isListening) 130.dp else 115.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "buttonSize"
    )

    // Pulse Wave animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "المحادثة الصوتية" else "Voice Mode", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Central Pulsing Orb
            Box(
                modifier = Modifier
                    .size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isListening || isAiSpeaking) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    
                    // Ripple 1
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        scale(waveScale1) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.25f),
                                        primaryColor.copy(alpha = 0.0f)
                                    )
                                ),
                                radius = size.minDimension / 4
                            )
                        }
                    }

                    // Ripple 2
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        scale(waveScale2) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        secondaryColor.copy(alpha = 0.15f),
                                        secondaryColor.copy(alpha = 0.0f)
                                    )
                                ),
                                radius = size.minDimension / 4
                            )
                        }
                    }
                }

                // Core Floating Circular Material 3 Microphone Button
                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (isAiSpeaking) {
                                    listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f))
                                } else if (isListening) {
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                } else {
                                    listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outlineVariant)
                                }
                            )
                        )
                        .clickable {
                            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (isAiSpeaking) {
                                ttsEngine?.stop()
                                isAiSpeaking = false
                                isListening = true
                            } else {
                                isListening = !isListening
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isAiSpeaking) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = "AI Speaking",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isListening) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                            contentDescription = "Microphone State",
                            tint = if (isListening) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Real-time scrolling spectrum spectrometer waveform!
            AnimatedVisibility(
                visible = isListening && waveformAmplitudes.isNotEmpty() && !isAiSpeaking,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        val barWidth = 4.dp.toPx()
                        val gap = 3.dp.toPx()
                        val totalWidth = (barWidth + gap) * waveformAmplitudes.size - gap
                        val startX = (size.width - totalWidth) / 2
                        
                        waveformAmplitudes.forEachIndexed { index, amp ->
                            val x = startX + index * (barWidth + gap)
                            val barHeight = (size.height * amp).coerceAtLeast(4.dp.toPx())
                            val y = (size.height - barHeight) / 2
                            
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clean status text
            Text(
                text = voiceStateText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
