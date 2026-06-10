package com.example.wikipedia_app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.Coil
import coil.ImageLoader
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
import com.example.wikipedia_app.ui.viewmodels.TTSViewModel
import com.example.wikipedia_app.ui.viewmodels.TrendingViewModel
import okhttp3.OkHttpClient
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var currentLocale: Locale = Locale.getDefault()
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Coil must send a User-Agent or upload.wikimedia.org returns 403 for images.
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
                .crossfade(true)
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
            var speechRate by remember { mutableFloatStateOf(prefs.getFloat("speech_rate", 1.0f)) }
            var speechPitch by remember { mutableFloatStateOf(prefs.getFloat("speech_pitch", 1.0f)) }
            var dynamicColor by remember { mutableStateOf(prefs.getBoolean("dynamic_color", true)) }

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

            WikipediaAppTheme(darkTheme = isDark, dynamicColor = dynamicColor) {
                MainScreen(
                    database = database,
                    ttsViewModel = ttsViewModel,
                    currentTheme = currentTheme,
                    onThemeChanged = { currentTheme = it; prefs.edit().putString("theme", it).apply() },
                    textSize = textSize,
                    onTextSizeChanged = { textSize = it; prefs.edit().putString("text_size", it).apply() },
                    speechRate = speechRate,
                    onSpeechRateChanged = {
                        speechRate = it
                        prefs.edit().putFloat("speech_rate", it).apply()
                        ttsViewModel.updateSpeechRate(it)
                    },
                    speechPitch = speechPitch,
                    onSpeechPitchChanged = {
                        speechPitch = it
                        prefs.edit().putFloat("speech_pitch", it).apply()
                        ttsViewModel.updateSpeechPitch(it)
                    },
                    dynamicColor = dynamicColor,
                    onDynamicColorChanged = { dynamicColor = it; prefs.edit().putBoolean("dynamic_color", it).apply() },
                    textScale = textScale,
                    onLanguageSelected = { languageCode ->
                        val newLocale = Locale(languageCode)
                        if (newLocale != currentLocale) {
                            currentLocale = newLocale
                            val config = resources.configuration
                            config.setLocale(newLocale)
                            @Suppress("DEPRECATION")
                            resources.updateConfiguration(config, resources.displayMetrics)
                            recreate()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    database: AppDatabase,
    ttsViewModel: TTSViewModel,
    currentTheme: String,
    onThemeChanged: (String) -> Unit,
    textSize: String,
    onTextSizeChanged: (String) -> Unit,
    speechRate: Float,
    onSpeechRateChanged: (Float) -> Unit,
    speechPitch: Float,
    onSpeechPitchChanged: (Float) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit,
    textScale: Float,
    onLanguageSelected: (String) -> Unit
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
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
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
                dynamicColor = dynamicColor,
                onDynamicColorChanged = onDynamicColorChanged,
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
    dynamicColor: Boolean,
    onDynamicColorChanged: (Boolean) -> Unit,
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
            route = Screen.Article.route,
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
                },
                onExit = { navController.popBackStack() }
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
                dynamicColor = dynamicColor,
                onDynamicColorChanged = onDynamicColorChanged,
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
