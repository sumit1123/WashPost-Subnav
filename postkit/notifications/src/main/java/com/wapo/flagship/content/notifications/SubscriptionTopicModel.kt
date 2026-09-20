/* Copyright (c) 2018 The Washington Post. All rights reserved. */

package com.wapo.flagship.content.notifications

data class SubscriptionTopicModel(
        val displayName: String,
        val topicKey: String,
        val alias: String,
        val imageName: String,
        val group: String?,
        val description: String = ""
)