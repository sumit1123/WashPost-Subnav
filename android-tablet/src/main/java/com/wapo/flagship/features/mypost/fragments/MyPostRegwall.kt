package com.wapo.flagship.features.mypost.fragments

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.util.rememberHtmlAnnotatedString
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.PostiniFontFamily
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

@Composable
fun MyPostRegwall(
    onSignInClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wpdsColors.wallPrimaryBg),
    ) {
        val scrollController = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 500.dp)
                .align(Alignment.Center)
                .verticalScroll(scrollController)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Sign in to access more",
                style = TextStyle(
                    fontFamily = PostiniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = wpdsColors.primary,
                )
            )
            Spacer(Modifier.height(20.dp))
            Image(
                painter = painterResource(R.drawable.saved_stories),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .widthIn(260.dp),
            )
            Spacer(Modifier.height(20.dp))
            HighlightItems(
                listOf(
                    HighlightItemData(
                        R.drawable.ic_save_articles,
                        "<b>Save articles</b> to read later"
                    ),
                    HighlightItemData(
                        com.wpds.wpds.R.drawable.newspaper,
                        "<b>Follow topics</b> that interest you"
                    ),
                    HighlightItemData(
                        com.wpds.wpds.R.drawable.time,
                        "<b>Revisit stories</b> in your history"
                    )
                )
            )
            Spacer(Modifier.height(20.dp))
            val signInContentDescription = stringResource(
                R.string.sign_in_or_create_account_button
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = signInContentDescription
                    },
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(12.dp),
                onClick = onSignInClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = wpdsColors.blue100,
                    contentColor = wpdsColors.onPrimary,
                ),
            ) {
                Text(
                    stringResource(R.string.sign_in_or_create_account),
                    style = TextStyle(
                        fontFamily = FranklinItcStandardFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = wpdsColors.gray700Static,
                    )
                )
            }
        }
    }
}

@Composable
private fun HighlightItems(items: List<HighlightItemData>) {
    var itemMinWidths by remember(items.map { it.text }) { mutableStateOf(List(items.size) { -1 }) }
    val maxWidth by remember {
        derivedStateOf { if (itemMinWidths.all { it > 0 }) itemMinWidths.maxOrNull() else null }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.forEachIndexed { index, item ->
            HighlightItem(
                icon = item.icon,
                text = item.text,
                onWidthMeasured = { width ->
                    if (itemMinWidths[index] != width) {
                        val updated = itemMinWidths.toMutableList()
                        updated[index] = width
                        itemMinWidths = updated
                    }
                },
                forcedWidth = maxWidth,
            )
            if (index != items.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HighlightItem(
    @DrawableRes icon: Int,
    text: String,
    forcedWidth: Int? = null,
    onWidthMeasured: ((Int) -> Unit)? = null,
) {
    val annotatedText = rememberHtmlAnnotatedString(text)
    Row(
        modifier = Modifier
            .then(
                if (forcedWidth != null) Modifier.width(with(LocalDensity.current) { forcedWidth.toDp() })
                else Modifier
            )
            .onGloballyPositioned { coordinates ->
                onWidthMeasured?.invoke(coordinates.size.width)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            modifier = Modifier.width(20.dp),
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(wpdsColors.blue100),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            annotatedText,
            style = TextStyle(
                fontFamily = FranklinItcStandardFontFamily,
                fontSize = 16.sp,
                color = wpdsColors.primary,
            )
        )
    }
}

private data class HighlightItemData(
    @DrawableRes val icon: Int,
    val text: String,
)

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun MyPostRegWallPreview() {
    AndroidClassicTheme {
        MyPostRegwall { }
    }
}
