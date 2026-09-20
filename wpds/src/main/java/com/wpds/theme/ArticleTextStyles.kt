package com.wpds.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wpds.utils.isTabletLandscapeUi
import com.wpds.utils.isTabletUi

/**
 * Applies the user's text-size preference (from Settings) to any [TextStyle].
 * Mirrors the legacy [com.wapo.text.GlobalFontAdjustmentSpan] formula: `size * 1.6f` sp added.
 */
private val TextStyle.withFontAdjustment: TextStyle
    @Composable get() {
        val adjustment = ArticleFontScale.adjustment.value
        return if (adjustment == 0f) this
        else this.copy(fontSize = (this.fontSize.value + adjustment * 1.6f).sp)
    }

enum class ArticleTextStyles {
    // TODO have AI rearrange in alphabetical order when done with all of them
    AUDIO_DURATION,
    AUDIO_STATUS,
    AUDIO_CAPTION,
    CORRECTION_TITLE,
    CORRECTION_BODY,
    DECK,
    IMAGE_CAPTION,
    TITLE_STYLE,
    TITLE_H1,
    TITLE_H2,
    TITLE_H3,
    TITLE_H4,
    TITLE_H5,
    TITLE_H6,
    TITLE_LIVE_REPORTER_INSIGHT,
    TITLE_DEFAULT,
    BYLINE_LIVE_UPDATE,
    BYLINE_DEFAULT,
    DATELINE,
    DATELINE_LIVE_UPDATE,
    SUBTEXT_LIVE_REPORTER_INSIGHT,
    SUBTEXT_DEFAULT,
    SANITIZED_HTML_OPINIONS,
    SANITIZED_HTML_SUBHEAD,
    SANITIZED_HTML_SUBHEAD_1,
    SANITIZED_HTML_SUBHEAD_2,
    SANITIZED_HTML_SUBHEAD_3,
    SANITIZED_HTML_SUBHEAD_4,
    SANITIZED_HTML_SUBHEAD_5,
    SANITIZED_HTML_SUBHEAD_6,
    SANITIZED_HTML_EXTRA,
    SANITIZED_HTML_TRAILER,
    SANITIZED_HTML_INTRO,
    SANITIZED_HTML_LETTER,
    SANITIZED_HTML_METATEXT,
    SANITIZED_HTML_EXPANDED_BYLINE,
    SANITIZED_HTML_BLOCKQUOTE,
    SANITIZED_HTML_ASK_THE_POST,
    SANITIZED_HTML_FROM_THE_SOURCE,
    SANITIZED_HTML_THE_7_LABEL,
    SANITIZED_HTML_BRIEFS_EXCLUSIVE_LABEL,
    SANITIZED_HTML_DEFAULT,
    KICKER_DEFAULT,
    KICKER_BRIEFS,
    KICKER_PILL_LIVE,
    KICKER_PILL_EXCLUSIVE,
    KICKER_DISPLAY_TRANSPARENCY,
    LIVE_OUTCOME_HEADLINE,
    LIVE_OUTCOME_SUBHEADLINE,
    PIN_TEXT,
    PULL_QUOTE_TEXT,
    PULL_QUOTE_CAPTION,
    BLOCK_QUOTE_TEXT,
    BLOCK_QUOTE_CAPTION,
    QUOTE_TEXT,
    QUOTE_CAPTION,

    ;

