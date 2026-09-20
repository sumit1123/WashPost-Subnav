package com.washingtonpost.android.save.viewholders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.save.R

class FollowArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val headlineView = itemView.findViewById<TextView>(com.washingtonpost.android.recirculation.R.id.headline)
    val timeView = itemView.findViewById<TextView>(com.wapo.view.R.id.time)
    val menu = itemView.findViewById<View>(R.id.ib_utility_menu)
}