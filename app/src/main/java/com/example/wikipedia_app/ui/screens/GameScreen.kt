package com.example.wikipedia_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wikipedia_app.data.GameException
import com.example.wikipedia_app.data.GameService
import com.example.wikipedia_app.model.GameState
import com.example.wikipedia_app.model.WikiLink
import com.example.wikipedia_app.ui.components.LoadingState
import com.example.wikipedia_app.ui.components.WikiArticle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// Extracted to avoid duplication between LaunchedEffect and Retry handler.
private suspend fun startNewGame(
    gameService: GameService,
    onLoading: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onReady: (GameState, Long) -> Unit
) {
    onLoading(true)
    onError(null)
    try {
        val startArticle = gameService.getRandomArticle()
        val targetTitle = gameService.getRandomArticleTitle()
        onReady(
            GameState(
                startArticle = startArticle,
                currentArticle = startArticle,
                targetArticleTitle = targetTitle,
                startTime = System.currentTimeMillis()
            ),
            0L
        )
    } catch (e: GameException) {
        onError(e.message)
    } catch (e: Exception) {
        onError("Failed to load game: ${e.message}")
    } finally {
        onLoading(false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    gameService: GameService,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    var gameState by remember { mutableStateOf(GameState()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var elapsedTime by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        startNewGame(
            gameService = gameService,
            onLoading = { isLoading = it },
            onError = { error = it },
            onReady = { state, time -> gameState = state; elapsedTime = time }
        )
    }

    // Live timer — only ticks while the game is active.
    LaunchedEffect(gameState.isGameWon, isLoading, error) {
        while (!gameState.isGameWon && !isLoading && error == null) {
            elapsedTime = System.currentTimeMillis() - gameState.startTime
            delay(100)
        }
        if (gameState.isGameWon) {
            elapsedTime = System.currentTimeMillis() - gameState.startTime
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Wikirace",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = "Exit game")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                isLoading -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LoadingState(Modifier.height(80.dp))
                    Text(
                        "Finding two random articles…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                error != null -> com.example.wikipedia_app.ui.components.ErrorState(
                    message = error ?: "",
                    onRetry = {
                        scope.launch {
                            startNewGame(
                                gameService = gameService,
                                onLoading = { isLoading = it },
                                onError = { error = it },
                                onReady = { state, time -> gameState = state; elapsedTime = time }
                            )
                        }
                    }
                )

                gameState.isGameWon -> GameWonScreen(
                    steps = gameState.steps,
                    timeElapsed = elapsedTime,
                    onPlayAgain = onPlayAgain,
                    onExit = onExit
                )

                else -> GameInProgress(
                    gameState = gameState,
                    elapsedTime = elapsedTime,
                    onLinkClick = { link ->
                        scope.launch {
                            val leaving = gameState.currentArticle ?: return@launch
                            try {
                                isLoading = true
                                error = null
                                val newArticle = gameService.getArticle(link.target)
                                val isWon = newArticle.title == gameState.targetArticleTitle
                                gameState = gameState.copy(
                                    currentArticle = newArticle,
                                    navigationPath = gameState.navigationPath + leaving,
                                    steps = gameState.steps + 1,
                                    isGameWon = isWon
                                )
                            } catch (e: GameException) {
                                error = e.message
                            } catch (e: Exception) {
                                error = "An unexpected error occurred: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    onBackClick = {
                        if (gameState.navigationPath.isNotEmpty()) {
                            gameState = gameState.copy(
                                currentArticle = gameState.navigationPath.last(),
                                navigationPath = gameState.navigationPath.dropLast(1),
                                steps = gameState.steps - 1
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GameInProgress(
    gameState: GameState,
    elapsedTime: Long,
    onLinkClick: (WikiLink) -> Unit,
    onBackClick: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Objective banner
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "REACH THIS ARTICLE",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = (gameState.targetArticleTitle ?: "…").replace("_", " "),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(label = "Hops", value = gameState.steps.toString())
                    StatChip(
                        label = "Time",
                        value = formatTime(elapsedTime),
                        leading = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    Spacer(Modifier.weight(1f))
                    if (gameState.navigationPath.isNotEmpty()) {
                        OutlinedButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Back")
                        }
                    }
                }
            }
        }

        // Current article
        gameState.currentArticle?.let { article ->
            WikiArticle(
                article = article,
                onLinkClick = onLinkClick,
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            )
        } ?: LoadingState(Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    leading: (@Composable () -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            leading?.invoke()
            Text("$label ", style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun GameWonScreen(
    steps: Int,
    timeElapsed: Long,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "You made it!",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ResultStat("Hops", steps.toString())
            ResultStat("Time", formatTime(timeElapsed))
        }
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Play again")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Explore")
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            Modifier.padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    val tenths = (millis / 100) % 10
    return String.format("%02d:%02d.%01d", minutes, seconds, tenths)
}
