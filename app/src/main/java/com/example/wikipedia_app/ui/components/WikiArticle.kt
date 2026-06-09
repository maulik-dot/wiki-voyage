package com.example.wikipedia_app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.wikipedia_app.model.Article
import com.example.wikipedia_app.model.WikiLink

@Composable
fun WikiArticle(
    article: Article,
    onLinkClick: (WikiLink) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            val linkColor = MaterialTheme.colorScheme.primary
            val textColor = MaterialTheme.colorScheme.onBackground
            val annotatedString = remember(article, linkColor) {
                buildAnnotatedString {
                    // Find first occurrence of each link, then sort by position — O(L×N + L log L)
                    // instead of the previous O(N × L × N) while-minByOrNull loop.
                    val sortedLinks = article.links
                        .mapNotNull { link ->
                            val pos = article.content.indexOf(link.text)
                            if (pos >= 0) pos to link else null
                        }
                        .sortedBy { it.first }

                    var cursor = 0
                    for ((pos, link) in sortedLinks) {
                        if (pos < cursor) continue // skip links that overlap a previously appended one
                        append(article.content.substring(cursor, pos))
                        pushStringAnnotation(tag = "link", annotation = link.target)
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                            append(link.text)
                        }
                        pop()
                        cursor = pos + link.text.length
                    }
                    if (cursor < article.content.length) {
                        append(article.content.substring(cursor))
                    }
                }
            }

            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "link", start = offset, end = offset)
                        .firstOrNull()
                        ?.let { annotation ->
                            article.links.find { it.target == annotation.item }
                                ?.let { onLinkClick(it) }
                        }
                }
            )
        }
    }
} 