package com.wapo.flagship.features.notification

import com.washingtonpost.android.notifications.R

fun getTextColorRes(nightMode: Boolean) : Int {
    return if (nightMode)
        R.color.notifications_text_color_night
    else
        R.color.notifications_text_color
}

fun getNotificationItemBgColorRes(nightMode: Boolean, shown: Boolean) : Int {
    return if(shown) {
        if (nightMode) R.color.notification_item_shown_night else R.color.notification_item_shown
    } else {
        if (nightMode) R.color.notification_item_new_night else R.color.notification_item_new
    }
}