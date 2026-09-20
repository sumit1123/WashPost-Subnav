/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils

import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.widget.ImageView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.washingtonpost.android.R

object PicassoImageLoader {

    fun loadImage(
        activity: Activity,
        imageUrl: String?, imageView: ImageView,
        callback: Callback? = null
    ) {
        val size = getScreenSize(activity)
        Picasso.get()
            .load(imageUrl)
            .resize(size.width, size.height)
            .centerCrop()
            .placeholder(R.drawable.image_placeholder_50)
            .error(R.drawable.image_placeholder_50)
            .into(imageView, callback)
    }

    fun loadSquareImage(
        activity: Activity,
        imageUrl: String?, imageView: ImageView,
        callback: Callback? = null
    ) {
        val size = getScreenSize(activity)
        Picasso.get()
            .load(imageUrl)
            .resize(size.width, size.width)
            .centerCrop()
            .placeholder(R.drawable.image_placeholder_50)
            .error(R.drawable.image_placeholder_50)
            .into(imageView, callback)
    }

    fun loadBackgroundImage(activity: Activity, imageUrl: String?, target: Target) {
        val size = getScreenSize(activity)
        Picasso.get()
            .load(imageUrl)
            .resize(size.width, size.height)
            .centerCrop()
            .into(target)
    }

    private fun getScreenSize(activity: Activity): Size {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            Size(bounds.width(), bounds.height())
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }

}