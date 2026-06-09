package com.example.wikipedia_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.wikipedia_app.data.AppDatabase
import com.example.wikipedia_app.data.BookmarkRepository
import com.example.wikipedia_app.data.GameService
import com.example.wikipedia_app.data.HistoryRepository
import com.example.wikipedia_app.data.TrendingRepository
import com.example.wikipedia_app.navigation.Screen
import com.example.wikipedia_app.ui.article.ArticleScreen
import com.example.wikipedia_app.ui.components.BottomNavBar
import com.example.wikipedia_app.ui.home.HomeScreen
import com.example.wikipedia_app.ui.screens.BookmarksScreen
import com.example.wikipedia_app.ui.screens.GameScreen
import com.example.wikipedia_app.ui.screens.LanguageSelectionScreen
import com.example.wikipedia_app.ui.screens.SettingsScreen
import com.example.wikipedia_app.ui.search.SearchScreen
import com.example.wikipedia_app.ui.theme.WikipediaAppTheme
import com.example.wikipedia_app.ui.viewmodels.ArticleViewModel
import com.example.wikipedia_app.ui.viewmodels.BookmarkViewModel
import com.example.wikipedia_app.ui.viewmodels.HistoryViewModel
import com.example.wikipedia_app.ui.viewmodels.TrendingViewModel
import com.example.wikipedia_app.ui.viewmodels.TTSViewModel
import coil.Coil
import coil.ImageLoader
import okhttp3.OkHttpClient
import java.util.*

class MainActivity : ComponentActivity() {
    private var currentLocale: Locale = Locale.getDefault()
    private var currentTheme: String = "System Default"
    private lateinit var database: AppDatabase
    private lateinit var ttsViewModel: TTSViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Give Coil a User-Agent so Wikimedia CDN serves images (it 403s anonymous requests)
        Coil.setImageLoader {
            ImageLoader.Builder(this)
                .okHttpClient(
                    OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            chain.proceed(
                                chain.request().newBuilder()
                                    .header("User-Agent", "WikiVoyage/1.0 (Android; educational project)")
                                    .build()
                            )
                        }
                        .build()
                )
                .build()
        }

        // Initialize database and TTS
        database = AppDatabase.getDatabase(this)
        ttsViewModel = TTSViewModel(this)

        setContent {
            WikipediaAppTheme {
                MainScreen(
                    onLanguageSelected = { languageCode ->
                        val newLocale = Locale(languageCode)
                        if (newLocale != currentLocale) {
                            currentLocale = newLocale
                            // Update the app's locale
                            val config = resources.configuration
                            config.setLocale(newLocale)
                            resources.updateConfiguration(config, resources.displayMetrics)
                            // Restart the activity to apply the new locale
                            recreate()
                        }
                    },
                    onThemeChanged = { theme ->
                        currentTheme = theme
                    },
                    currentTheme = currentTheme,
                    database = database,
                    ttsViewModel = ttsViewModel
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onLanguageSelected: (String) -> Unit,
    onThemeChanged: (String) -> Unit,
    currentTheme: String,
    database: AppDatabase,
    ttsViewModel: TTSViewModel
) {
    val navController = rememberNavController()
    val bookmarkRepository = remember {
        BookmarkRepository(database.bookmarkDao())
    }
    val bookmarkViewModel = remember {
        BookmarkViewModel(bookmarkRepository)
    }
    val historyRepository = remember {
        HistoryRepository(database.historyDao())
    }
    val historyViewModel = remember {
        HistoryViewModel(historyRepository)
    }
    val trendingRepository = remember {
        TrendingRepository()
    }
    val trendingViewModel = remember {
        TrendingViewModel(trendingRepository)
    }
    val gameService = remember {
        GameService(database.articleCacheDao())
    }
    val articleViewModel = remember {
        ArticleViewModel(bookmarkRepository)
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(navController)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            WikipediaNavGraph(
                navController = navController,
                bookmarkViewModel = bookmarkViewModel,
                articleViewModel = articleViewModel,
                historyViewModel = historyViewModel,
                trendingViewModel = trendingViewModel,
                gameService = gameService,
                ttsViewModel = ttsViewModel,
                currentTheme = currentTheme,
                onThemeChanged = onThemeChanged,
                onLanguageSelected = onLanguageSelected
            )
        }
    }
}

@Composable
fun WikipediaNavGraph(
    navController: NavHostController,
    bookmarkViewModel: BookmarkViewModel,
    articleViewModel: ArticleViewModel,
    historyViewModel: HistoryViewModel,
    trendingViewModel: TrendingViewModel,
    gameService: GameService,
    ttsViewModel: TTSViewModel,
    currentTheme: String,
    onThemeChanged: (String) -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                viewModel = trendingViewModel
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                navController = navController,
                historyViewModel = historyViewModel
            )
        }
        composable(
            route = "article/{title}",
            arguments = listOf(
                navArgument("title") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            ArticleScreen(
                title = title,
                navController = navController,
                viewModel = articleViewModel,
                historyViewModel = historyViewModel,
                ttsViewModel = ttsViewModel
            )
        }
        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                navController = navController,
                viewModel = bookmarkViewModel,
                onBookmarkClick = { url ->
                    val title = url.substringAfterLast("/")
                    navController.navigate(Screen.Article.createRoute(title))
                }
            )
        }
        composable(Screen.Game.route) {
            GameScreen(
                gameService = gameService,
                onPlayAgain = {
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                currentTheme = currentTheme,
                onThemeChanged = onThemeChanged
            )
        }
        composable(Screen.LanguageSelection.route) {
            LanguageSelectionScreen(
                navController = navController,
                onLanguageSelected = onLanguageSelected
            )
        }
    }
}