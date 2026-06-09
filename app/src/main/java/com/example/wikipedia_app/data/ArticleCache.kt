package com.example.wikipedia_app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "article_cache")
data class ArticleCache(
    @PrimaryKey
    val title: String,
    val content: String,
    val links: String, // JSON string of List<WikiLink>
    val timestamp: Long = System.currentTimeMillis(),
    val isGameArticle: Boolean = false,
    val isHyperlink: Boolean = false,
    val parentArticle: String? = null
) 