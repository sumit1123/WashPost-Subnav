package com.washingtonpost.android.recirculation.carousel.viewholders

import android.graphics.Bitmap
import com.washingtonpost.android.recirculation.carousel.models.CarouselViewItem
import com.washingtonpost.android.recirculation.carousel.models.EmptyCardItem
import com.washingtonpost.android.recirculation.databinding.EmptyItemBinding

class EmptyCardViewHolder(val binding: EmptyItemBinding) : CarouselViewHolder(binding.root) {

    override fun bind(item: CarouselViewItem) {
        val emptyCardItem = item as EmptyCardItem
        binding.text.text = emptyCardItem.text1
        binding.text2.text = emptyCardItem.text2
        binding.image.setImageResource(emptyCardItem.iconRes)
    }

    override fun onViewRecycled() {

    }

    override fun onBitmapLoaded(bitmap: Bitmap) {

    }

    override fun onBitmapError(bitmap: Bitmap?) {

    }

    override fun onLowDataModeChange(isEnable: Boolean) {

    }
}