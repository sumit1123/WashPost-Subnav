/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wpds.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

val LightColors = Colors(
    alpha400 = Color(0x1a000000),
    gray0 = Color(0xff000000),
    gray20 = Color(0xff111111),
    gray40 = Color(0xff2a2a2a),
    gray60 = Color(0xff494949),
    gray80 = Color(0xff595959),
    gray100 = Color(0xff737373),
    gray200 = Color(0xffaaaaaa),
    gray300 = Color(0xffd4d4d4),
    gray400 = Color(0xffe9e9e9),
    gray500 = Color(0xfff0f0f0),
    gray600 = Color(0xfff7f7f7),
    gray700 = Color(0xffffffff),

    blue80 = Color(0xff1366b3),
    blue100 = Color(0xff166dfc),
    blue600 = Color(0xffE7F0FE),

    onCta = Color(0xffffffff),
    onPrimary = Color(0xffffffff),
    onSecondary = Color(0xff111111),
    primary = Color(0xff111111),
    secondary = Color(0xffffffff),
    faint = Color(0xffe9e9e9),
    onBackground = Color(0xff111111),
    subtle = Color(0xffD4D4D4),

    surface = Color(0xffffffff),
    surfaceHighest = Color(0xffffffff),
    onSurface = Color(0xff111111),
    onSurfaceSubtle = Color(0xff595959),

    accessible = Color(0xff595959),
    outline = Color(0x1A000000),

    appBarBg = Color(0xffFFFFFF),
    appBarTitle = Color(0xff2A2A2A),
    appBarIcon = Color(0xff111111),
    cta = Color(0xff166dfc),
    findBorder = Color(0xffe9e9e9),
    findTile = Color(0xfff5f5f5),
    findBg = Color(0xffffffff),
    findPrintBox = Color(0xff2a2a2a),
    findComicsBox = Color(0xffd5edca),
    findRecipesBox = Color(0xffF7CA6F),
    findNewsprintBox = Color(0xFF130C23),
    findClimateBox = Color(0xffc7eaef),
    findElectionsBox = Color(0xFF1F3F60),
    toggleCheckedThumb = Color(0xffffffff),
    toggleCheckedTrack = Color(0xff166dfc),
    toggleUncheckedThumb = Color(0xffffffff),
    toggleUncheckedTrack = Color(0xff2c2c2c),
    wallPrimaryBg = Color(0xffffffff),
    wallSecondaryBg = Color(0xfff7f7f7),
    gridCardBg = Color(0xffffffff),
    gridCardShadowBg = Color(0x1f2a301a),
    sectionRibbonNonSelectedText = Color(0xff595959),
    liveUpdateTextColor = Color(0xffCF000E),
    questionItemStartColor = Color(0x4cb9b1ea),
    questionItemEndColor = Color(0x4ce7c0f7),
    findHoroscopesBox = Color(0xffb3e3fa),
    findRippleBox = Color(0xFFF0F0F0),
    mediaPlayerBackground = Color(0xFFFFFFFF),
    myPostBannerButtonBackground = Color(0xFFD5D5D5),
    articleText = Color(0xFF111111),
    authorNameClickable = Color(0xFF1955A5),
    correctionBackground = Color(0xFFF7F7F7),
    divider = Color(0xFFD5D5D5),
    transparencyKicker = Color(0xFF666666),
    atpFade = Color(0x1A806BFF),
    blockQuoteBackground = Color(0xFFD4D4D4),
    quote = Color(0xFF000000),
)


/**
 * IMPORTANT: Use this ONLY if you need to use a specific light mode color in both light and dark modes*
 */
val wpdsColorsLight: Colors
    @Composable
    @ReadOnlyComposable
    get() = LightColors
