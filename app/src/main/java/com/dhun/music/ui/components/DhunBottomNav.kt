package com.dhun.music.ui.components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dhun.music.ui.theme.*
enum class DhunNavTab(val title:String,val filledIcon:androidx.compose.ui.graphics.vector.ImageVector,val outlinedIcon:androidx.compose.ui.graphics.vector.ImageVector,val testTag:String){
 HOME("Home",Icons.Filled.Home,Icons.Outlined.Home,"nav_tab_home"),
 SEARCH("Search",Icons.Filled.Search,Icons.Outlined.Search,"nav_tab_search"),
 LIBRARY("Library",Icons.Filled.LibraryMusic,Icons.Outlined.LibraryMusic,"nav_tab_library"),
 SETTINGS("Settings",Icons.Filled.Settings,Icons.Outlined.Settings,"nav_tab_settings")
}
@Composable fun DhunBottomNav(selectedTab:DhunNavTab,onTabSelected:(DhunNavTab)->Unit,modifier:Modifier=Modifier){
 NavigationBar(modifier=modifier,containerColor=DhunBackground,tonalElevation=0.dp){ DhunNavTab.entries.forEach{tab->
  val selected=selectedTab==tab
  NavigationBarItem(selected=selected,onClick={onTabSelected(tab)},icon={Icon(if(selected)tab.filledIcon else tab.outlinedIcon,tab.title)},label={Text(tab.title)},colors=NavigationBarItemDefaults.colors(selectedIconColor=DhunPink,unselectedIconColor=DhunTextSecondary,selectedTextColor=DhunTextPrimary,unselectedTextColor=DhunTextSecondary,indicatorColor=DhunViolet.copy(alpha=.22f)))
 }}
}