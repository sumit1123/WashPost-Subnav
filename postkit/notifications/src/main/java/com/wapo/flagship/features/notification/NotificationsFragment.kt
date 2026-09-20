package com.wapo.flagship.features.notification

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.wapo.android.commons.engagement.PageEngagementLifecycleObserver
import com.wapo.android.commons.util.Logger
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.flagship.content.notifications.NotificationModel
import com.wapo.flagship.features.nightmode.NightModeProvider
import com.wapo.flagship.features.notification.state.UiState
import com.wapo.flagship.features.notification.viewmodels.AlertsViewModel
import com.wapo.text.WpLinkAppearanceSpan
import com.washingtonpost.android.notifications.BuildConfig
import com.washingtonpost.android.notifications.R
import com.washingtonpost.android.volley.toolbox.ImageLoaderProvider
import dagger.hilt.android.AndroidEntryPoint
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import javax.inject.Inject

private val PARAM_IS_NOTIFICATIONS_ENABLED = NotificationsFragment::class.java.simpleName + ".isNotifEnabled"

@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    @Inject
    lateinit var loadRenderMetrics: LoadRenderMetrics

    private lateinit var notificationView: NotificationView
    private lateinit var messageView: TextView
    private lateinit var newTopicsView: TextView

    private val NOTIFICATION_ID_GROUP = 1

    //easter egg detectors
    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mShakeDetector: ShakeDetector? = null

    private var contentSubscription: Subscription? = null
    private var nightModeSub: Subscription? = null

    private var hasNotificationsChecked = false
    private var isNotificationsEnabled = false

    private var notificationsBlockedDialog: AlertDialog? = null
    private var isNightMode: Boolean = false

    private val alertsViewModel: AlertsViewModel by activityViewModels()

    companion object {
        val D = BuildConfig.DEBUG
        val DTAG = "[d][notif][fr]"
        val TAG = "NotificationsFragment"
        const val ARG_SHOW_SETTINGS_ON_TOP = "ARG_SHOW_SETTINGS_ON_TOP"
        var navigationBehavior = "alert"
        private const val PAGE_FRONT_ALERTS_TITLE: String = "alerts"
        private const val PAGE_FRONT_ALERTS: String = "front - alerts"
        private const val CONTENT_TYPE_FRONT: String = "front"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.AlertsRenderEvent)
    }

    override fun onAttach(context: Context) {
        if (context !is AlertsActivityInterface) {
            throw IllegalArgumentException("activity of type ${AlertsActivityInterface::class.java.name} is expected")
        }
        if (context !is AlertManagerProvider) {
            throw IllegalArgumentException("activity of type ${AlertManagerProvider::class.java.name} is expected")
        }
        if (context !is ImageLoaderProvider) {
            throw IllegalArgumentException("activity of type ${ImageLoaderProvider::class.java.name} is expected")
        }

        super.onAttach(context)
    }

    private fun markAsRead() {
        Observable.combineLatest(
                getAlertManagerObs(),
                notificationView.getItemsOnScreen()
        ) { alertManager, notifications ->
            Pair(alertManager, notifications)
        }
                .flatMap { pair ->
                    val (alertManager, notifications) = pair
                    alertManager.readNotifications(notifications.map { it.notificationData })
                }
                .run {
                    this
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    private val alertsSettings: AlertsSettings
        get() = (activity as AlertsActivityInterface).alertsSettings

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        notificationView = view.findViewById<NotificationView>(R.id.notification_view)
        messageView = view.findViewById<TextView>(R.id.message)
        messageView.movementMethod = LinkMovementMethod.getInstance()

        newTopicsView = view.findViewById<TextView>(R.id.new_topics_message)

        if (D) {
            // ShakeDetector initialization for auto-opt easter egg
            /**
             * Disable auto opt easter egg until android endpoint is completed
             *
             * openAutoOptEasterEgg()
             */
        }

        notificationView.setNotificationClickListener(object : NotificationClickListener {
            override fun onNotificationClick(notification: NotificationModel) {
                getAlertManagerObs()
                        .flatMap { alertManager ->
                            alertManager.readNotifications(listOf(notification.notificationData))
                        }
                        .onErrorResumeNext { Observable.empty() }
                        .subscribeOn(Schedulers.io())
                        .subscribe()

                val notificationManager = activity?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notification.notificationData.notifId?.let { id ->
                    notificationManager?.cancel(Integer.parseInt(id))
                }
                val storyUrl = notification.notificationData.storyUrl
                if (storyUrl != null) {
                    openNotification(storyUrl, notification.notificationData.notifArticleType, notification.notificationData.type)
                }
            }
        })

        notificationView.swipeListener = object : NotificationSwipeListener {
            override fun onNotificationSwiped(notification: NotificationModel) {
                getAlertManagerObs()
                        .flatMap { alertManager -> alertManager.deleteNotification(notification.notificationData) }
                        .run {
                            this
                        }
                        .subscribeOn(Schedulers.io())
                        .subscribe()
            }

        }

        notificationView.setFooterClickListener(object : NotificationFooterClickListener {
            override fun onNotificationFooterClicked() {
                alertsViewModel.showCustomizeAlertsEvent()
            }
        })

        newTopicsView.setOnClickListener {
            if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                launchAppSettings()
            }
        }

        notificationView.setShowSettingsOnTop(isShowSettingsOnTop())

        if (savedInstanceState != null) {
            hasNotificationsChecked = savedInstanceState.containsKey(PARAM_IS_NOTIFICATIONS_ENABLED)
            if (hasNotificationsChecked) {
                isNotificationsEnabled = savedInstanceState.getBoolean(PARAM_IS_NOTIFICATIONS_ENABLED, false)
            }
        }

        observePageEngagement()

        return view
    }

    private fun observePageEngagement() {
        lifecycle.addObserver(
            PageEngagementLifecycleObserver(
                pageName = PAGE_FRONT_ALERTS,
                tabName = PAGE_FRONT_ALERTS_TITLE,
                contentType = CONTENT_TYPE_FRONT
            )
        )
    }

    override fun onStart() {
        super.onStart()

        if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
            showNotificationsBlockedScreen()
            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.AlertsRenderEvent)
            return
        }

        showContentScreen()
        if (alertsSettings.isFirstLaunch) {
            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.AlertsRenderEvent)
            alertsSettings.isFirstLaunch = false
            launchAppSettings()
            return
        }
        val alertCount = alertsSettings.alertLaunchCount
        if (alertCount <= 3) {
            alertsSettings.alertLaunchCount = (alertCount + 1);
            newTopicsView.visibility = View.VISIBLE
        } else {
            newTopicsView.visibility = View.GONE
        }

        showContentScreen()
        loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.AlertsRenderEvent)
    }

    override fun onStop() {
        super.onStop()

        notificationsBlockedDialog?.dismiss()
        notificationsBlockedDialog = null
    }

    override fun onResume() {
        super.onResume()

        val mNotificationManager = activity?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(NOTIFICATION_ID_GROUP)
        alertsSettings.trackAlertsTabPageView(navigationBehavior)

        if (D) {
            mSensorManager?.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        contentSubscription = getAlertManagerObs()
                .flatMap { alertManager ->
                    alertManager.recentNotifications
                            .flatMap {
                                Observable.from(it)
                                        .flatMap {
                                            alertManager.getNotificationModel(it)
                                        }
                                        .toList()
                            }
                }
                .run {
                    this
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ data ->
                    val activity = activity ?: return@subscribe
                    notificationView.setItems(data.filterNotNull(), (activity as ImageLoaderProvider).imageLoader)
                }, { t ->
                    (activity as AlertsActivityInterface).sendException(t)
                })

        val notificationEnabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        if (notificationEnabled) {
            if (hasNotificationsChecked && !isNotificationsEnabled) {
                alertsSettings.trackEnableNotifications(AlertsSettings.EntryPoint.PROMPT.trackingName, true)
            }
        } else {
            clearAllNotifications()
        }

        checkNightMode()
    }

    override fun onPause() {
        if (D) {
            mSensorManager?.unregisterListener(mShakeDetector)
        }
        super.onPause()
        //    handleDisabledNotifications() //TODO need it?

        contentSubscription?.unsubscribe()
        contentSubscription = null

        val mNotificationManager = activity?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        mNotificationManager?.cancelAll()

        if (isRemoving || activity == null || requireActivity().isFinishing) {
            markAsRead()
        }

        isNotificationsEnabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        hasNotificationsChecked = true

        nightModeSub?.unsubscribe()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState?.putBoolean(PARAM_IS_NOTIFICATIONS_ENABLED, isNotificationsEnabled)
    }


    private fun openNotification(notificationUrl: String, notifArticleType: String?, notificationTopic: String?) {
        val al = activity as AlertsActivityInterface
        al.openNotification(notificationUrl, notifArticleType, notificationTopic)
    }

    private fun showNotificationsBlockedScreen() {
        notificationView.visibility = View.GONE
        messageView.visibility = View.VISIBLE
        messageView.text = makeClickable(R.string.turn_on_notifications) {
            launchSystemAppSettings()
        }
        newTopicsView.visibility = View.GONE
    }

    private fun showContentScreen() {
        notificationView.visibility = View.VISIBLE
        messageView.visibility = View.GONE
        messageView.setOnClickListener(null)
    }

    private fun launchSystemAppSettings() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            )
        } else {
            startActivity(
                Intent()
                    .setClassName("com.android.settings", "com.android.settings.Settings\$AppNotificationSettingsActivity")
                    .putExtra("app_package", requireContext().packageName)
                    .putExtra("app_uid", requireContext().applicationInfo.uid)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            )
        }
    }

    private fun launchAppSettings() {
        //Set the alert launch count to 4 so that the banner doesn't appear anymore.
        alertsSettings.alertLaunchCount = 4
        alertsViewModel.showScreen(UiState.AlertSettingsScreen)
    }

    private fun makeClickable(resId: Int, callback: () -> Unit): CharSequence {
        val ssb = SpannableStringBuilder()
        val text = getText(resId)
        ssb.append(text)
        if (text is Spanned) {
            val clickable = object : WpLinkAppearanceSpan(context, true) {
                override fun onClick(widget: View) {
                    callback()
                }
            }

            text.getSpans(0, text.length, UnderlineSpan::class.java).forEach {
                ssb.setSpan(
                        clickable,
                        text.getSpanStart(it), text.getSpanEnd(it),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
        }

        return ssb
    }

    private fun clearAllNotifications() {
        getAlertManagerObs()
                .flatMap(AlertManager::clearAllNotifications)
                .onErrorResumeNext {
                    Logger.e(TAG, "an error while deleting notifications", it)
                    Observable.empty()
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    fun getAlertManagerObs(): rx.Observable<out AlertManager> {
        return (activity as AlertManagerProvider).alertManager
    }

    private fun checkNightMode() {
        try {
            nightModeSub = (activity as NightModeProvider)
                    .nightModeManager
                    .getNightModeStatus()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { setUpNightMode(it) },
                            { error -> Logger.e(TAG, "Night mode provider error", error) }
                    )
        } catch (e: Exception) {
            Logger.e(TAG, "Night mode provider error", e)
        }

    }

    private fun setUpNightMode(enabled: Boolean) {
        notificationView.setNightMode(enabled)
        messageView.setTextColor(requireContext().resources.getColor(getTextColorRes(enabled)))
        newTopicsView.setTextColor(requireContext().resources.getColor(getTextColorRes(enabled)))
        newTopicsView.setBackgroundColor(ContextCompat.getColor(requireContext(), getNotificationItemBgColorRes(enabled, false)))
    }

    fun showSettingsOnTop(showOnTop: Boolean) {
        val args = arguments ?: Bundle()
        args.putBoolean(ARG_SHOW_SETTINGS_ON_TOP, showOnTop)
        arguments = args
    }

    fun isShowSettingsOnTop() : Boolean = arguments?.getBoolean(ARG_SHOW_SETTINGS_ON_TOP, false) ?: false
}
