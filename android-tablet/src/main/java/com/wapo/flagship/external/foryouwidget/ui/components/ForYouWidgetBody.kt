package com.wapo.flagship.external.foryouwidget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import com.wapo.flagship.external.foryouwidget.data.ForYouWidgetItem
import com.washingtonpost.android.R

@Composable
fun ForYouWidgetBody(articles: List<ForYouWidgetItem>) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        LazyColumn() {
            itemsIndexed(articles) { index, item ->
                ForYouArticleContent(
                    item, index
                )
            }
        }
        Arrows()
    }
}

@Composable
fun Arrows() {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        // Up arrow positioned at top-end
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(top = 2.dp, end = 4.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Image(
                provider = ImageProvider(com.wapo.view.R.drawable.icon_uparrow),
                contentDescription = "Scroll up",
                modifier = GlanceModifier.size(24.dp),
            )
        }

        // Down arrow positioned at bottom-end
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(bottom = 10.dp, end = 4.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Image(
                provider = ImageProvider(com.wapo.view.R.drawable.icon_downarrow),
                contentDescription = "Scroll down",
                modifier = GlanceModifier.size(24.dp),
            )
        }
    }
}