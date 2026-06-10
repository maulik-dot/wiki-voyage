package com.example.wikipedia_app.ui.article

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.wikipedia_app.model.ParsedArticle
import com.example.wikipedia_app.model.ParsedParagraph
import com.example.wikipedia_app.navigation.Screen
import com.example.wikipedia_app.network.ApiConfig
import com.example.wikipedia_app.ui.components.ErrorState
import com.example.wikipedia_app.ui.components.LoadingState
import com.example.wikipedia_app.ui.viewmodels.ArticleViewModel
import com.example.wikipedia_app.ui.viewmodels.HistoryViewModel
import com.example.wikipedia_app.ui.viewmodels.TTSViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleScreen(
    title: String,
    navController: NavController,
    viewModel: ArticleViewModel,
    historyViewModel: HistoryViewModel,
    ttsViewModel: TTSViewModel,
    textScale: Float = 1.0f
) {
    val uiState by viewModel.uiState.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val isSpeaking by ttsViewModel.isSpeaking.collectAsState()

    LaunchedEffect(title) {
        viewModel.load(title)
        historyViewModel.addToHistory(title, "${ApiConfig.WIKIPEDIA_BASE_URL}wiki/$title")
    }

    // Stop narration when leaving the screen.
    DisposableEffect(Unit) {
        onDispose { ttsViewModel.stop() }
    }

    val successState = uiState as? ArticleViewModel.UiState.Success

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = successState?.article?.title ?: title.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    successState?.article?.let { article ->
                        IconButton(onClick = {
                            if (isSpeaking) ttsViewModel.stop()
                            else ttsViewModel.speak(buildSpeechText(article))
                        }) {
                            Icon(
                                if (isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop reading" else "Read aloud"
                            )
                        }
                        IconButton(onClick = {
                            viewModel.toggleBookmark(article.title, "${ApiConfig.WIKIPEDIA_BASE_URL}wiki/$title")
                        }) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
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
        when (val state = uiState) {
            is ArticleViewModel.UiState.Loading -> LoadingState(Modifier.padding(padding))
            is ArticleViewModel.UiState.Error -> ErrorState(
                message = state.message,
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.load(title) }
            )
            is ArticleViewModel.UiState.Success -> ArticleBody(
                article = state.article,
                textScale = textScale,
                contentPadding = padding,
                onLinkClick = { target ->
                    navController.navigate(Screen.Article.createRoute(target))
                }
            )
        }
    }
}

@Composable
private fun ArticleBody(
    article: ParsedArticle,
    textScale: Float,
    contentPadding: PaddingValues,
    onLinkClick: (String) -> Unit
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val bodyColor = MaterialTheme.colorScheme.onSurface
    val bodyStyle = MaterialTheme.typography.bodyLarge.scaled(textScale).copy(color = bodyColor)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 32.dp
        )
    ) {
        article.thumbnailUrl?.let { url ->
            item {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentScale = ContentScale.Crop
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
            }
        }

        article.sections.forEach { section ->
            if (section.title != null) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            items(count = section.paragraphs.size) { index ->
                ParagraphView(
                    paragraph = section.paragraphs[index],
                    bodyStyle = bodyStyle,
                    linkColor = linkColor,
                    textScale = textScale,
                    onLinkClick = onLinkClick
                )
            }
        }
    }
}

@Composable
private fun ParagraphView(
    paragraph: ParsedParagraph,
    bodyStyle: TextStyle,
    linkColor: Color,
    textScale: Float,
    onLinkClick: (String) -> Unit
) {
    when (paragraph) {
        is ParsedParagraph.Text -> {
            val annotated = remember(paragraph, linkColor) {
                buildLinkedString(paragraph.content, paragraph.links, linkColor)
            }
            ClickableText(
                text = annotated,
                style = bodyStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onClick = { offset ->
                    annotated.getStringAnnotations("link", offset, offset)
                        .firstOrNull()?.let { onLinkClick(it.item) }
                }
            )
        }

        is ParsedParagraph.BulletItem -> {
            val annotated = remember(paragraph, linkColor) {
                buildLinkedString(paragraph.content, paragraph.links, linkColor)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Text("•  ", style = bodyStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ClickableText(
                    text = annotated,
                    style = bodyStyle,
                    onClick = { offset ->
                        annotated.getStringAnnotations("link", offset, offset)
                            .firstOrNull()?.let { onLinkClick(it.item) }
                    }
                )
            }
        }

        is ParsedParagraph.SubHeading -> {
            Text(
                text = paragraph.text,
                style = MaterialTheme.typography.titleMedium.scaled(textScale),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            )
        }

        is ParsedParagraph.Image -> {
            Column(Modifier.padding(vertical = 12.dp)) {
                AsyncImage(
                    model = paragraph.url,
                    contentDescription = paragraph.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentScale = ContentScale.FillWidth
                )
                paragraph.caption?.takeIf { it.isNotBlank() }?.let { caption ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

private fun buildLinkedString(
    content: String,
    links: List<com.example.wikipedia_app.model.InlineLink>,
    linkColor: Color
) = buildAnnotatedString {
    append(content)
    links.forEach { link ->
        if (link.start in 0..content.length && link.end in link.start..content.length) {
            addStyle(
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                link.start, link.end
            )
            addStringAnnotation("link", link.target, link.start, link.end)
        }
    }
}

/** Scale a text style's font + line height by [factor] for the Settings text-size control. */
private fun TextStyle.scaled(factor: Float): TextStyle {
    if (factor == 1.0f) return this
    fun TextUnit.scale() = if (this.isSpecified) this * factor else this
    return copy(fontSize = fontSize.scale(), lineHeight = lineHeight.scale())
}

private fun buildSpeechText(article: ParsedArticle): String = buildString {
    append(article.title).append(". ")
    article.sections.forEach { section ->
        section.title?.let { append(it).append(". ") }
        section.paragraphs.forEach { p ->
            when (p) {
                is ParsedParagraph.Text -> append(p.content).append(" ")
                is ParsedParagraph.BulletItem -> append(p.content).append(" ")
                is ParsedParagraph.SubHeading -> append(p.text).append(". ")
                is ParsedParagraph.Image -> {}
            }
        }
    }
}
