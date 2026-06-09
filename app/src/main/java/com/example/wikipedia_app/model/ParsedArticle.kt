package com.example.wikipedia_app.model

data class ParsedArticle(
    val title: String,
    val thumbnailUrl: String?,
    val sections: List<ParsedSection>
)

data class ParsedSection(
    val title: String?,  // null = lead/intro section
    val paragraphs: List<ParsedParagraph>
)

sealed class ParsedParagraph {
    data class Text(val content: String, val links: List<InlineLink> = emptyList()) : ParsedParagraph()
    data class SubHeading(val text: String) : ParsedParagraph()
    data class BulletItem(val content: String, val links: List<InlineLink> = emptyList()) : ParsedParagraph()
    data class Image(val url: String, val caption: String? = null) : ParsedParagraph()
}

data class InlineLink(val start: Int, val end: Int, val target: String)
