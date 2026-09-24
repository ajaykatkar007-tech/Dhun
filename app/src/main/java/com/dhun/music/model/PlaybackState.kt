package com.dhun.music.model

enum class PlayerStatus { IDLE, BUFFERING, PLAYING, PAUSED, ERROR }
enum class RepeatMode { OFF, ALL, ONE }

data class PlaybackInfo(
    val currentSong: Song? = null,
    val status: PlayerStatus = PlayerStatus.IDLE,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val errorMessage: String? = null
) {
    val isPlaying get() = status == PlayerStatus.PLAYING
    val progress get() = if (durationMs > 0) (currentPositionMs.toFloat()/durationMs).coerceIn(0f,1f) else 0f
    fun formatCurrentPosition(): String { val s=(currentPositionMs/1000).coerceAtLeast(0); return String.format("%d:%02d",s/60,s%60) }
    fun formatDuration(): String { val s=(durationMs/1000).coerceAtLeast(0); return String.format("%d:%02d",s/60,s%60) }
}