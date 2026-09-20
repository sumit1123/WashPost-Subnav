package com.washingtonpost.android.config.domain.models.config

data class PushConfigStub(
    val userGroup: List<TopicGroup>?,
    val availableUserSubscriptionTopics: List<SubscriptionTopic>?,
    val topicGroups: List<TopicGroup>,
    val availableSubscriptionTopics: List<SubscriptionTopic>,
    val userData: String,
)

data class TopicGroup(
    val id: String,
    val label: String,
)

data class SubscriptionTopic(
    val id: String,
    val displayName: String,
    val key: String,
    val appTopicName: String,
    val isOptional: Boolean,
    val alias: String,
    val imageName: String,
    val reference: String,
    val description: String,
    val group: String,
    val isHidden: Boolean
)