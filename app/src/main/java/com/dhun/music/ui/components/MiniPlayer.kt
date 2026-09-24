package com.dhun.music.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.dhun.music.model.PlaybackInfo
import com.dhun.music.ui.theme.*
@Composable fun MiniPlayer(playbackInfo:PlaybackInfo,onOpenNowPlaying:()->Unit,onPlayPause:()->Unit,onNext:()->Unit,modifier:Modifier=Modifier){
 val s=playbackInfo.currentSong?:return
 Column(modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(14.dp)).background(DhunCardElevated).clickable{onOpenNowPlaying()}){
  Box(Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(listOf(DhunViolet,DhunPink))));Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
   ArtworkView(s.albumArtUri,s.title);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(s.title,maxLines=1);Text(s.artist,color=DhunTextSecondary,maxLines=1)}
   IconButton(onClick=onPlayPause){Icon(if(playbackInfo.isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow,null,tint=DhunPink)}
   IconButton(onClick=onNext){Icon(Icons.Default.SkipNext,null,tint=DhunTextPrimary)}
  }
 }
}