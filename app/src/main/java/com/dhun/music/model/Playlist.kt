package com.dhun.music.model
data class Playlist(val id:Long=0L,val name:String,val songCount:Int=0,val totalDurationMs:Long=0L,val createdAt:Long=System.currentTimeMillis()) {
 fun formatDuration():String { val m=(totalDurationMs/60000).coerceAtLeast(0); return if(m/60>0) String.format("%d hr %d min",m/60,m%60) else String.format("%d min",m) }
}