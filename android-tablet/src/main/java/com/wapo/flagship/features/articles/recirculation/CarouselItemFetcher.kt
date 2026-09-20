package com.wapo.flagship.features.articles.recirculation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.wapo.android.commons.util.Logger
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.wapo.android.commons.util.ViewUtil.findComponentActivity
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.models.deserialized.RecirculationItem
import com.wapo.flagship.features.lowdatamodelbanner.model.LowDataBanner
import com.wapo.flagship.network.request.CacheFallbackImageRequest
import com.wapo.flagship.network.request.LiveMapImageRequest
import com.washingtonpost.android.R
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselImageLoadedListener
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.volley.Response
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import java.util.concurrent.TimeUnit

/**
 * Fetch recirculation carousel items for new article viewholder
 */
class Carousel2ItemFetcher(
    private val carouselCache: RecirculationStorage,
) : Articles2RecirculationViewHolder.CarouselItemsFetcher {
    override fun getCarouselItems(
        context: Context,
        item: RecirculationItem,
        resultListener: Articles2RecirculationViewHolder.CarouselItemsFetcher.ResultListener,
    ) {
        carouselCache
            .getCarouselItems(item.recirculationType.sectionName, item.recirculationType)
            .subscribe(
                { result ->
                    showItems(result, item, resultListener)
                },
                { e ->
                    Logger.d("Carousel2ItemFetcher", "Could not load recirculation")
                    resultListener.onError(e)
                },
            )
    }

    private fun showItems(
        items: List<CarouselViewItem>,
        articleItem: RecirculationItem,
        resultListener: Articles2RecirculationViewHolder.CarouselItemsFetcher.ResultListener,
    ) {
        val filteredItems = items.filter { it.contentUrl != (articleItem.article.contenturl) }
        resultListener.onSuccess(filteredItems)
    }
}

class CarouselEnvironment(
    private val context: Context,
    private val lowDataBannerLive: LiveData<LowDataBanner>?,
) : CarouselProvider {
    private val softTtl = TimeUnit.HOURS.toMillis(8)

    var observer: Observer<LowDataBanner>? = null

    override fun makeImageRequest(
        url: String?,
        maxWidth: Int,
        maxHeight: Int,
        listener: OnCarouselImageLoadedListener,
    ) {
        observer =
            Observer { value ->
                listener.onLowDataModeChange(value.isLowDataBannerEnable)
                if (!value.isLowDataBannerEnable) {
                    setItem(url, maxWidth, maxHeight, listener)
                }
            }

        context.findComponentActivity()?.let { owner ->
            observer?.let { notNullObserver ->
                lowDataBannerLive?.observe(owner, notNullObserver)
            }
        }

        listener.onLowDataModeChange(lowDataBannerLive?.value?.isLowDataBannerEnable == true)
        if (lowDataBannerLive?.value?.isLowDataBannerEnable != true) {
            setItem(url, maxWidth, maxHeight, listener)
        }
    }

    private fun setItem(
        url: String?,
        maxWidth: Int,
        maxHeight: Int,
        listener: OnCarouselImageLoadedListener,
    ) {
        if (url != null) {
            val successListener =
                Response.Listener<Bitmap> { response ->
                    if (response is Bitmap) {
                        listener.onBitmapLoaded(response)
                    }
                }
            val errorListener =
                Response.ErrorListener { error ->
                    Logger.e("ArticleItemsInjectorHook", "Failed to download image for carousel")
                    showFallbackImage(listener)
                }
            val imageRequest =
                CacheFallbackImageRequest(
                    url,
                    successListener,
                    maxWidth,
                    maxHeight,
                    Bitmap.Config.RGB_565,
                    errorListener,
                ).apply { softTtlExtension = softTtl }
            FlagshipApplication.getInstance().requestQueue.add(imageRequest)
        } else {
            showFallbackImage(listener)
        }
    }

    override fun makeLiveImageRequest(
        url: String?,
        maxWidth: Int,
        maxHeight: Int,
        listener: OnCarouselImageLoadedListener,
    ) {
        if (url != null) {
            val successListener =
                Response.Listener<Bitmap> { response ->
                    if (response is Bitmap) {
                        listener.onBitmapLoaded(response, false)
                    }
                }
            val errorListener =
                Response.ErrorListener { error ->
                    Logger.e("ArticleItemsInjectorHook", "Failed to download image for carousel")
                    showFallbackImage(listener)
                }
            val imageRequest =
                LiveMapImageRequest(
                    url,
                    successListener,
                    maxWidth,
                    maxHeight,
                    errorListener,
                )
            FlagshipApplication.getInstance()?.requestQueue?.add(imageRequest)
        } else {
            showFallbackImage(listener)
        }
    }

    override fun getImageLoader(): AnimatedImageLoader = FlagshipApplication.getInstance().animatedImageLoader

    private fun showFallbackImage(listener: OnCarouselImageLoadedListener) {
        val placeHolderImage =
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.carousel_placeholder,
            )
        listener.onBitmapError(placeHolderImage)
    }

    override fun tearDown() {
        observer = null
    }
}
