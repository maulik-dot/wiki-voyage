package com.example.wikipedia_app.model

import com.google.gson.annotations.SerializedName

// Pageviews API: items[0].articles[{article, views, rank}]
data class TrendingArticleResponse(
    @SerializedName("items")
    val items: List<TrendingArticleDay>
)

data class TrendingArticleDay(
    @SerializedName("articles")
    val articles: List<TrendingArticleItem>
)

data class TrendingArticleItem(
    @SerializedName("article")
    val title: String,
    @SerializedName("views")
    val views: Int,
    @SerializedName("rank")
    val rank: Int,
    val thumbnailUrl: String? = null  // populated after a separate batch thumbnail fetch
)


data class FeaturedArticleResponse(
    @SerializedName("tfa")
    val today: FeaturedArticle?,
    @SerializedName("mostread")
    val mostRead: MostRead?,
    @SerializedName("onthisday")
    val onThisDay: List<OnThisDayItem>?,
    @SerializedName("image")
    val image: FeaturedImage?
)

data class FeaturedArticle(
    @SerializedName("title")
    val title: String,
    @SerializedName("extract")
    val extract: String,
    @SerializedName("thumbnail")
    val thumbnail: Thumbnail?,
    @SerializedName("content_urls")
    val contentUrls: ContentUrls?
)

data class OnThisDayItem(
    @SerializedName("text")
    val text: String,
    @SerializedName("year")
    val year: Int,
    @SerializedName("pages")
    val pages: List<FeaturedArticle>?
)

data class FeaturedImage(
    @SerializedName("title")
    val title: String,
    @SerializedName("thumbnail")
    val thumbnail: Thumbnail?,
    @SerializedName("description")
    val description: ImageDescription?
)

data class ImageDescription(
    @SerializedName("text")
    val text: String?,
    @SerializedName("html")
    val html: String?
)

data class Thumbnail(
    @SerializedName("source")
    val source: String,
    @SerializedName("width")
    val width: Int,
    @SerializedName("height")
    val height: Int
)

data class ContentUrls(
    @SerializedName("desktop")
    val desktop: PageUrls?
)

data class PageUrls(
    @SerializedName("page")
    val page: String
)

data class MostRead(
    @SerializedName("articles")
    val articles: List<FeaturedArticle>
)

 