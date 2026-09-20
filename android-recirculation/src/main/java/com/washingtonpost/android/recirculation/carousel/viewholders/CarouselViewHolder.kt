/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.washingtonpost.android.recirculation.carousel.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.listeners.OnCarouselImageLoadedListener

abstract class CarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
    OnCarouselImageLoadedListener {
    abstract fun bind(item: CarouselViewItem)
    abstract fun onViewRecycled()
}