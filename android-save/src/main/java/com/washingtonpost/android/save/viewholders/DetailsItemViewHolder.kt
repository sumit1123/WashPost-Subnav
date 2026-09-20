package com.washingtonpost.android.save.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.save.models.DetailItem

abstract class DetailsItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(detailItem: DetailItem)
}