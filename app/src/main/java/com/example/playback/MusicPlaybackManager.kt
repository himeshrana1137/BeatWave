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
import android.util.Log
import com.example.data.MusicRepository
import com.example.model.PlaybackSnapshot
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.widget.BeatWaveWidgetProvider
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
    private var sleepTimerJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    val equalizerManager = AudioEqualizerManager(context)
    val repository = MusicRepository(context)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        val settings = repository.getAppSettings()
        if (!settings.autoPauseOnCallAndUnplug) return@OnAudioFocusChangeListener

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
                val settings = repository.getAppSettings()
                if (settings.autoPauseOnCallAndUnplug && _snapshot.value.isPlaying) {
                    pause()
                }
            }
        }
    }

    private val shakeDetector = ShakeDetector(context) {
        val settings = repository.getAppSettings()
        if (settings.shakeToSkipEnabled && _snapshot.value.isPlaying) {
            skipNext()
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

    fun playSong(song: Song, newQueue: List<Song> = emptyList()) {
        if (newQueue.isNotEmpty()) {
            originalQueue.clear()
            originalQueue.addAll(newQueue)
            rebuildActiveQueue(keepCurrentSong = song)
        } else if (!activeQueue.contains(song)) {
            activeQueue.add(song)
            originalQueue.add(song)
        }

        val targetIndex = activeQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playIndex(targetIndex)
    }

    fun playQueue(queue: List<Song>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        originalQueue.clear()
        originalQueue.addAll(queue)
        rebuildActiveQueue()
        val index = startIndex.coerceIn(0, activeQueue.lastIndex)
        playIndex(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in activeQueue.indices && toIndex in activeQueue.indices) {
            val item = activeQueue.removeAt(fromIndex)
            activeQueue.add(toIndex, item)
            val currentSong = _snapshot.value.currentSong
            val newIndex = activeQueue.indexOfFirst { it.id == currentSong?.id }
            _snapshot.value = _snapshot.value.copy(
                queue = activeQueue.toList(),
                currentQueueIndex = newIndex
            )
        }
    }

    private fun playIndex(index: Int) {
        if (index !in activeQueue.indices) return

        startServiceIfAllowed()
        requestAudioFocus()
        registerNoisyReceiver()
        val song = activeQueue[index]
        releasePlayer()

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
                start()
            }

            mediaPlayer = player

            // Attach hardware equalizer
            equalizerManager.attachAudioSession(player.audioSessionId)

            val detectedDuration = if (player.duration > 0) player.duration.toLong() else song.durationMs

            _snapshot.value = _snapshot.value.copy(
                currentSong = song,
                isPlaying = true,
                currentPositionMs = 0L,
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
        val currentRepeat = _snapshot.value.repeatMode
        when (currentRepeat) {
            RepeatMode.ONE -> {
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
                    _snapshot.value = _snapshot.value.copy(isPlaying = false, currentPositionMs = _snapshot.value.durationMs)
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
            player.pause()
            _snapshot.value = _snapshot.value.copy(isPlaying = false)
            stopPositionTicker()
        } else {
            startServiceIfAllowed()
            player.start()
            _snapshot.value = _snapshot.value.copy(isPlaying = true)
            startPositionTicker()
        }
        notifyServicePlaybackChanged()
    }

    fun play() {
        startServiceIfAllowed()
        requestAudioFocus()
        registerNoisyReceiver()
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
                _snapshot.value = _snapshot.value.copy(isPlaying = true)
                startPositionTicker()
                notifyServicePlaybackChanged()
            }
        } ?: run {
            val song = _snapshot.value.currentSong ?: activeQueue.firstOrNull()
            if (song != null) playSong(song)
        }
    }

    fun pause() {
        unregisterNoisyReceiver()
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _snapshot.value = _snapshot.value.copy(isPlaying = false)
                stopPositionTicker()
                notifyServicePlaybackChanged()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        val safePos = positionMs.coerceIn(0L, _snapshot.value.durationMs.coerceAtLeast(1L))
        player.seekTo(safePos.toInt())
        _snapshot.value = _snapshot.value.copy(currentPositionMs = safePos)
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

    // Sleep Timer
    fun setSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return

        var secondsRemaining = minutes * 60
        _snapshot.value = _snapshot.value.copy(sleepTimerRemainingSeconds = secondsRemaining)

        sleepTimerJob = scope.launch {
            while (isActive && secondsRemaining > 0) {
                delay(1000)
                secondsRemaining--
                _snapshot.value = _snapshot.value.copy(sleepTimerRemainingSeconds = secondsRemaining)
            }
            if (secondsRemaining <= 0) {
                pause()
                _snapshot.value = _snapshot.value.copy(sleepTimerRemainingSeconds = null)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _snapshot.value = _snapshot.value.copy(sleepTimerRemainingSeconds = null)
    }

    // A-B Repeat Support
    fun setPointA() {
        val current = _snapshot.value.currentPositionMs
        val currentB = _snapshot.value.abRepeatB
        _snapshot.value = _snapshot.value.copy(
            abRepeatA = current,
            abRepeatB = if (currentB != null && currentB <= current) null else currentB
        )
    }

    fun setPointB() {
        val a = _snapshot.value.abRepeatA ?: return
        val current = _snapshot.value.currentPositionMs
        if (current > a) {
            _snapshot.value = _snapshot.value.copy(abRepeatB = current)
        }
    }

    fun clearABRepeat() {
        _snapshot.value = _snapshot.value.copy(abRepeatA = null, abRepeatB = null)
    }

    private fun startPositionTicker() {
        stopPositionTicker()
        tickerJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val current = player.currentPosition.toLong()
                        val dur = if (player.duration > 0) player.duration.toLong() else _snapshot.value.durationMs

                        val a = _snapshot.value.abRepeatA
                        val b = _snapshot.value.abRepeatB
                        if (a != null && b != null && current >= b) {
                            player.seekTo(a.toInt())
                            _snapshot.value = _snapshot.value.copy(
                                currentPositionMs = a,
                                durationMs = dur
                            )
                        } else {
                            _snapshot.value = _snapshot.value.copy(
                                currentPositionMs = current,
                                durationMs = dur
                            )
                        }
                    }
                }
                delay(250)
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
        BeatWaveWidgetProvider.updateWidget(context, currentSnapshot.currentSong, currentSnapshot.isPlaying)

        val settings = repository.getAppSettings()
        if (currentSnapshot.isPlaying && settings.shakeToSkipEnabled) {
            shakeDetector.start()
        } else {
            shakeDetector.stop()
        }
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
        cancelSleepTimer()
        releasePlayer()
        abandonAudioFocus()
        unregisterNoisyReceiver()
        shakeDetector.stop()
        equalizerManager.release()
        scope.launch {
            _snapshot.value = PlaybackSnapshot()
        }
    }
}
