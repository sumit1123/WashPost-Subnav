package com.wapo.flagship.features.articles2.utils

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.createBitmap
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import android.view.View
import android.webkit.URLUtil.isValidUrl
import androidx.core.content.ContextCompat
import com.wapo.flagship.features.comments.model.SourceAnnotation
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.Style
import com.wapo.flagship.features.comments.model.SourceComment
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.text.CustomUnderlineMarkerSpan
import com.wapo.text.DrawableOutlineSpan
import com.wapo.text.WpLinkAppearanceSpan
import com.washingtonpost.android.articles.R

object StylesHelper {
    fun getArticleItemStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticlesRecyclerView,
                0,
                0,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticlesRecyclerView_article_items_style,
                R.style.ArticleItemsStyle,
            ).also { typedArray.recycle() }
    }

    fun getTextItemStyle(
        context: Context,
        style: Style,
    ): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        return when (style) {
            Style.BRIEFS, Style.SEVEN_LIVE ->
                typedArray.getResourceId(
                    R.styleable.ArticleItems_article_text_briefs_style,
                    R.style.ArticleText_Briefs,
                )
            Style.DEFAULT, Style.OPINIONS ->
                typedArray.getResourceId(
                    R.styleable.ArticleItems_article_text_style,
                    R.style.ArticleText,
                )
        }.also { typedArray.recycle() }
    }

    fun getTextListItemStyle(
        context: Context,
        style: Style,
    ): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        return when (style) {
            Style.BRIEFS, Style.SEVEN_LIVE ->
                typedArray.getResourceId(
                    R.styleable.ArticleItems_article_text_list_item_briefs_style,
                    R.style.ArticleText_Briefs_List,
                )
            Style.DEFAULT, Style.OPINIONS ->
                typedArray.getResourceId(
                    R.styleable.ArticleItems_article_text_style,
                    R.style.ArticleText,
                )
        }.also { typedArray.recycle() }
    }

    fun getTextItemSubheadStyle(
        context: Context,
        style: Style,
        subheadLevel: Int? = null,
    ): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        return when (style) {
            Style.BRIEFS, Style.SEVEN_LIVE -> {
                if (subheadLevel == 3) {
                    typedArray.getResourceId(
                        R.styleable.ArticleItems_article_text_briefs_exclusive_label_style,
                        R.style.ArticleText_Briefs_ExclusiveLabel,
                    )
                } else {
                    typedArray.getResourceId(
                        R.styleable.ArticleItems_article_text_briefs_style,
                        R.style.ArticleText_Briefs,
                    )
                }
            }
            Style.DEFAULT, Style.OPINIONS ->
                typedArray.getResourceId(
                    R.styleable.ArticleItems_article_text_subhead_style,
                    R.style.ArticleText_Subhead,
                )
        }.also {
            typedArray.recycle()
        }
    }

    fun getTextItemBriefsNumberStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_text_briefs_number_style,
                R.style.ArticleText_Briefs_Number,
            ).also { typedArray.recycle() }
    }

    fun getTextItemFooterStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_text_footer_style,
                R.style.ArticleText_Footer,
            ).also { typedArray.recycle() }
    }

    fun getTextItemIntroStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_text_intro_style,
                R.style.ArticleText_Intro,
            ).also { typedArray.recycle() }
    }

    fun getTextItemLetterStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_text_letter_style,
                R.style.ArticleText_Letter,
            ).also { typedArray.recycle() }
    }

    fun getTextItemMetaTextStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_text_metatext_style,
                R.style.ArticleText_MetaText,
            ).also { typedArray.recycle() }
    }

    fun getBlockQuoteLineColor(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_blockquote_color,
                R.color.article_text_blockquote_margin_color,
            ),
        )
    }

    fun getBlockQuoteMargin(context: Context): Float {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return context.resources
            .getDimensionPixelSize(
                typedArray.getResourceId(
                    R.styleable.ArticleItems_article_blockquote_margin,
                    R.dimen.article_text_blockquote_margin_color_size,
                ),
            ).toFloat()
            .also { typedArray.recycle() }
    }

    fun getBlockQuoteLineWidth(context: Context): Float {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return context.resources
            .getDimensionPixelSize(
                typedArray.getResourceId(
                    R.styleable.ArticleItems_article_blockquote_width,
                    R.dimen.article_text_blockquote_margin_gap_size,
                ),
            ).toFloat()
            .also { typedArray.recycle() }
    }

    fun getVideoCaptionStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                getArticleItemStyle(context),
                R.styleable.ArticleItems,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_video_caption_style,
                R.style.ArticleVideoCaption,
            ).also { typedArray.recycle() }
    }

    fun isAllCaps(
        resId: Int,
        context: Context,
    ): Boolean {
        var a: TypedArray? = null
        return try {
            a = context.obtainStyledAttributes(resId, intArrayOf(android.R.attr.textAllCaps))
            a.getBoolean(0, false)
        } finally {
            a?.recycle()
        }
    }

    fun makeLinkClickable(
        charSequence: CharSequence,
        context: Context,
        articlesInteractionHelper: ArticlesInteractionHelper,
        questionSetIds: List<String?>? = emptyList(),
        sourceAnnotations: List<SourceAnnotation>? = emptyList(),
    ): SpannableStringBuilder {
        val strBuilder = charSequence as? SpannableStringBuilder ?: SpannableStringBuilder(charSequence)
        val urls =
            strBuilder.getSpans(
                0,
                charSequence.length,
                URLSpan::class.java,
            )
        if (urls != null) {
            for (span in urls) {
                val url = span.url
                when {
                    isValidAskThePost(url) -> {
                        val validQuestions = urlMatchesQuestionSet(url, questionSetIds)
                        styleATPText(
                            strBuilder,
                            span,
                            validQuestions,
                            context,
                            articlesInteractionHelper,
                        )
                    }
                    isValidFromTheSource(url, sourceAnnotations) -> {
                        styleFTSText(
                            strBuilder,
                            span,
                            getFtsComment(url, sourceAnnotations),
                            context,
                            articlesInteractionHelper
                        )
                    }
                    else -> {
                        makeLinkClickable(
                            strBuilder,
                            span,
                            context,
                            articlesInteractionHelper,
                        )
                    }
                }
            }
        }
        return strBuilder
    }

    private fun isValidAskThePost(url: String): Boolean {
        return url.startsWith(CustomUrlPrefixes.ASK_THE_POST.value, true)
    }

    private fun styleATPText(
        strBuilder: SpannableStringBuilder,
        span: URLSpan,
        validQuestions: Boolean,
        context: Context,
        articlesInteractionHelper: ArticlesInteractionHelper,
    ) {
        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        val questionId = span.url
        val clickable: WpLinkAppearanceSpan =
            object : WpLinkAppearanceSpan(context, shouldShowStandardUnderline()) {
                override fun onClick(view: View) {
                    if (validQuestions) {
                        articlesInteractionHelper.onEventFired(
                            ArticleInteractionEvent.AskThePostClickEvent(
                                questionId,
                            ),
                        )
                    }
                }
            }

        if (validQuestions) {
            strBuilder.insert(end, "\u2009")
            val drawable = createChipWithShadow(context, com.wpds.wpds.R.drawable.atp_chip)
            drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            val imageSpan = drawable?.let { DrawableOutlineSpan(it) }
            strBuilder.setSpan(imageSpan, end, end + 1, flags)
        }
        strBuilder.removeSpan(span)
        strBuilder.setSpan(clickable, start, end + 1, flags)
    }

    private fun isValidFromTheSource(sourceIdFromFeeds: String?, sourceAnnotations: List<SourceAnnotation>?): Boolean {
        val correctPrefix = sourceIdFromFeeds?.startsWith(CustomUrlPrefixes.FROM_THE_SOURCE_PREFIX.value, true) == true
        val feedsItemHasCommentMatch = getFtsComment(sourceIdFromFeeds, sourceAnnotations) != null
        return correctPrefix && feedsItemHasCommentMatch
    }

    /**
     * @return The first comment for the source that contains a video. If no comments contain a
     * video, just return the first comment.
     */
    private fun getFtsComment(sourceIdFromFeeds: String?, sourceAnnotations: List<SourceAnnotation>?): SourceComment? {
        sourceIdFromFeeds ?: return null
        val sourceAnnotation = sourceAnnotations?.firstOrNull {
            it.sourceId == stripFtsFromSourceId(sourceIdFromFeeds)
        }
        val sourceComments = sourceAnnotation?.sourceComments
        // Prioritize video comment if there is one
        return sourceComments?.firstOrNull { it?.video != null } ?: sourceComments?.firstOrNull()
    }

    private fun stripFtsFromSourceId(sourceIdFromFeeds: String): String {
        return sourceIdFromFeeds.replace(CustomUrlPrefixes.FROM_THE_SOURCE_PREFIX.value, "", true)
    }

    private fun createChipWithShadow(
        context: Context,
        @DrawableRes vectorId: Int
    ): Drawable? {
        // load the chip
        val vector = ContextCompat.getDrawable(context, vectorId) ?: return null

        // set strength of blur and padding
        val density = context.resources.displayMetrics.density
        val blurRadius = 3f * density
        val padding = (6 * density).toInt()

        val chipWidth = vector.intrinsicWidth
        val chipHeight = vector.intrinsicHeight

        // create mask
        val mask = createBitmap(chipWidth, chipHeight, Bitmap.Config.ALPHA_8)
        val maskCanvas = Canvas(mask)
        vector.setBounds(0, 0, chipWidth, chipHeight)
        vector.draw(maskCanvas)

        // apply the blur
        val blurPaint = Paint().apply {
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        }
        val offsetXY = IntArray(2)
        val shadowBitmap = mask.extractAlpha(blurPaint, offsetXY)

        val resultWidth = chipWidth
        val resultHeight = chipHeight + padding * 2
        val result = createBitmap(resultWidth, resultHeight)
        val canvas = Canvas(result)

        // shadow with 15% opacity
        val shadowPaint = Paint().apply {
            color = Color.argb((255 * 0.15f).toInt(), 0, 0, 0)
        }

        // position shadow
        val shadowX = offsetXY[0]
        val shadowY = padding + offsetXY[1] + (2f * density).toInt()

        canvas.drawBitmap(shadowBitmap, shadowX.toFloat(), shadowY.toFloat(), shadowPaint)

        // draw chip on top of the shadow
        vector.setBounds(0, padding, chipWidth, padding + chipHeight)
        vector.draw(canvas)

        val drawable = result.toDrawable(context.resources)
        drawable.setBounds(0, 0, resultWidth, resultHeight)
        mask.recycle()
        shadowBitmap.recycle()
        return drawable
    }

    private fun styleFTSText(
        strBuilder: SpannableStringBuilder,
        span: URLSpan,
        sourceComment: SourceComment?,
        context: Context,
        articlesInteractionHelper: ArticlesInteractionHelper,
    ) {
        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        val commentUrl = sourceComment?.url
        val authorName = sourceComment?.author?.name
        val hasVideoComment = sourceComment?.video != null
        val clickable: WpLinkAppearanceSpan =
            object  : WpLinkAppearanceSpan(context, commentUrl != null) {
                override fun onClick(view: View) {
                    commentUrl?.let {
                        articlesInteractionHelper.onEventFired(
                            ArticleInteractionEvent.FromTheSourceTapEvent(
                                commentUrl,
                                if (hasVideoComment) {
                                    Measurement.FTS_INLINE_VIDEO
                                } else {
                                    Measurement.FTS_INLINE
                                },
                                authorName
                            ),
                        )
                    }
                }
            }
        strBuilder.insert(end, "\u2009")
        strBuilder.setSpan(
            CustomUnderlineMarkerSpan(ContextCompat.getColor(context, R.color.deck_text)),
            start,
            end,
            flags,
        )
        val drawable = createChipWithShadow(context, com.wpds.wpds.R.drawable.fts_chip)
        drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val imageSpan = drawable?.let { DrawableOutlineSpan(it) }
        strBuilder.setSpan(imageSpan, end, end + 1, flags)
        strBuilder.removeSpan(span)
        strBuilder.setSpan(clickable, start, end + 1, flags)
    }

    private fun makeLinkClickable(
        strBuilder: SpannableStringBuilder,
        span: URLSpan,
        context: Context,
        articlesInteractionHelper: ArticlesInteractionHelper,
    ) {
        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        val url = span.url
        val clickable: WpLinkAppearanceSpan =
            object : WpLinkAppearanceSpan(context, isValidUrl(url)) {
                override fun onClick(view: View) {
                    if (isValidUrl(url)) {
                        articlesInteractionHelper.onEventFired(
                            ArticleInteractionEvent.LinkClickEvent(
                                url,
                            ),
                        )
                    }
                }
            }
        strBuilder.removeSpan(span)
        strBuilder.setSpan(clickable, start, end, flags)
    }

    fun getTextSpacingExtra(context: Context): Float {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        val textItemStyle =
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_text_style,
                R.style.ArticleText,
            )
        val a = context.obtainStyledAttributes(textItemStyle, R.styleable.ArticleItems)
        return a.getDimensionPixelSize(R.styleable.ArticleItems_article_line_spacing_extra, 0).toFloat().also { a.recycle() }
    }

    fun getTextSpacingMult(context: Context): Float {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        val textItemStyle =
            typedArray.getResourceId(
                R.styleable.ArticleItems_article_text_style,
                R.style.ArticleText,
            )
        val a = context.obtainStyledAttributes(textItemStyle, R.styleable.ArticleItems)
        return a.getFloat(R.styleable.ArticleItems_article_line_spacing_mult, 1f).also { a.recycle() }
    }

    fun getParagraphHeadingSize(
        context: Context,
        headingLevel: Int,
    ): Int =
        when (headingLevel) {
            1 ->
                context.resources.getDimensionPixelSize(
                    com.washingtonpost.android.R.dimen.article_text_h1,
                )
            2 ->
                context.resources.getDimensionPixelSize(
                    com.washingtonpost.android.R.dimen.article_text_h2,
                )
            3 ->
                context.resources.getDimensionPixelSize(
                    com.washingtonpost.android.R.dimen.article_text_h3,
                )
            4 ->
                context.resources.getDimensionPixelSize(
                    com.washingtonpost.android.R.dimen.article_text_h4,
                )
            5 ->
                context.resources.getDimensionPixelSize(
                    com.washingtonpost.android.R.dimen.article_text_h5,
                )
            6 ->
                context.resources.getDimensionPixelSize(
                    com.washingtonpost.android.R.dimen.article_text_h6,
                )
            else ->
                context.resources.getDimensionPixelSize(
                    com.washingtonpost.android.R.dimen.article_text_h6,
                )
        }

    fun getTruncateStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_truncate_text_style,
                R.style.TruncateText,
            ).also { typedArray.recycle() }
    }

    fun getExpendedBylineTextItemStyle(context: Context): Int {
        val typedArray: TypedArray =
            context.theme.obtainStyledAttributes(
                null,
                R.styleable.ArticleItems,
                0,
                0,
            )
        return typedArray
            .getResourceId(
                R.styleable.ArticleItems_article_text_style,
                R.style.expanded_byline_style,
            ).also { typedArray.recycle() }
    }

    private fun urlMatchesQuestionSet(
        url: String,
        questionSetIds: List<String?>?,
    ): Boolean = questionSetIds?.any { id -> url == id } ?: false

    private fun shouldShowStandardUnderline(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }
}

enum class CustomUrlPrefixes(val value: String) {
    ASK_THE_POST("atp"),
    FROM_THE_SOURCE_PREFIX("FTS_"),
    FROM_THE_SOURCE("fts"),
    DISCLAIMER("disclaimer"),
}
