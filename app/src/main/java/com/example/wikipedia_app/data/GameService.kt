package com.example.wikipedia_app.data

import android.util.Log
import com.example.wikipedia_app.model.Article
import com.example.wikipedia_app.model.WikiLink
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class GameService(
    private val articleCacheDao: ArticleCacheDao
) {
    private val timeout = 25L
    private val maxLinks = 100
    private val gson = Gson()
    // The paced prefetch *loop* — cancelled on every navigation.
    private val prefetchJob = SupervisorJob()
    private val prefetchScope = CoroutineScope(Dispatchers.IO + prefetchJob)

    // Long-lived scope for the actual network fetches. NOT cancelled on navigation,
    // so a fetch already in flight (started by prefetch or press) survives and a
    // click can join it instead of starting a duplicate request.
    private val fetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // (C) In-flight de-duplication: title-key -> the single shared fetch for it.
    private val inFlight = ConcurrentHashMap<String, Deferred<Article>>()

    /** Cancels the paced prefetch loop (in-flight fetches keep going). */
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
        // (E) Fresh slate for a new game: a new random race shares nothing with the
        // last one, so drop the whole cache. Combined with per-hop pruneLinksOf(),
        // this keeps storage to one game's window (path articles + current links)
        // instead of accumulating every prefetch across every game ever played.
        articleCacheDao.clearAll()
        inFlight.clear()
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
        // Stop the *previous* article's paced prefetch loop (its in-flight fetch,
        // if any, keeps running so we can join it below).
        cancelPrefetch()
        try {
            withTimeout(TimeUnit.SECONDS.toMillis(timeout)) {
                // (C) Joins an in-flight prefetch/press fetch, or the cache, or starts one.
                val article = getOrFetch(title, isGameArticle = true, parentArticle = null)
                // A visited article must be tagged as a game article so the
                // sliding-window eviction (E) never deletes it — it may have been
                // cached as a hyperlink by prefetch/press.
                cacheArticle(article, isGameArticle = true)
                prefetchHyperlinks(article) // warm the next hop's links
                article
            }
        } catch (e: Exception) {
            when (e) {
                is UnknownHostException -> throw GameException("No internet connection")
                else -> throw GameException("Failed to load article: ${e.message}")
            }
        }
    }

    /**
     * (D) Warm a single link in the background — called when the user *presses*
     * (finger-down) a link, before the tap completes. De-duplicated via [getOrFetch],
     * so the subsequent click joins this fetch instead of starting a new one. This
     * makes *any* link feel instant at the cost of one request per real press.
     */
    fun warm(parentTitle: String, target: String) {
        fetchScope.launch {
            try {
                if (peekCache(target) == null) {
                    getOrFetch(target, isGameArticle = false, parentArticle = parentTitle)
                }
            } catch (_: Exception) { /* best-effort */ }
        }
    }

    /**
     * (E) Sliding-window eviction: once we've moved on from [parentTitle], drop the
     * prefetched link blobs it warmed. The article we actually navigated *to* was
     * re-tagged as a game article (isHyperlink = 0), so it is never deleted here,
     * and articles on the back-path are game articles too.
     */
    suspend fun pruneLinksOf(parentTitle: String) = withContext(Dispatchers.IO) {
        try {
            articleCacheDao.deleteArticleLinks(parentTitle)
        } catch (_: Exception) { /* non-critical */ }
    }

    /**
     * (C) Returns the article from cache, or joins the single in-flight fetch for it,
     * or starts one in [fetchScope]. Guarantees at most one network fetch per title
     * regardless of how many callers (press + tap + prefetch) race for it.
     */
    private suspend fun getOrFetch(
        title: String,
        isGameArticle: Boolean,
        parentArticle: String?
    ): Article {
        peekCache(title)?.let {
            Log.d("GameService", "Loaded '$title' from cache.")
            return it
        }
        val key = keyOf(title)
        val deferred = inFlight.computeIfAbsent(key) {
            fetchScope.async {
                try {
                    Log.d("GameService", "Fetching '$title' from API.")
                    val article = fetchArticleFromApi(title)
                    cacheArticle(
                        article,
                        isGameArticle = isGameArticle,
                        isHyperlink = !isGameArticle,
                        parentArticle = parentArticle
                    )
                    article
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return deferred.await()
    }

    /** Cache lookup that tolerates slug (underscores) vs display title (spaces). */
    private suspend fun peekCache(title: String): Article? {
        val cached = articleCacheDao.getArticle(title)
            ?: articleCacheDao.getArticle(normalizeTitle(title))
        return cached?.let {
            Article(
                title = it.title,
                content = it.content,
                links = gson.fromJson(it.links, object : TypeToken<List<WikiLink>>() {}.type)
            )
        }
    }

    private fun normalizeTitle(t: String) = t.replace('_', ' ').trim()
    private fun keyOf(t: String) = normalizeTitle(t).lowercase()

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
            .map {
                // hrefs for non-ASCII titles are already percent-encoded
                // (e.g. /wiki/%E1%B9%9Ata). Decode here so fetchArticleFromApi
                // encodes exactly once — double-encoding yields "Bad title".
                val raw = it.attr("href").substring(6)
                val target = try { URLDecoder.decode(raw, "UTF-8") } catch (_: Exception) { raw }
                WikiLink(it.text(), target)
            }
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
                if (peekCache(link.target) != null) continue // already cached / in-flight handled below
                GameViz.prefetch(article.title, link.target)
                try {
                    // Shares the in-flight slot (C): a press/click on this link joins this fetch.
                    getOrFetch(link.target, isGameArticle = false, parentArticle = article.title)
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
