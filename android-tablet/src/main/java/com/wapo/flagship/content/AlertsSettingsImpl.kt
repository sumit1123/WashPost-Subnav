package com.wapo.flagship.content

import android.os.Looper
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.push.MigrationHelper
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.content.notifications.SubscriptionTopicModel
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.features.notification.AlertsSettings.AlertGroup
import com.wapo.flagship.features.notification.AlertsSettings.AlertTopicInfo
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.features.preferencesapi.state.TopicToggleBuffer
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.PushConfigStub
import com.washingtonpost.android.paywall.PaywallService
import rx.Observable
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import java.util.concurrent.Executors

internal class AlertsSettingsImpl : AlertsSettings {
    private val alertsStatus = PublishSubject.create<Boolean>()
    private val alertTopicsUpdates = PublishSubject.create<Long>()
    private val migrationHelper =
        object : MigrationHelper() {
            override fun updateAlertsTopic(
                key: String,
                isEnabled: Boolean,
            ) {
                enableAlertsTopic(key, isEnabled)
            }

            override fun isTopicEnabled(key: String): Boolean = AppContext.isTopicEnabled(key)

            override fun remoteLog(eventLogBuilder: EventLog.Builder?) {
                FlagshipApplication.getInstance().applicationContext?.let { context ->
                    eventLogBuilder?.let { builder ->
                        RemoteLog.p(context, builder.build())
                    }
                }
            }
        }

    private val pushConfigStub: PushConfigStub get() = ConfigManager.getInstance().config.airshipPushConfig

    private fun isUserSignedIn(): Boolean {
        val service = PaywallService.getInstance()
        return service?.isWpUserLoggedIn == true
    }

    private val executor =
        Executors.newSingleThreadExecutor { r ->
            object : Thread(r, "th-alertsStngs") {
                override fun run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                    super.run()
                }
            }
        }
    private val scheduler = Schedulers.from(executor)

    override var isFirstLaunch: Boolean
        get() = AppContext.isFirstAlertsLaunch()
        set(isFirst) = AppContext.setFirstAlertsLaunch(isFirst)

    override var alertLaunchCount: Int
        get() = AppContext.getAlertsLaunchCount()
        set(alertCount) = AppContext.setAlertsLaunchCount(alertCount)

    override fun trackAlertsTabPageView(navigationBehavior: String) {
        Measurement.trackAlertsPageView(navigationBehavior)
    }

    override fun trackEnableNotifications(
        entryPoint: String,
        isEnabled: Boolean,
    ) {
        Measurement.trackAlertTopicEnroll("Breaking News", entryPoint, isEnabled)
    }

    override fun enableAlertsTopic(
        topicName: String,
        isEnabled: Boolean,
    ) {
        enableAlertsTopics(mapOf(topicName to isEnabled))
        PreferencesSyncCoordinator.markDirty(PreferencesSyncCoordinator.TOPIC_NOTIFICATIONS)
        TopicToggleBuffer.onLocalTopicChanged()
    }

    override fun enableAlertsTopics(topics: Map<String, Boolean>) {
        topics.forEach {
            val (name, isSelected) = it
            AppContext.changeTopicEnabled(name, isSelected)
        }

        notifyAlertsTopicListChanged()
    }

    private fun notifyAlertsTopicListChanged() {
        if (isMainThread()) {
            executor.execute {
                notifyAlertsTopicListChanged()
            }
            return
        }

        alertTopicsUpdates.onNext(System.currentTimeMillis())
    }

    override fun getAlertsTopics(): Observable<List<AlertTopicInfo>> {
        val topicUpdates = alertTopicsUpdates.map { getAlertsTopicsList() }

        return Observable
            .fromCallable {
                getAlertsTopicsList()
            }.subscribeOn(scheduler)
            .concatWith(topicUpdates)
    }

    override fun getAlertsTopicsList(): List<AlertTopicInfo> {
        val subscriptionTopics = pushConfigStub.availableSubscriptionTopics
            .filter {
                !it.isHidden
            }.map {
                AlertTopicInfo(
                    SubscriptionTopicModel(
                        it.displayName,
                        it.key,
                        it.alias,
                        it.imageName,
                        it.group,
                        it.description,
                    ),
                    AppContext.isTopicEnabled(it.key),
                )
            }
        val userTopics = pushConfigStub.availableUserSubscriptionTopics
            ?.map {
                AlertTopicInfo(
                    SubscriptionTopicModel(
                        it.displayName,
                        it.key,
                        it.alias,
                        it.imageName,
                        it.group,
                        it.description,
                    ),
                    AppContext.isTopicEnabled(it.key),
                )
            }.orEmpty()
        return if (isUserSignedIn()) {
            subscriptionTopics + userTopics
        } else {
            subscriptionTopics
        }
    }

    override fun getAlertsGroupList(): List<AlertGroup> {
        val topicGroups = pushConfigStub.topicGroups
            .map {
                AlertGroup(it.id, it.label)
            }
        val userGroups = pushConfigStub.userGroup
            ?.map {
                AlertGroup(it.id, it.label)
            }.orEmpty()
        return if (isUserSignedIn()) {
            topicGroups + userGroups
        } else {
            topicGroups
        }
    }

    override fun syncTopicIfRequired(
        key: String,
        isEnabled: Boolean,
    ) {
        migrationHelper.syncSegment(key, isEnabled, pushConfigStub)
    }

    override fun migrateSegments() {
        migrationHelper.migrateSyncSegments(pushConfigStub)
    }

    private fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()
}
