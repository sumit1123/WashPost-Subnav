package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.DisclaimerInfo
import com.wapo.flagship.features.articles2.models.DisclaimerSection
import com.wapo.flagship.features.articles3.parseHtmlContent
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

@Composable
fun DisclaimerInfoBottomSheetView(
    disclaimerInfo: DisclaimerInfo,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    Column(
        modifier = Modifier
            .background(wpdsColors.surface)
            .wrapContentHeight()
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
            disclaimerInfo.title?.let {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = disclaimerInfo.title,
                    fontSize = 18.sp,
                    color = wpdsColors.articleText,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FranklinItcStandardFontFamily
                )
            }

            disclaimerInfo.sections?.let {
                LazyColumn(
                    modifier = Modifier.padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(it) { index, section ->
                        DisclaimerSectionView(section, articlesInteractionHelper)
                    }
                }
            }
        }
    }
}

@Composable
fun DisclaimerSectionView(
    section: DisclaimerSection,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    Column(
        modifier = Modifier.wrapContentHeight()
            .fillMaxWidth()
    ) {
        section.heading?.let {
            Text(
                modifier = Modifier.padding(bottom = 4.dp),
                fontWeight = FontWeight.Bold,
                text = section.heading,
                fontSize = 16.sp,
                color = wpdsColors.articleText,
                fontFamily = FranklinItcStandardFontFamily
            )
        }
        section.content?.let {
            Text(
                text = parseHtmlContent(
                    content = it,
                    articlesInteractionHelper = articlesInteractionHelper
                ),
                fontSize = 16.sp,
                color = wpdsColors.articleText,
                fontFamily = FranklinItcStandardFontFamily
            )
        }
    }
}


@Composable
@Preview
fun DisclaimerInfoBottomSheetViewPreview() {
    val disclaimerInfo = DisclaimerInfo(
        title = "About Ripple",
        sections = listOf(
            DisclaimerSection(
                heading = "What is Ripple?",
                content = "Ripple is a digital payment protocol that enables fast, low-cost international money transfers."
            ),
            DisclaimerSection(
                heading = "Disclaimer",
                content = "The information provided here is for educational purposes only and should not be considered financial"
            )
        )
    )
    DisclaimerInfoBottomSheetView(
        disclaimerInfo,
        articlesInteractionHelper = object : ArticlesInteractionHelper {
            override fun onEventFired(event: ArticleInteractionEvent) {
                // Handle events for preview
            }
        })
}