package com.wapo.flagship.features.notification

import com.wapo.flagship.content.notifications.SubscriptionTopicModel

interface AlertsSettings {
    var isFirstLaunch: Boolean
    var alertLaunchCount: Int

    fun trackAlertsTabPageView(navigationBehavior: String)
    fun trackEnableNotifications(entryPoint: String, isEnabled: Boolean)

    fun enableAlertsTopic(topicName: String, isEnabled: Boolean)
    fun enableAlertsTopics(topics: Map<String, Boolean>)
    fun getAlertsTopics(): rx.Observable<List<AlertTopicInfo>>

    fun getAlertsTopicsList(): List<AlertTopicInfo>
    fun getAlertsGroupList(): List<AlertGroup>?

    fun syncTopicIfRequired(key: String, isEnabled: Boolean)
    fun migrateSegments()

    data class AlertTopicInfo(
            val topic: SubscriptionTopicModel,
            val isEnabled: Boolean
    )

    data class AlertGroup(
        val id: String,
        val label: String
    )

    enum class EntryPoint (val trackingName: String) {
        PROMPT("prompt"),
        ALERTS_TAB("alerts"),
        SETTINGS("settings"),
        AUTO("auto"),
        INLINE_TOGGLE("article");

        companion object {
            @JvmStatic
            val TYPE = "EntryPointType"

            @JvmStatic
            fun getTrackingString(entryPointName: String) : String {
                return "push_enrollment_entry:$entryPointName"
            }
        }
    }
}