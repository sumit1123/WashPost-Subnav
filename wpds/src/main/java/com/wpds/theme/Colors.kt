package com.wpds.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.res.ResourcesCompat

/**
 * Official WPDS color keys and their corresponding hex values can be found here:
 * https://build.washingtonpost.com/foundations/color
 */
data class Colors(
    // Keep wpds colors here
    val alpha400: Color = Color.Unspecified,
    val gray0: Color = Color.Unspecified,
    val gray20: Color = Color.Unspecified,
    val gray40: Color = Color.Unspecified,
    val gray60: Color = Color.Unspecified,
    val gray80: Color = Color.Unspecified,
    val gray100: Color = Color.Unspecified,
    val gray200: Color = Color.Unspecified,
    val gray300: Color = Color.Unspecified,
    val gray400: Color = Color.Unspecified,
    val gray500: Color = Color.Unspecified,
    val gray600: Color = Color.Unspecified,
    val gray700: Color = Color.Unspecified,
    val blue80: Color = Color.Unspecified,
    val blue100: Color = Color.Unspecified,
    val blue600: Color = Color.Unspecified,
    val askPill: Color = Color(0x1A806BFF),
    val aiIcon: Color = Color(0xFF806BFF),
    val pillBorder: Color = Color(0xFFDB85FF),
    val onCta: Color = Color.Unspecified,
    val onPrimary: Color = Color.Unspecified,
    val onSecondary: Color = Color.Unspecified,
    val primary: Color = Color.Unspecified,
    val secondary: Color = Color.Unspecified,
    val faint: Color = Color.Unspecified,
    val onBackground: Color = Color.Unspecified,
    val subtle: Color = Color.Unspecified,
    val surface: Color = Color.Unspecified,
    val surfaceHighest: Color = Color.Unspecified,
    val onSurface: Color = Color.Unspecified,
    val onSurfaceSubtle: Color = Color.Unspecified,
    val accessible: Color = Color.Unspecified,
    val outline: Color = Color.Unspecified,
    // Keep feature specific colors here
    val appBarBg: Color = Color.Unspecified,
    val appBarTitle: Color = Color.Unspecified,
    val appBarIcon: Color = Color.Unspecified,
    val cta: Color = Color.Unspecified,
    val findBorder: Color = Color.Unspecified,
    val findBg: Color = Color.Unspecified,
    val findTile: Color = Color.Unspecified,
    val findPrintBox: Color = Color.Unspecified,
    val findRecipesBox: Color = Color.Unspecified,
    val findNewsprintBox: Color = Color.Unspecified,
    val findComicsBox: Color = Color.Unspecified,
    val findClimateBox: Color = Color.Unspecified,
    val findElectionsBox: Color = Color.Unspecified,
    val findHoroscopesBox: Color = Color.Unspecified,
    val findRippleBox: Color = Color.Unspecified,
    val toggleCheckedThumb: Color = Color.Unspecified,
    val toggleCheckedTrack: Color = Color.Unspecified,
    val toggleUncheckedThumb: Color = Color.Unspecified,
    val toggleUncheckedTrack: Color = Color.Unspecified,
    val wallPrimaryBg: Color = Color.Unspecified,
    val wallSecondaryBg: Color = Color.Unspecified,
    val gridCardBg: Color = Color.Unspecified,
    val gridCardShadowBg: Color = Color.Unspecified,
    val sectionRibbonNonSelectedText: Color = Color.Unspecified,
    val liveUpdateTextColor: Color = Color.Unspecified,
    val questionItemStartColor: Color = Color.Unspecified,
    val questionItemEndColor: Color = Color.Unspecified,
    val mediaPlayerBackground: Color = Color.Unspecified,
    val myPostBannerButtonBackground: Color = Color.Unspecified,
    val articleText: Color = Color.Unspecified,
    val authorNameClickable: Color = Color.Unspecified,
    val correctionBackground: Color = Color.Unspecified,
    val divider: Color = Color.Unspecified,
    val transparencyKicker: Color = Color.Unspecified,
    val blockQuoteBackground: Color = Color.Unspecified,
    val quote: Color = Color.Unspecified,
    val atpFade: Color = Color.Unspecified,

    //Static Colors
    val atpPurpleStatic: Color = Color(0xff806BFF),
    val atpPinkStatic: Color = Color(0xffd574ff),
    val theSevenDigit: Color = Color(0xff266BD9),
    val gray700Static: Color = Color(0xffFFFFFF),
    val gray600Static: Color = Color(0xfff7f7f7),
    val gray200Static: Color = Color(0xffaaaaaa),
    val gray300Static: Color = Color(0xff333333),
    val blue100Static: Color = Color(0xff166DFC),
    val purple100Static: Color = Color(0xffD138BF),
    val orange100Static: Color = Color(0xffF3750E),
    val gold500Static: Color = Color(0xffF3E4CD),
    val gray400Static: Color = Color(0xff252525),
    val gray500Static: Color = Color(0xff202020),
    val newsprintBg: Color = Color(0xff130C23),
    val newsprintButtonText: Color = Color(0xff1d418E),
    val appLogBetaStrip: Color = Color(0xFFB38F27)
)

val WPDSColors = staticCompositionLocalOf { Colors() }
val wpdsColors: Colors
    @Composable
    @ReadOnlyComposable
    get() = WPDSColors.current

fun getLightThemeColor(context: Context, colorResource: Int): Color {
    val lightContext = context.createConfigurationContext(
        Configuration().apply {
            uiMode = Configuration.UI_MODE_NIGHT_NO
        }
    )
    return Color(ResourcesCompat.getColor(lightContext.resources,colorResource, lightContext.theme))
}
