/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.topicfollow.events

sealed class TopicFollowEvent {
    class SignInStarted(val isSignUp: Boolean) : TopicFollowEvent()
    object FollowToggled : TopicFollowEvent()
    object NewsletterToggled : TopicFollowEvent()
    object NotificationToggled : TopicFollowEvent()
}
