package com.example.wikipedia_app.data

import com.example.wikipedia_app.model.InlineLink
import com.example.wikipedia_app.model.ParsedArticle
import com.example.wikipedia_app.model.ParsedParagraph
import com.example.wikipedia_app.model.ParsedSection
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URLDecoder

object ArticleParser {

    private val STOP_SECTIONS = setOf(
        "see also", "notes", "references", "footnotes",
        "external links", "further reading", "bibliography"
    )

    fun parse(title: String, html: String, thumbnailUrl: String?): ParsedArticle {
        val doc = Jsoup.parse(html, "https://en.wikipedia.org/")

        // Strip noise before walking the tree
        doc.select(
            "table, .infobox, .navbox, .navbox-inner, .hatnote, sup, " +
            ".mw-editsection, .reflist, .mw-references-wrap, .sistersitebox, " +
            ".metadata, .noprint, .mbox-small, style, script"
        ).remove()

        val output = doc.selectFirst("div.mw-parser-output") ?: doc.body()

        val sections = mutableListOf<ParsedSection>()
        var currentTitle: String? = null
        val currentParagraphs = mutableListOf<ParsedParagraph>()

        for (element in output.children()) {
            when (element.tagName()) {
                "h2" -> {
                    val text = element.text().trim()
                    if (STOP_SECTIONS.contains(text.lowercase())) break
                    flush(currentTitle, currentParagraphs, sections)
                    currentTitle = text
                }
                "h3", "h4" -> {
                    val text = element.text().trim()
                    if (text.isNotBlank()) currentParagraphs.add(ParsedParagraph.SubHeading(text))
                }
                "p" -> {
                    val text = element.text().trim()
                    if (text.isNotBlank()) {
                        currentParagraphs.add(ParsedParagraph.Text(text, extractLinks(element, text)))
                    }
                }
                "ul", "ol" -> {
                    element.select("li").forEach { li ->
                        val text = li.text().trim()
                        if (text.isNotBlank()) {
                            currentParagraphs.add(ParsedParagraph.BulletItem(text, extractLinks(li, text)))
                        }
                    }
                }
                "figure", "div" -> {
                    val img = element.selectFirst("img")
                    if (img != null) {
                        val rawSrc = img.attr("src").takeIf { it.isNotBlank() }
                            ?: img.attr("data-src").takeIf { it.isNotBlank() }
                        if (rawSrc != null) {
                            val url = if (rawSrc.startsWith("//")) "https:$rawSrc" else rawSrc
                            val caption = element.selectFirst("figcaption")?.text()
                            currentParagraphs.add(ParsedParagraph.Image(url, caption))
                        }
                    }
                }
            }
        }
        flush(currentTitle, currentParagraphs, sections)

        return ParsedArticle(
            title = cleanHtml(title),
            thumbnailUrl = thumbnailUrl,
            sections = sections.filter { it.paragraphs.isNotEmpty() }
        )
    }

    private fun flush(
        title: String?,
        paragraphs: MutableList<ParsedParagraph>,
        sections: MutableList<ParsedSection>
    ) {
        if (paragraphs.isNotEmpty()) {
            sections.add(ParsedSection(title, paragraphs.toList()))
            paragraphs.clear()
        }
    }

    private fun extractLinks(element: Element, fullText: String): List<InlineLink> {
        val links = mutableListOf<InlineLink>()
        var cursor = 0
        element.select("a[href^='/wiki/']")
            .sortedBy { fullText.indexOf(it.text()) }
            .forEach { a ->
                val href = a.attr("href")
                if (href.contains(":")) return@forEach
                // Decode percent-encoded slugs (non-ASCII titles) so callers encode
                // exactly once — double-encoding yields "Bad title" API errors.
                val rawTarget = href.removePrefix("/wiki/")
                val target = try { URLDecoder.decode(rawTarget, "UTF-8") } catch (_: Exception) { rawTarget }
                val text = a.text().trim()
                if (text.isBlank()) return@forEach
                val start = fullText.indexOf(text, cursor)
                if (start < 0) return@forEach
                val end = start + text.length
                links.add(InlineLink(start, end, target))
                cursor = end
            }
        return links
    }

    private fun cleanHtml(html: String) = html.replace(Regex("<[^>]+>"), "").trim()
}
