package com.wapo.flagship.auto

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal class CarArtworkLoader(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val artworkSizePixels =
        (ROW_ARTWORK_SIZE_DP * context.resources.displayMetrics.density)
            .roundToInt()
            .coerceAtLeast(1)

    suspend fun load(uri: Uri): CarIcon? =
        withContext(Dispatchers.IO) {
            val requestManager = Glide.with(applicationContext)
            val target =
                requestManager
                    .asBitmap()
                    .load(
                        GlideUrl(
                            uri.toString(),
                            LazyHeaders
                                .Builder()
                                .addHeader(NO_RESIZE_HEADER, "true")
                                .build(),
                        ),
                    ).override(artworkSizePixels, artworkSizePixels)
                    .fitCenter()
                    .submit()
            try {
                val bitmap =
                    target
                        .get()
                        .copy(Bitmap.Config.ARGB_8888, false)
                        ?: return@withContext null
                CarIcon
                    .Builder(IconCompat.createWithBitmap(bitmap))
                    .build()
            } catch (error: Exception) {
                Log.w(TAG, "Unable to load Android Auto artwork: $uri", error)
                null
            } finally {
                requestManager.clear(target)
            }
        }

    private companion object {
        const val TAG = "CarArtworkLoader"
        const val ROW_ARTWORK_SIZE_DP = 224
        const val NO_RESIZE_HEADER = "X-WAPO-NO-RESIZE"
    }
}
