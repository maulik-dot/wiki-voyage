package com.example.wikipedia_app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.wikipedia_app.model.Article
import com.example.wikipedia_app.model.InlineLink
import com.example.wikipedia_app.model.ParsedParagraph
import com.example.wikipedia_app.model.WikiLink
import kotlinx.coroutines.launch

private val HighlightColor = Color(0xFFFFE082) // amber — readable on light & dark text

/** A flattened, addressable view of the article for rendering + search. */
private sealed interface Block {
    val searchText: String
    data class Title(val text: String) : Block { override val searchText get() = text }
    data class Heading(val text: String) : Block { override val searchText get() = text }
    data class Para(val paragraph: ParsedParagraph) : Block {
        override val searchText get() = when (val p = paragraph) {
            is ParsedParagraph.Text -> p.content
            is ParsedParagraph.BulletItem -> p.content
            is ParsedParagraph.SubHeading -> p.text
            is ParsedParagraph.Image -> ""
        }
    }
}

private fun buildBlocks(article: Article): List<Block> {
    val blocks = mutableListOf<Block>(Block.Title(article.title))
    for (section in article.sections) {
        section.title?.takeIf { it.isNotBlank() }?.let { blocks.add(Block.Heading(it)) }
        section.paragraphs.forEach { blocks.add(Block.Para(it)) }
    }
    return blocks
}

private fun matchRanges(text: String, query: String): List<IntRange> {
    if (query.isBlank()) return emptyList()
    val out = ArrayList<IntRange>()
    var i = text.indexOf(query, 0, ignoreCase = true)
    while (i >= 0) {
        out.add(i until (i + query.length))
        i = text.indexOf(query, i + query.length, ignoreCase = true)
    }
    return out
}

/**
 * Renders a game article on a clean reading card — title, section headings,
 * subheadings, bullets and prose — with tappable + pressable inline links.
 * When [highlight] is non-blank, matches are highlighted and a results bar lets
 * the player jump between them.
 */
@Composable
fun WikiArticle(
    article: Article,
    onLinkClick: (WikiLink) -> Unit,
    onLinkPress: (WikiLink) -> Unit = {},
    highlight: String = "",
    modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val query = highlight.trim()

    val blocks = remember(article) { buildBlocks(article) }
    val matchBlocks = remember(blocks, query) {
        if (query.isBlank()) emptyList()
        else blocks.indices.filter { blocks[it].searchText.contains(query, ignoreCase = true) }
    }
    val totalMatches = remember(blocks, query) {
        if (query.isBlank()) 0 else blocks.sumOf { matchRanges(it.searchText, query).size }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var matchPos by remember(query, blocks) { mutableIntStateOf(0) }

    // Scroll to the first match whenever the query (or article) changes.
    LaunchedEffect(query, blocks) {
        matchPos = 0
        if (matchBlocks.isNotEmpty()) listState.animateScrollToItem(matchBlocks[0])
    }

    fun jump(delta: Int) {
        if (matchBlocks.isEmpty()) return
        matchPos = ((matchPos + delta) % matchBlocks.size + matchBlocks.size) % matchBlocks.size
        scope.launch { listState.animateScrollToItem(matchBlocks[matchPos]) }
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Results bar stays pinned above the scrolling article.
            if (query.isNotBlank()) {
                SearchResultsBar(
                    totalMatches = totalMatches,
                    groups = matchBlocks.size,
                    onPrev = { jump(-1) },
                    onNext = { jump(1) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(blocks) { _, block ->
                    BlockView(block, query, linkColor, onLinkClick, onLinkPress)
                }
            }
        }
    }
}

@Composable
private fun SearchResultsBar(
    totalMatches: Int,
    groups: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (totalMatches == 0) "No matches"
                else "$totalMatches match${if (totalMatches == 1) "" else "es"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            if (groups > 0) {
                IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
                }
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
                }
            }
        }
    }
}

