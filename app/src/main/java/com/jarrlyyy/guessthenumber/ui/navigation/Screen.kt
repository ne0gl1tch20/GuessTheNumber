package com.jarrlyyy.guessthenumber.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Play : Screen("play", "Play", Icons.Default.PlayArrow)
    object Upgrade : Screen("upgrade", "Upgrades", Icons.Default.Star)
    object Shop : Screen("shop", "Shop", Icons.Default.ShoppingCart)
    object More : Screen("more", "More", Icons.Default.Menu)
    object Arcade : Screen("arcade", "Arcade", Icons.Default.Favorite)
    object Stats : Screen("stats", "Statistics", Icons.Default.Face)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object About : Screen("about", "About", Icons.Default.Info)
    object DevSettings : Screen("dev_settings", "Dev Settings", Icons.Default.Build)
    object Prestige : Screen("prestige", "Prestige", Icons.Default.Star)
    object Ultra : Screen("ultra", "Ultra", Icons.Default.Lock)
    object Achievements : Screen("achievements", "Achievements", Icons.Default.EmojiEvents)
    object ProcessInspector : Screen("process_inspector", "Process Inspector", Icons.Default.Memory)
    object ChangelogViewer : Screen("changelog_viewer", "Changelog", Icons.Default.Info)
}
