package com.wpds.wptheme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.wpds.theme.wpdsColors

@Composable
fun textStyleStd() = TextStyle(
    fontFamily = WpTheme.Font.FranklinITCStd(),
    fontSize = 16.sp,
    color = wpdsColors.gray0
)