/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wpds.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val DarkColors = Colors (
    alpha400 = Color(0x1affffff),
    gray0 = Color(0xffffffff),
    gray20 = Color(0xfff2f2f2),
    gray40 = Color(0xffdedede),
    gray60 = Color(0xffb1b1b1),
    gray80 = Color(0xffa1a1a1),
    gray100 = Color(0xff7f7f7f),
    gray200 = Color(0xff4f4f4f),
    gray300 = Color(0xff333333),
    gray400 = Color(0xff252525),
    gray500 = Color(0xff202020),
    gray600 = Color(0xff1a1a1a),
    gray700 = Color(0xff141414),

    blue80 = Color(0xff5892c9),
    blue100 = Color(0xff166dfc),
    blue600 = Color(0xffE7F0FE),

    onCta = Color(0xffffffff),
    onPrimary = Color(0xff020202),
    onSecondary = Color(0xffffffff),
    primary = Color(0xfff2f2f2),
    secondary = Color(0xff020202),
    faint = Color(0xff252525),
    onBackground = Color(0xffffffff),
    subtle = Color(0xff333333),

    surface = Color(0xff202020),
    surfaceHighest = Color(0xff333333),
    onSurface = Color(0xffffffff),
    onSurfaceSubtle = Color(0xffb1b1b1),

    accessible = Color(0xffA1A1A1),
    outline = Color(0x1AFFFFFF),

    appBarBg = Color(0xFF141414),
    appBarTitle = Color(0xffF2F2F2),
    appBarIcon = Color(0xffF2F2F2),
    cta = Color(0xff166cf9),
    findBorder = Color(0xff2b2b2b),
    findBg = Color(0xff0c0c0c),
    findTile = Color(0xff1a1a1a),
    findPrintBox = Color(0xff2a2a2a),
    findComicsBox = Color(0xffd5edca),
    findRecipesBox = Color(0xffF7CA6F),
    findNewsprintBox = Color(0xFF130C23),
    findClimateBox = Color(0xffc7eaef),
    findElectionsBox = Color(0xFF0A3258),
    toggleCheckedThumb = Color(0xff166dfc),
    toggleCheckedTrack = Color(0xff166dfc),
    toggleUncheckedThumb = Color(0xffececec),
    toggleUncheckedTrack = Color(0xffececec),
    wallPrimaryBg = Color(0xff2a2a2a),
    wallSecondaryBg = Color(0xff252525),
    gridCardBg = Color(0xff141414),
    gridCardShadowBg = Color(0xff000000),
    sectionRibbonNonSelectedText = Color(0xffaaaaaa),
    liveUpdateTextColor = Color(0xffF27B81),
    questionItemStartColor = Color(0xb3836dff),
    questionItemEndColor = Color(0xb3d574ff),
    findHoroscopesBox = Color(0xffb3e3fa),
    findRippleBox = Color(0xFFF0F0F0),
    mediaPlayerBackground = Color(0xFF333333),
    myPostBannerButtonBackground = Color(0xFF777777),
    articleText = Color(0xFFFFFFFF),
    authorNameClickable = Color(0xFF659FFF),
    correctionBackground = Color(0xFF111111),
    divider = Color(0xFF484B4E),
    transparencyKicker = Color(0xFFDDDDDD),
    atpFade = Color(0x00000000),
    blockQuoteBackground = Color(0xFF333333),
    quote = Color(0xFFDDDDDD),
)

/**
 * IMPORTANT: Use this ONLY if you need to use a specific dark mode color in both dark and light modes 
 */
val wpdsColorsDark: Colors
    @Composable
    @ReadOnlyComposable
    get() = DarkColors
