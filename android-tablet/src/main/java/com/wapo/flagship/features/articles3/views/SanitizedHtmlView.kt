package com.wapo.flagship.features.articles3.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.webkit.WebView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.utils.CustomUrlPrefixes
import com.wapo.flagship.features.articles3.models.ui.MimeType
import com.wapo.flagship.features.articles3.models.ui.SanitizedHtmlUiModel
import com.wapo.flagship.features.articles3.parseContent
import com.wapo.flagship.features.articles3.parseContentWithMetadata
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors
import com.wpds.wpds.R
import kotlin.math.roundToInt

const val INLINE_CHIP_ID = "chipIcon"
private const val DISCLAIMER_ICON_SIZE_DP = 28

@Composable
@SuppressLint("SetJavaScriptEnabled")
fun SanitizedHtmlView(
    uiModel: SanitizedHtmlUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper,
) {
    when (uiModel.uiStyle) {
        SanitizedHtmlUiStyle.BLOCKQUOTE -> {
            val lineColor = wpdsColors.primary
            Text(
                text = parseContent(
                    content = uiModel.content,
                    mimeType = uiModel.mime ?: MimeType.HTML,
                    articlesInteractionHelper = articlesInteractionHelper,
                    uiStyle = uiModel.uiStyle,
                    questionSets = uiModel.questionSets,
                    sourceAnnotations = uiModel.sourceAnnotations,
                ),
                style = getSanitizedHtmlStyle(uiModel),
                modifier = Modifier
                    .drawBehind {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(start = 23.5.dp)
            )
        }
        SanitizedHtmlUiStyle.DEFAULT,
        SanitizedHtmlUiStyle.ASK_THE_POST,
        SanitizedHtmlUiStyle.FROM_THE_SOURCE -> {
            val parsedContent = parseContentWithMetadata(
                content = uiModel.content,
                mimeType = uiModel.mime ?: MimeType.HTML,
                articlesInteractionHelper = articlesInteractionHelper,
                uiStyle = uiModel.uiStyle,
                questionSets = uiModel.questionSets,
                sourceAnnotations = uiModel.sourceAnnotations,
            )
            Text(
                text = parsedContent.text,
                style = getSanitizedHtmlStyle(uiModel),
                inlineContent = getInlineChip(parsedContent.inlineChipTarget),
            )
        }
        SanitizedHtmlUiStyle.SUBHEAD_BRIEFS -> {
            val backgroundColor = wpdsColors.theSevenDigit
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = wpdsColors.theSevenDigit,
                        shape = CircleShape
                    )
            ) {
                Text(
                    text = parseContent(
                        content = uiModel.content,
                        mimeType = uiModel.mime ?: MimeType.HTML,
                        articlesInteractionHelper = articlesInteractionHelper,
                        uiStyle = uiModel.uiStyle,
                        questionSets = uiModel.questionSets,
                        sourceAnnotations = uiModel.sourceAnnotations,
                    ),
                    style = getSanitizedHtmlStyle(uiModel),
                    modifier = Modifier
                        .drawBehind {
                            drawCircle(
                                color = backgroundColor,
                                radius = 32f
                            )
                        }
                )
            }
        }
        SanitizedHtmlUiStyle.BRIEFS_EXCLUSIVE_LABEL -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = wpdsColors.primary,
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(start = 8.dp, top = 5.dp, end = 8.dp, bottom = 5.dp)
            ) {
                Icon(
                    painter = painterResource(com.washingtonpost.android.sections.R.drawable.ic_wp_briefs_exclusive_label),
                    contentDescription = "WP logo",
                    tint = wpdsColors.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = parseContent(
                        content = uiModel.content.uppercase(),
                        mimeType = uiModel.mime ?: MimeType.HTML,
                        articlesInteractionHelper = articlesInteractionHelper,
                        uiStyle = uiModel.uiStyle,
                        questionSets = uiModel.questionSets,
                        sourceAnnotations = uiModel.sourceAnnotations,
                    ),
                    style = getSanitizedHtmlStyle(uiModel),
                )
            }
        }
        SanitizedHtmlUiStyle.SOCIAL_EMBED -> {
            uiModel.oEmbed ?: return
            Text(
                text = parseContent(
                    content = uiModel.content,
                    mimeType = uiModel.mime ?: MimeType.HTML,
                    articlesInteractionHelper = articlesInteractionHelper,
                    uiStyle = uiModel.uiStyle,
                    questionSets = uiModel.questionSets,
                    sourceAnnotations = uiModel.sourceAnnotations,
                )
            )
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        // Enable JS for trusted social embed widget rendering.
                        enableJavascriptForSocialEmbed(this)

                        // Use a base URL so the widget's relative scripts load
                        loadData(uiModel.oEmbed, "text/html", "utf-8")
                    }
                },
                update = { webView ->
                    webView.loadData(uiModel.oEmbed, "text/html", "utf-8")
                }
            )
        }
        else -> {
            Text(
                text = parseContent(
                    content = uiModel.content,
                    mimeType = uiModel.mime ?: MimeType.HTML,
                    articlesInteractionHelper = articlesInteractionHelper,
                    uiStyle = uiModel.uiStyle,
                    questionSets = uiModel.questionSets,
                    sourceAnnotations = uiModel.sourceAnnotations,
                ),
                style = getSanitizedHtmlStyle(uiModel)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun enableJavascriptForSocialEmbed(webView: WebView) {
    webView.settings.javaScriptEnabled = true
}

@Composable
private fun getInlineChip(
    targetValue: String?,
): Map<String, InlineTextContent> {
    val vectorId = when {
        targetValue == null -> null
        targetValue.contains(CustomUrlPrefixes.ASK_THE_POST.value, ignoreCase = true) -> R.drawable.atp_chip
        targetValue.contains(CustomUrlPrefixes.DISCLAIMER.value, ignoreCase = true) -> R.drawable.info
        targetValue.contains(CustomUrlPrefixes.FROM_THE_SOURCE_PREFIX.value, ignoreCase = true) -> R.drawable.fts_chip
        else -> null
    }

    val bitmap = if (vectorId != null) {
        createChipWithShadow(
            context = LocalContext.current,
            drawableHeightDp = if (vectorId == R.drawable.info) DISCLAIMER_ICON_SIZE_DP else null,
            vectorId = vectorId
        )
    } else {
        null
    }

    bitmap ?: return mapOf()

    val chipDescription = when (vectorId) {
        R.drawable.atp_chip -> "Ask The Post"
        R.drawable.fts_chip -> "From The Source"
        else -> "Disclaimer"
    }

    val inlineContent = mapOf(
        INLINE_CHIP_ID to InlineTextContent(
            placeholder = Placeholder(
                height = with(LocalDensity.current) { bitmap.height.toSp() },
                width = with(LocalDensity.current) { bitmap.width.toSp() },
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            ),
            children = {
                Image(
                    painter = BitmapPainter(bitmap),
                    contentDescription = chipDescription,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        )
    )

    return inlineContent
}

private fun createChipWithShadow(
    context: Context,
    @DrawableRes vectorId: Int,
    drawableHeightDp: Int? = null,
): ImageBitmap? {
    // load the chip
    val vector = ContextCompat.getDrawable(context, vectorId) ?: return null

    // set strength of blur and padding
    val density = context.resources.displayMetrics.density
    val blurRadius = 3f * density
    val padding = (6 * density).toInt()

    val chipHeight = drawableHeightDp?.let { (it * density).roundToInt() } ?: vector.intrinsicHeight
    val chipWidth = if (drawableHeightDp == null) {
        vector.intrinsicWidth
    } else {
        (chipHeight * vector.intrinsicWidth.toFloat() / vector.intrinsicHeight).roundToInt()
    }

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

    val resultHeight = chipHeight + padding * 2
    val result = createBitmap(chipWidth, resultHeight)
    val canvas = Canvas(result)

    // shadow with 15% opacity
    val shadowPaint = Paint().apply {
        color = android.graphics.Color.argb((255 * 0.15f).toInt(), 0, 0, 0)
    }

    // position shadow
    val shadowX = offsetXY[0]
    val shadowY = padding + offsetXY[1] + (2f * density).toInt()

    canvas.drawBitmap(shadowBitmap, shadowX.toFloat(), shadowY.toFloat(), shadowPaint)

    // draw chip on top of the shadow
    vector.setBounds(0, padding, chipWidth, padding + chipHeight)
    vector.draw(canvas)

    val drawable = result.toDrawable(context.resources)
    drawable.setBounds(0, 0, chipWidth, resultHeight)
    mask.recycle()
    shadowBitmap.recycle()
    return drawable.bitmap?.asImageBitmap()
}

@Composable
private fun getSanitizedHtmlStyle(uiModel: SanitizedHtmlUiModel): TextStyle {
    return when (uiModel.uiStyle) {
        SanitizedHtmlUiStyle.PARAGRAPH -> ArticleTextStyles.SANITIZED_HTML_DEFAULT.style
        SanitizedHtmlUiStyle.PARAGRAPH_BRIEFS -> ArticleTextStyles.SANITIZED_HTML_SUBHEAD.style
        SanitizedHtmlUiStyle.OPINIONS -> ArticleTextStyles.SANITIZED_HTML_OPINIONS.style
        SanitizedHtmlUiStyle.SUBHEAD -> {
            when (uiModel.subheadLevel) {
                1 -> ArticleTextStyles.SANITIZED_HTML_SUBHEAD_1.style
                2 -> ArticleTextStyles.SANITIZED_HTML_SUBHEAD_2.style
                3 -> ArticleTextStyles.SANITIZED_HTML_SUBHEAD_3.style
                4 -> ArticleTextStyles.SANITIZED_HTML_SUBHEAD_4.style
                5 -> ArticleTextStyles.SANITIZED_HTML_SUBHEAD_5.style
                6 -> ArticleTextStyles.SANITIZED_HTML_SUBHEAD_6.style
                else -> ArticleTextStyles.SANITIZED_HTML_SUBHEAD.style
            }
        }
        SanitizedHtmlUiStyle.EXTRA -> ArticleTextStyles.SANITIZED_HTML_EXTRA.style
        SanitizedHtmlUiStyle.TRAILER -> ArticleTextStyles.SANITIZED_HTML_TRAILER.style
        SanitizedHtmlUiStyle.INTRO -> ArticleTextStyles.SANITIZED_HTML_INTRO.style
        SanitizedHtmlUiStyle.LETTER -> ArticleTextStyles.SANITIZED_HTML_LETTER.style
        SanitizedHtmlUiStyle.METATEXT -> ArticleTextStyles.SANITIZED_HTML_METATEXT.style
        SanitizedHtmlUiStyle.EXPANDED_BYLINE -> ArticleTextStyles.SANITIZED_HTML_EXPANDED_BYLINE.style
        SanitizedHtmlUiStyle.BLOCKQUOTE -> ArticleTextStyles.SANITIZED_HTML_BLOCKQUOTE.style
        SanitizedHtmlUiStyle.ASK_THE_POST -> ArticleTextStyles.SANITIZED_HTML_ASK_THE_POST.style
        SanitizedHtmlUiStyle.FROM_THE_SOURCE -> ArticleTextStyles.SANITIZED_HTML_FROM_THE_SOURCE.style
        SanitizedHtmlUiStyle.SUBHEAD_BRIEFS -> ArticleTextStyles.SANITIZED_HTML_THE_7_LABEL.style
        SanitizedHtmlUiStyle.BRIEFS_EXCLUSIVE_LABEL -> ArticleTextStyles.SANITIZED_HTML_BRIEFS_EXCLUSIVE_LABEL.style
        SanitizedHtmlUiStyle.SOCIAL_EMBED,
        SanitizedHtmlUiStyle.DEFAULT -> ArticleTextStyles.SANITIZED_HTML_DEFAULT.style
    }
}

enum class SanitizedHtmlUiStyle {
    PARAGRAPH,
    PARAGRAPH_BRIEFS,
    OPINIONS,
    SUBHEAD,
    EXTRA,
    TRAILER,
    LETTER,
    INTRO,
    METATEXT,
    EXPANDED_BYLINE,
    BLOCKQUOTE,
    SUBHEAD_BRIEFS,
    BRIEFS_EXCLUSIVE_LABEL,
    ASK_THE_POST,
    FROM_THE_SOURCE,
    SOCIAL_EMBED,
    DEFAULT
}

@Preview(name = "Opinions Preview")
@Composable
private fun SanitizedHtmlViewOpinionsPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for Opinions",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.OPINIONS
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Subhead Preview")
@Composable
private fun SanitizedHtmlViewSubheadPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for Subhead",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.SUBHEAD
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Extra Preview")
@Composable
private fun SanitizedHtmlViewExtraPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for Extra",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.EXTRA
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Trailer Preview")
@Composable
private fun SanitizedHtmlViewTrailerPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for Trailer",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.TRAILER
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Intro Preview")
@Composable
private fun SanitizedHtmlViewIntroPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for Intro",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.INTRO
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Letter Preview")
@Composable
private fun SanitizedHtmlViewLetterPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for Letter",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.LETTER
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Metatext Preview")
@Composable
private fun SanitizedHtmlViewMetatextPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for Metatext",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.METATEXT
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Expanded Byline Preview")
@Composable
private fun SanitizedHtmlViewExpandedBylinePreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for ExpandedByline",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.EXPANDED_BYLINE
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Blockquote Preview")
@Composable
private fun SanitizedHtmlViewBlockquotePreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for BlockQuote",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.BLOCKQUOTE
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Ask The Post Preview")
@Composable
private fun SanitizedHtmlViewAskThePostPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for a view with an ATP chip.",
                    questionSets = null, // TODO mock some data
                    sourceAnnotations = null, // TODO mock some data
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.ASK_THE_POST
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "From The Source Preview")
@Composable
private fun SanitizedHtmlViewFromTheSourcePreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for a view with an FTS chip.",
                    questionSets = null, // TODO mock some data
                    sourceAnnotations = null, // TODO mock some data
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.FROM_THE_SOURCE
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "The 7 Label Preview")
@Composable
private fun SanitizedHtmlViewThe7LabelPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "7",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.SUBHEAD_BRIEFS
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Briefs Exclusive Label Preview")
@Composable
private fun SanitizedHtmlViewBriefsExclusiveLabelPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "EXCLUSIVE",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.BRIEFS_EXCLUSIVE_LABEL
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Social Embed")
@Composable
private fun SanitizedHtmlSocialEmbedPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for a Social Embed",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = "<blockquote class=\"twitter-tweet\"><p lang=\"en\" dir=\"ltr\">From <a href=\"https://twitter.com/LauncherWP?ref_src=twsrc%5Etfw\">@LauncherWP</a>: PlayStation reveals PS VR2, the next generation of their virtual reality headset <a href=\"https://t.co/Pv0l8CFd0o\">https://t.co/Pv0l8CFd0o</a></p>&mdash; The Washington Post (@washingtonpost) <a href=\"https://twitter.com/washingtonpost/status/1496120609750798351?ref_src=twsrc%5Etfw\">February 22, 2022</a></blockquote>\n<script async src=\"https://platform.twitter.com/widgets.js\" charset=\"utf-8\"></script>\n\n",
                    uiStyle = SanitizedHtmlUiStyle.SOCIAL_EMBED
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}

@Preview(name = "Default Preview")
@Composable
private fun SanitizedHtmlViewDefaultPreview() {
    AndroidClassicTheme {
        Surface(color = wpdsColors.secondary) {
            SanitizedHtmlView(
                uiModel = SanitizedHtmlUiModel(
                    content = "This is a preview for Default",
                    questionSets = null,
                    sourceAnnotations = null,
                    oEmbed = null,
                    uiStyle = SanitizedHtmlUiStyle.DEFAULT
                ),
                dummyArticlesInteractionHelper
            )
        }
    }
}
