package com.wpds.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wpds.theme.wpdsColors
import com.wpds.wpds.R
import com.wpds.wptheme.WpTheme


@Composable
fun WpTheme.Button.CtaButton(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    text: String,
    backgroundColorResId: Int = R.color.theme_cta,
    textColor: Color = Color.White,
    onClick: () -> Unit,
    textSize: Int = 12,
    horizontalPadding: Int = 8,
    verticalPadding: Int = 4,
    useElevation: Boolean = true
) {
    val elevation = if (useElevation) {
        ButtonDefaults.elevation()
    } else {
        ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    }
    Button(
        modifier = modifier.wrapContentHeight(),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = elevation,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(backgroundColorResId),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(
            horizontal = horizontalPadding.dp,
            vertical = verticalPadding.dp
        )
    ) {
        WpTheme.Text.WapoText(textModifier, text, textSize, textColor, isBold = true)
    }
}

@Composable
fun WpTheme.Button.CtaOutlinedButton(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    text: String,
    backgroundColorResId: Int = R.color.theme_cta,
    textColor: Color = Color.White,
    onClick: () -> Unit,
    textSize: Int = 12,
    useElevation: Boolean = true
) {
    val elevation = if (useElevation) {
        ButtonDefaults.elevation()
    } else {
        ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    }
    OutlinedButton(
        modifier = modifier.wrapContentHeight(),
        onClick = onClick,
        border = BorderStroke(1.dp, wpdsColors.gray0.copy(alpha = .1f)),
        shape = RoundedCornerShape(16.dp),
        elevation = elevation,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(backgroundColorResId),
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        WpTheme.Text.WapoText(textModifier, text, textSize, textColor, isBold = true)
    }
}

@Composable
@Preview
fun CtaButtonPreview() {
    WpTheme.Button.CtaButton(
        modifier = Modifier,
        text = "Create free account",
        onClick = { /* Handle click */ }
    )
}
