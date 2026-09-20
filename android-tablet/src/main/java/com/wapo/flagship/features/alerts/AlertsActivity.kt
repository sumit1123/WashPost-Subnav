package com.wapo.flagship.features.alerts

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.content.notifications.NotificationArticleType
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.notification.AlertManager
import com.wapo.flagship.features.notification.AlertManagerProvider
import com.wapo.flagship.features.notification.AlertsActivityInterface
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.features.notification.state.UiState
import com.wapo.flagship.features.notification.viewmodels.AlertsViewModel
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.settings.SettingsAlertsViewModel
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.shared.fragments.TopBarFragment
import com.wapo.flagship.features.video.VideoActivity
import com.wapo.flagship.push.PushListener
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.wapo.flagship.wrappers.CrashWrapper
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivityAlertsBinding
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import com.washingtonpost.android.volley.toolbox.ImageLoaderProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import rx.Observable

@AndroidEntryPoint
class AlertsActivity :
    BaseActivity(),
    AlertsActivityInterface,
    AlertManagerProvider,
    ImageLoaderProvider {

    private val alertsSettingsViewModel: SettingsAlertsViewModel by viewModels()

    private val alertsViewModel: AlertsViewModel by viewModels()

    private lateinit var binding: ActivityAlertsBinding

    private var notificationsBlockedDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }

        alertsSettingsViewModel.entryPoint = intent?.getStringExtra(AlertsSettings.EntryPoint.TYPE) ?: ""
        onBackPressedDispatcher.addCallback(this) {
            onSupportNavigateUp()
        }
        observeUiState()
        observeCustomizeAlerts()
        alertsViewModel.showAlertsOnly = intent?.getBooleanExtra(SHOW_ALERTS_SETTINGS, false) ?: false
    }

    private fun observeCustomizeAlerts() {
        alertsViewModel.customizeAlertsEvent.observe(this) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                showNotificationsBlockedDialog()
            } else {
                alertsViewModel.showScreen(UiState.AlertSettingsScreen)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean =
        when (alertsViewModel.uiState.value) {
            UiState.AlertSettingsScreen -> {
                menu?.clear()
                false
            }
            UiState.NotificationsScreen -> {
                menuInflater.inflate(R.menu.alerts_menu, menu)
                true
            }
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_alert_settings) {
            alertsViewModel.showCustomizeAlertsEvent()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        if (alertsViewModel.showAlertsOnly) {
            Measurement.setNavigationBehavior(NavigationBehavior.BACK_TO_FRONT)
            finish()
            return false
        }
        if (alertsViewModel.uiState.value == UiState.AlertSettingsScreen) {
            alertsViewModel.showScreen(UiState.NotificationsScreen)
        } else {
            Measurement.setNavigationBehavior(NavigationBehavior.BACK_TO_FRONT)
            finish()
            return false
        }

        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun setTheme(resId: Int) {
        super.setTheme(R.style.WaPo_Settings)
    }

    override val alertsSettings: AlertsSettings
        get() = FlagshipApplication.getInstance().alertsSettings

    override fun openAlertsSettings() {
        TODO("Not yet implemented")
    }

    override fun openNotification(
        notificationUrl: String,
        notificationArticleType: String?,
        notificationTopic: String?,
    ) {
        val intent: Intent
        if (NotificationArticleType.VIDEO.name == notificationArticleType) {
            intent = Intent(this, VideoActivity::class.java)
            intent.putExtra(VideoActivity.VideoInfoUrlExtraParamName, notificationUrl)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(TopBarFragment.BackActivityClassParam, MainActivity::class.java.name)
            startActivity(intent)
        } else {
            val activeTabName = getString(R.string.tab_alerts)
            val builder =
                ArticlesParcel
                    .builder()
                    .setArticleSingleUrl(notificationUrl)
                    .alertOriginated(true)
                    .setNavigationBehavior(NAVIGATION_BEHAVIOR)
                    .setTabName(activeTabName)
                    .setAppSection(activeTabName)
            if (TextUtils.equals(notificationTopic, PushListener.OPINION_TOPIC_KEY)) {
                builder.opinionPushOriginated(true)
            }
            intent = builder.buildIntent(this)
            intent.action = "ACTION_READ"
            startActivity(intent)
        }
    }

    override fun logExtras(str: String) {
        CrashWrapper.logExtras(str)
    }

    override fun sendException(t: Throwable) {
        CrashWrapper.sendException(t)
    }

    override fun getAlertManager(): Observable<out AlertManager?> = getContentManagerObs()

    override fun getImageLoader(): AnimatedImageLoader = FlagshipApplication.getInstance().animatedImageLoader

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                alertsViewModel.uiState.collect {
                    val navController = findNavController(R.id.nav_host_fragment)
                    invalidateOptionsMenu()
                    when (it) {
                        UiState.AlertSettingsScreen -> {
                            navController.navigate(R.id.settingsAlertsFragment)
                            binding.tvToolbarTitle.text =
                                resources.getString(
                                    R.string.alert_settings_title,
                                )
                        }
                        UiState.NotificationsScreen -> {
                            binding.tvToolbarTitle.text = resources.getString(R.string.alerts)
                        }
                    }
                }
            }
        }
    }

    override fun onOneTrustFunctionalityStateChanged(state: Boolean) {
        super.onOneTrustFunctionalityStateChanged(state)
        if(OneTrustHelper.isTargetingEnabled()) {
            alertsSettingsViewModel.updateDailyRead(state)
        }
        alertsSettingsViewModel.setOneTrustConsentState(state)
    }

    override fun onOneTrustTargetingStateChanged(state: Boolean) {
        super.onOneTrustTargetingStateChanged(state)
        if(OneTrustHelper.isFunctionalityEnabled()) {
            alertsSettingsViewModel.updateDailyRead(state)
        }
        alertsSettingsViewModel.setOneTrustConsentState(state)
    }

    private fun showNotificationsBlockedDialog() {
        notificationsBlockedDialog?.dismiss()
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(
            resources.getString(
                com.washingtonpost.android.notifications.R.string.notifications_blocked_title,
            ),
        )
        alertDialog.setMessage(
            resources.getString(
                com.washingtonpost.android.notifications.R.string.notifications_blocked_message,
            ),
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK") { dialog, whichButton ->
                alertDialog.cancel()
            }
        } else {
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL,
                resources.getString(
                    com.washingtonpost.android.notifications.R.string.go_settings_message,
                ),
            ) { dialog, whichButton ->
                launchSystemAppSettings()
            }
            alertDialog.setButton(
                AlertDialog.BUTTON_NEGATIVE,
                resources.getString(com.washingtonpost.android.notifications.R.string.cancelLabel),
            ) { dialog, whichButton ->
                alertDialog.cancel()
            }
        }

        notificationsBlockedDialog = alertDialog

        alertDialog.show()
    }

    private fun launchSystemAppSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName),
            )
        } else {
            startActivity(
                Intent()
                    .setClassName(
                        "com.android.settings",
                        "com.android.settings.Settings\$AppNotificationSettingsActivity",
                    ).putExtra("app_package", this.packageName)
                    .putExtra("app_uid", this.applicationInfo.uid)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),
            )
        }
    }

    companion object {
        @JvmStatic
        val SHOW_ALERTS_SETTINGS = "ShowAlertsSettings"
        val NAVIGATION_BEHAVIOR = "alert"
    }
}
