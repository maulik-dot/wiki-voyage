package com.example.wikipedia_app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.wikipedia_app.R
import com.example.wikipedia_app.navigation.Screen

private data class NavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelResId: Int
)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Home.route, Icons.Filled.Home, Icons.Outlined.Home, R.string.home),
    NavItem(Screen.Search.route, Icons.Filled.Search, Icons.Outlined.Search, R.string.search),
    NavItem(Screen.Bookmarks.route, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, R.string.bookmarks),
    NavItem(Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings, R.string.settings)
)

/** Routes that should display the bottom bar. Detail screens hide it. */
val TOP_LEVEL_ROUTES: Set<String> = NAV_ITEMS.map { it.route }.toSet()

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: return

    // Only show on top-level destinations.
    if (currentRoute !in TOP_LEVEL_ROUTES) return

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        NAV_ITEMS.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.labelResId)
                    )
                },
                label = { Text(stringResource(item.labelResId)) },
                colors = NavigationBarItemDefaults.colors()
            )
        }
    }
}
