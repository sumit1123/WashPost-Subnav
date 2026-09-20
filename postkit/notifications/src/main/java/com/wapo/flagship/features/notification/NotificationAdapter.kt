package com.wapo.flagship.features.notification

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.wapo.flagship.content.notifications.NotificationModel
import com.washingtonpost.android.notifications.R
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

class NotificationAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        val VIEW_TYPE_ITEM = 0
        val VIEW_TYPE_HEADER = 1
        val VIEW_TYPE_FOOTER = 2
        val VIEW_TYPE_NOTHING = 3
        val VIEW_TYPE_ITEM_IMAGE = 4
    }

    private var nightModeEnabled = false
    val items = mutableListOf<NotificationAdapterItem>()
    val formatter = SimpleDateFormat("h:mm a", Locale.US)
    private val handler = Handler(Looper.getMainLooper())
    var itemClickListener: NotificationClickListener? = null
    var footerClickListener: NotificationFooterClickListener? = null
    var headerVisibility: Boolean by Delegates.observable(false) { prop, old, new ->
        setupHeader(new)
    }

    var showSettingsOnTop = false

    private var imageLoader: AnimatedImageLoader? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is NotificationHeader -> VIEW_TYPE_HEADER
            is NotificationItemImage -> VIEW_TYPE_ITEM_IMAGE
            is NotificationFooter -> VIEW_TYPE_FOOTER
            is NotificationNoAlerts -> VIEW_TYPE_NOTHING
            else -> VIEW_TYPE_ITEM
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].getId()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NotificationHolder -> holder.bind((items[position] as NotificationItem).model, itemClickListener, formatter, nightModeEnabled)
            is NotificationItemImageHolder -> holder.bind((items[position] as NotificationItemImage).model, itemClickListener, formatter, nightModeEnabled)
            is NotificationFooterHolder -> holder.bind(footerClickListener, nightModeEnabled)
            is NotificationNothingHolder -> holder.bind(nightModeEnabled)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> NotificationHolder(LayoutInflater.from(parent.context).inflate(R.layout.notification_item, parent, false))
            VIEW_TYPE_HEADER -> NotificationHeaderHolder(LayoutInflater.from(parent.context).inflate(R.layout.notification_header, parent, false))
            VIEW_TYPE_FOOTER -> NotificationFooterHolder(LayoutInflater.from(parent.context).inflate(R.layout.notification_footer, parent, false))
            VIEW_TYPE_NOTHING -> NotificationNothingHolder(LayoutInflater.from(parent.context).inflate(R.layout.notification_nothing, parent, false))
            VIEW_TYPE_ITEM_IMAGE -> NotificationItemImageHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.notification_item_image, parent, false),
                    imageLoader ?: throw IllegalStateException("image loader hasn't been provided")
            )
            else -> NotificationHolder(LayoutInflater.from(parent.context).inflate(R.layout.notification_item, parent, false))
        }
    }

    fun setItems(notifications: List<NotificationModel>, imageLoader: AnimatedImageLoader) {
        this.imageLoader = imageLoader
        items.clear()

        if (notifications.isEmpty()) {
            headerVisibility = false
            items.add(NotificationNoAlerts())
        } else {
            headerVisibility = notifications.any { model -> !model.notificationData.isRead }

            items.addAll(
                    notifications
                            .sortedByDescending { it.time }
                            .map {
                                if (it.imageUrl.isNotEmpty()) {
                                    NotificationItemImage(it)
                                } else {
                                    NotificationItem(it)
                                }
                            }
            )
        }

        if (showSettingsOnTop) {
            val headerIndex = items.indexOfFirst { it is NotificationHeader }
            items.add(headerIndex + 1, NotificationFooter())
        } else {
            items.add(NotificationFooter())
        }

        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, 1)
        if (items.size <= 2) {
            if (items[0] is NotificationHeader || items[0] is NotificationFooter) {
                items.clear()
                headerVisibility = false
                if (showSettingsOnTop) {
                    items.add(NotificationFooter())
                    items.add(NotificationNoAlerts())
                } else {
                    items.add(NotificationNoAlerts())
                    items.add(NotificationFooter())
                }
            }
        }
        notifyDataSetChanged()
    }

    private fun setupHeader(visible: Boolean) {
        if (visible) {
            if (items.size == 0 || items[0] !is NotificationHeader) {
                items.add(NotificationHeader())
            }
        } else {
            if (items.size > 0 && items[0] is NotificationHeader) {
                items.removeAt(0)
            }
        }
        handler.post { notifyDataSetChanged() }
    }

    fun setNightMode(nightModeEnabled: Boolean) {
        this.nightModeEnabled = nightModeEnabled
        notifyDataSetChanged()
    }
}
