package com.dhun.music.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
@Composable fun DhunTheme(content: @Composable () -> Unit){MaterialTheme(colorScheme=darkColorScheme(background=DhunBackground,surface=DhunSurface,primary=DhunViolet,secondary=DhunPink,onBackground=DhunTextPrimary,onSurface=DhunTextPrimary),content=content)}