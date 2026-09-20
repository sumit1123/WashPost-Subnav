/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.topicfollow.states

sealed class TopicFollowUiState {
    object Loading : TopicFollowUiState()
    object Register : TopicFollowUiState()
    object Follow : TopicFollowUiState()
}

sealed class FollowingUiState {
    object Following : FollowingUiState()
    object NotFollowing : FollowingUiState()
}

sealed class NewsletterUiState {
    object Enabled : NewsletterUiState()
    object Disabled : NewsletterUiState()
}

sealed class NotificationUiState {
    object Enabled : NotificationUiState()
    object Disabled : NotificationUiState()
}