    val style: TextStyle
        @Composable
        get() {
            val isTablet = isTabletUi()
            val isTabletLandscape = isTabletLandscapeUi()
            return when (this) {
                AUDIO_DURATION -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 14.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Light,
                        lineHeight = 20.sp
                    )
                }
                AUDIO_STATUS -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 14.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                }
                AUDIO_CAPTION -> {
                    TextStyle(
                        color = wpdsColors.gray100,
                        fontSize = 14.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp
                    )
                }
                CORRECTION_TITLE -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.06.sp
                    )
                }
                CORRECTION_BODY -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 16.sp else 14.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.05.sp,
                        lineHeight = if (isTablet) 22.sp else 20.sp
                    )
                }
                DECK -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 20.sp else 18.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Light,
                        lineHeight = if (isTablet) 25.sp else 22.5.sp
                    )
                }
                IMAGE_CAPTION -> {
                    TextStyle(
                        color = wpdsColors.gray80,
                        fontSize = 15.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Light,
                        lineHeight = 20.sp
                    )
                }
                TITLE_STYLE -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 48.sp else 28.sp,
                        fontFamily = PostiniFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = if (isTablet) 39.sp else 28.sp
                    )
                }
                TITLE_H1 -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = when {
                            isTabletLandscape -> 48.sp
                            isTablet -> 40.sp
                            else -> 28.sp
                        },
                        fontFamily = PostiniFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = when {
                            isTabletLandscape -> 53.sp
                            isTablet -> 44.sp
                            else -> 31.sp
                        },
                    )
                }
                TITLE_H2 -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 36.sp else 26.sp,
                        fontFamily = PostiniFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = if (isTablet) 36.sp else 28.6.sp
                    )
                }
                TITLE_H3 -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 36.sp else 26.sp,
                        fontFamily = PostiniFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = if (isTablet) 36.sp else 28.6.sp
                    )
                }
                TITLE_H4 -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 24.sp else 20.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = if (isTablet) 30.sp else 25.sp
                    )
                }
                TITLE_H5 -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 18.sp else 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = if (isTablet) 22.5.sp else 20.sp
                    )
                }
                TITLE_H6 -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 18.sp else 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Light,
                        lineHeight = if (isTablet) 22.5.sp else 20.sp
                    )
                }
                TITLE_LIVE_REPORTER_INSIGHT -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 14.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TITLE_DEFAULT -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 35.sp else 28.sp,
                        fontFamily = PostiniFontFamily,
                        fontWeight = FontWeight.Bold,
                    )
                }
                BYLINE_LIVE_UPDATE -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Light,
                        lineHeight = 20.sp
                    )
                }
                BYLINE_DEFAULT -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 17.sp else 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Light,
                        lineHeight = 20.sp
                    )
                }
                DATELINE -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 16.sp else 14.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Normal,
                    )
                }
                DATELINE_LIVE_UPDATE -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 16.sp else 14.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                    )
                }
                SUBTEXT_LIVE_REPORTER_INSIGHT -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 14.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Light
                    )
                }
                SUBTEXT_DEFAULT -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 17.sp else 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Light
                    )
                }
                SANITIZED_HTML_OPINIONS -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 18.sp,
                        fontFamily = GeorgiaFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 25.sp
                    )
                }
                SANITIZED_HTML_SUBHEAD -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 18.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 25.sp
                    )
                }
                SANITIZED_HTML_SUBHEAD_1,
                SANITIZED_HTML_SUBHEAD_2,
                SANITIZED_HTML_SUBHEAD_3 -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 24.sp else 21.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 25.sp
                    )
                }
                SANITIZED_HTML_SUBHEAD_4 -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 21.sp else 18.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 25.sp
                    )
                }
                SANITIZED_HTML_SUBHEAD_5,
                SANITIZED_HTML_SUBHEAD_6 -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 18.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 25.sp
                    )
                }
                SANITIZED_HTML_EXTRA,
                SANITIZED_HTML_TRAILER -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 18.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 25.sp
                    )
                }
                SANITIZED_HTML_INTRO -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 18.sp,
                        fontFamily = GeorgiaFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 25.sp
                    )
                }
                SANITIZED_HTML_LETTER -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 18.sp,
                        fontFamily = GeorgiaFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 25.sp
                    )
                }
                SANITIZED_HTML_METATEXT -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 18.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 25.sp
                    )
                }
                SANITIZED_HTML_EXPANDED_BYLINE -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 14.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Light,
                        lineHeight = 20.sp
                    )
                }
                SANITIZED_HTML_BLOCKQUOTE -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 20.sp,
                        fontFamily = PostiniFontFamily,
                        fontWeight = FontWeight.Light,
                        lineHeight = 28.sp
                    )
                }
                KICKER_PILL_EXCLUSIVE,
                SANITIZED_HTML_BRIEFS_EXCLUSIVE_LABEL -> {
                    TextStyle(
                        color = wpdsColors.secondary,
                        fontSize = 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                }
                SANITIZED_HTML_THE_7_LABEL -> {
                    TextStyle(
                        color = wpdsColors.secondary,
                        fontSize = 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )
                }
                SANITIZED_HTML_ASK_THE_POST,
                SANITIZED_HTML_FROM_THE_SOURCE,
                SANITIZED_HTML_DEFAULT -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = if (isTablet) 20.sp else 18.sp,
                        fontFamily = GeorgiaFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = if (isTablet) 32.sp else 28.8.sp,
                    )
                }
                KICKER_DEFAULT -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.06.sp
                    )
                }
                KICKER_BRIEFS -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.06.sp
                    )
                }
                KICKER_PILL_LIVE -> {
                    TextStyle(
                        color = wpdsColors.gray700Static,
                        fontSize = 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.06.sp
                    )
                }
                KICKER_DISPLAY_TRANSPARENCY -> {
                    TextStyle(
                        color = wpdsColors.transparencyKicker,
                        fontSize = 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.06.sp
                    )
                }
                LIVE_OUTCOME_HEADLINE -> {
                    TextStyle(
                        color = wpdsColors.articleText,
                        fontSize = 20.sp,
                        fontFamily = PostiniFontFamily,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp
                    )
                }
                LIVE_OUTCOME_SUBHEADLINE -> {
                    TextStyle(
                        color = wpdsColors.gray80,
                        fontSize = 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp
                    )
                }
                PIN_TEXT -> {
                    TextStyle(
                        color = wpdsColors.primary,
                        fontSize = 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                PULL_QUOTE_TEXT -> {
                    TextStyle(
                        color = wpdsColors.onBackground,
                        fontSize = 28.sp,
                        fontFamily = PostiniFontFamily,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 28.sp
                    )
                }
                PULL_QUOTE_CAPTION -> {
                    TextStyle(
                        color = wpdsColors.gray80,
                        fontSize = 15.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Normal
                    )
                }
                BLOCK_QUOTE_TEXT -> {
                    TextStyle(
                        color = wpdsColors.onBackground,
                        fontSize = if (isTablet) 18.sp else 16.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Light,
                        lineHeight = if (isTablet) 28.sp else 24.sp,
                    )
                }
                BLOCK_QUOTE_CAPTION -> {
                    TextStyle(
                        color = wpdsColors.gray80,
                        fontSize = 15.sp,
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Normal
                    )
                }
                QUOTE_TEXT, QUOTE_CAPTION -> {
                    TextStyle(
                        color = wpdsColors.quote,
                        fontSize = if (isTablet) 18.sp else 16.sp,
                        fontFamily = GeorgiaFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        lineHeight = if (isTablet) 28.sp else 24.sp,
                    )
                }
            }.withFontAdjustment
        }
}
