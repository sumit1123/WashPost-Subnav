package com.wapo.flagship.features.articles3.views

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.ExpandCollapseUiModel
import com.wapo.flagship.features.articles3.models.ui.GalleryUiModel
import com.wapo.flagship.features.articles3.models.ui.ImageUiModel
import com.wapo.flagship.features.articles3.models.ui.WidthFactor
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

@Composable
fun GalleryView(
    uiModel: GalleryUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper
) {
    var isExpanded by remember { mutableStateOf(uiModel.expandCollapseUiModel?.isExpanded) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = wpdsColors.secondary
        )
    ) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize()
        ) {

            val itemsToShow = if (uiModel.expandCollapseUiModel == null || isExpanded == true) {
                uiModel.images
            } else {
                uiModel.images.take(uiModel.expandCollapseUiModel.minItemsCount ?: 3)
            }

            itemsToShow.forEach { image ->
                ImageView(image, articlesInteractionHelper)
                Spacer(modifier = Modifier.height(8.dp))
            }

            uiModel.expandCollapseUiModel?.let {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ExpandCollapseView(it.copy(isExpanded = isExpanded == true), articlesInteractionHelper) {
                        isExpanded = !isExpanded!!
                    }
                }
            }
        }
    }
}

enum class GalleryUiStyle {
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun GalleryViewCollapsedPreview() {
    AndroidClassicTheme {
        GalleryView(
            uiModel = GalleryUiModel(
                images = listOf(
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg",
                        darkModeImageUrl = null,
                        caption = "First image in the gallery. (Photo by John Smith/The Washington Post)",
                        imageWidth = 1200,
                        imageHeight = 800,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    ),
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/OIBALHVNOAI6XAWBRFVMVFK3XE.jpg",
                        darkModeImageUrl = null,
                        caption = "Second image in the gallery. (Photo by Jane Doe/The Washington Post)",
                        imageWidth = 1600,
                        imageHeight = 900,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    ),
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/SAMPLE3.jpg",
                        darkModeImageUrl = null,
                        caption = "Third image in the gallery.",
                        imageWidth = 1400,
                        imageHeight = 1050,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    ),
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/SAMPLE4.jpg",
                        darkModeImageUrl = null,
                        caption = "Fourth image - only visible when expanded.",
                        imageWidth = 1200,
                        imageHeight = 800,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    ),
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/SAMPLE5.jpg",
                        darkModeImageUrl = null,
                        caption = "Fifth image - only visible when expanded.",
                        imageWidth = 1600,
                        imageHeight = 900,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    )
                ),
                expandCollapseUiModel = ExpandCollapseUiModel(
                    isExpanded = false,
                    minItemsCount = 2,
                    expandedLabel = "Show fewer photos",
                    truncatedLabel = "Show more photos",
                    group = "123"
                )
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryViewExpandedPreview() {
    AndroidClassicTheme {
        GalleryView(
            uiModel = GalleryUiModel(
                images = listOf(
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg",
                        darkModeImageUrl = null,
                        caption = "First image in the gallery. (Photo by John Smith/The Washington Post)",
                        imageWidth = 1200,
                        imageHeight = 800,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    ),
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/OIBALHVNOAI6XAWBRFVMVFK3XE.jpg",
                        darkModeImageUrl = null,
                        caption = "Second image in the gallery. (Photo by Jane Doe/The Washington Post)",
                        imageWidth = 1600,
                        imageHeight = 900,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    ),
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V2GTS4SWZYI6ZKAIGGL2EKYZ7I.jpg",
                        darkModeImageUrl = null,
                        caption = "Third image in the gallery.",
                        imageWidth = 1400,
                        imageHeight = 1050,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    ),
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/T5POWRCWZYI6ZKAIGGL2EKYZ7I.jpg",
                        darkModeImageUrl = null,
                        caption = "Fourth image - now visible when expanded.",
                        imageWidth = 1200,
                        imageHeight = 800,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    )
                ),
                expandCollapseUiModel = ExpandCollapseUiModel(
                    isExpanded = true,
                    minItemsCount = 3,
                    expandedLabel = "Show fewer photos",
                    truncatedLabel = "Show more photos",
                    group = "123"
                )
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryViewNoExpandCollapsePreview() {
    AndroidClassicTheme {
        GalleryView(
            uiModel = GalleryUiModel(
                images = listOf(
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg",
                        darkModeImageUrl = null,
                        caption = "First image in a simple gallery without expand/collapse.",
                        imageWidth = 1200,
                        imageHeight = 800,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    ),
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/OIBALHVNOAI6XAWBRFVMVFK3XE.jpg",
                        darkModeImageUrl = null,
                        caption = "Second image - all images always visible.",
                        imageWidth = 1600,
                        imageHeight = 900,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    )
                ),
                expandCollapseUiModel = null
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryViewSingleImagePreview() {
    AndroidClassicTheme {
        GalleryView(
            uiModel = GalleryUiModel(
                images = listOf(
                    ImageUiModel(
                        imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg",
                        darkModeImageUrl = null,
                        caption = "A single image gallery. (Photo by John Smith/The Washington Post)",
                        imageWidth = 1200,
                        imageHeight = 800,
                        isLive = false,
                        widthFactor = WidthFactor.DEFAULT,
                        refreshRateMs = 0
                    )
                ),
                expandCollapseUiModel = null
            ),
            dummyArticlesInteractionHelper
        )
    }
}


