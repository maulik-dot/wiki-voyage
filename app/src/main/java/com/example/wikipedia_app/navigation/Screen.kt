package com.example.wikipedia_app.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Article : Screen("article/{title}") {
        fun createRoute(title: String) = "article/${Uri.encode(title)}"
    }
    object Bookmarks : Screen("bookmarks")
    object Settings : Screen("settings")
    object Game : Screen("game")
    object LanguageSelection : Screen("language_selection")
} 