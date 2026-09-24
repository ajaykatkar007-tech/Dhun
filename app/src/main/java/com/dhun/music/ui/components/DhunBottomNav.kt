package com.dhun.music.ui.components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dhun.music.R
import com.dhun.music.ui.theme.*
enum class DhunNavTab(val titleRes:Int,val filledIcon:androidx.compose.ui.graphics.vector.ImageVector,val outlinedIcon:androidx.compose.ui.graphics.vector.ImageVector,val testTag:String){
 HOME(R.string.nav_home,Icons.Filled.Home,Icons.Outlined.Home,"nav_tab_home"),SEARCH(R.string.nav_search,Icons.Filled.Search,Icons.Outlined.Search,"nav_tab_search"),LIBRARY(R.string.nav_library,Icons.Filled.LibraryMusic,Icons.Outlined.LibraryMusic,"nav_tab_library"),SETTINGS(R.string.nav_settings,Icons.Filled.Settings,Icons.Outlined.Settings,"nav_tab_settings")
}
@Composable fun DhunBottomNav(selectedTab:DhunNavTab,onTabSelected:(DhunNavTab)->Unit,modifier:Modifier=Modifier){
 NavigationBar(modifier=modifier,containerColor=DhunBackground,tonalElevation=0.dp){ DhunNavTab.entries.forEach{tab->
  val selected=selectedTab==tab
  NavigationBarItem(selected=selected,onClick={onTabSelected(tab)},icon={Icon(if(selected)tab.filledIcon else tab.outlinedIcon,stringResource(tab.titleRes))},label={Text(stringResource(tab.titleRes))},colors=NavigationBarItemDefaults.colors(selectedIconColor=DhunPink,unselectedIconColor=DhunTextSecondary,selectedTextColor=DhunTextPrimary,unselectedTextColor=DhunTextSecondary,indicatorColor=DhunViolet.copy(alpha=.22f)))
 }}
}