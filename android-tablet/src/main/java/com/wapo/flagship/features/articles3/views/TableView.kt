package com.wapo.flagship.features.articles3.views

import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.TableUiModel
import com.wapo.flagship.features.articles3.parseHtmlContent
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

enum class TableUiStyle {
    DEFAULT
}

@Composable
fun TableView(
    uiModel: TableUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    if (uiModel.header.isEmpty() && uiModel.rows.isEmpty()) return

    val borderColor = wpdsColors.outline
    val headerBg = wpdsColors.surfaceHighest

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor)
    ) {
        // Header row
        if (uiModel.header.isNotEmpty()) {
            TableRow(
                cells = uiModel.header,
                cellStyle = getTableHeaderStyle(),
                backgroundColor = headerBg,
                borderColor = borderColor,
                isFirstRow = true,
                articlesInteractionHelper = articlesInteractionHelper
            )
        }

        // Data rows
        uiModel.rows.forEachIndexed { rowIndex, rowCells ->
            val isFirst = uiModel.header.isEmpty() && rowIndex == 0
            TableRow(
                cells = rowCells,
                cellStyle = getTableBodyStyle(),
                headerCellStyle = getTableColumnHeaderStyle(),
                backgroundColor = wpdsColors.surface,
                borderColor = borderColor,
                isFirstRow = isFirst,
                highlightFirstCell = uiModel.hasColumnHeaders,
                articlesInteractionHelper = articlesInteractionHelper
            )
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    cellStyle: TextStyle,
    headerCellStyle: TextStyle = cellStyle,
    backgroundColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color,
    isFirstRow: Boolean,
    highlightFirstCell: Boolean = false,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    if (!isFirstRow) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(borderColor)
        )
    }

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        cells.forEachIndexed { cellIndex, cellText ->
            val isColumnHeader = highlightFirstCell && cellIndex == 0
            val style = if (isColumnHeader) headerCellStyle else cellStyle
            val cellBg = if (isColumnHeader) wpdsColors.surfaceHighest else backgroundColor

            if (cellIndex > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(borderColor)
                )
            }
            Text(
                text = parseHtmlContent(cellText, articlesInteractionHelper),
                style = style,
                modifier = Modifier
                    .weight(1f)
                    .background(cellBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun getTableHeaderStyle(): TextStyle = TextStyle(
    color = wpdsColors.articleText,
    fontSize = 14.sp,
    fontFamily = FranklinItcStandardFontFamily,
    fontWeight = FontWeight.Bold,
    lineHeight = 20.sp
)

@Composable
private fun getTableColumnHeaderStyle(): TextStyle = TextStyle(
    color = wpdsColors.articleText,
    fontSize = 14.sp,
    fontFamily = FranklinItcStandardFontFamily,
    fontWeight = FontWeight.Medium,
    lineHeight = 20.sp
)

@Composable
private fun getTableBodyStyle(): TextStyle = TextStyle(
    color = wpdsColors.articleText,
    fontSize = 14.sp,
    fontFamily = FranklinItcStandardFontFamily,
    fontWeight = FontWeight.Normal,
    lineHeight = 20.sp
)

// region Previews

@Preview(showBackground = true)
@Composable
private fun TableViewDefaultPreview() {
    AndroidClassicTheme {
        TableView(
            uiModel = TableUiModel(
                header = listOf("Country", "Population", "Area"),
                rows = listOf(
                    listOf("United States", "331M", "9.8M km²"),
                    listOf("Canada", "38M", "10M km²"),
                    listOf("Mexico", "128M", "2M km²")
                )
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TableViewNoHeaderPreview() {
    AndroidClassicTheme {
        TableView(
            uiModel = TableUiModel(
                rows = listOf(
                    listOf("Category", "Value"),
                    listOf("Revenue", "$1.2B"),
                    listOf("Growth", "+14%")
                ),
                hasColumnHeaders = true
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TableViewWithColumnHeadersPreview() {
    AndroidClassicTheme {
        TableView(
            uiModel = TableUiModel(
                header = listOf("Candidate", "Votes", "Percentage"),
                rows = listOf(
                    listOf("Candidate A", "1,234,567", "52.3%"),
                    listOf("Candidate B", "1,123,456", "47.7%")
                ),
                hasColumnHeaders = true
            ),
            dummyArticlesInteractionHelper
        )
    }
}

// endregion

