package com.dhun.music
import android.app.Application
import com.dhun.music.database.DhunDatabase
import com.dhun.music.playback.AudioPlaybackManager
import com.dhun.music.repository.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
class DhunApplication:Application(){
 lateinit var database:DhunDatabase; private set
 lateinit var repository:MusicRepository; private set
 lateinit var playbackManager:AudioPlaybackManager; private set
 override fun onCreate(){super.onCreate();instance=this;database=DhunDatabase.getInstance(this);repository=MusicRepository(this,database.dhunDao());playbackManager=AudioPlaybackManager.getInstance(this);playbackManager.setOnSongChangeListener{repository.recordRecentlyPlayed(it.id)};CoroutineScope(Dispatchers.IO).launch{runCatching{if(repository.allSongs.first().isEmpty())repository.rescanMusic()}}}
 companion object { lateinit var instance:DhunApplication; private set }
}