package com.wapo.flagship.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.washingtonpost.android.volley.RequestQueue
import java.io.InputStream

class VolleyUrlLoader(
    val requestQueue: RequestQueue,
    val requestFactory: GlideImageRequestFactory,
) : ModelLoader<GlideUrl, InputStream> {
    override fun buildLoadData(
        model: GlideUrl,
        width: Int,
        height: Int,
        options: Options,
    ): ModelLoader.LoadData<InputStream>? = ModelLoader.LoadData(model, VolleyStreamFetcher(requestQueue, model, requestFactory))

    override fun handles(model: GlideUrl): Boolean = true
}

class VolleyUrlLoaderFactory(
    val requestQueue: RequestQueue,
    val requestFactory: GlideImageRequestFactory,
) : ModelLoaderFactory<GlideUrl, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> =
        VolleyUrlLoader(requestQueue, requestFactory)

    override fun teardown() {
        // Do nothing
    }
}
