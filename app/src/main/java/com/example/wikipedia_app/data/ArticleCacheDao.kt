package com.example.wikipedia_app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleCacheDao {
    @Query("SELECT * FROM article_cache WHERE title = :title")
    suspend fun getArticle(title: String): ArticleCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleCache)

    @Query("SELECT * FROM article_cache WHERE isGameArticle = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentGameArticles(limit: Int = 50): Flow<List<ArticleCache>>

    @Query("DELETE FROM article_cache WHERE timestamp < :timestamp")
    suspend fun deleteOldArticles(timestamp: Long)

    @Query("SELECT COUNT(*) FROM article_cache")
    suspend fun getCacheSize(): Int

    @Query("DELETE FROM article_cache WHERE isGameArticle = 0")
    suspend fun clearNonGameArticles()

    @Query("SELECT * FROM article_cache WHERE title LIKE :query || '%' LIMIT :limit")
    suspend fun searchArticles(query: String, limit: Int = 10): List<ArticleCache>

    @Query("DELETE FROM article_cache WHERE isHyperlink = 1 AND parentArticle = :articleTitle")
    suspend fun deleteArticleLinks(articleTitle: String)

    @Query("SELECT * FROM article_cache WHERE isHyperlink = 1 AND parentArticle = :articleTitle")
    suspend fun getArticleLinks(articleTitle: String): List<ArticleCache>

    @Query("SELECT * FROM article_cache WHERE isHyperlink = 1 ORDER BY timestamp DESC")
    suspend fun getAllHyperlinks(): List<ArticleCache>

    @Query("DELETE FROM article_cache")
    suspend fun clearAll()
} 