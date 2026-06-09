package com.example.wikipedia_app.data

import android.util.Log
import com.example.wikipedia_app.model.*
import com.example.wikipedia_app.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.*

class TrendingRepository {
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    fun getTodayFeaturedContent(): Flow<FeaturedArticleResponse> = flow {
        // Featured content for today may not be published yet — fall back to yesterday on 404
        val datesToTry = listOf(
            calendar.time,
            Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time
        )

        for (date in datesToTry) {
            val (year, month, day) = dateFormat.format(date).split("/")
            Log.d("TRENDING_REPO", "Fetching featured article for $year-$month-$day")

            val response = RetrofitInstance.api.getFeaturedContent(year, month, day).execute()
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("TRENDING_REPO", "Featured article response: $body")
                emit(body ?: throw Exception("Empty response"))
                return@flow
            }
            Log.e("TRENDING_REPO", "Error ${response.code()} for $year-$month-$day")
            if (response.code() != 404) break // only retry on 404
        }
        throw Exception("Failed to fetch featured content")
    }.flowOn(Dispatchers.IO)

    fun getTrendingArticles(): Flow<List<TrendingArticleItem>> = flow {
        // Wikimedia pageviews data has a ~2 day lag — try today then fall back up to 3 days
        for (daysBack in 0..3) {
            val date = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -daysBack) }.time
            val (year, month, day) = dateFormat.format(date).split("/")
            Log.d("TRENDING_REPO", "Fetching trending articles for $year-$month-$day")

            val response = RetrofitInstance.api.getTrendingArticles(year, month, day).execute()
            if (response.isSuccessful) {
                val articles = response.body()?.items?.firstOrNull()?.articles
                    ?.filter { !it.title.contains(":") } // drop Special:, Wikipedia:, etc.
                    ?.filter { it.title != "Main_Page" }
                    ?.take(10)
                    ?: emptyList()

                val enriched = coroutineScope {
                    articles.map { article ->
                        async {
                            val thumbnailUrl = try {
                                RetrofitInstance.api.getArticleSummary(article.title)
                                    .execute().body()?.thumbnail?.source
                            } catch (_: Exception) { null }
                            article.copy(thumbnailUrl = thumbnailUrl)
                        }
                    }.awaitAll()
                }
                val withThumb = enriched.count { it.thumbnailUrl != null }
                Log.d("TRENDING_REPO", "Trending articles count: ${enriched.size}, with thumbnails: $withThumb")
                emit(enriched)
                return@flow
            }
            Log.e("TRENDING_REPO", "Error ${response.code()} for $year-$month-$day")
            if (response.code() != 404) break
        }
        emit(emptyList())
    }.flowOn(Dispatchers.IO)

}