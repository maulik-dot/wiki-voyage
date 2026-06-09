package com.example.wikipedia_app.ui.search

import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.text.Html
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.wikipedia_app.data.History
import com.example.wikipedia_app.model.SearchResult
import com.example.wikipedia_app.navigation.Screen
import com.example.wikipedia_app.network.ApiConfig
import com.example.wikipedia_app.ui.theme.CreamOffWhite
import com.example.wikipedia_app.ui.theme.TealCyan
import com.example.wikipedia_app.ui.viewmodels.HistoryViewModel
import com.example.wikipedia_app.ui.viewmodels.SearchViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: SearchViewModel,
    historyViewModel: HistoryViewModel
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val suggestion by viewModel.suggestion.collectAsState()
    val totalHits by viewModel.totalHits.collectAsState()
    val history by historyViewModel.history.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let { viewModel.updateQuery(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Search",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = CreamOffWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CreamOffWhite)
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearHistoryDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear all history", tint = CreamOffWhite)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealCyan)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateQuery(it) },
                label = { Text("Search Wikipedia") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
                            }
                            voiceSearchLauncher.launch(intent)
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            suggestion?.let { suggest ->
                Text(
                    text = "Did you mean: $suggest",
                    style = MaterialTheme.typography.bodySmall,
                    color = TealCyan,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { viewModel.updateQuery(suggest) }
                )
            }

            if (totalHits > 0 && query.isNotBlank()) {
                Text(
                    text = "$totalHits results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TealCyan)
                }

                query.isBlank() && history.isNotEmpty() -> {
                    Text(
                        "Recent",
                        style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(history, key = { it.title }) { item ->
                            HistoryItem(
                                history = item,
                                onItemClick = {
                                    navController.navigate(Screen.Article.createRoute(item.title))
                                },
                                onDeleteClick = { historyViewModel.deleteHistory(item) }
                            )
                        }
                    }
                }

                query.isBlank() -> { /* no history, no query — show nothing */ }

                results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No results for \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.title }) { result ->
                        SearchResultItem(result) {
                            historyViewModel.addToHistory(
                                result.title,
                                "${ApiConfig.WIKIPEDIA_BASE_URL}wiki/${result.title}"
                            )
                            navController.navigate(Screen.Article.createRoute(result.title))
                        }
                    }
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear History") },
            text = { Text("Remove all search history?") },
            confirmButton = {
                TextButton(onClick = {
                    historyViewModel.clearHistory()
                    showClearHistoryDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun HistoryItem(
    history: History,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            modifier = Modifier
                .size(20.dp)
                .padding(end = 0.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = history.title.replace("_", " "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = formatRelativeDate(history.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
}

@Composable
fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = decodeHtml(result.snippet).let {
                    if (it.length > 150) it.take(150) + "…" else it
                },
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
                maxLines = 3
            )
            result.wordcount?.takeIf { it > 0 }?.let { wc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$wc words",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealCyan
                )
            }
        }
    }
}

fun decodeHtml(html: String): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
    else
        @Suppress("DEPRECATION") Html.fromHtml(html).toString()

private fun formatRelativeDate(date: Date): String {
    val diff = System.currentTimeMillis() - date.time
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}
