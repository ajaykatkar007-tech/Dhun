package com.dhun.music.database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(tableName="cached_songs") data class SongEntity(@PrimaryKey val id:Long,val title:String,val artist:String,val album:String,val durationMs:Long,val contentUri:String,val albumArtUri:String?,val folder:String,val dateAdded:Long,val isLocalSample:Boolean=false)
@Entity(tableName="favorites") data class FavoriteEntity(@PrimaryKey val songId:Long,val addedAt:Long=System.currentTimeMillis())
@Entity(tableName="playlists") data class PlaylistEntity(@PrimaryKey(autoGenerate=true) val id:Long=0L,val name:String,val createdAt:Long=System.currentTimeMillis())
@Entity(tableName="playlist_songs",primaryKeys=["playlistId","songId"],foreignKeys=[ForeignKey(entity=PlaylistEntity::class,parentColumns=["id"],childColumns=["playlistId"],onDelete=ForeignKey.CASCADE)],indices=[Index(value=["playlistId"]),Index(value=["songId"])])
data class PlaylistSongEntity(val playlistId:Long,val songId:Long,val orderIndex:Int,val addedAt:Long=System.currentTimeMillis())
@Entity(tableName="recently_played") data class RecentlyPlayedEntity(@PrimaryKey val songId:Long,val playedAt:Long=System.currentTimeMillis())