package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.PushConfigStub
import com.washingtonpost.android.config.domain.models.config.SubscriptionTopic
import com.washingtonpost.android.config.domain.models.config.TopicGroup
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams

@JsonClass(generateAdapter = true)
data class RawPushConfigStub(
    @Json(name = "groups") val topicGroups: List<RawTopicGroup>? = null,
    @Json(name = "availableSubscriptionTopics") val availableSubscriptionTopics: List<RawSubscriptionTopic>? = null,
    @Json(name = "userGroups") val userGroups: List<RawTopicGroup>? = null,
    @Json(name = "availableUserSubscriptionTopics") val availableUserSubscriptionTopics: List<RawSubscriptionTopic>? = null,
) {
    fun mapToDomain(params: MapConfigParams): PushConfigStub {
        return PushConfigStub(
            topicGroups = topicGroups?.map { it.mapToDomain() }.orEmpty(),
            availableSubscriptionTopics = availableSubscriptionTopics?.map { it.mapToDomain() }
                .orEmpty(),
            userData = params.deviceUniqueId,
            userGroup = userGroups?.map { it.mapToDomain() }.orEmpty(),
            availableUserSubscriptionTopics = availableUserSubscriptionTopics?.map { it.mapToDomain() }.orEmpty()
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawTopicGroup(
    @Json(name = "id") val id: String? = null,
    @Json(name = "label") val label: String? = null,
) {
    fun mapToDomain(): TopicGroup {
        return TopicGroup(
            id = id.orEmpty(),
            label = label.orEmpty(),
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawSubscriptionTopic(
    @Json(name = "id") val id: String? = null,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "key") val key: String? = null,
    @Json(name = "appTopicName") val appTopicName: String? = null,
    @Json(name = "isOptional") val isOptional: Boolean? = null,
    @Json(name = "alias") val alias: String? = null,
    @Json(name = "imageName") val imageName: String? = null,
    @Json(name = "reference") val reference: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "group") val group: String? = null,
    @Json(name = "hide") val isHidden: Boolean? = null
) {
    fun mapToDomain(): SubscriptionTopic {
        return SubscriptionTopic(
            id = id.orEmpty(),
            displayName = displayName.orEmpty(),
            key = key.orEmpty(),
            appTopicName = appTopicName.orEmpty(),
            isOptional = isOptional ?: true,
            alias = alias.orEmpty(),
            imageName = imageName.orEmpty(),
            reference = reference.orEmpty(),
            description = description.orEmpty(),
            group = group.orEmpty(),
            isHidden = isHidden ?: false
        )
    }
}