package com.wapo.flagship.features.notification

interface AlertsActivityInterface {
    val alertsSettings: AlertsSettings
    fun openAlertsSettings()
    fun openNotification(notificationUrl: String, notificationArticleType: String?, notificationTopic : String?)
    fun sendException(t: Throwable)
    fun logExtras(str: String)
}