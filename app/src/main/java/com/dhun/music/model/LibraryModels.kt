package com.dhun.music.model
data class Album(val name:String,val artist:String,val songCount:Int,val albumArtUri:String?=null)
data class Artist(val name:String,val songCount:Int,val albumCount:Int)
data class Folder(val name:String,val path:String,val songCount:Int)