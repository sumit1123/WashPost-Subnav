package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.articles3.models.ui.DividerUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

@Composable
fun DividerView(uiModel: DividerUiModel) {
    val color = when (uiModel.uiStyle) {
        DividerUiStyle.DEFAULT -> wpdsColors.divider
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

enum class DividerUiStyle {
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun DividerViewPreview() {
    AndroidClassicTheme {
        DividerView(uiModel = DividerUiModel())
    }
}


