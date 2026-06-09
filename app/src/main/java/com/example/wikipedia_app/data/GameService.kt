package com.example.wikipedia_app.data

import android.util.Log
import com.example.wikipedia_app.model.Article
import com.example.wikipedia_app.model.WikiLink
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class GameService(
    private val articleCacheDao: ArticleCacheDao
) {
    private val timeout = 15L
    private val maxLinks = 100
    private val gson = Gson()
    private val prefetchJob = SupervisorJob()
    private val prefetchScope = CoroutineScope(Dispatchers.IO + prefetchJob)

    fun cancelPrefetch() = prefetchJob.cancelChildren()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(timeout, TimeUnit.SECONDS)
        .readTimeout(timeout, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "WikiVoyage/1.0 (Android; educational project)")
                    .build()
            )
        }
        .build()

    suspend fun getRandomArticle(): Article = withContext(Dispatchers.IO) {
        cancelPrefetch() // cancel stale prefetches from any previous game
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(timeout)) {
                val title = fetchRandomTitle()
                val article = fetchArticleFromApi(title)
                prefetchHyperlinks(article)
                cacheArticle(article, isGameArticle = true)
                article
            }
        } catch (e: Exception) {
            when (e) {
                is UnknownHostException -> throw GameException("No internet connection")
                else -> throw GameException("Failed to load article: ${e.message}")
            }
        }
    }

    // Cheaper than getRandomArticle() — only fetches the title, no HTML parse.
    // Use for the target article since only its title is needed for the win condition.
    suspend fun getRandomArticleTitle(): String = withContext(Dispatchers.IO) {
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(timeout)) {
                fetchRandomTitle()
            }
        } catch (e: Exception) {
            when (e) {
                is UnknownHostException -> throw GameException("No internet connection")
                else -> throw GameException("Failed to get target: ${e.message}")
            }
        }
    }

    suspend fun getArticle(title: String): Article = withContext(Dispatchers.IO) {
        val cached = articleCacheDao.getArticle(title)
        if (cached != null) {
            Log.d("GameService", "Loaded '$title' from cache.")
            return@withContext Article(
                title = cached.title,
                content = cached.content,
                links = gson.fromJson(cached.links, object : TypeToken<List<WikiLink>>() {}.type)
            )
        }
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(timeout)) {
                Log.d("GameService", "Fetching '$title' from API.")
                val article = fetchArticleFromApi(title)
                prefetchHyperlinks(article)
                cacheArticle(article, isGameArticle = true)
                article
            }
        } catch (e: Exception) {
            when (e) {
                is UnknownHostException -> throw GameException("No internet connection")
                else -> throw GameException("Failed to load article: ${e.message}")
            }
        }
    }

    private fun fetchRandomTitle(): String {
        val json = fetchJson(
            "https://en.wikipedia.org/w/api.php?action=query&list=random&rnnamespace=0&rnlimit=1&format=json"
        )
        return json.getAsJsonObject("query")
            .getAsJsonArray("random")
            .get(0).asJsonObject
            .get("title").asString
    }

    private fun fetchArticleFromApi(title: String): Article {
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val json = fetchJson(
            "https://en.wikipedia.org/w/api.php?action=parse&page=$encodedTitle" +
            "&prop=text&format=json&formatversion=2&disableeditsection=1&mobileformat=1"
        )
        if (json.has("error")) {
            val error = json.getAsJsonObject("error").get("info").asString
            throw IOException("API error: $error")
        }
        val parseObj = json.getAsJsonObject("parse")
        val articleTitle = parseObj.get("title").asString
        val htmlText = parseObj.get("text").asString
        return parseArticleFromHtml(articleTitle, htmlText)
    }

    private fun fetchJson(url: String): com.google.gson.JsonObject {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} from $url")
            }
            val body = response.body?.string() ?: throw IOException("Empty response from $url")
            return JsonParser.parseString(body).asJsonObject
        }
    }

    private fun parseArticleFromHtml(title: String, html: String): Article {
        val doc = Jsoup.parse(html, "https://en.wikipedia.org/")
        val content = doc.text()
        val links = doc.select("a[href^='/wiki/']")
            .filter { !it.attr("href").contains(":") }
            .filter { !it.attr("href").contains("#") }
            .filter { it.text().isNotBlank() }
            .filter { !it.text().contains("edit") }
            .filter { !it.text().contains("citation needed") }
            .take(maxLinks)
            .map { WikiLink(it.text(), it.attr("href").substring(6)) }
            .distinctBy { it.target }
        return Article(title, content, links)
    }

    private fun prefetchHyperlinks(article: Article) {
        // Limit to 10 and stagger by 400ms each to avoid hitting Wikipedia's rate limiter
        article.links.take(10).forEachIndexed { index, link ->
            prefetchScope.launch {
                delay(index * 400L)
                Log.d("GameService", "Prefetching '${link.target}'...")
                try {
                    val prefetched = fetchArticleFromApi(link.target)
                    cacheArticle(prefetched, isGameArticle = false, isHyperlink = true, parentArticle = article.title)
                    Log.d("GameService", "Prefetched '${link.target}'.")
                } catch (_: Exception) {
                    Log.d("GameService", "Failed to prefetch '${link.target}'.")
                }
            }
        }
    }

    private suspend fun cacheArticle(
        article: Article,
        isGameArticle: Boolean = false,
        isHyperlink: Boolean = false,
        parentArticle: String? = null
    ) {
        articleCacheDao.insertArticle(
            ArticleCache(
                title = article.title,
                content = article.content,
                links = gson.toJson(article.links),
                isGameArticle = isGameArticle,
                isHyperlink = isHyperlink,
                parentArticle = parentArticle
            )
        )
    }
}

class GameException(message: String) : Exception(message)
