package com.example.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.data.MusicRepository
import com.example.model.PlaybackSnapshot
import com.example.model.RepeatMode
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MusicPlaybackManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    val repository = MusicRepository(context)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false

    // Real-time listening statistics tracking
    private var currentSongForStats: Song? = null
    private var lastPlayStartTimeRealtime: Long = 0L
    private var accumulatedActiveListeningMs: Long = 0L
    private var hasPlayBeenCountedForCurrentInstance: Boolean = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_snapshot.value.isPlaying) {
                    wasPlayingBeforeFocusLoss = true
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (wasPlayingBeforeFocusLoss) {
                    wasPlayingBeforeFocusLoss = false
                    play()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPlayingBeforeFocusLoss = false
                pause()
            }
        }
    }

    private var isNoisyReceiverRegistered = false
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (_snapshot.value.isPlaying) {
                    pause()
                }
            }
        }
    }

    private val originalQueue = mutableListOf<Song>()
    private val activeQueue = mutableListOf<Song>()

    var onSongPlayedListener: ((Song) -> Unit)? = null

    private val _snapshot = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    init {
        MusicService.playbackManager = this
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            focusRequest = req
            am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            try {
                context.registerReceiver(
                    noisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                )
                isNoisyReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                context.unregisterReceiver(noisyReceiver)
            } catch (_: Exception) {}
            isNoisyReceiverRegistered = false
        }
    }

    private fun startServiceIfAllowed() {
        try {
            MusicService.playbackManager = this
            val intent = Intent(context, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.w("MusicPlaybackManager", "Service start exception: ${e.message}")
        }
    }

    fun playSong(song: Song, newQueue: List<Song> = emptyList(), startPositionMs: Long = 0L) {
        if (newQueue.isNotEmpty()) {
            originalQueue.clear()
            originalQueue.addAll(newQueue)
            rebuildActiveQueue(keepCurrentSong = song)
        } else if (!activeQueue.contains(song)) {
            activeQueue.add(song)
            originalQueue.add(song)
        }

        val targetIndex = activeQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playIndex(targetIndex, startPositionMs)
    }

    fun playQueue(queue: List<Song>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        originalQueue.clear()
        originalQueue.addAll(queue)
        rebuildActiveQueue()
        val index = startIndex.coerceIn(0, activeQueue.lastIndex)
        playIndex(index)
    }

    fun playNext(song: Song) {
        if (activeQueue.isEmpty()) {
            playSong(song)
            return
        }
        val currentIndex = _snapshot.value.currentQueueIndex
        val targetIndex = (currentIndex + 1).coerceIn(0, activeQueue.size)
        val existingIndex = activeQueue.indexOfFirst { it.id == song.id }
        if (existingIndex > currentIndex) {
            activeQueue.removeAt(existingIndex)
            originalQueue.removeAll { it.id == song.id }
        }
        activeQueue.add(targetIndex, song)
        originalQueue.add(targetIndex.coerceIn(0, originalQueue.size), song)
        val newCurrentIndex = activeQueue.indexOfFirst { it.id == _snapshot.value.currentSong?.id }
        _snapshot.value = _snapshot.value.copy(
            queue = activeQueue.toList(),
            currentQueueIndex = newCurrentIndex
        )
    }

    fun addToQueue(song: Song) {
        if (activeQueue.isEmpty()) {
            playSong(song)
            return
        }
        activeQueue.add(song)
        originalQueue.add(song)
        val currentSong = _snapshot.value.currentSong
        val newCurrentIndex = activeQueue.indexOfFirst { it.id == currentSong?.id }
        _snapshot.value = _snapshot.value.copy(
            queue = activeQueue.toList(),
            currentQueueIndex = newCurrentIndex
        )
    }

    private fun playIndex(index: Int, startPositionMs: Long = 0L) {
        if (index !in activeQueue.indices) return

        // Flush any accumulated listening time for previous track before switching
        flushListeningTimeToDb(isCurrentTrackEnding = true)

        startServiceIfAllowed()
        requestAudioFocus()
        registerNoisyReceiver()
        val song = activeQueue[index]
        releasePlayer()

        // Reset listening statistics state for the new song instance
        currentSongForStats = song
        accumulatedActiveListeningMs = 0L
        hasPlayBeenCountedForCurrentInstance = false
        lastPlayStartTimeRealtime = SystemClock.elapsedRealtime()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (song.uriString.startsWith("content://") || song.uriString.startsWith("android.resource://")) {
                    setDataSource(context, Uri.parse(song.uriString))
                } else if (song.uriString.startsWith("file://")) {
                    setDataSource(context, Uri.parse(song.uriString))
                } else {
                    val file = File(song.uriString)
                    if (file.exists()) {
                        setDataSource(file.absolutePath)
                    } else {
                        setDataSource(context, Uri.parse(song.uriString))
                    }
                }

                prepare()
                if (startPositionMs > 0L) {
                    try {
                        seekTo(startPositionMs.toInt())
                    } catch (_: Exception) {}
                }
            }

            mediaPlayer = player
            player.start()

            val detectedDuration = if (player.duration > 0) player.duration.toLong() else song.durationMs
            val initialPos = if (startPositionMs > 0L) startPositionMs else 0L

            _snapshot.value = _snapshot.value.copy(
                currentSong = song,
                isPlaying = true,
                currentPositionMs = initialPos,
                durationMs = detectedDuration,
                queue = activeQueue.toList(),
                currentQueueIndex = index
            )

            onSongPlayedListener?.invoke(song)

            player.setOnCompletionListener {
                handleTrackCompletion()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e("MusicPlaybackManager", "MediaPlayer error: what=$what extra=$extra")
                _snapshot.value = _snapshot.value.copy(isPlaying = false)
                true
            }

            startPositionTicker()
            notifyServicePlaybackChanged()

        } catch (e: Exception) {
            Log.e("MusicPlaybackManager", "Failed to play ${song.title}: ${e.message}")
            _snapshot.value = _snapshot.value.copy(isPlaying = false)
        }
    }

    private fun handleTrackCompletion() {
        flushListeningTimeToDb(isCurrentTrackEnding = true)

        val currentRepeat = _snapshot.value.repeatMode
        when (currentRepeat) {
            RepeatMode.ONE -> {
                // Replay same song; reset stats counters so this new instance counts toward plays
                accumulatedActiveListeningMs = 0L
                hasPlayBeenCountedForCurrentInstance = false
                lastPlayStartTimeRealtime = SystemClock.elapsedRealtime()

                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()
                _snapshot.value = _snapshot.value.copy(isPlaying = true, currentPositionMs = 0L)
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                val currentIndex = _snapshot.value.currentQueueIndex
                if (currentIndex < activeQueue.lastIndex) {
                    skipNext()
                } else {
                    _snapshot.value = _snapshot.value.copy(
                        isPlaying = false,
                        currentPositionMs = _snapshot.value.durationMs
                    )
                    stopPositionTicker()
                    notifyServicePlaybackChanged()
                }
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player == null) {
            val song = _snapshot.value.currentSong ?: activeQueue.firstOrNull()
            if (song != null) {
                playSong(song)
            }
            return
        }

        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        startServiceIfAllowed()
        requestAudioFocus()
        registerNoisyReceiver()
        val player = mediaPlayer
        if (player != null) {
            if (!player.isPlaying) {
                lastPlayStartTimeRealtime = SystemClock.elapsedRealtime()
                player.start()
                _snapshot.value = _snapshot.value.copy(isPlaying = true)
                startPositionTicker()
                notifyServicePlaybackChanged()
            }
        } else {
            val song = _snapshot.value.currentSong ?: activeQueue.firstOrNull()
            if (song != null) playSong(song, startPositionMs = _snapshot.value.currentPositionMs)
        }
    }

    fun pause() {
        unregisterNoisyReceiver()
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            flushListeningTimeToDb(isCurrentTrackEnding = false)
            val currentPos = try { player.currentPosition.toLong() } catch (_: Exception) { null } ?: _snapshot.value.currentPositionMs
            player.pause()
            _snapshot.value = _snapshot.value.copy(
                isPlaying = false,
                currentPositionMs = currentPos
            )
            stopPositionTicker()
            notifyServicePlaybackChanged()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer
        val target = positionMs.coerceAtLeast(0L)
        if (player != null) {
            try {
                player.seekTo(target.toInt())
            } catch (_: Exception) {}
        }
        _snapshot.value = _snapshot.value.copy(currentPositionMs = target)
        notifyServicePlaybackChanged()
    }

    fun skipNext() {
        if (activeQueue.isEmpty()) return
        val currentIndex = _snapshot.value.currentQueueIndex
        val nextIndex = if (currentIndex + 1 < activeQueue.size) {
            currentIndex + 1
        } else {
            0
        }
        playIndex(nextIndex)
    }

    fun skipPrevious() {
        if (activeQueue.isEmpty()) return
        val currentPos = _snapshot.value.currentPositionMs
        if (currentPos > 3000L) {
            // Replay from beginning: reset stats counters so this replay counts
            flushListeningTimeToDb(isCurrentTrackEnding = false)
            accumulatedActiveListeningMs = 0L
            hasPlayBeenCountedForCurrentInstance = false
            lastPlayStartTimeRealtime = SystemClock.elapsedRealtime()

            seekTo(0L)
            return
        }

        val currentIndex = _snapshot.value.currentQueueIndex
        val prevIndex = if (currentIndex - 1 >= 0) {
            currentIndex - 1
        } else {
            activeQueue.lastIndex
        }
        playIndex(prevIndex)
    }

    fun toggleShuffle() {
        val currentShuffle = _snapshot.value.isShuffle
        val newShuffle = !currentShuffle
        val currentSong = _snapshot.value.currentSong

        _snapshot.value = _snapshot.value.copy(isShuffle = newShuffle)
        rebuildActiveQueue(keepCurrentSong = currentSong)
    }

    private fun rebuildActiveQueue(keepCurrentSong: Song? = null) {
        val isShuffle = _snapshot.value.isShuffle
        activeQueue.clear()
        if (isShuffle) {
            val shuffled = originalQueue.toMutableList()
            if (keepCurrentSong != null) {
                shuffled.remove(keepCurrentSong)
                shuffled.shuffle()
                activeQueue.add(keepCurrentSong)
                activeQueue.addAll(shuffled)
            } else {
                shuffled.shuffle()
                activeQueue.addAll(shuffled)
            }
        } else {
            activeQueue.addAll(originalQueue)
        }

        val currentSong = keepCurrentSong ?: _snapshot.value.currentSong
        val newIndex = activeQueue.indexOfFirst { it.id == currentSong?.id }
        _snapshot.value = _snapshot.value.copy(
            queue = activeQueue.toList(),
            currentQueueIndex = newIndex
        )
    }

    fun cycleRepeatMode() {
        val nextMode = when (_snapshot.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _snapshot.value = _snapshot.value.copy(repeatMode = nextMode)
    }

    /**
     * Flushes active listening time to the Room database on a background thread.
     * Evaluates play qualification: >= 30 seconds (or >= 50% duration if song is shorter).
     */
    private fun flushListeningTimeToDb(isCurrentTrackEnding: Boolean) {
        val song = currentSongForStats ?: return
        if (lastPlayStartTimeRealtime <= 0L) return

        val now = SystemClock.elapsedRealtime()
        val sessionDelta = (now - lastPlayStartTimeRealtime).coerceAtLeast(0L)
        lastPlayStartTimeRealtime = if (isCurrentTrackEnding) 0L else now

        accumulatedActiveListeningMs += sessionDelta

        // Determine if a play should be recorded
        val shouldCountPlay = if (!hasPlayBeenCountedForCurrentInstance) {
            val threshold = if (song.durationMs in 1..29999L) {
                song.durationMs / 2
            } else {
                30000L
            }
            accumulatedActiveListeningMs >= threshold
        } else {
            false
        }

        if (shouldCountPlay) {
            hasPlayBeenCountedForCurrentInstance = true
        }

        if (sessionDelta > 0L || shouldCountPlay) {
            scope.launch(Dispatchers.IO) {
                try {
                    repository.recordPlaybackSession(
                        song = song,
                        timeListenedMs = sessionDelta,
                        isCountedPlay = shouldCountPlay
                    )
                } catch (e: Exception) {
                    Log.e("MusicPlaybackManager", "Error saving listening stats: ${e.message}")
                }
            }
        }
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        tickerJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val current = player.currentPosition.toLong()
                        val dur = if (player.duration > 0) player.duration.toLong() else _snapshot.value.durationMs

                        _snapshot.value = _snapshot.value.copy(
                            currentPositionMs = current,
                            durationMs = dur
                        )

                        // Periodic flush of real listening time every ~5 seconds
                        flushListeningTimeToDb(isCurrentTrackEnding = false)
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopPositionTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun notifyServicePlaybackChanged() {
        val currentSnapshot = _snapshot.value
        MusicService.instance?.updateState(currentSnapshot)
    }

    private fun releasePlayer() {
        stopPositionTicker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        flushListeningTimeToDb(isCurrentTrackEnding = true)
        releasePlayer()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        scope.launch {
            _snapshot.value = PlaybackSnapshot()
        }
    }
}
