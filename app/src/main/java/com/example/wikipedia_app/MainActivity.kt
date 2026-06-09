package com.example.wikipedia_app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.example.wikipedia_app.ui.viewmodels.SearchViewModel
import com.example.wikipedia_app.ui.viewmodels.SettingsViewModel
import com.example.wikipedia_app.ui.viewmodels.TrendingViewModel
import com.example.wikipedia_app.ui.viewmodels.TTSViewModel
import coil.Coil
import coil.ImageLoader
import okhttp3.OkHttpClient
import java.util.*

class MainActivity : ComponentActivity() {
    private var currentLocale: Locale = Locale.getDefault()
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        database = AppDatabase.getDatabase(this)
        val prefs = getSharedPreferences("wiki_prefs", Context.MODE_PRIVATE)

        setContent {
            var currentTheme by remember {
                mutableStateOf(prefs.getString("theme", "System Default") ?: "System Default")
            }
            var textSize by remember {
                mutableStateOf(prefs.getString("text_size", "Normal") ?: "Normal")
            }
            var speechRate by remember {
                mutableStateOf(prefs.getFloat("speech_rate", 1.0f))
            }
            var speechPitch by remember {
                mutableStateOf(prefs.getFloat("speech_pitch", 1.0f))
            }

            // TTSViewModel created with saved rate/pitch so they apply from the first word
            val ttsViewModel = remember {
                TTSViewModel(this@MainActivity, speechRate, speechPitch)
            }

            val isDark = when (currentTheme) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }
            val textScale = when (textSize) {
                "Small" -> 0.85f
                "Large" -> 1.15f
                "Huge" -> 1.3f
                else -> 1.0f
            }

            WikipediaAppTheme(darkTheme = isDark) {
                MainScreen(
                    onLanguageSelected = { languageCode ->
                        val newLocale = Locale(languageCode)
                        if (newLocale != currentLocale) {
                            currentLocale = newLocale
                            val config = resources.configuration
                            config.setLocale(newLocale)
                            resources.updateConfiguration(config, resources.displayMetrics)
                            recreate()
                        }
                    },
                    onThemeChanged = { theme ->
                        currentTheme = theme
                        prefs.edit().putString("theme", theme).apply()
                    },
                    currentTheme = currentTheme,
                    textSize = textSize,
                    onTextSizeChanged = { size ->
                        textSize = size
                        prefs.edit().putString("text_size", size).apply()
                    },
                    speechRate = speechRate,
                    onSpeechRateChanged = { rate ->
                        speechRate = rate
                        prefs.edit().putFloat("speech_rate", rate).apply()
                        ttsViewModel.updateSpeechRate(rate)
                    },
                    speechPitch = speechPitch,
                    onSpeechPitchChanged = { pitch ->
                        speechPitch = pitch
                        prefs.edit().putFloat("speech_pitch", pitch).apply()
                        ttsViewModel.updateSpeechPitch(pitch)
                    },
                    textScale = textScale,
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
    textSize: String,
    onTextSizeChanged: (String) -> Unit,
    speechRate: Float,
    onSpeechRateChanged: (Float) -> Unit,
    speechPitch: Float,
    onSpeechPitchChanged: (Float) -> Unit,
    textScale: Float,
    database: AppDatabase,
    ttsViewModel: TTSViewModel
) {
    val navController = rememberNavController()
    val bookmarkRepository = remember { BookmarkRepository(database.bookmarkDao()) }
    val bookmarkViewModel = remember { BookmarkViewModel(bookmarkRepository) }
    val historyRepository = remember { HistoryRepository(database.historyDao()) }
    val historyViewModel = remember { HistoryViewModel(historyRepository) }
    val trendingViewModel = remember { TrendingViewModel(TrendingRepository()) }
    val gameService = remember { GameService(database.articleCacheDao()) }
    val articleViewModel = remember { ArticleViewModel(bookmarkRepository) }
    val searchViewModel = remember { SearchViewModel() }
    val settingsViewModel = remember { SettingsViewModel(database.articleCacheDao()) }

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            WikipediaNavGraph(
                navController = navController,
                bookmarkViewModel = bookmarkViewModel,
                articleViewModel = articleViewModel,
                searchViewModel = searchViewModel,
                historyViewModel = historyViewModel,
                trendingViewModel = trendingViewModel,
                gameService = gameService,
                ttsViewModel = ttsViewModel,
                settingsViewModel = settingsViewModel,
                currentTheme = currentTheme,
                onThemeChanged = onThemeChanged,
                textSize = textSize,
                onTextSizeChanged = onTextSizeChanged,
                speechRate = speechRate,
                onSpeechRateChanged = onSpeechRateChanged,
                speechPitch = speechPitch,
                onSpeechPitchChanged = onSpeechPitchChanged,
                textScale = textScale,
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
    searchViewModel: SearchViewModel,
    historyViewModel: HistoryViewModel,
    trendingViewModel: TrendingViewModel,
    gameService: GameService,
    ttsViewModel: TTSViewModel,
    settingsViewModel: SettingsViewModel,
    currentTheme: String,
    onThemeChanged: (String) -> Unit,
    textSize: String,
    onTextSizeChanged: (String) -> Unit,
    speechRate: Float,
    onSpeechRateChanged: (Float) -> Unit,
    speechPitch: Float,
    onSpeechPitchChanged: (Float) -> Unit,
    textScale: Float,
    onLanguageSelected: (String) -> Unit
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController, viewModel = trendingViewModel)
        }
        composable(Screen.Search.route) {
            SearchScreen(
                navController = navController,
                viewModel = searchViewModel,
                historyViewModel = historyViewModel
            )
        }
        composable(
            route = "article/{title}",
            arguments = listOf(navArgument("title") { type = NavType.StringType })
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            ArticleScreen(
                title = title,
                navController = navController,
                viewModel = articleViewModel,
                historyViewModel = historyViewModel,
                ttsViewModel = ttsViewModel,
                textScale = textScale
            )
        }
        composable(Screen.Bookmarks.route) {
            BookmarksScreen(
                navController = navController,
                viewModel = bookmarkViewModel,
                onBookmarkClick = { url ->
                    navController.navigate(Screen.Article.createRoute(url.substringAfterLast("/")))
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
                onThemeChanged = onThemeChanged,
                textSize = textSize,
                onTextSizeChanged = onTextSizeChanged,
                speechRate = speechRate,
                onSpeechRateChanged = onSpeechRateChanged,
                speechPitch = speechPitch,
                onSpeechPitchChanged = onSpeechPitchChanged,
                settingsViewModel = settingsViewModel
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
