package com.wapo.flagship.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.wapo.flagship.FlagshipApplication
import com.washingtonpost.android.BuildConfig
import android.util.Log
import java.io.InputStream

@GlideModule
class WapoGlideModule : AppGlideModule() {
    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry,
    ) {
        super.registerComponents(context, glide, registry)
        val requestQueue = FlagshipApplication.getInstance().requestQueue
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            VolleyUrlLoaderFactory(requestQueue, GlideImageRequestFactory()),
        )
    }

    override fun applyOptions(
        context: Context,
        builder: GlideBuilder,
    ) {
        val defaultOptions =
            RequestOptions()
                .fitCenter()
                .format(DecodeFormat.PREFER_RGB_565)
                .override(Target.SIZE_ORIGINAL)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Volley takes care of disk cache
        if (BuildConfig.DEBUG) {
            builder.setLogLevel(Log.VERBOSE)
        }
        builder
            .setDefaultRequestOptions(defaultOptions)
    }

    override fun isManifestParsingEnabled(): Boolean = false
}
