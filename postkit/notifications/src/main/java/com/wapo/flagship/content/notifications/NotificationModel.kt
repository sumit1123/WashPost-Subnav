package com.wapo.flagship.content.notifications

import java.util.Date

data class NotificationModel(
        val title: String,
        val time: Date,
        val url: String,
        val notificationData: NotificationData,
        val imageUrl: String
)