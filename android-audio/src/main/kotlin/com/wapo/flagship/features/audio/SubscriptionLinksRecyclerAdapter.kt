package com.wapo.flagship.features.audio

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.wapo.android.commons.util.Logger
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.wapo.flagship.features.audio.service2.media.library.SUBSCRIPTION_LINK_DELIMITER

/**
 * Created by adkinsj on 12/19/18.
 */
class SubscriptionLinksRecyclerAdapter(
    val context: Context?,
    val items: List<String>,
    val onClick: (String, CharSequence?) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.podcast_app_list_item, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val values = items[position].split(SUBSCRIPTION_LINK_DELIMITER)
        if (values.size == 2) {
            holder.appName.text = values[0]
            holder.itemView.setOnClickListener {
                onClick.invoke(values[1], holder.appName.text)
            }
        } else {
            Logger.d(TAG, "Subscription link is invalid: $values")
            holder.appName.visibility = View.GONE
        }
    }

    companion object {
        val TAG: String = SubscriptionLinksRecyclerAdapter::class.java.simpleName
    }
}

class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    val appName: TextView = view.findViewById(R.id.tv_podcast_app_name)
}