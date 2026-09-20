package com.washingtonpost.android.save.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.save.models.PreviewItem

abstract class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView.rootView) {

    open fun bind(previewItem: PreviewItem) {

    }

    open fun unbind() = Unit
}