@Composable
private fun BlockView(
    block: Block,
    highlight: String,
    linkColor: Color,
    onLinkClick: (WikiLink) -> Unit,
    onLinkPress: (WikiLink) -> Unit
) {
    when (block) {
        is Block.Title -> {
            HighlightText(
                text = block.text,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                highlight = highlight
            )
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
        }

        is Block.Heading -> {
            Spacer(Modifier.height(14.dp))
            HighlightText(
                text = block.text,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                highlight = highlight
            )
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(6.dp))
        }

        is Block.Para -> Paragraph(block.paragraph, highlight, linkColor, onLinkClick, onLinkPress)
    }
}

@Composable
private fun Paragraph(
    paragraph: ParsedParagraph,
    highlight: String,
    linkColor: Color,
    onLinkClick: (WikiLink) -> Unit,
    onLinkPress: (WikiLink) -> Unit
) {
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    when (paragraph) {
        is ParsedParagraph.Text -> LinkedText(
            content = paragraph.content,
            links = paragraph.links,
            highlight = highlight,
            style = bodyStyle,
            linkColor = linkColor,
            onLinkClick = onLinkClick,
            onLinkPress = onLinkPress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )

        is ParsedParagraph.BulletItem -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text("•  ", style = bodyStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinkedText(
                content = paragraph.content,
                links = paragraph.links,
                highlight = highlight,
                style = bodyStyle,
                linkColor = linkColor,
                onLinkClick = onLinkClick,
                onLinkPress = onLinkPress
            )
        }

        is ParsedParagraph.SubHeading -> HighlightText(
            text = paragraph.text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            highlight = highlight,
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
        )

        is ParsedParagraph.Image -> { /* the game stays text-focused */ }
    }
}

/** Plain text with search-match highlighting. */
@Composable
private fun HighlightText(
    text: String,
    style: TextStyle,
    color: Color,
    highlight: String,
    modifier: Modifier = Modifier
) {
    val annotated = remember(text, highlight) {
        buildAnnotatedString {
            append(text)
            matchRanges(text, highlight).forEach { r ->
                addStyle(SpanStyle(background = HighlightColor, color = Color.Black), r.first, r.last + 1)
            }
        }
    }
    Text(text = annotated, style = style, color = color, modifier = modifier)
}

/** Text whose inline links are tappable (navigate) + pressable (warm), with search highlights. */
@Composable
private fun LinkedText(
    content: String,
    links: List<InlineLink>,
    highlight: String,
    style: TextStyle,
    linkColor: Color,
    onLinkClick: (WikiLink) -> Unit,
    onLinkPress: (WikiLink) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotated = remember(content, links, linkColor, highlight) {
        buildAnnotatedString {
            append(content)
            links.forEach { l ->
                if (l.start in 0..content.length && l.end in l.start..content.length) {
                    addStyle(
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        l.start, l.end
                    )
                    addStringAnnotation("link", l.target, l.start, l.end)
                }
            }
            matchRanges(content, highlight).forEach { r ->
                addStyle(SpanStyle(background = HighlightColor, color = Color.Black), r.first, r.last + 1)
            }
        }
    }
    var layout by remember(annotated) { mutableStateOf<TextLayoutResult?>(null) }
    fun linkAt(pos: Offset): WikiLink? {
        val lr = layout ?: return null
        val offset = lr.getOffsetForPosition(pos)
        val ann = annotated.getStringAnnotations("link", offset, offset).firstOrNull() ?: return null
        return WikiLink(content.substring(ann.start, ann.end), ann.item)
    }

    Text(
        text = annotated,
        style = style,
        onTextLayout = { layout = it },
        modifier = modifier.pointerInput(annotated) {
            detectTapGestures(
                onPress = { pos -> linkAt(pos)?.let(onLinkPress) },
                onTap = { pos -> linkAt(pos)?.let(onLinkClick) }
            )
        }
    )
}
