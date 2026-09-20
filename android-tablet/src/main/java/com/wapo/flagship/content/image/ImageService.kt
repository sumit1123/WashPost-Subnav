package com.wapo.flagship.content.image

import android.graphics.Bitmap
import com.wapo.flagship.features.pagebuilder.holders.LiveImageRequestData
import com.wapo.flagship.network.request.LiveMapImageRequest
import com.washingtonpost.android.volley.Request
import com.washingtonpost.android.volley.RequestQueue
import com.washingtonpost.android.volley.Response
import com.washingtonpost.android.volley.VolleyError
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import com.washingtonpost.android.wapocontent.ILoader
import com.washingtonpost.android.wapocontent.ImageRequestData
import com.washingtonpost.android.wapocontent.ImageResponse
import rx.Observable
import rx.android.MainThreadSubscription

class ImageService(
    val animatedImageLoader: AnimatedImageLoader,
    val requestQueue: RequestQueue,
) : ILoader {
    override fun getImage(requestData: ImageRequestData): Observable<ImageResponse> {
        if (isLiveImageRequest(requestData)) {
            return liveImageObservable(requestData)
        }
        return imageObservable(requestData)
    }

    private fun isLiveImageRequest(requestData: ImageRequestData): Boolean = requestData is LiveImageRequestData

    private fun imageObservable(requestData: ImageRequestData): Observable<ImageResponse> =
        Observable.create { subscriber ->
            val listener =
                object : AnimatedImageLoader.AnimatedImageListener {
                    val key = requestData.key

                    override fun onErrorResponse(error: VolleyError?) {
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onError(error)
                        }
                    }

                    override fun onResponse(
                        response: AnimatedImageLoader.AnimatedImageContainer,
                        isImmediate: Boolean,
                    ) {
                        if (!subscriber.isUnsubscribed) {
                            val imageResponse = ImageResponse(response.requestUrl, key, response.data)
                            subscriber.onNext(imageResponse)
                            if (isImmediate) {
                                if (response.data != null) {
                                    subscriber.onCompleted()
                                }
                            } else {
                                subscriber.onCompleted()
                            }
                        }
                    }
                }

            val container =
                animatedImageLoader.get(
                    requestData.url,
                    listener,
                    requestData.width,
                    requestData.height,
                    Request.Priority(requestData.priority.toInt()),
                )

            subscriber.add(
                object : MainThreadSubscription() {
                    override fun onUnsubscribe() {
                        container.cancelRequest()
                    }
                },
            )
        }

    private fun liveImageObservable(requestData: ImageRequestData): Observable<ImageResponse> =
        Observable.create { subscriber ->
            val listener =
                Response.Listener<Bitmap> { response ->
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onNext(ImageResponse(requestData.url, requestData.key, response))
                        subscriber.onCompleted()
                    }
                }
            val errorListener =
                Response.ErrorListener { err ->
                    subscriber.onError(err)
                }
            val liveMapImageRequest =
                LiveMapImageRequest(
                    requestData.url,
                    listener,
                    requestData.width,
                    requestData.height,
                    errorListener,
                )
            requestQueue.add(liveMapImageRequest)
        }
}
