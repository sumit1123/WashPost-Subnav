package com.wapo.flagship.features.find.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.find.events.FindClickEvent
import com.washingtonpost.android.R
import com.wpds.theme.wpdsColors

@Preview
@Composable
fun SearchBarButton(onClick: (FindClickEvent) -> Unit = {}) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(color = wpdsColors.faint, shape = RoundedCornerShape(8.dp))
                .padding(10.dp)
                .clickable {
                    onClick(FindClickEvent.SearchBarClick)
                },
    ) {
        Image(
            painter = painterResource(R.drawable.ask_post_search),
            contentDescription = null,
            modifier =
                Modifier.wrapContentSize().padding(end = 5.dp).align(
                    alignment = Alignment.CenterVertically,
                ),
        )
        Text(
            text = stringResource(R.string.search_hint),
            modifier = Modifier.weight(1f).align(alignment = Alignment.CenterVertically),
            style = TextStyle(color = wpdsColors.primary),
        )
    }
}

@Preview
@Composable
fun ColumnScope.CollapsingTopBar(
    progress: Float = 1.0f,
    onClick: (FindClickEvent) -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .wrapContentHeight()
                .widthIn(max = 700.dp)
                .background(color = Color.Transparent)
                .padding(16.dp)
                .align(Alignment.CenterHorizontally),
    ) {
        SearchBarButton(onClick)
    }
}

@Preview
@Composable
fun Header(
    text: String = "Find",
    fontSize: TextUnit = 32.sp,
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontFamily = FontFamily(Font(com.wapo.view.R.font.postoni_display_mag_ultra, FontWeight.Normal)),
    )
}
