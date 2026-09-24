package com.dhun.music

import com.dhun.music.model.PlaybackInfo
import com.dhun.music.model.PlayerStatus
import com.dhun.music.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackModelTest {
    private val song = Song(
        id = 1L,
        title = "Test Song",
        artist = "Test Artist",
        album = "Test Album",
        durationMs = 180_000L,
        contentUri = "content://media/external/audio/1"
    )

    @Test
    fun pausedPlaybackIsNotPlaying() {
        val info = PlaybackInfo(currentSong = song, status = PlayerStatus.PAUSED)
        assertFalse(info.isPlaying)
    }

    @Test
    fun playingPlaybackIsPlaying() {
        val info = PlaybackInfo(currentSong = song, status = PlayerStatus.PLAYING)
        assertTrue(info.isPlaying)
    }

    @Test
    fun progressUsesPositionAndDuration() {
        val info = PlaybackInfo(
            currentSong = song,
            status = PlayerStatus.PLAYING,
            currentPositionMs = 90_000L,
            durationMs = 180_000L
        )
        assertEquals(0.5f, info.progress, 0.001f)
    }
}
