package com.dhun.music.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhun.music.ui.theme.*
@Composable fun QuickAccessCard(title:String,subtitle:String,icon:ImageVector,iconGradient:List<Color>,onClick:()->Unit,modifier:Modifier=Modifier,testTag:String="quick_access_card"){
 Card(modifier=modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick=onClick),colors=CardDefaults.cardColors(containerColor=DhunCardElevated),shape=RoundedCornerShape(14.dp)){
  Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(iconGradient)),contentAlignment=Alignment.Center){Icon(icon,null,tint=Color.White)};Spacer(Modifier.width(12.dp));Column{Text(title,fontWeight=FontWeight.Bold,color=DhunTextPrimary);Text(subtitle,color=DhunTextSecondary)}}}
 }
}