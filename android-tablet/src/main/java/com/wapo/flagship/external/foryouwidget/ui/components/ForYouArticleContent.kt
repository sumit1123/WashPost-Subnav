package com.wapo.flagship.external.foryouwidget.ui.components

import com.wapo.flagship.external.foryouwidget.actions.ArticleClickAction
import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.flagship.external.foryouwidget.data.ForYouWidgetItem
import com.washingtonpost.android.R
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.external.BitmapUtils
import com.wapo.flagship.util.network.CacheHeadersInterceptor
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.ImageServiceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit


@Composable
fun ForYouArticleContent(
    article: ForYouWidgetItem,
    position: Int
) {
    val context = LocalContext.current
    val maxWidth:Int = context.resources.getDimensionPixelSize(com.washingtonpost.foryou.R.dimen.for_you_card_size) * 2
    val maxHeight:Int = 200 * 2
    val forYouArcId: String? = article.arcId
    val forYouArticleHeadline: String? = article.headline
    val forYouArticleUrl: String? = article.url
    val forYouArticleImageURL: String? = article.imageUrl
    val forYouArticleCategory: String? = article.category
    val forYouArticleAge: String? = article.displayAge

    var loadedBitmap by remember(forYouArticleImageURL) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(forYouArticleImageURL) {
        forYouArticleImageURL ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            // Format image url with resizer
            val formattedImageUrl = ConfigManager
                .getInstance()
                .config
                .createImageRequestUrl(forYouArticleImageURL ?: "",
                    ImageServiceConfig(maxHeight, maxWidth)
                )
            val request = ImageRequest.Builder(context).data(formattedImageUrl).apply {
                bitmapConfig(Bitmap.Config.RGB_565)
            }.build()

            // Build an OkHttpClient with timeouts
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // connection timeout
                .readTimeout(30, TimeUnit.SECONDS)    // socket read timeout
                .writeTimeout(30, TimeUnit.SECONDS)   // socket write timeout
                .addInterceptor(DefaultHeadersInterceptor())
                .addInterceptor(CacheHeadersInterceptor())
                .build()

            // Create an ImageLoader with this client
            val imageLoader = ImageLoader.Builder(context)
                .okHttpClient(okHttpClient)
                .build()

            loadedBitmap = when (val result = imageLoader.execute(request)) {
                is ErrorResult -> {
                    Logger.e("ForYouWidget", "Image load error: ${result.throwable.message}", result.throwable)
                    null
                }
                is SuccessResult -> resizeBitmapForWidget(
                    result.drawable.toBitmapOrNull(),
                    maxWidth, maxHeight)
            }
        }
    }

    val imageProvider = loadedBitmap?.let { bitmap ->
        ImageProvider(bitmap)
    } ?: ImageProvider(R.drawable.wp_logo_icon)

    // Use the new ArticleContent composable
    ArticleContent(
        forYouArticleHeadline = forYouArticleHeadline ?: "",
        forYouArticleImage = imageProvider,
        forYouArticleCategory = forYouArticleCategory ?: "",
        forYouArticleAge = forYouArticleAge ?: "",
        onClick = actionRunCallback<ArticleClickAction>(
            parameters = actionParametersOf(
                ArticleClickAction.articleIdKey to (forYouArcId ?: ""),
                ArticleClickAction.articleUrlKey to (article.url ?: ""),
                ArticleClickAction.positionKey to position,
            )
        )
    )
}

@Composable
private fun ArticleContent(
    forYouArticleHeadline: String,
    forYouArticleImage: ImageProvider,
    forYouArticleCategory: String,
    forYouArticleAge: String,
    onClick: androidx.glance.action.Action
) {
    Box(modifier = GlanceModifier
        .padding(bottom = 8.dp)
        .fillMaxWidth()
    ) {
        Box(
            modifier = GlanceModifier
                .height(200.dp)
                .fillMaxWidth()
                .background(
                    imageProvider = forYouArticleImage,
                    contentScale = ContentScale.Crop
                )
                .cornerRadius(16.dp)
                .clickable(onClick)
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color.Black.copy(alpha = 0.3f)))
            ) {
                ForYouText(
                    forYouArticleHeadline = forYouArticleHeadline,
                    forYouArticleAge = forYouArticleAge,
                    forYouArticleCategory = forYouArticleCategory
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun ForYouText(
    forYouArticleHeadline: String,
    forYouArticleAge: String,
    forYouArticleCategory: String
) {
    Column(
        modifier = GlanceModifier.fillMaxHeight().padding(4.dp),
        horizontalAlignment = Alignment.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = forYouArticleHeadline,
            style = TextStyle(
                fontSize = 18.sp,
                textAlign = TextAlign.Left,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color.White),
            ),
            modifier = GlanceModifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            maxLines = 3,
        )
        Text(
            text = (forYouArticleCategory.takeIf { it.isNotEmpty() }?.let { "$it  •  " } ?: "") + forYouArticleAge,
            style = TextStyle(color = ColorProvider(Color.White)),
            modifier = GlanceModifier.padding(all = 8.dp),
            maxLines = 1
        )
    }
}

private fun resizeBitmapForWidget(originalBitmap: Bitmap?, maxWidth: Int, maxHeight: Int): Bitmap? {
    originalBitmap.let {
        return try {
            // Convert bitmap to byte array
            val stream = ByteArrayOutputStream()
            originalBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()

            val resizedBitmap = BitmapUtils.
            parseBitmap(byteArray, false, maxWidth, maxHeight) as? Bitmap

            resizedBitmap ?: originalBitmap
        } catch (e: Exception) {
            Logger.e("ForYouWidget", "Failed to resize bitmap, using original: ${e.message}", e)
            originalBitmap
        }
    }
}