package com.wpds.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.wpds.theme.PostiniFontFamily
import com.wpds.theme.wpdsColors
import com.wpds.wptheme.FranklinITCStd
import com.wpds.wptheme.WpTheme
import com.wpds.wptheme.textStyleStd

@Composable
fun WpTheme.Text.HeadlineBold200(
    modifier: Modifier = Modifier,
    text: String,
    textSize: Int = 32,
    textAlign: TextAlign? = TextAlign.Center,
    textColor: Color = wpdsColors.gray0
) {
    Text(
        modifier = modifier,
        text = text,
        textAlign = textAlign,
        style = TextStyle(
            fontFamily = PostiniFontFamily,
            fontSize = textSize.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    )
}

@Composable
fun WpTheme.Text.MetaLight112(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        modifier = modifier,
        text = text,
        textAlign = textAlign,
        style = TextStyle(
            fontFamily = WpTheme.Font.FranklinITCStd(),
            fontSize = 18.sp,
            lineHeight = 22.5.sp,
            fontWeight = FontWeight.Light,
            color = Color.White
        )
    )
}

@Composable
fun WpTheme.Text.WapoText(
    modifier: Modifier = Modifier,
    text: String,
    textSize: Int,
    textColor: Color = wpdsColors.gray0,
    isBold: Boolean = false
) {
    val bold = if (isBold) FontWeight.Bold else FontWeight.Light
    Text(
        modifier = modifier,
        text = text,
        style = TextStyle(
            fontFamily = WpTheme.Font.FranklinITCStd(),
            fontSize = textSize.sp,
            fontWeight = bold,
            color = textColor
        )
    )
}


@Composable
fun WpTheme.Text.AnnotatedHeadline(
    modifier: Modifier = Modifier,
    text: String
) {
    val annotatedString = AnnotatedString.fromHtml(text)

    Text(
        modifier = modifier,
        text = annotatedString,
        style = textStyleStd()
    )
}

@Composable
fun WpTheme.Text.Subtitle(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier,
        text = text,
        style = textStyleStd()
    )
}

@Composable
@Preview
fun TextPreview() {
    Column {
        WpTheme.Text.HeadlineBold200(text = "This is a headline")
        WpTheme.Text.MetaLight112(text = "This is a meta text")
        WpTheme.Text.WapoText(text = "This is a wapo text", textSize = 16, isBold = true)
        WpTheme.Text.AnnotatedHeadline(text = "<b>This is an annotated headline</b>")
        WpTheme.Text.Subtitle(text = "This is a subtitle")
    }
}
