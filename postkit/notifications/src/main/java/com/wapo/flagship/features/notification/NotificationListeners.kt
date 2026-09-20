package com.wapo.flagship.features.notification

import com.wapo.flagship.content.notifications.NotificationModel

interface NotificationClickListener {
    fun onNotificationClick(notification: NotificationModel)
}

interface NotificationFooterClickListener {
    fun onNotificationFooterClicked()
}

interface NotificationSwipeListener {
    fun onNotificationSwiped(notification: NotificationModel)
}