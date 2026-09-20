/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.ui

import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.transition.Transition

/**
 * After the image is loaded, another request is fired to load the blurry image.
 */
class ImageViewTargetWithBlur(
    private val url: String,
    private val imageView: ImageView,
    private val blurView: ImageView
) : BitmapImageViewTarget(imageView) {
    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        super.onResourceReady(resource, transition)
        Glide.with(imageView.context)
            .asBitmap()
            .load(url)
            .override(4, 4)
            .into(blurView)
    }
}