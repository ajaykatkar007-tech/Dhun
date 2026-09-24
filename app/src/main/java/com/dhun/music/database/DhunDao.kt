package com.dhun.music.database
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao interface DhunDao {
 @Query("SELECT * FROM cached_songs ORDER BY title COLLATE NOCASE ASC") fun getAllSongs():Flow<List<SongEntity>>
 @Query("SELECT * FROM cached_songs WHERE id=:songId LIMIT 1") suspend fun getSongById(songId:Long):SongEntity?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertSongs(songs:List<SongEntity>)
 @Query("DELETE FROM cached_songs") suspend fun clearScannedSongs()
 @Transaction
 suspend fun replaceSongs(songs:List<SongEntity>) { clearScannedSongs(); insertSongs(songs) }
 @Query("SELECT * FROM favorites ORDER BY addedAt DESC") fun getAllFavorites():Flow<List<FavoriteEntity>>
 @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId=:songId)") fun isFavorite(songId:Long):Flow<Boolean>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertFavorite(favorite:FavoriteEntity)
 @Query("DELETE FROM favorites WHERE songId=:songId") suspend fun deleteFavorite(songId:Long)
 @Query("SELECT * FROM playlists ORDER BY createdAt DESC") fun getAllPlaylists():Flow<List<PlaylistEntity>>
 @Query("SELECT * FROM playlists WHERE id=:playlistId LIMIT 1") suspend fun getPlaylistById(playlistId:Long):PlaylistEntity?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertPlaylist(playlist:PlaylistEntity):Long
 @Query("UPDATE playlists SET name=:name WHERE id=:id") suspend fun updatePlaylistName(id:Long,name:String)
 @Query("DELETE FROM playlists WHERE id=:id") suspend fun deletePlaylist(id:Long)
 @Query("SELECT * FROM playlist_songs WHERE playlistId=:playlistId ORDER BY orderIndex ASC") fun getPlaylistSongs(playlistId:Long):Flow<List<PlaylistSongEntity>>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertPlaylistSong(playlistSong:PlaylistSongEntity)
 @Query("DELETE FROM playlist_songs WHERE playlistId=:playlistId AND songId=:songId") suspend fun removeSongFromPlaylist(playlistId:Long,songId:Long)
 @Query("DELETE FROM playlist_songs WHERE playlistId=:playlistId") suspend fun clearPlaylistSongs(playlistId:Long)
 @Query("SELECT * FROM recently_played ORDER BY playedAt DESC LIMIT 50") fun getRecentlyPlayed():Flow<List<RecentlyPlayedEntity>>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertRecentlyPlayed(recent:RecentlyPlayedEntity)
 @Query("DELETE FROM recently_played WHERE songId=:songId") suspend fun deleteRecentlyPlayed(songId:Long)
}