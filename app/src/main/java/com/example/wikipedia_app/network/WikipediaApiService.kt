package com.example.wikipedia_app.network

import com.example.wikipedia_app.model.ArticleDescriptionResponse
import com.example.wikipedia_app.model.ArticleResponse
import com.example.wikipedia_app.model.FeaturedArticle
import com.example.wikipedia_app.model.FeaturedArticleResponse
import com.example.wikipedia_app.model.TrendingArticleResponse
import com.example.wikipedia_app.model.WikipediaResponse
//import com.example.wikipedia_app.model.TrendingResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call
import retrofit2.http.Path

interface WikipediaApiService {
    @GET("w/api.php?action=query&list=search&format=json")
    fun searchArticles(
        @Query("srsearch") query: String,
        @Query("srlimit") limit: Int = 10,
        @Query("sroffset") offset: Int = 0,
        @Query("srwhat") what: String = "text",
        @Query("srinfo") info: String = "totalhits|suggestion"
    ): Call<WikipediaResponse>

    @GET("w/api.php?action=parse&format=json")
    fun getArticleContent(
        @Query("page") title: String,
        @Query("section") section: Int? = null,
        @Query("redirects") redirects: Boolean = true,
        @Query("prop") props: String = "text|sections|displaytitle",
        @Query("formatversion") formatVersion: Int = 2,
        @Query("mobileformat") mobileFormat: Boolean = true,
        @Query("disableeditsection") disableEditSection: Boolean = true,
        @Query("disabletoc") disableToc: Boolean = false
    ): Call<ArticleResponse>

    @GET("w/api.php?action=query&format=json")
    fun getArticleDescription(
        @Query("titles") title: String,
        @Query("prop") props: String = "extracts",
        @Query("exintro") exintro: Boolean = true,
        @Query("explaintext") explaintext: Boolean = true,
        @Query("formatversion") formatVersion: Int = 2
    ): Call<ArticleDescriptionResponse>

    @GET("api/rest_v1/feed/featured/{year}/{month}/{day}")
    fun getFeaturedContent(
        @Path("year") year: String,
        @Path("month") month: String,
        @Path("day") day: String
    ): Call<FeaturedArticleResponse>

    // Pageviews metrics live on wikimedia.org, not wikipedia.org — absolute URL overrides base URL
    @GET("https://wikimedia.org/api/rest_v1/metrics/pageviews/top/en.wikipedia/all-access/{year}/{month}/{day}")
    fun getTrendingArticles(
        @Path("year") year: String,
        @Path("month") month: String,
        @Path("day") day: String
    ): Call<TrendingArticleResponse>

    // Single call that returns the full article HTML + section list + images
    @GET("w/api.php?action=parse&format=json&formatversion=2&prop=text|sections|displaytitle|images&disableeditsection=1")
    suspend fun getFullArticle(@Query("page") title: String): ArticleResponse

    @GET("api/rest_v1/page/summary/{title}")
    fun getArticleSummary(
        @Path("title", encoded = true) title: String
    ): Call<FeaturedArticle>

}