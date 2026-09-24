package com.dhun.music
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dhun.music.model.Song
import com.dhun.music.ui.components.*
import com.dhun.music.ui.theme.*
import com.dhun.music.ui.viewmodel.DhunViewModel

class MainActivity:ComponentActivity(){
 private val permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);requestMusicPermission();setContent{DhunTheme{DhunApp()}}}
 private fun requestMusicPermission(){val p=if(Build.VERSION.SDK_INT>=33)arrayOf(Manifest.permission.READ_MEDIA_AUDIO,Manifest.permission.POST_NOTIFICATIONS) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);val needed=p.filter{checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED};if(needed.isNotEmpty())permissionLauncher.launch(needed.toTypedArray())}
}
@Composable private fun DhunApp(vm:DhunViewModel=viewModel()){
 val playback by vm.playbackInfo.collectAsStateWithLifecycle()
 val songs by vm.allSongs.collectAsStateWithLifecycle()
 val favs by vm.favoriteSongs.collectAsStateWithLifecycle()
 var tab by remember{mutableStateOf(DhunNavTab.HOME)}
 var nowPlaying by remember{mutableStateOf(false)}
 Scaffold(containerColor=DhunBackground,bottomBar={DhunBottomNav(tab,{tab=it})}){pad->
  Box(Modifier.fillMaxSize().padding(pad)){
   when(tab){
    DhunNavTab.HOME->Home(songs,playback,{vm.playSong(it,songs)},{nowPlaying=true},vm)
    DhunNavTab.SEARCH->Search(vm,{vm.playSong(it,songs);nowPlaying=true})
    DhunNavTab.LIBRARY->Library(songs,favs,vm,{nowPlaying=true})
    DhunNavTab.SETTINGS->Settings(vm)
   }
   if(playback.currentSong!=null&&!nowPlaying)MiniPlayer(playback,{nowPlaying=true},vm::playOrPause,vm::next,Modifier.align(Alignment.BottomCenter))
   if(nowPlaying&&playback.currentSong!=null)NowPlaying(playback,vm,{nowPlaying=false})
  }
 }
}
@Composable private fun Home(songs:List<Song>,playback:com.dhun.music.model.PlaybackInfo,onPlay:(Song)->Unit,onOpen:()->Unit,vm:DhunViewModel){
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("DHUN",style=MaterialTheme.typography.headlineSmall,color=DhunPink);Text("Your music, your vibe.",color=DhunTextSecondary)}
 item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({if(songs.isNotEmpty())onPlay(songs.first())}){Text("Play All")};OutlinedButton({vm.rescanMusic()}){Text("Rescan")}}}
 item{Text("Recently Added",style=MaterialTheme.typography.titleLarge,color=DhunTextPrimary)}
 items(songs.take(20),key={it.id}){SongRow(it,{onPlay(it)},{vm.toggleFavorite(it)})}
 }}
@Composable private fun Search(vm:DhunViewModel,onPlay:(Song)->Unit){
 val q by vm.searchQuery.collectAsStateWithLifecycle();val results by vm.searchResults.collectAsStateWithLifecycle()
 Column(Modifier.fillMaxSize().padding(16.dp)){OutlinedTextField(q,{vm.searchQuery.value=it},Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("Search songs, artists, albums")});Spacer(Modifier.height(12.dp));LazyColumn{items(results,key={it.id}){SongRow(it,{onPlay(it)},{vm.toggleFavorite(it)})}}}
}
@Composable private fun Library(songs:List<Song>,favs:List<Song>,vm:DhunViewModel,onOpen:()->Unit){
 Column(Modifier.fillMaxSize().padding(16.dp)){Text("Your Library",style=MaterialTheme.typography.headlineSmall,color=DhunTextPrimary);Text("Favorites: "+favs.size,color=DhunTextSecondary);Spacer(Modifier.height(8.dp));LazyColumn{items(songs,key={it.id}){SongRow(it,{vm.playSong(it,songs);onOpen()},{vm.toggleFavorite(it)})}}}
}
@Composable private fun Settings(vm:DhunViewModel){
 Column(Modifier.fillMaxSize().padding(16.dp)){Text("Settings",style=MaterialTheme.typography.headlineSmall,color=DhunTextPrimary);Spacer(Modifier.height(16.dp));Button({vm.rescanMusic()}){Text("Rescan Music")};Spacer(Modifier.height(8.dp));Text("Dhun 1.0.0 • Local music player",color=DhunTextSecondary)}
}
@Composable private fun SongRow(song:Song,onPlay:()->Unit,onFavorite:()->Unit){
 Row(Modifier.fillMaxWidth().clickable{onPlay()}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){ArtworkView(song.albumArtUri,song.title);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(song.title,color=DhunTextPrimary,maxLines=1);Text(song.artist,color=DhunTextSecondary,maxLines=1)};IconButton(onClick=onFavorite){Icon(if(song.isFavorite)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(song.isFavorite)DhunPink else DhunTextSecondary)}}
}
@Composable private fun NowPlaying(info:com.dhun.music.model.PlaybackInfo,vm:DhunViewModel,onClose:()->Unit){
 val song=info.currentSong!!
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DhunVioletDark,DhunBackground,DhunBackground))).padding(20.dp)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.fillMaxWidth()){IconButton(onClick=onClose){Icon(Icons.Default.KeyboardArrowDown,"Close",tint=DhunTextPrimary)};Spacer(Modifier.weight(1f))};Spacer(Modifier.height(30.dp));ArtworkView(song.albumArtUri,song.title,size=290.dp,shape=RoundedCornerShape(22.dp),iconSize=64.dp);Spacer(Modifier.height(22.dp));Text(song.title,style=MaterialTheme.typography.headlineSmall,color=DhunTextPrimary);Text(song.artist,color=DhunTextSecondary);Spacer(Modifier.height(24.dp));Slider(value=info.progress,valueRange=0f..1f,onValueChange={vm.seekTo((it*info.durationMs).toLong())});Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically){IconButton(onClick=vm::toggleShuffle){Icon(Icons.Default.Shuffle,null,tint=if(info.isShuffle)DhunPink else DhunTextPrimary)};IconButton(onClick=vm::previous){Icon(Icons.Default.SkipPrevious,null,tint=DhunTextPrimary)};IconButton(onClick=vm::playOrPause,modifier=Modifier.size(68.dp)){Surface(shape=CircleShape,color=DhunViolet){Icon(if(info.isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow,null,tint=DhunTextPrimary,modifier=Modifier.padding(18.dp))}};IconButton(onClick=vm::next){Icon(Icons.Default.SkipNext,null,tint=DhunTextPrimary)};IconButton(onClick=vm::toggleRepeat){Icon(Icons.Default.Repeat,null,tint=DhunTextPrimary)}};IconButton(onClick={vm.toggleFavorite(song)}){Icon(if(song.isFavorite)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=DhunPink)}}}
}