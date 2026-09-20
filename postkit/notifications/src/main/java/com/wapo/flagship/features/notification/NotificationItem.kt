package com.wapo.flagship.features.notification

import com.wapo.flagship.content.notifications.NotificationModel

interface NotificationAdapterItem {
    fun getId(): Long
}

class NotificationItem(val model: NotificationModel) : NotificationAdapterItem {
    override fun getId(): Long {
        return model.notificationData.id.toLong()
    }
}

class NotificationItemImage(val model: NotificationModel) : NotificationAdapterItem {
    override fun getId(): Long {
        return model.notificationData.id.toLong()
    }
}

class NotificationHeader : NotificationAdapterItem {
    override fun getId(): Long {
        return -1
    }
}

class NotificationFooter : NotificationAdapterItem {
    override fun getId(): Long {
        return -2
    }
}

class NotificationNoAlerts : NotificationAdapterItem {
    override fun getId(): Long {
        return -3
    }
}