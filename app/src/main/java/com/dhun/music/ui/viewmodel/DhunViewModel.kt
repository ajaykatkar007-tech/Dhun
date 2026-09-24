package com.dhun.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhun.music.DhunApplication
import com.dhun.music.model.Album
import com.dhun.music.model.Artist
import com.dhun.music.model.Folder
import com.dhun.music.model.PlaybackInfo
import com.dhun.music.model.Playlist
import com.dhun.music.model.Song
import com.dhun.music.ui.components.DhunNavTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DhunViewModel : ViewModel() {
    private val repository = DhunApplication.instance.repository
    private val playbackManager = DhunApplication.instance.playbackManager

    val playbackInfo: StateFlow<PlaybackInfo> = playbackManager.playbackInfo
    val allSongs: StateFlow<List<Song>> = repository.allSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentlyPlayedSongs: StateFlow<List<Song>> = repository.recentlyPlayedSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists: StateFlow<List<Playlist>> = repository.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val albums: StateFlow<List<Album>> = repository.albums.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val artists: StateFlow<List<Artist>> = repository.artists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders: StateFlow<List<Folder>> = repository.folders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    val selectedTab = MutableStateFlow(DhunNavTab.HOME)
    val searchQuery = MutableStateFlow("")
    val showNowPlaying = MutableStateFlow(false)
    val showQueue = MutableStateFlow(false)
    val activePlaylist = MutableStateFlow<Playlist?>(null)

    val searchResults: StateFlow<List<Song>> = combine(allSongs, searchQuery) { songs, query ->
        if (query.isBlank()) emptyList() else {
            val q = query.trim().lowercase()
            songs.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q) || it.folder.lowercase().contains(q) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playSong(song: Song, queue: List<Song>? = null) = playbackManager.playSong(song, queue)
    fun playQueueItem(index: Int) = playbackManager.playQueueItem(index)
    fun playOrPause() = playbackManager.playOrPause()
    fun next() = playbackManager.next()
    fun previous() = playbackManager.previous()
    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun toggleShuffle() = playbackManager.toggleShuffle()
    fun toggleRepeat() = playbackManager.toggleRepeat()
    fun addToQueue(song: Song) = playbackManager.addToQueue(song)
    fun playNext(song: Song) = playbackManager.playNext(song)
    fun removeFromQueue(index: Int) = playbackManager.removeFromQueue(index)
    fun clearQueue() = playbackManager.clearQueue()

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val newFavoriteState = repository.toggleFavorite(song.id)
            playbackManager.updateCurrentSongFavorite(song.id, newFavoriteState)
        }
    }

    fun createPlaylist(name: String) { viewModelScope.launch { repository.createPlaylist(name) } }
    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch { repository.renamePlaylist(playlistId, newName) }
    }
    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (activePlaylist.value?.id == playlistId) activePlaylist.value = null
        }
    }
    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.addSongToPlaylist(playlistId, songId) }
    }
    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { repository.removeSongFromPlaylist(playlistId, songId) }
    }
    fun getPlaylistSongs(playlistId: Long) = repository.getPlaylistSongs(playlistId)

    fun rescanMusic(ignoreShortAudio: Boolean = true) {
        viewModelScope.launch {
            _isScanning.value = true
            try { repository.rescanMusic(ignoreShortAudio) } finally { _isScanning.value = false }
        }
    }
}