package com.example.utils

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

object VoiceRecorderManager {
    private const val TAG = "VoiceRecorderManager"

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var timerJob: Job? = null
    private var scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()

    private val _maxAmplitude = MutableStateFlow(0)
    val maxAmplitude: StateFlow<Int> = _maxAmplitude.asStateFlow()

    fun startRecording(context: Context): Boolean {
        if (_isRecording.value) {
            stopRecording()
        }

        try {
            val voiceDir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            val file = File(voiceDir, "voice_${System.currentTimeMillis()}.m4a")
            currentFile = file

            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)
            
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            _isRecording.value = true
            _recordingDurationSeconds.value = 0
            _maxAmplitude.value = 0

            startTimerAndAmplitude()
            Log.d(TAG, "Recording started: ${file.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            return false
        }
    }

    private fun startTimerAndAmplitude() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var duration = 0
            while (_isRecording.value) {
                delay(100)
                try {
                    val amp = mediaRecorder?.maxAmplitude ?: 0
                    _maxAmplitude.value = amp
                } catch (_: Exception) {}

                duration += 100
                if (duration % 1000 == 0) {
                    _recordingDurationSeconds.value = duration / 1000
                }
            }
        }
    }

    fun stopRecording(): RecordResult? {
        if (!_isRecording.value) return null

        var result: RecordResult? = null
        try {
            mediaRecorder?.stop()
            val file = currentFile
            val duration = _recordingDurationSeconds.value
            if (file != null && file.exists() && file.length() > 0) {
                val uri = Uri.fromFile(file)
                result = RecordResult(
                    uri = uri,
                    file = file,
                    durationSeconds = if (duration == 0) 1 else duration,
                    sizeBytes = file.length()
                )
                Log.d(TAG, "Recording stopped successfully. File size: ${file.length()} bytes, duration: $duration s")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            cleanup()
        }

        return result
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}
        
        currentFile?.delete()
        cleanup()
        Log.d(TAG, "Recording cancelled and file deleted")
    }

    private fun cleanup() {
        timerJob?.cancel()
        timerJob = null
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        currentFile = null
        _isRecording.value = false
        _recordingDurationSeconds.value = 0
        _maxAmplitude.value = 0
    }

    data class RecordResult(
        val uri: Uri,
        val file: File,
        val durationSeconds: Int,
        val sizeBytes: Long
    )
}
