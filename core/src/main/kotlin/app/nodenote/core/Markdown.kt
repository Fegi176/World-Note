package app.nodenote.core

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.html.UrlSanitizer

object Markdown {
    private val parser = Parser.builder().build()
    private val renderer =
        HtmlRenderer.builder()
            .escapeHtml(true)
            .sanitizeUrls(true)
            .urlSanitizer(
                object : UrlSanitizer {
                    override fun sanitizeLinkUrl(url: String): String =
                        if (
                            url.startsWith("nodenote://entry/") ||
                                url.startsWith("https://") ||
                                url.startsWith("http://") ||
                                url.startsWith("mailto:") ||
                                url.startsWith("#")
                        )
                            url
                        else ""

                    override fun sanitizeImageUrl(url: String): String = ""
                }
            )
            .build()

    fun html(source: String): String =
        renderer
            .render(parser.parse(source))
            .replace(Regex("<img\\b[^>]*>"), "[Image reference — use managed gallery]")
}
