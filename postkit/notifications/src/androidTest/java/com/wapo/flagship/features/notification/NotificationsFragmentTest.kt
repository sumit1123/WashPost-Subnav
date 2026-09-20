package com.wapo.flagship.features.notification

import androidx.test.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import com.wapo.flagship.content.notifications.NotificationData
import com.wapo.flagship.content.notifications.NotificationModel
import com.washingtonpost.android.notifications.test.R
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rx.Observable
import rx.subjects.BehaviorSubject
import java.util.*

@Ignore("Failing test")
@RunWith(AndroidJUnit4::class)
class NotificationsFragmentTest {

    @Rule @JvmField var rule: ActivityTestRule<NotificationTestActivity> = ActivityTestRule(NotificationTestActivity::class.java)

    @Test
    fun basic() {
        val testAlertManager = TestAlertManager()
        val activity = rule.activity

        activity.alertManager = testAlertManager
        activity._alertsSettings = TestAlertSettings()

        val notificationsFragment = NotificationsFragment()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.root, notificationsFragment)
                    .commitNow()
        }

        Thread.sleep(1000)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(notificationsFragment.notificationView().childCount, 3 + 2) // 1 footer and 1 header

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            notificationsFragment.notificationView().getChildAt(1).performClick()
            notificationsFragment.notificationView().getChildAt(2).performClick()
        }

        Thread.sleep(1000)

        activity.clickedUrl.contains("url3")
        activity.clickedUrl.contains("url2")

        assertFalse(testAlertManager.notifications[0].isRead)
        assertTrue(testAlertManager.notifications[1].isRead)
        assertTrue(testAlertManager.notifications[2].isRead)
    }

    @Test
    fun checkFirstLaunch() {
        val testAlertManager = TestAlertManager()
        val activity = rule.activity

        activity.alertManager = testAlertManager
        activity._alertsSettings = TestAlertSettings().apply { _firstLaunch = true }

        val notificationsFragment = NotificationsFragment()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.root, notificationsFragment)
                    .commitNow()
        }

        Thread.sleep(1000)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertTrue(activity.openAlertSettingsCalled)
    }

    @Test
    fun checkDisabled() {
        val testAlertManager = TestAlertManager()
        val activity = rule.activity

        activity.alertManager = testAlertManager
        activity._alertsSettings = TestAlertSettings().apply { _alertsEnabled = false }

        val notificationsFragment = NotificationsFragment()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity
                    .supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.root, notificationsFragment)
                    .commitNow()
        }

        Thread.sleep(1000)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(notificationsFragment.messageView().visibility, View.VISIBLE)
        assertNotEquals(notificationsFragment.notificationView().visibility, View.VISIBLE)
    }

    class TestAlertManager : AlertManager {

        val notifications = mutableListOf(
                makeNotification(1),
                makeNotification(2),
                makeNotification(3).apply { isRead = true }
        )

        val notifSubj = BehaviorSubject.create<MutableList<NotificationData>>().apply { onNext(notifications) }

        override fun getRecentNotifications(): Observable<MutableList<NotificationData>> {
            return notifSubj.asObservable()
        }

        override fun getNotificationModel(notificationData: NotificationData): Observable<NotificationModel> {
            return Observable.just(model(notificationData))
        }

        override fun readNotifications(notifications: MutableList<NotificationData>): Observable<Void> {
            return Observable.fromCallable {
                this.notifications
                        .filter { origin ->
                            notifications.find { it.id == origin.id } != null
                        }
                        .forEach { it.isRead = true }

                notifSubj.onNext(this.notifications)

                return@fromCallable null
            }
        }

        override fun deleteNotification(notification: NotificationData): Observable<Void> {
            return Observable.fromCallable {
                this.notifications.removeAll { it.id == notification.id }
                notifSubj.onNext(this.notifications)
                return@fromCallable null
            }
        }

        override fun clearAllNotifications(): Observable<Void> {
            return Observable.fromCallable {
                notifications.clear()
                notifSubj.onNext(this.notifications)
                return@fromCallable null
            }
        }

        private fun model(notificationData: NotificationData): NotificationModel {
            val date = Date(notificationData.timestamp!!.toLong())
            return NotificationModel(notificationData.headline.orEmpty(), date, "test-url", notificationData, "image-url")
        }

        private fun makeNotification(id: Int): NotificationData {
            return NotificationData("alert$id").apply {
                type = ""
                this.id = id
                headline = "headline$id"
                kicker = "kicker$id"
                timestamp = "150242851152$id"
                notifId = "$id"
                storyUrl = "url$id"
            }
        }
    }

    class TestAlertSettings : AlertsSettings {
        var _firstLaunch = false
        var _alertsEnabled = true
        var _alertLaunchCount = 0;

        override var alertLaunchCount: Int
            get() = _alertLaunchCount
            set(value) {}

        override fun trackAlertsTabPageView(navigationBehavior: String) {

        }

        override var isFirstLaunch: Boolean
            get() = _firstLaunch
            set(value) {}

        override fun trackEnableNotifications(entryPoint: String, isEnabled: Boolean) {

        }

        override fun enableAlertsTopic(topicName: String, isEnabled: Boolean) {
        }

        override fun enableAlertsTopics(topics: Map<String, Boolean>) {
        }

        override fun getAlertsTopics(): Observable<List<AlertsSettings.AlertTopicInfo>> {
            return Observable.empty()
        }

        override fun getAlertsTopicsList(): List<AlertsSettings.AlertTopicInfo> {
            return emptyList()
        }

        override fun getAlertsGroupList(): List<AlertsSettings.AlertGroup>? {
            return emptyList()
        }

        override fun syncTopicIfRequired(key: String, isEnabled: Boolean) {
        }

        override fun migrateSegments() {
            
        }

    }

    private fun NotificationsFragment.notificationView() = view!!.findViewById(R.id.notification_view) as NotificationView
    private fun NotificationsFragment.messageView() = view!!.findViewById<TextView>(androidx.appcompat.R.id.message)
}