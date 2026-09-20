// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.topicfollow.viewmodels

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.newsletter.domain.models.NewslettersKey
import com.wapo.flagship.features.newsletter.repo.NewslettersRepository
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.features.preferencesapi.models.Followable
import com.wapo.flagship.features.preferencesapi.models.Newsletter
import com.wapo.flagship.features.preferencesapi.models.Notification
import com.wapo.flagship.features.preferencesapi.repo.ContentPacksRepo
import com.wapo.flagship.features.topicfollow.events.TopicFollowEvent
import com.wapo.flagship.features.topicfollow.states.FollowingUiState
import com.wapo.flagship.features.topicfollow.states.NewsletterUiState
import com.wapo.flagship.features.topicfollow.states.NotificationUiState
import com.wapo.flagship.features.topicfollow.states.TopicFollowUiState
import com.wapo.flagship.util.UtilsKt
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.util.PaywallConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicFollowBottomSheetViewModel
    @Inject
    constructor(
        private val contentPacksRepo: ContentPacksRepo,
        private val newslettersRepository: NewslettersRepository,
    ) : ViewModel() {
        private val _topicFollowEvent = LiveEvent<TopicFollowEvent>()
        val topicFollowEvent: LiveData<TopicFollowEvent> = _topicFollowEvent

        fun dispatchTopicFollowEvent(event: TopicFollowEvent) {
            _topicFollowEvent.value = event
        }

        private val _mainContentUiState =
            MutableLiveData<TopicFollowUiState>(
                TopicFollowUiState.Loading,
            )
        var mainContentUiState: LiveData<TopicFollowUiState> = _mainContentUiState

        private val _followingUiState = MediatorLiveData<FollowingUiState>(FollowingUiState.Following)
        var followingUiState: LiveData<FollowingUiState> = _followingUiState

        private val _newsletterUiState = MutableLiveData<NewsletterUiState>(NewsletterUiState.Disabled)
        var newsletterUiState: LiveData<NewsletterUiState> = _newsletterUiState

        private val _notificationUiState =
            MutableLiveData<NotificationUiState>(
                NotificationUiState.Disabled,
            )
        var notificationUiState: LiveData<NotificationUiState> = _notificationUiState

        private lateinit var contentPackId: String
        lateinit var followable: Followable

        var following: Boolean = true

        var newsletter: Newsletter? = null
        private var newsletterEnabled = false

        var notification: Notification? = null
        private var notificationsEnabled = false

        private var trackingPageName: String = ""

        private val trackingWallName: String = "topic_follow"

        private val alertSettings
            get() = FlagshipApplication.getInstance().alertsSettings

        private val isSignedIn
            get() = PaywallService.getInstance()?.isWpUserLoggedIn ?: false

        fun initialize(
            contentPackId: String,
            followable: Followable,
            context: Context,
            areSystemNotificationsEnabled: Boolean,
            trackingPageName: String,
        ) {
            this.contentPackId = contentPackId
            this.followable = followable
            this.newsletter = followable.newsletters?.firstOrNull()
            this.notification = followable.notifications?.firstOrNull()
            this.trackingPageName = trackingPageName

            // If the user was not following before opening the sheet, we want to toggle newsletters and notifications on
            val didLaunchToggleFollow = !contentPacksRepo.isTopicFollowed(context, contentPackId)
            updateTopicsFollowed(context)

            newsletter?.let {
                newsletterEnabled = newslettersRepository.isNewsletterEnrolled(it.id) || didLaunchToggleFollow
                val uiState =
                    if (newsletterEnabled) {
                        NewsletterUiState.Enabled
                    } else {
                        NewsletterUiState.Disabled
                    }
                _newsletterUiState.postValue(uiState)

                if (didLaunchToggleFollow) {
                    updateNewsletterEnrollment()
                }
            }

            notification?.let { notification ->
                if (areSystemNotificationsEnabled(context)) {
                    notificationsEnabled =
                        alertSettings.getAlertsTopicsList().firstOrNull { it.topic.topicKey == notification.id }?.isEnabled ?: false ||
                        didLaunchToggleFollow
                    val uiState =
                        if (notificationsEnabled) {
                            NotificationUiState.Enabled
                        } else {
                            NotificationUiState.Disabled
                        }
                    _notificationUiState.postValue(uiState)

                    if (didLaunchToggleFollow) {
                        updateNotifications(context)
                    }
                }
            }
        }

        private fun areSystemNotificationsEnabled(context: Context): Boolean =
            NotificationManagerCompat.from(context).areNotificationsEnabled()

        fun updateMainContentUiState(
            context: Context,
            returningFromSignIn: Boolean,
        ) {
            if (PaywallService.getInstance().isWpUserLoggedIn) {
                _mainContentUiState.postValue(TopicFollowUiState.Follow)
                Measurement.trackOnboardingSeen(
                    Measurement.TOPIC_FOLLOW,
                    Measurement.PAGE_FRONT_MY_POST_TOPICS,
                )
                if (returningFromSignIn) {
                    updateTopicsFollowed(context)
                    updateNewsletterEnrollment()
                    updateNotifications(context)
                }
            } else {
                _mainContentUiState.postValue(TopicFollowUiState.Register)
            }
        }

        fun startSignIn(
            fragmentManager: FragmentManager?,
            isSignUp: Boolean,
        ) {
            val authIntent = AuthIntentBuilder().addIsSignUp(isSignUp).build()
            if (isSignUp) {
                PaywallService.getConnector().showSignUpScreen(
                    fragmentManager,
                    authIntent,
                    trackingWallName,
                    PaywallConstants.WallType.SOFTWALL,
                )
            } else {
                PaywallService.getConnector().showSignInScreen(
                    fragmentManager,
                    authIntent,
                    trackingWallName,
                    PaywallConstants.WallType.SOFTWALL,
                    true,
                    null
                )
            }
        }

        fun trackSoftwall() {
            FlagshipApplication.getInstance().paywallOmniture.trackPaywallBlockOverlay(
                PaywallConstants.WallType.SOFTWALL,
                trackingWallName,
            )
        }

        fun toggleFollowing(context: Context) {
            following = !following
            updateTopicsFollowed(context)

            if (following) {
                _followingUiState.postValue(FollowingUiState.Following)
                if (!newsletterEnabled) {
                    toggleNewsletter()
                }
                if (!notificationsEnabled) {
                    toggleNotifications(context)
                }
            } else {
                _followingUiState.postValue(FollowingUiState.NotFollowing)
            }
        }

        private fun trackContentPackSelection() {
            val contentPackName =
                contentPacksRepo.getContentPackForId(contentPackId)?.heading?.let {
                    UtilsKt.toAnalyticsSnakeCase(it)
                } ?: ""
            val miscellany = "${Measurement.PROFILE_PREFERENCE_CONTENT_PACK};$contentPackName;${if (following) {
                "selected"
            } else {
                "deselected"
            }};${Measurement.TOPIC_FOLLOW}"
            Measurement.trackOnboardingClick(miscellany, trackingPageName)
        }

        fun toggleNewsletter() {
            newsletterEnabled = !newsletterEnabled
            updateNewsletterEnrollment()

            if (newsletterEnabled) {
                _newsletterUiState.postValue(NewsletterUiState.Enabled)
            } else {
                _newsletterUiState.postValue(NewsletterUiState.Disabled)
            }
        }

        private fun trackNewsletterEnrollment() {
            newsletter?.name?.let { name ->
                Measurement.trackNewsletterEnroll(
                    AlertsSettings.EntryPoint.INLINE_TOGGLE.trackingName,
                    trackingPageName,
                    UtilsKt.toAnalyticsSnakeCase(name).replace("_newsletter", ""),
                    newsletterEnabled,
                )
            }
        }

        private fun updateTopicsFollowed(context: Context) {
            if (!isSignedIn) {
                return
            }

            viewModelScope.launch {
                contentPacksRepo.updateTopicsFollowed(context, following, contentPackId)
                trackContentPackSelection()
            }
        }

        private fun updateNewsletterEnrollment() {
            if (!isSignedIn) {
                return
            }

            viewModelScope.launch {
                newsletter?.id?.let {
                    if (newsletterEnabled) {
                        newslettersRepository.enrollNewsletter(
                            listOf(NewslettersKey.Id(it)),
                            CARTA_PROFILE_METHOD,
                            CARTA_PROFILE_LOCATION,
                            trackingPageName,
                        )
                    } else {
                        newslettersRepository.unenrollNewsletter(
                            listOf(it),
                            CARTA_PROFILE_METHOD,
                            CARTA_PROFILE_LOCATION,
                            trackingPageName,
                        )
                    }
                    trackNewsletterEnrollment()
                }
            }
        }

        fun toggleNotifications(context: Context) {
            if (!areSystemNotificationsEnabled(context)) {
                _notificationUiState.value = NotificationUiState.Disabled
                return
            }

            notificationsEnabled = !notificationsEnabled
            updateNotifications(context)
            if (notificationsEnabled) {
                _notificationUiState.postValue(NotificationUiState.Enabled)
            } else {
                _notificationUiState.postValue(NotificationUiState.Disabled)
            }
        }

        private fun updateNotifications(context: Context) {
            if (!isSignedIn) {
                return
            }

            if (!areSystemNotificationsEnabled(context)) {
                _notificationUiState.value = NotificationUiState.Disabled
                return
            }

            notification?.id?.let {
                alertSettings.enableAlertsTopic(it, notificationsEnabled)
                trackNotificationEnrollment()
            }
        }

        private fun trackNotificationEnrollment() {
            notification?.id?.let {
                Measurement.trackAlertTopicEnroll(
                    it,
                    Measurement.TOPIC_FOLLOW,
                    trackingPageName,
                    notificationsEnabled,
                )
            }
        }

        companion object {
            const val CARTA_PROFILE_METHOD = "TOPIC_FOLLOW_APP"
            const val CARTA_PROFILE_LOCATION = "TOPIC_FOLLOW_APP"
        }
    }
