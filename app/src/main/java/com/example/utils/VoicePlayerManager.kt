package com.example.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object VoicePlayerManager {
    private const val TAG = "VoicePlayerManager"

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentlyPlayingUri = MutableStateFlow<Uri?>(null)
    val currentlyPlayingUri: StateFlow<Uri?> = _currentlyPlayingUri.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentProgress = MutableStateFlow(0f)
    val currentProgress: StateFlow<Float> = _currentProgress.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0)
    val totalDurationMs: StateFlow<Int> = _totalDurationMs.asStateFlow()

    fun playOrToggle(context: Context, uri: Uri) {
        if (_currentlyPlayingUri.value == uri) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        // Stop any current playback
        stop()

        try {
            val player = MediaPlayer().apply {
                setDataSource(context.applicationContext, uri)
                prepare()
            }

            player.setOnCompletionListener {
                _isPlaying.value = false
                _currentProgress.value = 0f
                _currentPositionMs.value = 0
                _currentlyPlayingUri.value = null
                stopProgressTracker()
            }

            mediaPlayer = player
            _currentlyPlayingUri.value = uri
            _totalDurationMs.value = player.duration
            player.start()
            _isPlaying.value = true
            startProgressTracker()

            Log.d(TAG, "Started playback for $uri, duration: ${player.duration}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio from $uri", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    _isPlaying.value = true
                    startProgressTracker()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming audio", e)
        }
    }

    fun seekTo(progress: Float) {
        try {
            mediaPlayer?.let { player ->
                val targetMs = (player.duration * progress.coerceIn(0f, 1f)).toInt()
                player.seekTo(targetMs)
                _currentPositionMs.value = targetMs
                _currentProgress.value = progress
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio", e)
        }
    }

    fun stop() {
        stopProgressTracker()
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentlyPlayingUri.value = null
            _currentProgress.value = 0f
            _currentPositionMs.value = 0
            _totalDurationMs.value = 0
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            val cur = player.currentPosition
                            val dur = player.duration
                            _currentPositionMs.value = cur
                            _totalDurationMs.value = dur
                            _currentProgress.value = if (dur > 0) cur.toFloat() / dur.toFloat() else 0f
                        }
                    } catch (_: Exception) {}
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun formatMs(ms: Int): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
    }
}
