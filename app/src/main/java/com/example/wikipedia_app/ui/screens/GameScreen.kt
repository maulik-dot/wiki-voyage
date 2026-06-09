package com.example.wikipedia_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wikipedia_app.data.GameException
import com.example.wikipedia_app.data.GameService
import com.example.wikipedia_app.model.GameState
import com.example.wikipedia_app.ui.components.WikiArticle
import com.example.wikipedia_app.ui.theme.CreamOffWhite
import com.example.wikipedia_app.ui.theme.TealCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun GameScreen(
    gameService: GameService,
    onPlayAgain: () -> Unit
) {
    var gameState by remember { mutableStateOf(GameState()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Live timer state
    var elapsedTime by remember { mutableStateOf(0L) }

    // Initialize game
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                error = null
                val startArticle = gameService.getRandomArticle()
                val targetTitle = gameService.getRandomArticleTitle()
                gameState = gameState.copy(
                    startArticle = startArticle,
                    currentArticle = startArticle,
                    targetArticleTitle = targetTitle
                )
                elapsedTime = 0L
            } catch (e: GameException) {
                error = e.message
            } catch (e: Exception) {
                error = "An unexpected error occurred: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Live timer effect
    LaunchedEffect(gameState.isGameWon, isLoading, error) {
        while (!gameState.isGameWon && !isLoading && error == null) {
            elapsedTime = System.currentTimeMillis() - gameState.startTime
            delay(100) // update every 0.1 second for smoothness
        }
        if (gameState.isGameWon) {
            elapsedTime = System.currentTimeMillis() - gameState.startTime
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (error != null) {
        val errorMsg = error ?: ""
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        error = null
                        isLoading = true
                        scope.launch {
                            try {
                                val startArticle = gameService.getRandomArticle()
                                val targetTitle = gameService.getRandomArticleTitle()
                                gameState = gameState.copy(
                                    startArticle = startArticle,
                                    currentArticle = startArticle,
                                    targetArticleTitle = targetTitle
                                )
                                elapsedTime = 0L
                            } catch (e: GameException) {
                                error = e.message
                            } catch (e: Exception) {
                                error = "An unexpected error occurred: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealCyan)
                ) {
                    Text("Retry", color = CreamOffWhite)
                }
            }
        }
    } else if (gameState.isGameWon) {
        GameWonScreen(
            steps = gameState.steps,
            timeElapsed = elapsedTime,
            onPlayAgain = onPlayAgain
        )
    } else {
        GameInProgressScreen(
            gameState = gameState,
            elapsedTime = elapsedTime,
            onLinkClick = { link ->
                scope.launch {
                    try {
                        isLoading = true
                        error = null
                        val newArticle = gameService.getArticle(link.target)
                        val isWon = newArticle.title == gameState.targetArticleTitle
                        gameState = gameState.copy(
                            currentArticle = newArticle,
                            navigationPath = gameState.navigationPath + newArticle,
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
                    val previousArticle = gameState.navigationPath.last()
                    gameState = gameState.copy(
                        currentArticle = previousArticle,
                        navigationPath = gameState.navigationPath.dropLast(1),
                        steps = gameState.steps - 1
                    )
                }
            }
        )
    }
}

@Composable
fun GameInProgressScreen(
    gameState: GameState,
    elapsedTime: Long,
    onLinkClick: (com.example.wikipedia_app.model.WikiLink) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        // Game info header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Target",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TealCyan
                    )
                )
                Text(
                    text = gameState.targetArticleTitle ?: "Loading...",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Steps: ${gameState.steps}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatTime(elapsedTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TealCyan
                    )
                }
            }
        }

        // Back button
        if (gameState.navigationPath.isNotEmpty()) {
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp)
                    .align(Alignment.Start),
                colors = ButtonDefaults.buttonColors(containerColor = TealCyan)
            ) {
                Text("← Back", color = CreamOffWhite)
            }
        }

        // Article content
        gameState.currentArticle?.let { article ->
            WikiArticle(
                article = article,
                onLinkClick = onLinkClick,
                modifier = Modifier.weight(1f)
            )
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun GameWonScreen(
    steps: Int,
    timeElapsed: Long,
    onPlayAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "You made it!",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TealCyan
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$steps steps",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = formatTime(timeElapsed),
                    style = MaterialTheme.typography.titleLarge.copy(color = TealCyan)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onPlayAgain,
            colors = ButtonDefaults.buttonColors(containerColor = TealCyan)
        ) {
            Text("Play Again", color = CreamOffWhite)
        }
    }
}

private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    val tenths = (millis / 100) % 10
    return String.format("%02d:%02d.%01d", minutes, seconds, tenths)
}