package com.wapo.flagship.features.articles3.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.signature.ObjectKey
import com.wapo.flagship.features.articles2.interfaces.ArticleInteractionEvent
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles3.models.ui.ImageUiModel
import com.wapo.flagship.features.articles3.models.ui.WidthFactor
import com.wapo.flagship.features.articles3.parseHtmlContent
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.ArticleTextStyles
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ImageView(
    uiModel: ImageUiModel,
    articlesInteractionHelper: ArticlesInteractionHelper,
    captionPadding: PaddingValues = PaddingValues(top = 8.dp),
) {
    val isDarkMode = isSystemInDarkTheme()
    val imageUrl = if (isDarkMode && !uiModel.darkModeImageUrl.isNullOrEmpty()) {
        uiModel.darkModeImageUrl
    } else {
        uiModel.imageUrl
    }

    // State to trigger recomposition for live image refresh
    var refreshTrigger by remember { mutableLongStateOf(0L) }

    // Periodic refresh only for live images
    if (uiModel.isLive) {
        val refreshRateMs = uiModel.refreshRateMs ?: 60_000L
        LaunchedEffect(refreshRateMs) {
            while (true) {
                delay(refreshRateMs)
                refreshTrigger = System.currentTimeMillis()
            }
        }
    }

    val aspectRatio = getAspectRatio(uiModel.imageWidth, uiModel.imageHeight)

    // Build the image URL - append refresh trigger only for live images
    val finalImageUrl = if (uiModel.isLive) {
        "$imageUrl?refresh=$refreshTrigger"
    } else {
        imageUrl
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (aspectRatio > 0f) {
                        Modifier.aspectRatio(aspectRatio)
                    } else {
                        Modifier
                    }
                )
                .background(wpdsColors.gray400)
                .clickable {
                    articlesInteractionHelper.onEventFired(
                        ArticleInteractionEvent.ImageClickEvent(uiModel.imageUrl)
                    )
                }
        ) {
            GlideImage(
                model = finalImageUrl,
                contentDescription = uiModel.caption ?: if (uiModel.isLive) "Live image" else "Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) { requestBuilder ->
                if (uiModel.isLive) {
                    requestBuilder.signature(ObjectKey(refreshTrigger))
                } else {
                    requestBuilder
                }
            }
        }

        if (!uiModel.caption.isNullOrEmpty()) {
            Text(
                text = parseHtmlContent(uiModel.caption, articlesInteractionHelper),
                style = getImageCaptionStyle(uiModel.uiStyle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(captionPadding)
            )
        }
    }
}

@Composable
private fun getImageCaptionStyle(uiStyle: ImageUiStyle) = when (uiStyle) {
    ImageUiStyle.DEFAULT -> ArticleTextStyles.IMAGE_CAPTION.style
}

private fun getAspectRatio(imageWidth: Int?, imageHeight: Int?): Float {
    val width = imageWidth ?: 0
    val height = imageHeight ?: 0
    return if (width > 0 && height > 0) {
        width.toFloat() / height
    } else {
        0f
    }
}

enum class ImageUiStyle {
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun ImageViewPreview() {
    AndroidClassicTheme {
        ImageView(
            uiModel = ImageUiModel(
                imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/V5IZ4G7Y763R54RSBQGN6M3XZE.jpg",
                darkModeImageUrl = null,
                caption = "A standard image in the article. (Photo by John Smith/The Washington Post)",
                imageWidth = 1200,
                imageHeight = 800,
                refreshRateMs = 60000,
                widthFactor = WidthFactor.DEFAULT,
                isLive = false
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveImageViewPreview() {
    AndroidClassicTheme {
        ImageView(
            uiModel = ImageUiModel(
                imageUrl = "https://dohdeick6sqa6.cloudfront.net/screenshots/staging/live-head-to-head-president@sm.png",
                darkModeImageUrl = "https://s3.amazonaws.com/wpmobileprodipad2.0/classic_test/live-images/live-head-to-head-president%40sm-dark.png",
                caption = "A live image showing real-time updates from the event. (Photo by John Smith/The Washington Post)",
                imageWidth = 1200,
                imageHeight = 800,
                refreshRateMs = 60000,
                widthFactor = WidthFactor.DEFAULT,
                isLive = true
            ),
            dummyArticlesInteractionHelper
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImageViewNoCaptionPreview() {
    AndroidClassicTheme {
        ImageView(
            uiModel = ImageUiModel(
                imageUrl = "https://arc-anglerfish-washpost-prod-washpost.s3.amazonaws.com/public/OIBALHVNOAI6XAWBRFVMVFK3XE.jpg",
                darkModeImageUrl = null,
                imageWidth = 1600,
                imageHeight = 900,
                refreshRateMs = 60000,
                caption = null,
                widthFactor = WidthFactor.DEFAULT,
                isLive = false
            ),
            dummyArticlesInteractionHelper
        )
    }
}
