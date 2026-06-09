package com.example.wikipedia_app.ui.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.wikipedia_app.model.ParsedArticle
import com.example.wikipedia_app.model.ParsedParagraph
import com.example.wikipedia_app.model.ParsedSection
import com.example.wikipedia_app.navigation.Screen
import com.example.wikipedia_app.network.ApiConfig
import com.example.wikipedia_app.ui.theme.BackgroundBeige
import com.example.wikipedia_app.ui.theme.CreamOffWhite
import com.example.wikipedia_app.ui.theme.DarkBrown
import com.example.wikipedia_app.ui.theme.TealCyan
import com.example.wikipedia_app.ui.viewmodels.ArticleViewModel
import com.example.wikipedia_app.ui.viewmodels.HistoryViewModel
import com.example.wikipedia_app.ui.viewmodels.TTSViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    navController: NavController,
    title: String,
    viewModel: ArticleViewModel,
    historyViewModel: HistoryViewModel,
    ttsViewModel: TTSViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val isSpeaking by ttsViewModel.isSpeaking.collectAsState()

    LaunchedEffect(title) {
        viewModel.load(title)
        historyViewModel.addToHistory(title, "${ApiConfig.WIKIPEDIA_BASE_URL}wiki/$title")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "WIKI-VOYAGE",
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
                    IconButton(onClick = {
                        if (isSpeaking) {
                            ttsViewModel.stop()
                        } else {
                            val state = uiState
                            if (state is ArticleViewModel.UiState.Success) {
                                ttsViewModel.speak(state.article.toSpeakableText())
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop reading" else "Read aloud",
                            tint = CreamOffWhite
                        )
                    }
                    IconButton(onClick = {
                        viewModel.toggleBookmark(title, "${ApiConfig.WIKIPEDIA_BASE_URL}wiki/$title")
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                            tint = CreamOffWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealCyan,
                    titleContentColor = CreamOffWhite
                )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ArticleViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TealCyan)
                }
            }
            is ArticleViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.load(title) },
                            colors = ButtonDefaults.buttonColors(containerColor = TealCyan)
                        ) {
                            Text("Retry", color = CreamOffWhite)
                        }
                    }
                }
            }
            is ArticleViewModel.UiState.Success -> {
                ArticleBody(
                    article = state.article,
                    onLinkClick = { target ->
                        navController.navigate(Screen.Article.createRoute(target))
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ArticleBody(
    article: ParsedArticle,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBeige)
    ) {
        // Header thumbnail
        article.thumbnailUrl?.let { url ->
            item {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Article title
        item {
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkBrown
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        // Sections
        article.sections.forEach { section ->
            // Section heading (null = intro section, no heading needed)
            section.title?.let { sectionTitle ->
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TealCyan
                            )
                        )
                        Divider(
                            color = TealCyan.copy(alpha = 0.35f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            items(section.paragraphs) { paragraph ->
                ParagraphItem(
                    paragraph = paragraph,
                    onLinkClick = onLinkClick
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun ParagraphItem(
    paragraph: ParsedParagraph,
    onLinkClick: (String) -> Unit
) {
    val linkColor = TealCyan

    when (paragraph) {
        is ParsedParagraph.Text -> {
            val annotated = remember(paragraph, linkColor) {
                buildAnnotatedString {
                    val text = paragraph.content
                    val sorted = paragraph.links.sortedBy { it.start }
                    var cursor = 0
                    for (link in sorted) {
                        if (link.start < cursor || link.end > text.length) continue
                        append(text.substring(cursor, link.start))
                        pushStringAnnotation(tag = "link", annotation = link.target)
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                            append(text.substring(link.start, link.end))
                        }
                        pop()
                        cursor = link.end
                    }
                    if (cursor < text.length) append(text.substring(cursor))
                }
            }
            ClickableText(
                text = annotated,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = DarkBrown,
                    lineHeight = 28.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                onClick = { offset ->
                    annotated.getStringAnnotations("link", offset, offset)
                        .firstOrNull()?.let { onLinkClick(it.item) }
                }
            )
        }

        is ParsedParagraph.SubHeading -> {
            Text(
                text = paragraph.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DarkBrown
                ),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            )
        }

        is ParsedParagraph.BulletItem -> {
            val annotated = remember(paragraph, linkColor) {
                buildAnnotatedString {
                    val text = paragraph.content
                    val sorted = paragraph.links.sortedBy { it.start }
                    var cursor = 0
                    for (link in sorted) {
                        if (link.start < cursor || link.end > text.length) continue
                        append(text.substring(cursor, link.start))
                        pushStringAnnotation(tag = "link", annotation = link.target)
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                            append(text.substring(link.start, link.end))
                        }
                        pop()
                        cursor = link.end
                    }
                    if (cursor < text.length) append(text.substring(cursor))
                }
            }
            Row(
                modifier = Modifier
                    .padding(start = 24.dp, end = 16.dp, top = 2.dp, bottom = 2.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "•",
                    color = DarkBrown,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp, top = 6.dp)
                )
                ClickableText(
                    text = annotated,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = DarkBrown,
                        lineHeight = 26.sp
                    ),
                    onClick = { offset ->
                        annotated.getStringAnnotations("link", offset, offset)
                            .firstOrNull()?.let { onLinkClick(it.item) }
                    }
                )
            }
        }

        is ParsedParagraph.Image -> {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AsyncImage(
                    model = paragraph.url,
                    contentDescription = paragraph.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 300.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                paragraph.caption?.let { caption ->
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DarkBrown.copy(alpha = 0.7f),
                            fontStyle = FontStyle.Italic
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun ParsedArticle.toSpeakableText(): String =
    sections.joinToString("\n\n") { section ->
        val heading = section.title?.let { "$it.\n" } ?: ""
        val body = section.paragraphs
            .filterIsInstance<ParsedParagraph.Text>()
            .joinToString("\n") { it.content }
        heading + body
    }
