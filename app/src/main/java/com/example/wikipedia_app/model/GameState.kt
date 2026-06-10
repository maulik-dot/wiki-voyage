package com.example.wikipedia_app.model

import androidx.compose.runtime.Immutable

@Immutable
data class GameState(
    val startArticle: Article? = null,
    val targetArticleTitle: String? = null,
    val currentArticle: Article? = null,
    val navigationPath: List<Article> = emptyList(),
    val steps: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val isGameWon: Boolean = false
)

data class Article(
    val title: String,
    val links: List<WikiLink>,
    // Structured content (sections / headings / bullets) for clean rendering,
    // parsed with the same ArticleParser the reader screen uses.
    val sections: List<ParsedSection> = emptyList()
)

data class WikiLink(
    val text: String,
    val target: String
)