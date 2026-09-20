package com.wapo.flagship.features.articles3

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.core.text.HtmlCompat
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.QuestionSet
import com.wapo.flagship.features.articles2.utils.CustomUrlPrefixes
import com.wapo.flagship.features.articles3.models.ui.MimeType
import com.wapo.flagship.features.articles3.views.INLINE_CHIP_ID
import com.wapo.flagship.features.articles3.views.SanitizedHtmlUiStyle
import com.wapo.flagship.features.comments.model.SourceAnnotation
import com.wapo.flagship.features.comments.model.SourceComment
import com.wpds.theme.wpdsColors
import org.jsoup.Jsoup
import kotlin.collections.orEmpty

private data class HtmlLinkSpan(
    val urlSpan: URLSpan,
    val start: Int,
    val end: Int,
    val target: String?,
)

data class ParsedContent(
    val text: AnnotatedString,
    val inlineChipTarget: String? = null,
)

/**
 * Parses content into an [AnnotatedString], based on the [mimeType]
 */
@Composable
fun parseContent(
    content: String,
    mimeType: MimeType,
    articlesInteractionHelper: ArticlesInteractionHelper,
    uiStyle: SanitizedHtmlUiStyle? = null,
    questionSets: List<QuestionSet>? = null,
    sourceAnnotations: List<SourceAnnotation>? = null
): AnnotatedString {
    return parseContentWithMetadata(
        content = content,
        mimeType = mimeType,
        articlesInteractionHelper = articlesInteractionHelper,
        uiStyle = uiStyle,
        questionSets = questionSets,
        sourceAnnotations = sourceAnnotations,
    ).text
}

@Composable
fun parseContentWithMetadata(
    content: String,
    mimeType: MimeType,
    articlesInteractionHelper: ArticlesInteractionHelper,
    uiStyle: SanitizedHtmlUiStyle? = null,
    questionSets: List<QuestionSet>? = null,
    sourceAnnotations: List<SourceAnnotation>? = null,
): ParsedContent {
    return when (mimeType) {
        MimeType.HTML -> parseHtmlContentWithMetadata(
            content,
            articlesInteractionHelper,
            uiStyle,
            questionSets,
            sourceAnnotations
        )

        MimeType.PLAIN -> ParsedContent(
            text = buildAnnotatedString { append(content) },
        )
    }
}

/**
 * Parses HTML content into an [AnnotatedString], applying bold, italic, and anchor spans.
 */
@Composable
fun parseHtmlContent(
    content: String,
    articlesInteractionHelper: ArticlesInteractionHelper,
    uiStyle: SanitizedHtmlUiStyle? = null,
    questionSets: List<QuestionSet>? = null,
    sourceAnnotations: List<SourceAnnotation>? = null,
): AnnotatedString {
    return parseHtmlContentWithMetadata(
        content = content,
        articlesInteractionHelper = articlesInteractionHelper,
        uiStyle = uiStyle,
        questionSets = questionSets,
        sourceAnnotations = sourceAnnotations,
    ).text
}

