package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.articles3.models.ui.TaglineUiModel
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.GeorgiaFontFamily
import com.wpds.theme.wpdsColors

enum class TaglineUiStyle {
    DEFAULT
}

@Composable
fun TaglineView(uiModel: TaglineUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.75f)
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.twp_logo),
                contentDescription = "The Washington Post",
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = uiModel.tagline,
            style = getTaglineStyle(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun getTaglineStyle(): TextStyle = TextStyle(
    color = wpdsColors.articleText,
    fontSize = 15.sp,
    fontFamily = GeorgiaFontFamily,
    fontStyle = FontStyle.Italic,
    lineHeight = 20.sp
)

// region Previews

@Preview(showBackground = true)
@Composable
private fun TaglineViewPreview() {
    AndroidClassicTheme {
        TaglineView(
            uiModel = TaglineUiModel()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TaglineViewCustomTextPreview() {
    AndroidClassicTheme {
        TaglineView(
            uiModel = TaglineUiModel()
        )
    }
}

// endregion

