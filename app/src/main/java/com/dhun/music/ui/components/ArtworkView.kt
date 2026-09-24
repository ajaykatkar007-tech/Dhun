package com.dhun.music.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dhun.music.ui.theme.*
@Composable fun ArtworkView(artworkUri:String?,songTitle:String,modifier:Modifier=Modifier,size:Dp=48.dp,shape:RoundedCornerShape=RoundedCornerShape(8.dp),iconSize:Dp=24.dp){
 var failed by remember(artworkUri){mutableStateOf(false)}
 Box(modifier.size(size).clip(shape).background(Brush.linearGradient(listOf(DhunVioletDark,DhunViolet,DhunPink))),contentAlignment=Alignment.Center){
  if(!artworkUri.isNullOrEmpty()&&!failed) AsyncImage(model=artworkUri,contentDescription="Artwork for $songTitle",contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize(),onError={failed=true})
  else Icon(Icons.Default.MusicNote,"Music Note",tint=Color.White,modifier=Modifier.size(iconSize))
 }
}