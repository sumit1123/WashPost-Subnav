package com.wapo.flagship.features.settings

import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.wapo.adsinf.models.AdsModel
import com.wapo.adsinf.policy.AdService
import com.wapo.android.commons.util.Utils.isAmazonDevice
import com.wapo.flagship.Utils
import com.wapo.flagship.features.articles2.activities.ArticlesParcel
import com.wapo.flagship.features.notification.AlertsSettings
import com.wapo.flagship.features.settings.preferences.AccountSubPrimaryPreference
import com.wapo.flagship.features.settings.preferences.AccountSubSecondaryPreference
import com.wapo.flagship.features.settings.preferences.AppVersionPreference
import com.wapo.flagship.features.settings.preferences.DebugPanelPreference
import com.wapo.flagship.features.settings.preferences.MobileFreeTrialBannerPreference
import com.wapo.flagship.features.settings.preferences.SignOutPreference
import com.wapo.flagship.features.settings.preferences.SubLinkErrorPreference
import com.wapo.flagship.features.settings.preferences.SubPauseBannerPreference
import com.wapo.flagship.features.settings.preferences.SettingsPlanPreference
import com.wapo.flagship.push.PushPreferencesHelper
import com.wapo.flagship.sdk.iterable.viewmodels.IterableActivityViewModel
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.NavigationBehavior
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PaywallSheet2ViewModel
import com.washingtonpost.android.paywall.models.BannerPaywallMessage
import com.washingtonpost.android.paywall.util.PaywallConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.wapo.flagship.features.settings.preferences.SettingsTopBannerPreference
import com.wapo.flagship.features.subscribebanner.state.BannerEvent
import com.wapo.flagship.features.subscribebanner.state.BannerLifecycleEvent
import com.wapo.flagship.features.subscribebanner.viewmodel.GlobalBannerViewModel
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment :
    BasePreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener,
    Preference.OnPreferenceClickListener {

    @Inject
    lateinit var adService: AdService


    private var prefSignIn: Preference? = null
    private var prefSignOut: Preference? = null
    private var prefSubLinkError: SubLinkErrorPreference? = null
    private var prefSubPauseBanner: SubPauseBannerPreference? = null
    private var prefMobileFreeTrialBanner: MobileFreeTrialBannerPreference? = null
    private var prefFooter: AppVersionPreference? = null
    private var prefPrimaryAccountSubPref: AccountSubPrimaryPreference? = null
    private var prefSecondaryAccountSubPref: AccountSubSecondaryPreference? = null
    private var prefTestOptions: Preference? = null
    private var prefContentPacks: Preference? = null
    private var prefSubCategory: PreferenceCategory? = null
    private var prefSubType: Preference? = null
    private var prefManageSub: Preference? = null
    private var prefSubPaymentError: AccountSubPrimaryPreference? = null
    private var prefSubBenefits: Preference? = null
    private var prefSubTermsCategory: PreferenceCategory? = null
    private var prefDeveloperMode: androidx.preference.SwitchPreferenceCompat? = null
    private var prefOpenDebugPanel: Preference? = null
    private var prefDebugPanelCategory: PreferenceCategory? = null
    private var prefDebugPanel: DebugPanelPreference? = null
    private var prefWpWeekly: SettingsPlanPreference? = null
    private var wpWeeklySettingsMessage: BannerPaywallMessage? = null
    private var prefSettingsTopBanner: SettingsTopBannerPreference? = null
    private var settingsTopMessage: BannerPaywallMessage? = null
    private val iterableActivityViewModel: IterableActivityViewModel by activityViewModels()
    private val globalBannerViewModel: GlobalBannerViewModel by activityViewModels()


    private val config get() = ConfigManager.getInstance().config

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        Measurement.trackSettingsPageView()
        setPreferencesFromResource(R.xml.pref_settings, rootKey)

        prefSubCategory = findPreference(AppPreferences.PREF_SUB_CATEGORY)
        prefSubType = findPreference(AppPreferences.PREF_SUB_TYPE)
        prefManageSub = findPreference(AppPreferences.PREF_MANAGE_SUB)
        prefSubPaymentError = findPreference(AppPreferences.PREF_SUB_PAYMENT_ERROR)
        prefSubPaymentError?.setPaymentErrorMode()
        prefSubPaymentError?.setViewModel(settingsViewModel)
        prefSubBenefits = findPreference(AppPreferences.PREF_SUB_BENEFITS)
        prefSubLinkError = findPreference(AppPreferences.PREF_SUB_LINK_ERROR)
        prefSubTermsCategory = findPreference(AppPreferences.PREF_SUB_TERMS_CATEGORY)


        // Set listeners
        prefSignIn =
            findPreference<Preference>(AppPreferences.PREF_SIGN_IN)?.apply {
                onPreferenceClickListener = this@SettingsFragment
            }

        prefSignOut =
            findPreference<SignOutPreference>(AppPreferences.PREF_SIGN_OUT)?.apply {
                onPreferenceClickListener = this@SettingsFragment
            }

        prefSubLinkError =
            findPreference<SubLinkErrorPreference>(
                AppPreferences.PREF_SUB_LINK_ERROR,
            )?.apply {
                onPreferenceClickListener = this@SettingsFragment
            }

        prefSubPauseBanner =
            findPreference<SubPauseBannerPreference>(
                AppPreferences.PREF_SUB_PAUSE_BANNER,
            )?.apply {
                onPreferenceClickListener = this@SettingsFragment
            }

        prefMobileFreeTrialBanner =
            findPreference<MobileFreeTrialBannerPreference>(
                AppPreferences.PREF_MOBILE_FREE_TRIAL_BANNER,
            )?.apply {
                onPreferenceClickListener = this@SettingsFragment
            }
        prefWpWeekly =
            findPreference<SettingsPlanPreference>(AppPreferences.PREF_WP_WEEKLY)?.apply {
                onPreferenceClickListener = this@SettingsFragment
                setOnBindCallback {
                    wpWeeklySettingsMessage?.attributionInfo?.let {
                        globalBannerViewModel.setBannerEvent(
                            BannerEvent.ImpressionEvent(
                                BannerLifecycleEvent.StartImpression(it)
                            )
                        )
                    }
                }
                setOnDetachedCallback {
                    wpWeeklySettingsMessage?.attributionInfo?.let {
                        globalBannerViewModel.setBannerEvent(
                            BannerEvent.ImpressionEvent(
                                BannerLifecycleEvent.EndImpression(it)
                            )
                        )
                    }
                }
            }

        findPreference<Preference>(AppPreferences.PREF_TEXT_SIZE)?.apply {
            onPreferenceChangeListener = this@SettingsFragment
            dependency = AppPreferences.PREF_DEFAULT_FONT_SIZE
        }
        findPreference<Preference>(AppPreferences.PREF_ALERTS)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_NEWSLETTERS_AND_EMAIL_ALERTS)?.onPreferenceClickListener =
            this
        prefContentPacks =
            findPreference<Preference>(AppPreferences.PREF_ABOUT_ME)?.apply {
                onPreferenceClickListener = this@SettingsFragment
            }
        findPreference<Preference>(AppPreferences.PREF_CUSTOM_NAV)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_AUDIO)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_STORAGE)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_TEST_OPTIONS)?.apply {
            prefTestOptions = this
            onPreferenceClickListener = this@SettingsFragment
            isVisible = AppPreferences.canShowTestOptions()
        }
        findPreference<Preference>(AppPreferences.PREF_HELP)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_CONTACT_US)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_PRIVACY)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_FEEDBACK)?.onPreferenceClickListener = this
        findPreference<Preference>(AppPreferences.PREF_TERMS)?.onPreferenceClickListener = this
        prefFooter =
            findPreference<AppVersionPreference>(AppPreferences.PREF_FOOTER)?.apply {
                setViewModel(settingsViewModel)
            }

        prefPrimaryAccountSubPref =
            (findPreference(AppPreferences.PREF_ACCOUNT_SUB_PRIMARY) as? AccountSubPrimaryPreference)?.apply {
                setViewModel(settingsViewModel)
                onPreferenceClickListener = this@SettingsFragment
            }
        prefMobileFreeTrialBanner =
            (findPreference(AppPreferences.PREF_MOBILE_FREE_TRIAL_BANNER) as? MobileFreeTrialBannerPreference)?.apply {
                setViewModel(settingsViewModel)
                onPreferenceClickListener = this@SettingsFragment
            }
        prefSettingsTopBanner =
            (findPreference(AppPreferences.PREF_SETTINGS_UPGRADE_BANNER) as? SettingsTopBannerPreference)?.apply {
                onPreferenceClickListener = this@SettingsFragment
                setOnBindCallback {
                    settingsTopMessage?.attributionInfo?.let {
                        iterableActivityViewModel.trackEvent(
                            PaywallSheet2ViewModel.EventType.START_IMPRESSION_EVENT, it
                        )
                    }
                }
                setOnDetachedCallback {
                    settingsTopMessage?.attributionInfo?.let {
                        iterableActivityViewModel.trackEvent(
                            PaywallSheet2ViewModel.EventType.PAUSE_IMPRESSION_EVENT, it
                        )
                    }
                }
            }
        prefSecondaryAccountSubPref =
            (findPreference(AppPreferences.PREF_ACCOUNT_SUB_SECONDARY) as? AccountSubSecondaryPreference)?.apply {
                setViewModel(settingsViewModel)
                onPreferenceClickListener = this@SettingsFragment
            }
        prefSubLinkError?.onPreferenceClickListener = this
        prefSubBenefits?.onPreferenceClickListener = this
        prefManageSub?.onPreferenceClickListener = this

        val isBeta = BuildConfig.BUILD_TYPE.contains("beta")
        findPreference<PreferenceCategory>(AppPreferences.PREF_BETA)?.isVisible = isBeta
        findPreference<Preference>(AppPreferences.PREF_BETA_AGGREGATOR)?.let {
            it.isVisible = isBeta
            it.onPreferenceClickListener = this@SettingsFragment
        }
        findPreference<Preference>(AppPreferences.PREF_BETA_ASK_THE_POST_AI_BOT)?.let {
            it.isVisible = isBeta
            it.onPreferenceClickListener = this@SettingsFragment
        }
        findPreference<Preference>(AppPreferences.PREF_BETA_CLIMATE_BOT)?.let {
            it.isVisible = isBeta
            it.onPreferenceClickListener = this@SettingsFragment
        }

        findPreference<Preference>(AppPreferences.PREF_BETA_POST_LLM)?.let {
            it.isVisible = isBeta
            it.onPreferenceClickListener = this@SettingsFragment
        }

        // Open Debug Panel preference (hidden by default, shown when Developer Mode is ON)
        prefOpenDebugPanel =
            findPreference<Preference>(AppPreferences.PREF_OPEN_DEBUG_PANEL)?.apply {
                onPreferenceClickListener = this@SettingsFragment
            }

        // Debug Panel category and preference (shown when Developer Mode is ON)
        prefDebugPanelCategory =
            findPreference<PreferenceCategory>(AppPreferences.PREF_DEBUG_PANEL_CATEGORY)
        prefDebugPanel =
            findPreference<DebugPanelPreference>(AppPreferences.PREF_DEBUG_PANEL)?.also {
                it.paywallClick = {
                    requireView().findNavController().navigate(R.id.settings_account)
                }
            }
        // Developer Mode preference (hidden by default, shown when shake is detected)
        prefDeveloperMode =
            findPreference<androidx.preference.SwitchPreferenceCompat>(AppPreferences.PREF_DEVELOPER_MODE)?.apply {
                // Restore the saved Developer Mode state
                val isDeveloperModeEnabled = AppPreferences.isDeveloperModeEnabled()
                isVisible = isDeveloperModeEnabled
                isChecked = isDeveloperModeEnabled

                if (isDeveloperModeEnabled) {
                    prefOpenDebugPanel?.isVisible = false // Hide the "Open Debug Panel" button
                    prefDebugPanelCategory?.isVisible = true // Show the inline debug panel
                }

                // Handle toggle OFF - hide both Developer Mode and Debug Panel, and save state
                onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { preference, newValue ->
                        val isEnabled = newValue as Boolean
                        if (!isEnabled) {
                            preference.isVisible = false
                            // Save any pending overrides before hiding
                            prefDebugPanel?.saveSelectedOverrides()
                        }
                        prefOpenDebugPanel?.isVisible = false // Always hide the old button
                        prefDebugPanelCategory?.isVisible =
                            isEnabled // Show/hide inline debug panel

                        AppPreferences.setDeveloperModeEnabled(isEnabled)
                        true
                    }
            }

        settingsViewModel.updateStates()

        settingsViewModel.signInState.value?.apply {
            updateUI(this)
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Measurement.setNavigationBehavior(NavigationBehavior.SETTINGS)
        settingsViewModel.wpWeeklyPlacement.observe(viewLifecycleOwner) {
            it?.let {
                wpWeeklySettingsMessage = it
                prefWpWeekly?.isVisible = true
                prefWpWeekly?.settingsMessage = it
            } ?: run {
                prefWpWeekly?.isVisible = false
                prefWpWeekly?.settingsMessage = null
            }
        }
        settingsViewModel.getWpWeeklyPlacement(
            iterableActivityViewModel.getBannerForPlacement(
                IamMessageType.SETTINGS_PLAN
            )
        )
        observeSignInStateChanges()
        observeTestOptionsToggleCountChanges()
        observeSubscriptionState()
        observeSettingsTopBanner()

        viewLifecycleOwner.lifecycleScope.launch {
            adService.adsMode.collect {
                updateSubscriptionUI(
                    settingsViewModel.subscriptionState.value ?: SubscriptionState.NotSubscribed
                )
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            adService.isAdFreeRenewable.collect {
                updateSubscriptionUI(
                    settingsViewModel.subscriptionState.value ?: SubscriptionState.NotSubscribed
                )
            }
        }
    }

    private fun observeSubscriptionState() {
        settingsViewModel.subscriptionState.observe(viewLifecycleOwner) {
            updateSubscriptionUI(it)
        }
    }

    private fun observeSettingsTopBanner() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                iterableActivityViewModel
                    .getBannerFlowForPlacement(IamMessageType.SETTINGS_TOP)
                    .collect { message ->
                        settingsTopMessage = message
                        prefSettingsTopBanner?.isVisible = message != null
                        prefSettingsTopBanner?.setMessage(message)
                    }
            }
        }
    }

    private fun updateSubscriptionUI(state: SubscriptionState) {
        prefSubPaymentError?.showPaymentError(state)
        prefSubLinkError?.isVisible = settingsViewModel.isSubscriptionClaimed()
        when (state) {
            SubscriptionState.NotSubscribed,
            SubscriptionState.Terminated,
                -> {
                prefSubCategory?.isVisible = false
                prefSubTermsCategory?.isVisible = false
                prefSubPaymentError?.isVisible = false
            }

            SubscriptionState.Subscribed -> {
                val isNonRenewable = settingsViewModel.isNonRenewableSubscription()
                prefSubCategory?.isVisible = true
                prefSubPaymentError?.isVisible = false
                // If there is an amazon sub on a non amazon device, hide manage sub button.
                // when user has a flex subscription, hide manage sub and subscription benefits buttons
                prefManageSub?.isVisible =
                    !isNonRenewable && !(settingsViewModel.isAmazonSubOnNonAmazonDevice())
                prefSubTermsCategory?.isVisible =
                    !isNonRenewable && settingsViewModel.isPlaystoreSub()
                val subSourceText = settingsViewModel.getSubSourceText()
                if (isNonRenewable) {
                    val expirationText = getString(R.string.one_day_pass_expiry_message)
                    prefSubType?.summary = "$subSourceText\n\n$expirationText"
                } else if (adService.currentAdsMode == AdsModel.Disabled) {
                    val addOnLabel = getString(R.string.settings_add_ons_ad_free)
                    val adFreeTitle = try {
                        val fromSubs =
                            PaywallService.getInstance().getAdFreeShortTitleFromSubscriptions()
                        if (!fromSubs.isNullOrEmpty()) fromSubs else getString(R.string.settings_ad_free_default_title)
                    } catch (e: Exception) {
                        getString(R.string.settings_ad_free_default_title)
                    }

                    val addOnLine = SpannableStringBuilder()
                        .append("$addOnLabel ")
                        .append(
                            adFreeTitle,
                            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                    val summary = SpannableStringBuilder()
                        .append(subSourceText)
                        .append("\n")
                        .append(addOnLine)
                    prefSubType?.summary = summary
                } else {
                    prefSubType?.summary = subSourceText
                }
                prefManageSub?.summary = settingsViewModel.getRenewalDate()
                prefSubBenefits?.isVisible = !isNonRenewable &&
                        settingsViewModel.signInState.value is SignInState.SignedIn
                prefSubBenefits?.summary = getString(R.string.pref_sub_benefit_summary)
            }

            SubscriptionState.FreeDays -> {
                prefSubCategory?.isVisible = true
                prefSubPaymentError?.isVisible = false
                prefManageSub?.isVisible = true
                prefSubTermsCategory?.isVisible = settingsViewModel.isPlaystoreSub()
                prefSubType?.summary = settingsViewModel.getSubSourceText()
                prefManageSub?.summary = settingsViewModel.getFreeDaysExpiry()
            }

            is SubscriptionState.FreeArticles -> {
                prefSubCategory?.isVisible = true
                prefSubPaymentError?.isVisible = false
                prefManageSub?.isVisible = true
                prefSubTermsCategory?.isVisible = settingsViewModel.isPlaystoreSub()
                prefSubType?.summary = settingsViewModel.getSubSourceText()
                prefManageSub?.summary =
                    settingsViewModel.getFreeArticlesExpiry(
                        state.remainingCount,
                    )
            }

            is SubscriptionState.MobileFreeTrial -> {
                prefSubCategory?.isVisible = true
                prefSubPaymentError?.isVisible = false
                prefManageSub?.isVisible = true
                prefSubTermsCategory?.isVisible = settingsViewModel.isPlaystoreSub()
                prefSubType?.summary = settingsViewModel.getSubSourceText()
                prefManageSub?.summary = settingsViewModel.getMobileFreeTrialExpiry()
            }

            SubscriptionState.GracePeriod -> {
                prefSubCategory?.isVisible = true
                prefSubTermsCategory?.isVisible = settingsViewModel.isPlaystoreSub()
                prefManageSub?.isVisible = false
                prefSubType?.summary = settingsViewModel.getSubSourceText()
            }

            SubscriptionState.OnHold -> {
                prefSubCategory?.isVisible = true
                prefSubPaymentError?.isVisible = true
                prefManageSub?.isVisible = false
                prefSubTermsCategory?.isVisible = settingsViewModel.isPlaystoreSub()
                prefSubType?.summary = settingsViewModel.getSubSourceText()
            }

            is SubscriptionState.Paused -> {
                prefSubPauseBanner?.isVisible = true
                prefSubCategory?.isVisible = false
                prefSubTermsCategory?.isVisible = false
                prefSubPaymentError?.isVisible = false
            }

            is SubscriptionState.ScheduledPause -> {
                prefSubPauseBanner?.isVisible = true
                prefSubCategory?.isVisible = true
                prefSubPaymentError?.isVisible = false
                // If there is an amazon sub on a non amazon device, hide manage sub button.
                prefManageSub?.isVisible = !(settingsViewModel.isAmazonSubOnNonAmazonDevice())
                prefSubTermsCategory?.isVisible = settingsViewModel.isPlaystoreSub()
                prefSubType?.summary = settingsViewModel.getSubSourceText()
                prefManageSub?.summary = settingsViewModel.getRenewalDate()
            }
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            AppPreferences.PREF_ACCOUNT_SUB_PRIMARY -> {
                when (settingsViewModel.subscriptionState.value) {
                    SubscriptionState.GracePeriod,
                    SubscriptionState.OnHold,
                        -> openPlayStore()

                    SubscriptionState.Subscribed, is SubscriptionState.Paused, is SubscriptionState.ScheduledPause -> {
                        if (!settingsViewModel.isUserSignedIn()) {
                            processUser()
                        }
                    }

                    else -> showPaywallDialog()
                }
                return true
            }

            AppPreferences.PREF_MANAGE_SUB -> {
                val adsMode = adService.currentAdsMode
                val isAdFreeRenewable = adService.currentIsAdFreeRenewable
                val shouldOpenManageSubscription =
                    adsMode == AdsModel.Disabled &&
                            isAdFreeRenewable &&
                            (settingsViewModel.isClassicAppPlaystoreSub() || settingsViewModel.isAmazonSub())

                if (shouldOpenManageSubscription) {
                    val action =
                        SettingsFragmentDirections.actionSettingsFragmentToManageSubscriptionFragment()
                    requireView().findNavController().navigate(action)
                } else if (settingsViewModel.isPlaystoreSub() ||
                    settingsViewModel.subscriptionState.value is SubscriptionState.OnHold ||
                    settingsViewModel.subscriptionState.value is SubscriptionState.GracePeriod
                ) {
                    openPlayStore()
                } else if (settingsViewModel.isAmazonSub() && isAmazonDevice) {
                    openAmazonStore()
                } else if (settingsViewModel.isDirectSource() || settingsViewModel.shouldCancelAdFreeViaWeb()) {
                    Utils.startWebActivity(
                        settingsViewModel.getManageSubUrl(),
                        activity,
                        false,
                        true,
                    )
                } else {
                    val action = SettingsFragmentDirections.navActionZendeskForm()
                    requireView().findNavController().navigate(action)
                }
                Measurement.trackSettingsAccountManageSubPageView()
            }

            AppPreferences.PREF_SUB_LINK_ERROR -> {
                val action = SettingsFragmentDirections.navActionZendeskForm()
                requireView().findNavController().navigate(action)
            }

            AppPreferences.PREF_SUB_BENEFITS -> {
                Utils.startWebActivity(
                    settingsViewModel.getSubBenefitsUrl(),
                    activity,
                    false,
                    false,
                )
                Measurement.trackSettingsAccountBenefitsPageView()
            }

            AppPreferences.PREF_ACCOUNT_SUB_SECONDARY -> {
                Navigation.findNavController(requireView()).navigate(R.id.settings_account)
                return true
            }

            AppPreferences.PREF_SIGN_IN -> {
                processUser()
                return true
            }

            AppPreferences.PREF_SIGN_OUT -> {
                settingsViewModel.startSignOutProcess()
                return true
            }

            AppPreferences.PREF_ALERTS -> {
                val args = Bundle()
                args.putString(
                    AlertsSettings.EntryPoint.TYPE,
                    AlertsSettings.EntryPoint.SETTINGS.trackingName,
                )

                if (!PushPreferencesHelper.areNotificationsEnabled(context)) {
                    PushPreferencesHelper.showNotificationsBlockedDialog(parentFragmentManager)
                } else {
                    Navigation.findNavController(requireView()).navigate(R.id.settings_alerts, args)
                }
                return true
            }

            AppPreferences.PREF_NEWSLETTERS_AND_EMAIL_ALERTS -> {
                Utils.startWebActivity(config.newslettersAndEmailAlertsUrl, activity)
                return true
            }

            AppPreferences.PREF_ABOUT_ME -> {
                Utils.startWebActivity(settingsViewModel.getAboutMeUrl(), activity, false)
                return true
            }

            AppPreferences.PREF_CUSTOM_NAV -> {
                Navigation.findNavController(requireView()).navigate(R.id.settings_custom_nav)
                return true
            }

            AppPreferences.PREF_AUDIO -> {
                Navigation.findNavController(requireView()).navigate(R.id.settings_audio)
                return true
            }

            AppPreferences.PREF_STORAGE -> {
                Navigation.findNavController(requireView()).navigate(R.id.settings_storage)
                return true
            }

            AppPreferences.PREF_TEST_OPTIONS -> {
                Navigation.findNavController(requireView()).navigate(R.id.settings_test_options)
                return true
            }

            AppPreferences.PREF_HELP -> {
                Measurement.trackHelpCenter()
                Navigation.findNavController(requireView()).navigate(
                    com.wapo.zendesk.R.id.nav_action_zendesk_help_center,
                )
                return true
            }

            AppPreferences.PREF_CONTACT_US, AppPreferences.PREF_SUB_LINK_ERROR -> {
                val action = SettingsFragmentDirections.actionSettingsFragmentToContactUsActivity()
                Navigation.findNavController(requireView()).navigate(action)
                return true
            }

            AppPreferences.PREF_SUB_PAUSE_BANNER -> {
                val paywallService = PaywallService.getInstance()
                when {
                    paywallService.isSiteSubscriptionPaused -> {
                        PaywallService.getConnector().openSiteSubManagement(
                            context,
                            true,
                            PaywallConstants.ManageSubUrlItids.APP_SETTINGS.value,
                        )
                        PaywallService.getOmniture().trackBannerProfileResume(
                            NavigationBehavior.SETTINGS.value,
                            PaywallService.getConnector().getSiteSubManagementUrl(
                                true,
                                PaywallConstants.ManageSubUrlItids.APP_SETTINGS.value,
                            ),
                        )
                    }

                    paywallService.isSiteSubscriptionPauseScheduled -> {
                        PaywallService.getConnector().openSiteSubManagement(
                            context,
                            false,
                            PaywallConstants.ManageSubUrlItids.APP_SETTINGS.value,
                        )
                        PaywallService.getOmniture().trackBannerProfileResume(
                            NavigationBehavior.SETTINGS.value,
                            PaywallService.getConnector().getSiteSubManagementUrl(
                                false,
                                PaywallConstants.ManageSubUrlItids.APP_SETTINGS.value,
                            ),
                        )
                    }

                    else -> {
                        openPlayStore()
                        PaywallService.getOmniture().trackBannerProfileResume(
                            NavigationBehavior.SETTINGS.value,
                            PaywallService.getConnector().getPlayStoreUrl(context),
                        )
                    }
                }

                return true
            }

            AppPreferences.PREF_MOBILE_FREE_TRIAL_BANNER -> {
                showPaywallDialog()
                return true
            }

            AppPreferences.PREF_SETTINGS_UPGRADE_BANNER -> {
                val message =
                    iterableActivityViewModel.getBannerForPlacement(IamMessageType.SETTINGS_TOP)
                        ?: return true
                globalBannerViewModel.setBannerEvent(BannerEvent.BannerClicked(message))
                return true
            }

            AppPreferences.PREF_PRIVACY -> {
                Navigation.findNavController(requireView()).navigate(R.id.settings_privacy)
                return true
            }

            AppPreferences.PREF_FEEDBACK -> {
                Measurement.trackReviewApp()
                startActivity(Utils.initMarketIntent(activity))
                return true
            }

            AppPreferences.PREF_TERMS -> {
                Utils.startWebActivity(config.termsOfServiceUrl, activity, false)
                return true
            }

            AppPreferences.PREF_BETA_AGGREGATOR -> {
                val url = requireContext().resources.getString(R.string.beta_aggregator_url)
                launchArticleLink(url)
            }

            AppPreferences.PREF_BETA_ASK_THE_POST_AI_BOT -> {
                val url = requireContext().resources.getString(R.string.beta_ask_the_post_ai_url)
                launchArticleLink(url)
            }

            AppPreferences.PREF_BETA_CLIMATE_BOT -> {
                val url = requireContext().resources.getString(R.string.climate_bot_url)
                launchArticleLink(url)
            }

            AppPreferences.PREF_BETA_POST_LLM -> {
                val url = requireContext().resources.getString(R.string.post_llm_url)
                launchArticleLink(url)
            }

            AppPreferences.PREF_WP_WEEKLY -> {
                wpWeeklySettingsMessage?.let {
                    settingsViewModel.resetWpWeeklyPlacement()
                    globalBannerViewModel.setBannerEvent(BannerEvent.BannerClicked(it))
                }
            }
        }
        return false
    }

    override fun onPreferenceChange(
        preference: Preference,
        newValue: Any?,
    ): Boolean = true

    private fun observeSignInStateChanges() {
        settingsViewModel.signInState.observe(
            viewLifecycleOwner,
            Observer {
                updateUI(it)
            },
        )
    }

    private fun updateUI(state: SignInState) {
        val paused = settingsViewModel.subscriptionState.value is SubscriptionState.Paused
        val scheduledPause =
            settingsViewModel.subscriptionState.value is SubscriptionState.ScheduledPause
        val mobileFreeTrial =
            settingsViewModel.subscriptionState.value is SubscriptionState.MobileFreeTrial
        var subTypeString = ""
        when (state) {
            is SignInState.SignedIn -> {
                prefSecondaryAccountSubPref?.updateProfile(
                    state.name,
                    state.email,
                    state.subscription,
                    state.photoUrl,
                    true,
                )
                prefPrimaryAccountSubPref?.updateAction(
                    true,
                    settingsViewModel.subscriptionState.value,
                )
                prefSignIn?.isVisible = false
                prefSignOut?.isVisible = true
                prefSubLinkError?.isVisible = settingsViewModel.isSubscriptionClaimed()
                prefSubPauseBanner?.isVisible = paused || scheduledPause
                prefContentPacks?.isVisible = true
                prefMobileFreeTrialBanner?.isVisible = mobileFreeTrial
                prefSubBenefits?.isVisible =
                    settingsViewModel.subscriptionState.value is SubscriptionState.Subscribed
                            && !settingsViewModel.isNonRenewableSubscription()
                subTypeString = state.subscription ?: ""
                settingsViewModel.getWpWeeklyPlacement(
                    iterableActivityViewModel.getBannerForPlacement(
                        IamMessageType.SETTINGS_PLAN
                    )
                )
            }

            SignInState.ConfirmSignOut -> {
                processUser()
                settingsViewModel.trackSignOutStarted()
            }

            is SignInState.SignedOut -> {
                prefSecondaryAccountSubPref?.updateProfile(
                    null,
                    null,
                    state.subscription,
                    null,
                    settingsViewModel.isUserSubscribed(),
                )
                subTypeString = state.subscription ?: ""
                prefPrimaryAccountSubPref?.updateAction(
                    false,
                    settingsViewModel.subscriptionState.value,
                )
                prefSignIn?.isVisible = !settingsViewModel.isUserSubscribed() && !paused
                prefSignOut?.isVisible = false
                prefContentPacks?.isVisible = false
                prefSubLinkError?.isVisible = false
                prefWpWeekly?.isVisible = false
                prefSubPauseBanner?.isVisible = paused || scheduledPause
                settingsViewModel.removeTestGroupDailyRead(requireActivity())
            }

            else -> {
            }
        }
        if (subTypeString.isNotEmpty()) {
            val spannable =
                if (!settingsViewModel.isFreeTrialSub()) {
                    // Add spacing to the left of text if burst icon is shown
                    val span = SpannableString("   $subTypeString")
                    VectorDrawableCompat
                        .create(
                            resources,
                            R.drawable.ic_subs_burst,
                            requireContext().theme,
                        )?.apply {
                            this.setBounds(0, 0, this.intrinsicWidth, this.intrinsicHeight)
                            val imageSpan =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    ImageSpan(
                                        this,
                                        DynamicDrawableSpan.ALIGN_CENTER,
                                    )
                                } else {
                                    ImageSpan(
                                        this,
                                    )
                                }
                            span.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    span
                } else {
                    // If icon is not shown, send string as is.
                    SpannableString(subTypeString)
                }

            prefSubType?.title = spannable
        }
    }

    override fun onResume() {
        super.onResume()
        // We call this if we need info from tetro. ie. Free Article count
        settingsViewModel.syncWithTetro()
        iterableActivityViewModel.syncInAppMessages()
    }

    private fun processUser() {
        val user = PaywallService.getInstance().loggedInUser
        if (user != null) {
            startSignOut(user)
        } else {
            startSignIn()
        }
    }

    /**
     * Method to observe testOptionsToggleCount live data to show/hide "Test Options" preference.
     * Observer toggles "Test Options" visibility when count is a multiplier of X (X is 15 now)
     * and also handles Toast and Remote logging when preference visibility  changes.
     */
    private fun observeTestOptionsToggleCountChanges() {
        settingsViewModel.testOptionsToggleCount.observe(viewLifecycleOwner) {
            if (it % 15 == 0) {
                prefTestOptions?.apply {
                    val show = !AppPreferences.canShowTestOptions()
                    AppPreferences.showTestOptions(show)
                    isVisible = show
                    val text = if (show) "Enabled" else "Disabled"
                    Toast.makeText(context, "Test Options $text", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchArticleLink(url: String) {
        ArticlesParcel
            .builder()
            .setArticleSingleUrl(url)
            .buildIntent(requireContext())
            .also { intent ->
                requireContext().startActivity(intent)
            }
    }
}
