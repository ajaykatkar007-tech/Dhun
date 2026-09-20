package com.dhun.music.repository

import android.content.Context
import com.dhun.music.database.DhunDao
import com.dhun.music.database.FavoriteEntity
import com.dhun.music.database.PlaylistEntity
import com.dhun.music.database.PlaylistSongEntity
import com.dhun.music.database.RecentlyPlayedEntity
import com.dhun.music.database.SongEntity
import com.dhun.music.model.Album
import com.dhun.music.model.Artist
import com.dhun.music.model.Folder
import com.dhun.music.model.Playlist
import com.dhun.music.model.Song
import com.dhun.music.scanner.MediaScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context, private val dao: DhunDao) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    val allSongs: Flow<List<Song>> = combine(dao.getAllSongs(), dao.getAllFavorites()) { entities, favorites ->
        val favoriteIds = favorites.map { it.songId }.toSet()
        entities.map { entity ->
            Song(entity.id, entity.title, entity.artist, entity.album, entity.durationMs,
                entity.contentUri, entity.albumArtUri, entity.folder, entity.dateAdded,
                favoriteIds.contains(entity.id), entity.isLocalSample)
        }
    }

    val favoriteSongs: Flow<List<Song>> = allSongs.map { songs -> songs.filter { it.isFavorite } }

    val recentlyPlayedSongs: Flow<List<Song>> = combine(allSongs, dao.getRecentlyPlayed()) { songs, recentEntries ->
        val songMap = songs.associateBy { it.id }
        recentEntries.mapNotNull { recent -> songMap[recent.songId] }
    }

    val playlists: Flow<List<Playlist>> = dao.getAllPlaylists().map { entities ->
        entities.map { Playlist(it.id, it.name, it.createdAt) }
    }

    val albums: Flow<List<Album>> = allSongs.map { songs ->
        songs.groupBy { it.album }.map { (name, group) ->
            Album(name, group.firstOrNull()?.artist ?: "Various Artists", group.size,
                group.firstOrNull { it.albumArtUri != null }?.albumArtUri)
        }.sortedBy { it.name.lowercase() }
    }

    val artists: Flow<List<Artist>> = allSongs.map { songs ->
        songs.groupBy { it.artist }.map { (name, group) ->
            Artist(name, group.size, group.map { it.album }.distinct().size)
        }.sortedBy { it.name.lowercase() }
    }

    val folders: Flow<List<Folder>> = allSongs.map { songs ->
        songs.groupBy { it.folder }.map { (name, group) -> Folder(name, name, group.size) }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun rescanMusic(ignoreShortAudio: Boolean = true) = withContext(Dispatchers.IO) {
        val minDuration = if (ignoreShortAudio) 30_000L else 5_000L
        val entities = MediaScanner.scanDeviceAudio(context, minDuration).map { song ->
            SongEntity(song.id, song.title, song.artist, song.album, song.durationMs,
                song.contentUri, song.albumArtUri, song.folder, song.dateAdded, song.isLocalSample)
        }
        dao.clearScannedSongs()
        dao.insertSongs(entities)
    }

    suspend fun toggleFavorite(songId: Long) = withContext(Dispatchers.IO) {
        if (dao.isFavorite(songId).first()) dao.deleteFavorite(songId)
        else dao.insertFavorite(FavoriteEntity(songId = songId))
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        dao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) =
        withContext(Dispatchers.IO) { dao.updatePlaylistName(playlistId, newName) }

    suspend fun deletePlaylist(playlistId: Long) =
        withContext(Dispatchers.IO) { dao.deletePlaylist(playlistId) }

    fun getPlaylistSongs(playlistId: Long): Flow<List<Song>> =
        combine(allSongs, dao.getPlaylistSongs(playlistId)) { songs, playlistSongs ->
            val songMap = songs.associateBy { it.id }
            playlistSongs.mapNotNull { songMap[it.songId] }
        }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val currentCount = dao.getPlaylistSongs(playlistId).first().size
        dao.insertPlaylistSong(PlaylistSongEntity(playlistId, songId, currentCount))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) =
        withContext(Dispatchers.IO) { dao.removeSongFromPlaylist(playlistId, songId) }

    fun recordRecentlyPlayed(songId: Long) {
        repositoryScope.launch { dao.insertRecentlyPlayed(RecentlyPlayedEntity(songId = songId)) }
    }
}
