package com.wapo.flagship.features.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.wapo.android.commons.engagement.PageEngagementLifecycleObserver
import com.wapo.android.commons.extensions.toUri
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.URLParser
import com.wapo.flagship.Utils
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.features.newsletter.domain.models.NewslettersKey
import com.wapo.flagship.features.onetrust.OneTrustHelper
import com.wapo.flagship.features.sections.ConnectivityActivity
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.viewmodel.GlobalBannerViewModel
import com.wapo.flagship.sdk.iterable.IterablePlugin
import com.wapo.flagship.sdk.iterable.viewmodels.IterableActivityViewModel
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.util.ShakeDetector
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.zendesk.model.ZendeskDestinations
import com.wapo.zendesk.viewmodel.Action
import com.wapo.zendesk.viewmodel.ZendeskDestinationViewModel
import com.wapo.zendesk.viewmodel.ZendeskSharedViewModel
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.databinding.ActivitySettingsBinding
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.util.PaywallConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class SettingsActivity :
    BaseActivity(),
    ConnectivityActivity,
    IterablePlugin.IterableActivity {

    private lateinit var binding: ActivitySettingsBinding
    private val sharedViewModel: ZendeskSharedViewModel by viewModels()
    private val zendeskDestinationViewModel: ZendeskDestinationViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val iterableActivityViewModel: IterableActivityViewModel by viewModels()
    private lateinit var iterablePlugin: IterablePlugin
    private val alertsSettingsViewModel: SettingsAlertsViewModel by viewModels()
    private val globalBannerViewModel: GlobalBannerViewModel by viewModels()

    private val config get() = ConfigManager.getInstance().config

    // Shake detector for opening debug panel (debug builds only)
    private var shakeDetector: ShakeDetector? = null

    private val navController: NavController? by lazy {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navHostFragment?.navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iterablePlugin =
            IterablePlugin(this, iterableActivityViewModel, getPaywallSheetHelper()).also {
                lifecycle.addObserver(it)
            }
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowTitleEnabled(false)
        }
        if (savedInstanceState == null) {
            //  Handle internal navigation only on first launch
            handleRouting(intent)
        }

        listenToZendeskActions()
        observePageEngagement()
        observeForFormSubmissionAction()
        observeBannerMessage()
        setupDeveloperModetriggers()
        observerBannerMessage()
    }

    private fun observerBannerMessage() {
        globalBannerViewModel.bannerEvent.observe(this) {
            when(it) {
                is BannerEvent.BannerClicked -> {
                    iterablePlugin.executeBannerAction(it.message, null)
                }
                is BannerEvent.BannerDismissed -> {
                    iterablePlugin.dismissBanner(it.message, null)
                }
                is BannerEvent.ImpressionEvent -> {
                    iterablePlugin.handleBannerLifecycleEvent(event = it.event)
                }
                else -> {

                }
            }
        }
    }

    private fun observeBannerMessage() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    iterableActivityViewModel
                        .getBannerFlowForPlacement(IamMessageType.SETTINGS_PLAN)
                        .collect { banner -> settingsViewModel.getWpWeeklyPlacement(banner) }
                }
            }
        }
    }

    private fun setupDeveloperModetriggers() {
        // Only enable shake-to-open/long press debug panel in debug/beta builds
        if (AppContextUtils.isDebuggableBuild()) {
            shakeDetector = ShakeDetector(this) { openDebugPanel() }
            binding.toolbar.getChildAt(1)?.setOnLongClickListener {
                openDebugPanel()
                true
            }
        }
    }

    private fun openDebugPanel() {
        // Check if Developer Mode is already enabled
        val isDeveloperModeAlreadyEnabled = AppPreferences.isDeveloperModeEnabled()
        if (isDeveloperModeAlreadyEnabled) {
            // Already enabled, just show a message
            Toast.makeText(this, "Developer Mode already enabled", Toast.LENGTH_SHORT).show()
            return
        }

        // Show Developer Mode and Debug Panel when shake is detected
        val settingsFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.childFragmentManager?.fragments?.firstOrNull { it is SettingsFragment } as? SettingsFragment
        settingsFragment?.let {
            val prefDeveloperMode = it.preferenceScreen?.findPreference<androidx.preference.SwitchPreferenceCompat>(
                AppPreferences.PREF_DEVELOPER_MODE
            )
            prefDeveloperMode?.isVisible = true
            prefDeveloperMode?.isChecked = true

            // Show the inline Debug Panel category
            val prefDebugPanelCategory = it.preferenceScreen?.findPreference<androidx.preference.PreferenceCategory>(
                AppPreferences.PREF_DEBUG_PANEL_CATEGORY
            )
            prefDebugPanelCategory?.isVisible = true

            AppPreferences.setDeveloperModeEnabled(true)
        }

        Toast.makeText(this, "Developer Mode Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRouting(intent)
    }

    /**
     * [DeepLinksProcessor] and [com.wapo.flagship.IntentHelper] are only opening the SettingsActivity.
     * This method handles internal settings navigation.
     */
    private fun handleRouting(intent: Intent) {
        val urlParser = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> intent.getParcelableExtra(
                DeepLinksProcessor.ARG_URL_PARSER,
                URLParser::class.java
            )

            else -> intent.getParcelableExtra(DeepLinksProcessor.ARG_URL_PARSER)
        }
        when (urlParser?.getPath()) {
            "/settings/alerts" -> {
                val navController = this.navController ?: return
                navController.navigate(
                    R.id.settingsAlertsFragment,
                    urlParser.getParametersBundle(),
                    NavOptions.Builder()
                        .setPopUpTo(navController.graph.startDestinationId, true)
                        .build()
                )
            }

            "/settings/newsletters" -> {
                val parametersBundle = urlParser.getParametersBundle()
                val enroll = parametersBundle?.getString("enroll")
                if (!enroll.isNullOrEmpty()) {
                    settingsViewModel.enrollNewsletter(listOf(NewslettersKey.List(enroll)))
                }

                if (PaywallService.getInstance().loggedInUser != null) {
                    val url = config.newslettersAndEmailAlertsUrl.toUri()
                        .buildUpon()
                        .appendPath("manage")
                        .toString()
                    Utils.startWebActivity(url, this)
                } else {
                    PaywallService.getConnector().showSignInScreen(
                        supportFragmentManager,
                        AuthIntentBuilder().build(),
                        null,
                        PaywallConstants.WallType.SETTINGS_PAYWALL,
                        false,
                        null
                    )
                }
            }
        }
    }

    private fun observePageEngagement() {
        lifecycle.addObserver(
            PageEngagementLifecycleObserver(
                pageName = Measurement.PAGE_SETTINGS,
                tabName = Measurement.PAGE_SETTINGS_TITLE,
                contentType = Measurement.CONTENT_TYPE_FRONT
            )
        )
    }


    private fun observeForFormSubmissionAction() {
        zendeskDestinationViewModel.destination.observe(this) {
            when (it) {
                ZendeskDestinations.Finish, ZendeskDestinations.Resubmit, ZendeskDestinations.SubmitSuccess -> {
                    /*
                        Do nothing.
                     */
                }

                ZendeskDestinations.StartFormSubmissionFromHelpCenter -> {
                    navController?.navigate(
                        R.id.action_settingsZendeskFragment_to_contactUsActivity,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkConnectivity()
        shakeDetector?.start()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.stop()
    }

    override fun onSupportNavigateUp(): Boolean {
        // calling onBackPressed() here to have the same behavior with device back button and up arrow.
        onBackPressed()
        return true
    }

    override fun updateSubscriptionState() {
        settingsViewModel.updateStates()
    }

    override fun checkConnectivity() {
        if (!ReachabilityUtil.isConnected(this)) {
            notifyNetworkProblem(binding.coordinator, false)
        }
    }

    override fun setTheme(resId: Int) {
        super.setTheme(R.style.WaPo_Settings)
    }

    override fun onOneTrustTargetingStateChanged(state: Boolean) {
        super.onOneTrustTargetingStateChanged(state)
        if (OneTrustHelper.isFunctionalityEnabled()) {
            alertsSettingsViewModel.updateDailyRead(state)
        }
        alertsSettingsViewModel.setOneTrustConsentState(state)
    }

    override fun onOneTrustFunctionalityStateChanged(state: Boolean) {
        super.onOneTrustFunctionalityStateChanged(state)
        if (OneTrustHelper.isTargetingEnabled()) {
            alertsSettingsViewModel.updateDailyRead(state)
        }
        alertsSettingsViewModel.setOneTrustConsentState(state)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // calling popBackStack() to pop entries until it reaches the startDestination.
        // popBackStack() returns false once back stack has no entries, then onBackPressed() finishes the activity.
        // In the deep link case, there is only one destination entry in the stack. So providing an option to go
        // back to the startDestination using ARG_POP_BACK_TO_START_DESTINATION.
        val navController = findNavController(R.id.nav_host_fragment)
        val currentDestinationId = navController.currentDestination?.id
        if (!navController.popBackStack()) {
            if (navController.graph.startDestinationId == currentDestinationId || !shouldNavigateStartDestination()) {
                super.onBackPressed()
            } else {
                navController.navigate(navController.graph.startDestinationId)
            }
        }
    }

    private fun listenToZendeskActions() {
        sharedViewModel.actions.observe(this) { action ->
            when (action) {
                is Action.ContactUsClick -> {
                    Measurement.trackContactUs()
                }
            }
        }
    }

    private fun shouldNavigateStartDestination(): Boolean =
        intent.getBooleanExtra(ARG_POP_BACK_TO_START_DESTINATION, false)


    companion object {
        const val ARG_POP_BACK_TO_START_DESTINATION = "POP_BACK_TO_START_DESTINATION"
    }
}
