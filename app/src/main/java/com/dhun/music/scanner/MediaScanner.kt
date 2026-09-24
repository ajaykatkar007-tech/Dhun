package com.dhun.music.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.dhun.music.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaScanner {
    private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")

    /** Returns null when the scan itself failed (permission revoked, provider error), so callers keep the old library. */
    suspend fun scanDeviceAudio(context: Context, minDurationMs: Long = 30_000L): List<Song>? =
        withContext(Dispatchers.IO) {
            val songs = mutableListOf<Song>()
            val contentResolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.DATA
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
            val selectionArgs = arrayOf(minDurationMs.toString())
            val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            try {
                val cursor = contentResolver.query(collection, projection, selection, selectionArgs, sortOrder) ?: return@withContext null
                cursor.use {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Track $id"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val album = cursor.getString(albumColumn) ?: "Unknown Album"
                        val duration = cursor.getLong(durationColumn)
                        val albumId = cursor.getLong(albumIdColumn)
                        val dateAdded = cursor.getLong(dateAddedColumn)
                        val contentUri = ContentUris.withAppendedId(collection, id).toString()
                        val albumArtUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId).toString()
                        val filePath = if (dataColumn != -1) cursor.getString(dataColumn) else null
                        val folder = filePath?.let { File(it).parentFile?.name } ?: "Music"
                        songs.add(Song(id, title,
                            if (artist == "<unknown>") "Unknown Artist" else artist,
                            if (album == "<unknown>") "Unknown Album" else album,
                            duration, contentUri, albumArtUri, folder, dateAdded))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
            songs
        }
}