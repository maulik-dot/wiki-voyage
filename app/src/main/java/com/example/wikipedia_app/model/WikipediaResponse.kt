package com.example.wikipedia_app.model

data class WikipediaResponse(
    val query: Query?,
    val error: Error? = null,
    val warnings: Warnings? = null
)

data class Query(
    val search: List<SearchResult>,
    val searchinfo: SearchInfo? = null
)

data class SearchResult(
    val title: String,
    val snippet: String,
    @com.google.gson.annotations.SerializedName("pageid") val pageId: Int,
    val size: Int? = null,
    val wordcount: Int? = null,
    val timestamp: String? = null
)

data class SearchInfo(
    val totalhits: Int? = null,
    val suggestion: String? = null
)

data class ArticleResponse(
    val parse: Parse?,
    val error: Error? = null
)

data class Parse(
    val title: String,
    val text: String,
    val sections: List<Section>? = null,
    val displaytitle: String? = null,
    val images: List<String>? = null,
    val links: List<Link>? = null
)

data class Section(
    val toclevel: Int,
    val level: String,
    val line: String,
    val number: String,
    val index: String,
    val fromtitle: String,
    val byteoffset: Int,
    val anchor: String
)

data class Link(
    val title: String,
    val ns: Int,
    val exists: Boolean? = null
)

data class Error(
    val code: String,
    val info: String
)

data class Warnings(
    val search: Warning? = null
)

data class Warning(
    val warnings: String
)

data class ArticleDescriptionResponse(
    val query: DescriptionQuery?,
    val error: Error? = null
)

data class DescriptionQuery(
    val pages: List<DescriptionPage>
)

data class DescriptionPage(
    val pageid: Int,
    val title: String,
    val extract: String?
)