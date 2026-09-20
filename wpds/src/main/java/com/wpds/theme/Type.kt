package com.wpds.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wpds.wpds.R

val FranklinItcStandardFontFamily = FontFamily(
    Font(R.font.franklinitcstd_light, FontWeight.Normal),
    Font(R.font.franklinitcstd_bold, FontWeight.Bold),
    Font(R.font.franklinitcstd_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.franklinitcstd_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

val PostiniFontFamily = FontFamily(
    Font(R.font.postoniwide_regular, FontWeight.Normal),
    Font(R.font.postoniwide_bold, FontWeight.Bold),
    Font(R.font.postoniwide_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.postoni_display_mag_ultra, FontWeight.ExtraBold)
)

val GeorgiaFontFamily = FontFamily(
    Font(R.font.georgia_regular, FontWeight.Normal),
    Font(R.font.georgia_bold, FontWeight.Bold),
    Font(R.font.georgia_italic, FontWeight.Normal, FontStyle.Italic)
)

val Typography = Typography(
    h1 = TextStyle(
        fontFamily = FranklinItcStandardFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    ),
    h2 = TextStyle(
        fontFamily = FranklinItcStandardFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    ),
    body1 = TextStyle(
        fontFamily = FranklinItcStandardFontFamily,
        fontSize = 12.sp
    ),
    subtitle1 = TextStyle(
        fontFamily = PostiniFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)
