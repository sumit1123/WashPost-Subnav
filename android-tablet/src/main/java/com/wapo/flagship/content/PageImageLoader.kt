package com.wapo.flagship.content

import com.wapo.flagship.features.grid.ChainEntity
import com.wapo.flagship.features.grid.GridEntity
import com.wapo.flagship.features.grid.HomepageStoryEntity
import com.washingtonpost.android.volley.Request
import com.washingtonpost.android.volley.VolleyError
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader.AnimatedImageListener
import com.washingtonpost.android.wapocontent.Priority
import rx.Observable
import rx.Observable.OnSubscribe

class PageImageLoader(
    val animatedImageLoader: AnimatedImageLoader,
) {

    fun loadImagesToCache(
        fusionPage: GridEntity,
        priority: Priority,
        settings: PageImageLoadSettings,
    ): Observable<Void> {
        val imageUrls = ArrayList<String>()
        fusionPage.regions
            .flatMap { it.items }
            .filterIsInstance<ChainEntity>()
            .flatMap { it.items }
            .filterNotNull()
            .flatMap { it.items }
            .filterIsInstance<HomepageStoryEntity>()
            .forEach {
                val url = it.media?.url
                if (!url.isNullOrBlank()) {
                    imageUrls.add(url)
                }
                val slideshowUrls = it.slideshow?.images?.mapNotNull { it?.url }
                if (!slideshowUrls.isNullOrEmpty()) {
                    imageUrls.addAll(slideshowUrls)
                }
            }

        val observables =
            imageUrls
                .distinct()
                .map { url ->
                    ImageLoadObservable.create(
                        url,
                        animatedImageLoader,
                        Request.Priority(priority.toInt()),
                        settings,
                    )
                }

        return Observable
            .merge(observables)
            .onErrorResumeNext { Observable.empty() }
    }

    private class ImageLoadObservable protected constructor(
        onSubscribe: OnSubscribe<Void>,
    ) : Observable<Void>(
            onSubscribe,
        ) {
        companion object {
            fun create(
                url: String,
                loader: AnimatedImageLoader,
                priority: Request.Priority,
                settings: PageImageLoadSettings,
            ) = ImageLoadObservable(
                OnSubscribe<Void> { subscriber ->
                    if (settings != null && !settings.canLoad()) {
                        subscriber.onCompleted()
                        return@OnSubscribe
                    }
                    loader.loadToCache(
                        url,
                        object : AnimatedImageListener {
                            override fun onErrorResponse(error: VolleyError?) {
                                if (!subscriber.isUnsubscribed) {
                                    // ignore errors
                                    subscriber.onCompleted()
                                }
                            }

                            override fun onResponse(
                                response: AnimatedImageLoader.AnimatedImageContainer?,
                                isImmediate: Boolean,
                            ) {
                                if (!subscriber.isUnsubscribed) {
                                    subscriber.onCompleted()
                                }
                            }
                        },
                        priority,
                    )
                },
            )
        }
    }

    interface PageImageLoadSettings {
        fun canLoad(): Boolean
    }
}