@Composable
private fun parseHtmlContentWithMetadata(
    content: String,
    articlesInteractionHelper: ArticlesInteractionHelper,
    uiStyle: SanitizedHtmlUiStyle? = null,
    questionSets: List<QuestionSet>? = null,
    sourceAnnotations: List<SourceAnnotation>? = null,
): ParsedContent {
    val spanned = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_LEGACY)
    val plainText = spanned.toString()
    val anchorElements = Jsoup.parse(content).select("a[href]")
    val urlSpans = spanned.getSpans(0, spanned.length, URLSpan::class.java).sortedBy { spanned.getSpanStart(it) }
    val links = buildList {
        urlSpans.forEachIndexed { index, urlSpan ->
            val start = spanned.getSpanStart(urlSpan)
            val end = spanned.getSpanEnd(urlSpan)
            if (start in 0..<end) {
                add(
                    HtmlLinkSpan(
                        urlSpan = urlSpan,
                        start = start,
                        end = end,
                        target = anchorElements.getOrNull(index)?.attr("target"),
                    ),
                )
            }
        }
    }

    val styleSpans = spanned.getSpans(0, spanned.length, StyleSpan::class.java)
        .map { span -> Triple(span, spanned.getSpanStart(span), spanned.getSpanEnd(span)) }
        .filter { (_, start, end) -> start in 0..<end }

    if (links.isEmpty() && styleSpans.isEmpty()) {
        return ParsedContent(text = AnnotatedString(plainText))
    }

    val validQuestionIds = questionSets?.mapNotNull { it.id }.orEmpty()
    var inlineChipTarget: String? = null

    val annotatedString = buildAnnotatedString {
        var cursor = 0

        links.forEach { (urlSpan, start, end, targetValue) ->
            if (start > cursor) append(plainText.substring(cursor, start))

            val href = urlSpan.url.orEmpty()
            val ftsComment = getFtsComment(href, sourceAnnotations)
            val isAskThePost =
                targetValue?.contains(CustomUrlPrefixes.ASK_THE_POST.value, ignoreCase = true)
                    ?: false
            val isFromTheSource =
                targetValue?.contains(CustomUrlPrefixes.FROM_THE_SOURCE.value, ignoreCase = true)
                    ?: false
            val isValidAtp =
                uiStyle == SanitizedHtmlUiStyle.ASK_THE_POST &&
                        isAskThePost &&
                        validQuestionIds.contains(href)
            val isValidFts =
                uiStyle == SanitizedHtmlUiStyle.FROM_THE_SOURCE &&
                        isFromTheSource &&
                        ftsComment?.url != null
            val isDisclaimer =
                targetValue?.contains(CustomUrlPrefixes.DISCLAIMER.value, ignoreCase = true)
                    ?: false

            val isUrlLink = href.startsWith("http", ignoreCase = true) || href.startsWith(
                "mailto",
                ignoreCase = true
            )

            val clickable = isValidAtp || isValidFts || isDisclaimer || isUrlLink
            if (clickable) {
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "link_$start",
                        linkInteractionListener = {
                            when {
                                isValidAtp -> {
                                    articlesInteractionHelper.onEventFired(
                                        ArticleInteractionEvent.AskThePostClickEvent(href)
                                    )
                                }

                                isValidFts -> {
                                    val hasVideo = ftsComment?.video != null
                                    articlesInteractionHelper.onEventFired(
                                        ArticleInteractionEvent.FromTheSourceTapEvent(
                                            commentUrl = ftsComment?.url.orEmpty(),
                                            miscellany = if (hasVideo) "fts-inline-video" else "fts-inline",
                                            sourceName = ftsComment?.author?.name
                                        )
                                    )
                                }

                                isDisclaimer -> {
                                    articlesInteractionHelper.onEventFired(
                                        ArticleInteractionEvent.DisclaimerInfoClicked(href)
                                    )
                                }
                                else -> {
                                    articlesInteractionHelper.onEventFired(
                                        ArticleInteractionEvent.LinkClickEvent(href)
                                    )
                                }
                            }
                        }
                    )
                ) {
                    withStyle(
                        SpanStyle(
                            textDecoration = TextDecoration.Underline,
                            color = wpdsColors.articleText
                        )
                    ) {
                        append(plainText.substring(start, end))
                        when (uiStyle) {
                            SanitizedHtmlUiStyle.ASK_THE_POST if isValidAtp -> {
                                appendInlineContent(INLINE_CHIP_ID)
                                inlineChipTarget = inlineChipTarget ?: targetValue
                            }

                            SanitizedHtmlUiStyle.FROM_THE_SOURCE if isValidFts -> {
                                appendInlineContent(INLINE_CHIP_ID)
                                inlineChipTarget = inlineChipTarget ?: targetValue
                            }

                            SanitizedHtmlUiStyle.DEFAULT if isDisclaimer -> {
                                appendInlineContent(INLINE_CHIP_ID)
                                inlineChipTarget = inlineChipTarget ?: targetValue
                            }

                            else -> {}
                        }
                    }
                }
            } else {
                append(plainText.substring(start, end))
            }

            cursor = end
        }

        if (cursor < plainText.length) append(plainText.substring(cursor))

        // Apply bold / italic formatting from HTML tags (<b>, <strong>, <i>, <em>)
        styleSpans.forEach { (styleSpan, start, end) ->
            val spanStyle = when (styleSpan.style) {
                Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                Typeface.BOLD_ITALIC -> SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )

                else -> null
            }
            spanStyle?.let { addStyle(it, start, end) }
        }
    }

    return ParsedContent(
        text = annotatedString,
        inlineChipTarget = inlineChipTarget,
    )
}

private fun getFtsComment(
    sourceIdFromFeeds: String?,
    sourceAnnotations: List<SourceAnnotation>?,
): SourceComment? {
    val sourceId = sourceIdFromFeeds ?: return null
    val stripped = sourceId.replace(CustomUrlPrefixes.FROM_THE_SOURCE_PREFIX.value, "", ignoreCase = true)
    val sourceAnnotation = sourceAnnotations?.firstOrNull { it.sourceId == stripped }
    val sourceComments = sourceAnnotation?.sourceComments
    return sourceComments?.firstOrNull { it?.video != null } ?: sourceComments?.firstOrNull()
}