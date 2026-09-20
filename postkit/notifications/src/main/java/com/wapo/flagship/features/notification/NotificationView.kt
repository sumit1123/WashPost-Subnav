package com.wapo.flagship.features.notification

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.util.AttributeSet
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.content.notifications.NotificationModel
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import rx.Observable

class NotificationView : RecyclerView {

    var swipeListener: NotificationSwipeListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr)

    init {
        layoutManager = LinearLayoutManager(context)

        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                try {
                    if (newState == SCROLL_STATE_DRAGGING) {
                        if (childCount > 0) {
                            val vh = recyclerView?.getChildViewHolder(getChildAt(0))
                            if (vh is NotificationHeaderHolder) {
                                if (adapter.headerVisibility) {
                                    adapter.headerVisibility = false
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(this.javaClass.toString(), "Scroll error", e)
                }
                super.onScrollStateChanged(recyclerView, newState)
            }
        })

        super.setAdapter(NotificationAdapter())

        val swipeCallback = SwipeToDismissCallback(this)
        val swipeHelper = ItemTouchHelper(swipeCallback)
        swipeHelper.attachToRecyclerView(this)
    }

    override fun setAdapter(adapter: Adapter<*>?) {
        throw UnsupportedOperationException()
    }

    override fun getAdapter(): NotificationAdapter {
        return super.getAdapter() as NotificationAdapter
    }

    fun setNotificationClickListener(listener: NotificationClickListener) {
        adapter.itemClickListener = listener
    }

    fun setFooterClickListener(listener: NotificationFooterClickListener) {
        adapter.footerClickListener = listener
    }

    fun getItemsOnScreen(): Observable<List<NotificationModel>> {
        return Observable.create<NotificationModel> { subscriber ->
            val items = adapter.items
            (0..childCount - 1)
                    .map { getChildAt(it) }
                    .map { getChildAdapterPosition(it) }
                    .filter { it >= 0 && it < items.size }
                    .map { items[it] }
                    .forEach {
                        when (it) {
                            is NotificationItem -> subscriber.onNext(it.model)
                            is NotificationItemImage -> subscriber.onNext(it.model)
                        }
                    }
            subscriber.onCompleted()
        }
                .toList()
                .defaultIfEmpty(emptyList())
    }

    fun setItems(items: List<NotificationModel>, imageLoader: AnimatedImageLoader) {
        adapter.setItems(items.distinct(), imageLoader)
    }

    internal fun onItemSwiped(viewHolder: ViewHolder) {
        val adapterItem = adapter.items[viewHolder.adapterPosition]
        adapter.removeItem(viewHolder.adapterPosition)
        when (adapterItem) {
            is NotificationItem -> swipeListener?.onNotificationSwiped(adapterItem.model)
            is NotificationItemImage -> swipeListener?.onNotificationSwiped(adapterItem.model)
        }
    }

    fun setNightMode(nightModeEnabled: Boolean) {
        adapter.setNightMode(nightModeEnabled)
    }

    fun setShowSettingsOnTop(showSettingsOnTop: Boolean) {
        adapter.showSettingsOnTop = showSettingsOnTop
    }
}



