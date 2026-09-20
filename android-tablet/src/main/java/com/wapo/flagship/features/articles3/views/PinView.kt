package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.articles3.models.ui.PinUiModel
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles

enum class PinUiStyle {
    DEFAULT
}

@Composable
fun PinView(uiModel: PinUiModel) {
    if (uiModel.content.isNullOrEmpty()) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.pin),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(width = 12.dp, height = 20.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = uiModel.content,
            style = ArticleTextStyles.PIN_TEXT.style
        )
    }
}

// region Previews

@Preview(showBackground = true)
@Composable
private fun PinViewPreview() {
    AndroidClassicTheme {
        PinView(
            uiModel = PinUiModel(
                content = "Pinned post"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PinViewLongTextPreview() {
    AndroidClassicTheme {
        PinView(
            uiModel = PinUiModel(
                content = "Pinned: Read our full coverage of the election results"
            )
        )
    }
}

// endregion

