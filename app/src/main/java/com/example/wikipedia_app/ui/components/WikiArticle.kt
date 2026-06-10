package com.example.wikipedia_app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

/**
 * Renders a game article on a clean reading card — title, section headings,
 * subheadings, bullets and prose — mirroring the reader screen. Inline links are
 * tappable (navigate) and pressable (warm the fetch on finger-down).
 */
@Composable
fun WikiArticle(
    article: Article,
    onLinkClick: (WikiLink) -> Unit,
    onLinkPress: (WikiLink) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)) {
            item {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
            }

            article.sections.forEach { section ->
                val heading = section.title
                if (!heading.isNullOrBlank()) {
                    item {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(6.dp))
                    }
                }
                section.paragraphs.forEach { paragraph ->
                    item {
                        Paragraph(paragraph, linkColor, onLinkClick, onLinkPress)
                    }
                }
            }
        }
    }
}

@Composable
private fun Paragraph(
    paragraph: ParsedParagraph,
    linkColor: Color,
    onLinkClick: (WikiLink) -> Unit,
    onLinkPress: (WikiLink) -> Unit
) {
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    when (paragraph) {
        is ParsedParagraph.Text -> LinkedText(
            content = paragraph.content,
            links = paragraph.links,
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
                style = bodyStyle,
                linkColor = linkColor,
                onLinkClick = onLinkClick,
                onLinkPress = onLinkPress
            )
        }

        is ParsedParagraph.SubHeading -> Text(
            text = paragraph.text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
        )

        is ParsedParagraph.Image -> { /* the game stays text-focused */ }
    }
}

/** A text block whose inline links are both tappable (navigate) and pressable (warm). */
@Composable
private fun LinkedText(
    content: String,
    links: List<InlineLink>,
    style: TextStyle,
    linkColor: Color,
    onLinkClick: (WikiLink) -> Unit,
    onLinkPress: (WikiLink) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotated = remember(content, links, linkColor) {
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
