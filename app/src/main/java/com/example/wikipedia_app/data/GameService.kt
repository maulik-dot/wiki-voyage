package com.example.wikipedia_app.data

import android.util.Log
import com.example.wikipedia_app.model.Article
import com.example.wikipedia_app.model.WikiLink
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    private val timeout = 25L
    private val maxLinks = 100
    private val gson = Gson()
    private val prefetchJob = SupervisorJob()
    private val prefetchScope = CoroutineScope(Dispatchers.IO + prefetchJob)

    /** Cancels any in-flight background prefetches. Called before every navigation. */
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
                cacheArticle(article, isGameArticle = true)
                prefetchHyperlinks(article)
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
        // Stop the *previous* article's background prefetches first — otherwise they
        // pile up across hops and trip Wikipedia's rate limiter (HTTP 429).
        cancelPrefetch()

        // Prefetch stores articles under their display title (spaces), but links
        // arrive as slugs (underscores) — look up both so prefetched multi-word
        // articles still hit the cache and open instantly.
        val cached = articleCacheDao.getArticle(title)
            ?: articleCacheDao.getArticle(title.replace('_', ' '))
        if (cached != null) {
            Log.d("GameService", "Loaded '$title' from cache.")
            val article = Article(
                title = cached.title,
                content = cached.content,
                links = gson.fromJson(cached.links, object : TypeToken<List<WikiLink>>() {}.type)
            )
            prefetchHyperlinks(article) // warm the next hop's links
            return@withContext article
        }
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(timeout)) {
                Log.d("GameService", "Fetching '$title' from API.")
                val article = fetchArticleFromApi(title)
                cacheArticle(article, isGameArticle = true)
                prefetchHyperlinks(article)
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

    /**
     * Performs a GET and parses JSON, retrying on HTTP 429 (Too Many Requests)
     * with backoff. Honours the server's Retry-After header when present.
     */
    private fun fetchJson(url: String): JsonObject {
        var attempt = 0
        while (true) {
            val request = Request.Builder().url(url).build()
            var backoffMs: Long
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: throw IOException("Empty response from $url")
                    return JsonParser.parseString(body).asJsonObject
                }
                if (response.code == 429 && attempt < MAX_RETRIES) {
                    val retryAfterSec = response.header("Retry-After")?.toLongOrNull()
                    backoffMs = (retryAfterSec?.times(1000) ?: (BASE_BACKOFF_MS shl attempt))
                        .coerceAtMost(MAX_BACKOFF_MS)
                } else {
                    throw IOException("HTTP ${response.code} from $url")
                }
            }
            // Only reached on the 429-retry path (response already closed by use{}).
            attempt++
            Log.d("GameService", "429 rate-limited, backing off ${backoffMs}ms (attempt $attempt)")
            Thread.sleep(backoffMs)
        }
    }

    private fun parseArticleFromHtml(title: String, html: String): Article {
        val doc = Jsoup.parse(html, "https://en.wikipedia.org/")

        // Strip the infobox/tables/refs first. Otherwise the flattened text begins
        // with infobox soup ("Screenplay by … Produced by …") where dozens of links
        // are crammed together with no spacing and are nearly impossible to tap —
        // which made the first several links feel like they "don't open".
        doc.select(
            "table, .infobox, .navbox, .navbox-inner, .vertical-navbox, .hatnote, sup, " +
            ".mw-editsection, .reflist, .mw-references-wrap, .sistersitebox, .gallery, " +
            ".metadata, .noprint, .mbox-small, .thumb, figure, style, script"
        ).remove()

        val output = doc.selectFirst("div.mw-parser-output") ?: doc.body()
        val content = output.text()
        val links = output.select("a[href^='/wiki/']")
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

    /**
     * Warms the cache for the first few links of [article], one at a time with
     * gentle pacing. Runs in the cancellable prefetch scope so a navigation
     * (which calls cancelPrefetch) stops it immediately — keeping request volume
     * low enough to stay under Wikipedia's rate limiter.
     */
    private fun prefetchHyperlinks(article: Article) {
        prefetchScope.launch {
            delay(PREFETCH_START_DELAY_MS) // let the just-opened article settle first
            for (link in article.links.take(PREFETCH_COUNT)) {
                if (!isActive) break
                if (articleCacheDao.getArticle(link.target) != null) continue // already cached
                GameViz.prefetch(article.title, link.target)
                try {
                    val prefetched = fetchArticleFromApi(link.target)
                    cacheArticle(prefetched, isGameArticle = false, isHyperlink = true, parentArticle = article.title)
                    GameViz.prefetchDone(article.title, link.target)
                    Log.d("GameService", "Prefetched '${link.target}'.")
                } catch (_: Exception) {
                    Log.d("GameService", "Failed to prefetch '${link.target}'.")
                }
                if (!isActive) break
                delay(PREFETCH_PACING_MS)
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

    companion object {
        private const val MAX_RETRIES = 2
        private const val BASE_BACKOFF_MS = 800L
        private const val MAX_BACKOFF_MS = 5000L
        private const val PREFETCH_COUNT = 5
        private const val PREFETCH_START_DELAY_MS = 1200L
        private const val PREFETCH_PACING_MS = 900L
    }
}

class GameException(message: String) : Exception(message)
