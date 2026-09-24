package com.dhun.music.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import com.dhun.music.model.PlaybackInfo
import com.dhun.music.model.PlayerStatus
import com.dhun.music.model.RepeatMode
import com.dhun.music.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlaybackManager private constructor(private val appContext: Context) {

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var playWhenReady = false
    private var audioFocusRequest: AudioFocusRequest? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _playbackInfo = MutableStateFlow(PlaybackInfo())
    val playbackInfo: StateFlow<PlaybackInfo> = _playbackInfo.asStateFlow()

    private var originalQueue: List<Song> = emptyList()
    private var onSongChangeListener: ((Song) -> Unit)? = null

    private var userPaused = false
    private var resumeOnFocusGain = false

    private val reusableAudioFocusRequest: AudioFocusRequest? by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@lazy null
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build())
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                userPaused = false
                pause(userInitiated = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnFocusGain = _playbackInfo.value.isPlaying && !userPaused
                pause(userInitiated = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                if (resumeOnFocusGain && !userPaused) {
                    resumeFromFocusGain()
                }
                resumeOnFocusGain = false
            }
        }
    }

    fun setOnSongChangeListener(listener: (Song) -> Unit) {
        onSongChangeListener = listener
    }

    /** Favorite metadata update only; never changes playback transport state. */
    fun updateCurrentSongFavorite(songId: Long, isFavorite: Boolean) {
        _playbackInfo.update { info ->
            val current = info.currentSong
            val updatedCurrent = if (current?.id == songId) current.copy(isFavorite = isFavorite) else current
            val updatedQueue = info.queue.map { if (it.id == songId) it.copy(isFavorite = isFavorite) else it }
            info.copy(currentSong = updatedCurrent, queue = updatedQueue)
        }
    }

    fun playSong(song: Song, queue: List<Song>? = null) {
        userPaused = false
        resumeOnFocusGain = false
        val targetQueue = queue ?: listOf(song)
        originalQueue = targetQueue
        val activeQueue = if (_playbackInfo.value.isShuffle) {
            val shuffled = targetQueue.toMutableList()
            shuffled.remove(song)
            shuffled.shuffle()
            listOf(song) + shuffled
        } else targetQueue

        val index = activeQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        _playbackInfo.update {
            it.copy(currentSong = song, queue = activeQueue, queueIndex = index,
                status = PlayerStatus.BUFFERING, currentPositionMs = 0L, durationMs = song.durationMs)
        }
        playWhenReady = true
        startPlayback(song)
        onSongChangeListener?.invoke(song)
        updateServiceNotification()
    }

    fun playQueueItem(index: Int) {
        if (index !in _playbackInfo.value.queue.indices) return
        userPaused = false
        resumeOnFocusGain = false
        playWhenReady = true
        startQueuedSong(index)
    }

    private fun startPlayback(song: Song) {
        stopProgressTracker()
        releasePlayer()
        if (!requestAudioFocus()) {
            _playbackInfo.update { it.copy(status = PlayerStatus.ERROR, errorMessage = "Could not gain audio focus") }
            return
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build())
                setDataSource(appContext, Uri.parse(song.contentUri))
                setOnPreparedListener { mp ->
                    isPrepared = true
                    if (playWhenReady) mp.start()
                    val actualDuration = mp.duration.toLong().takeIf { it > 0 } ?: song.durationMs
                    _playbackInfo.update { it.copy(status = if (playWhenReady) PlayerStatus.PLAYING else PlayerStatus.PAUSED, durationMs = actualDuration, errorMessage = null) }
                    if (playWhenReady) startProgressTracker()
                    updateServiceNotification()
                }
                setOnCompletionListener { handleTrackCompletion() }
                setOnErrorListener { _, what, extra ->
                    releasePlayer()
                    _playbackInfo.update { it.copy(status = PlayerStatus.ERROR, errorMessage = "Playback error: $what, $extra") }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            _playbackInfo.update { it.copy(status = PlayerStatus.ERROR, errorMessage = e.localizedMessage ?: "Failed to play audio") }
        }
    }

    fun playOrPause() {
        if (_playbackInfo.value.isPlaying) pause() else resume()
    }

    fun pause(userInitiated: Boolean = true) {
        if (userInitiated) {
            userPaused = true
            resumeOnFocusGain = false
        }
        playWhenReady = false
        mediaPlayer?.let { if (it.isPlaying) it.pause() }
        stopProgressTracker()
        _playbackInfo.update { it.copy(status = PlayerStatus.PAUSED) }
        updateServiceNotification()
    }

    private fun resumeFromFocusGain() {
        val currentSong = _playbackInfo.value.currentSong ?: return
        val player = mediaPlayer ?: run {
            playWhenReady = true
            startPlayback(currentSong)
            return
        }
        if (!isPrepared) {
            playWhenReady = true
            return
        }
        try {
            playWhenReady = true
            player.start()
            _playbackInfo.update { it.copy(status = PlayerStatus.PLAYING) }
            startProgressTracker()
            updateServiceNotification()
        } catch (_: IllegalStateException) { }
    }

    fun resume() {
        userPaused = false
        resumeOnFocusGain = false
        val currentSong = _playbackInfo.value.currentSong
        if (currentSong == null) {
            _playbackInfo.value.queue.firstOrNull()?.let { playSong(it) }
            return
        }
        if (mediaPlayer == null) {
            playWhenReady = true
            startPlayback(currentSong)
            return
        }
        if (!isPrepared) {
            playWhenReady = true
        } else if (requestAudioFocus()) {
            playWhenReady = true
            mediaPlayer?.start()
            _playbackInfo.update { it.copy(status = PlayerStatus.PLAYING) }
            startProgressTracker()
            updateServiceNotification()
        }
    }

    fun next() {
        val current = _playbackInfo.value
        if (current.queue.isEmpty()) return
        val nextIndex = if (current.queueIndex + 1 < current.queue.size) current.queueIndex + 1
        else if (current.repeatMode == RepeatMode.ALL) 0 else -1
        if (nextIndex != -1) {
            val nextSong = current.queue[nextIndex]
            _playbackInfo.update { it.copy(queueIndex = nextIndex) }
            playQueueItem(nextIndex)
        } else {
            pause()
            seekTo(0)
        }
    }

    fun previous() {
        val current = _playbackInfo.value
        if (current.queue.isEmpty()) return
        if (current.currentPositionMs > 3000L) {
            seekTo(0)
            return
        }
        val prevIndex = if (current.queueIndex - 1 >= 0) current.queueIndex - 1
        else if (current.repeatMode == RepeatMode.ALL) current.queue.size - 1 else 0
        val prevSong = current.queue[prevIndex]
        _playbackInfo.update { it.copy(queueIndex = prevIndex) }
        playQueueItem(prevIndex)
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            if (!isPrepared) return
            val safePos = positionMs.coerceIn(0L, player.duration.toLong().coerceAtLeast(1L))
            player.seekTo(safePos.toInt())
            _playbackInfo.update { it.copy(currentPositionMs = safePos) }
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_playbackInfo.value.isShuffle
        val currentSong = _playbackInfo.value.currentSong
        val updatedQueue = if (newShuffle) {
            val shuffled = originalQueue.toMutableList()
            if (currentSong != null) {
                shuffled.remove(currentSong)
                shuffled.shuffle()
                listOf(currentSong) + shuffled
            } else shuffled.shuffled()
        } else originalQueue
        val newIndex = currentSong?.let { song -> updatedQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0) } ?: 0
        _playbackInfo.update { it.copy(isShuffle = newShuffle, queue = updatedQueue, queueIndex = newIndex) }
    }

    fun toggleRepeat() {
        val nextMode = when (_playbackInfo.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackInfo.update { it.copy(repeatMode = nextMode) }
    }

    fun addToQueue(song: Song) {
        val currentQueue = _playbackInfo.value.queue.toMutableList()
        currentQueue.add(song)
        _playbackInfo.update { it.copy(queue = currentQueue) }
        if (_playbackInfo.value.currentSong == null) playSong(song, currentQueue)
    }

    fun playNext(song: Song) {
        val current = _playbackInfo.value
        val currentQueue = current.queue.toMutableList()
        val insertIndex = (current.queueIndex + 1).coerceAtMost(currentQueue.size)
        currentQueue.add(insertIndex, song)
        _playbackInfo.update { it.copy(queue = currentQueue) }
        if (current.currentSong == null) playSong(song, currentQueue)
    }

    fun removeFromQueue(index: Int) {
        val current = _playbackInfo.value
        if (index !in current.queue.indices) return
        val currentQueue = current.queue.toMutableList().apply { removeAt(index) }
        val wasCurrent = index == current.queueIndex
        val newIndex = when {
            index < current.queueIndex -> current.queueIndex - 1
            index == current.queueIndex -> if (currentQueue.isEmpty()) { pause(); -1 } else current.queueIndex.coerceAtMost(currentQueue.size - 1)
            else -> current.queueIndex
        }
        _playbackInfo.update {
            it.copy(queue = currentQueue, queueIndex = newIndex,
                currentSong = if (newIndex >= 0) currentQueue[newIndex] else null)
        }
        if (wasCurrent && newIndex >= 0) {
            playWhenReady = true
            startQueuedSong(newIndex)
        }
    }

    fun clearQueue() {
        pause()
        releasePlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
        audioFocusRequest = null
        _playbackInfo.update { it.copy(currentSong = null, queue = emptyList(), queueIndex = -1,
            status = PlayerStatus.IDLE, currentPositionMs = 0L, durationMs = 0L) }
        DhunPlaybackService.stopService(appContext)
    }

    private fun handleTrackCompletion() {
        val current = _playbackInfo.value
        when (current.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                startProgressTracker()
            }
            RepeatMode.ALL -> next()
            RepeatMode.OFF -> if (current.queueIndex + 1 < current.queue.size) next() else {
                pause()
                seekTo(0)
            }
        }
    }

    private fun startQueuedSong(index: Int) {
        val queue = _playbackInfo.value.queue
        val song = queue.getOrNull(index) ?: return
        _playbackInfo.update { it.copy(currentSong = song, queueIndex = index,
            status = PlayerStatus.BUFFERING, currentPositionMs = 0L, durationMs = song.durationMs) }
        startPlayback(song)
        onSongChangeListener?.invoke(song)
        updateServiceNotification()
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) _playbackInfo.update { it.copy(currentPositionMs = player.currentPosition.toLong()) }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = reusableAudioFocusRequest ?: return false
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun releasePlayer() {
        isPrepared = false
        mediaPlayer?.let {
            try { it.stop() } catch (_: Exception) { }
            it.release()
        }
        mediaPlayer = null
    }

    private fun updateServiceNotification() {
        val song = _playbackInfo.value.currentSong ?: return
        DhunPlaybackService.startOrUpdateService(appContext, song, _playbackInfo.value.isPlaying)
    }

    companion object {
        @Volatile private var INSTANCE: AudioPlaybackManager? = null
        fun getInstance(context: Context): AudioPlaybackManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: AudioPlaybackManager(context.applicationContext).also { INSTANCE = it }
        }
    }
}
