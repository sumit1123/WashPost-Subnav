package com.wapo.android.push

interface PushManager {
    fun enablePushTopic(topicName: String, isEnabled: Boolean)
    fun enablePushTopicBundle(topicNames: List<String>, isEnabled: Boolean)
    fun enablePushes(isEnabled: Boolean)
    fun getPushId(): String?
    fun getUserId(): String?
    fun enableRegistration()
    fun trackScreen(screenName: String)
    fun pauseInAppAutomation(paused: Boolean)
    fun setUrlAllowListScopeOpenUrl(urlsList: List<String>?)
    fun getEnabledPushTopics(): Set<String>
}