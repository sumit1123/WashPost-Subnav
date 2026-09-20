package com.wapo.flagship.features.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.wapo.android.commons.util.Utils.isAmazonDevice
import com.wapo.flagship.Utils
import com.wapo.flagship.features.settings.preferences.AccountSubPrimaryPreference
import com.wapo.flagship.features.settings.preferences.SubLinkErrorPreference
import com.wapo.flagship.features.settings.preferences.SubPauseBannerPreference
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallReactive
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.metering.MeteringPrefs
import com.washingtonpost.android.paywall.metering.MeteringService
import com.washingtonpost.android.paywall.newdata.model.Subscription
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class SettingsAccountFragment :
    BasePreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener {

    private var prefSignIn: Preference? = null
    private var prefEditEmail: Preference? = null
    private var prefEditName: Preference? = null
    private var prefSubType: Preference? = null
    private var prefAppStoreTerms: Preference? = null
    private var prefSubPaymentError: AccountSubPrimaryPreference? = null
    private var prefSubCategory: PreferenceCategory? = null
    private var prefSubTermsCategory: PreferenceCategory? = null
    private var prefSubLinkError: SubLinkErrorPreference? = null
    private var prefSubPauseBanner: SubPauseBannerPreference? = null
    private var isDevModeEnabled = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Measurement.trackSettingsAccountPageView()
        isDevModeEnabled = AppPreferences.isDeveloperModeEnabled()

        setPreferencesFromResource(R.xml.pref_settings_account, rootKey)
        prefSignIn = findPreference(AppPreferences.PREF_SIGN_IN)
        prefEditEmail = findPreference(AppPreferences.PREF_EDIT_EMAIL)
        prefEditName = findPreference(AppPreferences.PREF_EDIT_NAME)
        prefSubType = findPreference(AppPreferences.PREF_SUB_TYPE)
        prefAppStoreTerms = findPreference(AppPreferences.PREF_APP_STORE_TERMS)
        prefSubPaymentError = findPreference(AppPreferences.PREF_SUB_PAYMENT_ERROR)
        prefSubPaymentError?.setPaymentErrorMode()
        prefSubPaymentError?.setViewModel(settingsViewModel)
        prefSubCategory = findPreference(AppPreferences.PREF_SUB_CATEGORY)
        prefSubTermsCategory = findPreference(AppPreferences.PREF_SUB_TERMS_CATEGORY)
        prefSubLinkError = findPreference(AppPreferences.PREF_SUB_LINK_ERROR)
        prefSubPauseBanner = findPreference(AppPreferences.PREF_SUB_PAUSE_BANNER)
        findPreference<PreferenceCategory>(AppPreferences.PREF_DEVELOPER_CATEGORY)?.isVisible = isDevModeEnabled
        findPreference<PreferenceCategory>(AppPreferences.PREF_USER_CATEGORY)?.isVisible = isDevModeEnabled
        findPreference<PreferenceCategory>(AppPreferences.PREF_SUBSCRIPTION_CATEGORY)?.isVisible = isDevModeEnabled
        findPreference<PreferenceCategory>(AppPreferences.PREF_ADD_ON_CATEGORY)?.isVisible = isDevModeEnabled
        findPreference<PreferenceCategory>(AppPreferences.PREF_NETWORKING_CATEGORY)?.isVisible = isDevModeEnabled

        prefEditEmail?.onPreferenceClickListener = this
        prefEditName?.onPreferenceClickListener = this
        prefAppStoreTerms?.onPreferenceClickListener = this
        prefSignIn?.onPreferenceClickListener = this
        prefSubLinkError?.onPreferenceClickListener = this

        settingsViewModel.signInState.value?.apply {
            updateUI(this)
            updateDebugUI(this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeSignInState()
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.signInState.value?.let { updateDebugUI(it) }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            AppPreferences.PREF_SIGN_IN -> {
                startSignIn()
            }
            AppPreferences.PREF_EDIT_EMAIL -> {
                Utils.startWebActivity(
                    settingsViewModel.getEditEmailPasswordUrl(),
                    activity,
                    false,
                    true,
                )
                Measurement.trackSettingsAccountEditEmailPageView()
                return true
            }
            AppPreferences.PREF_EDIT_NAME -> {
                Utils.startWebActivity(
                    settingsViewModel.getEditNamePhotoUrl(),
                    activity,
                    false,
                    true,
                )
                Measurement.trackSettingsAccountEditNamePageView()
                return true
            }
            AppPreferences.PREF_APP_STORE_TERMS -> {
                val appStoreTOC =
                    if (isAmazonDevice) {
                        getString(R.string.amazon_terms_url)
                    } else {
                        getString(R.string.playstore_terms_url)
                    }
                Utils.startWebActivity(appStoreTOC, activity, false)
                return true
            }
            AppPreferences.PREF_SUB_LINK_ERROR -> {
                val action = SettingsAccountFragmentDirections.navActionZendeskForm()
                Navigation.findNavController(requireView()).navigate(action)
            }
            AppPreferences.PREF_SUB_PAUSE_BANNER -> {
                openPlayStore()
                return true
            }
        }
        return false
    }

    private fun observeSignInState() {
        settingsViewModel.signInState.observe(viewLifecycleOwner) {
            updateUI(it)
            updateDebugUI(it)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    PaywallReactive.subAttributes.collect {
                        settingsViewModel.signInState.value?.let { state -> updateDebugUI(state) }
                    }
                }
                launch {
                    PaywallReactive.verifyComplete.collect {
                        settingsViewModel.signInState.value?.let { state -> updateDebugUI(state) }
                    }
                }
            }
        }
    }

    private fun updateUI(state: SignInState) {
        when (state) {
            is SignInState.SignedIn -> {
                prefSignIn?.isVisible = false
                prefEditName?.isVisible = true
                prefEditEmail?.isVisible = true
                prefEditEmail?.summary = state.email ?: ""
            }
            is SignInState.SignedOut -> {
                prefSignIn?.isVisible = true
                prefEditName?.isVisible = false
                prefEditEmail?.isVisible = false
            }
            else -> {}
        }
    }

    // ---- Debug UI Helpers ----

    /** Adds a display-only or copyable preference row to a category. */
    private fun addPrefToCategory(
        category: PreferenceCategory,
        key: String,
        title: String,
        summary: String,
        copyable: Boolean = false,
    ) {
        Preference(requireContext()).apply {
            this.key = key
            this.title = title
            this.summary = summary
            layoutResource = R.layout.preference_item_with_divider
            isSelectable = copyable
            if (copyable) {
                onPreferenceClickListener = Preference.OnPreferenceClickListener { pref ->
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(pref.key, pref.summary))
                    Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            category.addPreference(this)
        }
    }

    /** Formats an epoch millis value to a human-readable date string in device local time. */
    private fun formatEpochMillis(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
        sdf.timeZone = Calendar.getInstance().timeZone
        return sdf.format(Date(millis))
    }

    /** Parses a server date string (UTC) and formats it in device local time. */
    private fun parseAndFormatServerDate(raw: String): String? {
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val outputSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
            outputSdf.timeZone = Calendar.getInstance().timeZone
            parser.parse(raw)?.let { outputSdf.format(it) }
        }.getOrNull()
    }

    // ---- Debug UI ----

    private fun updateDebugUI(state: SignInState) {
        val paywallInstance = PaywallService.getInstance()
        val isSignedIn = state is SignInState.SignedIn
        val signedInState = state as? SignInState.SignedIn
        val billingSubscription = PaywallService.getBillingHelper()?.cachedSubscription()
        val userSubscription = if (paywallInstance.isWpUserLoggedIn()) paywallInstance.loggedInUser?.subStatus else null
        val bestAvailableSub = PaywallUtil.getBestAvailableBaseSubscription(
            paywallInstance.loggedInUser?.subscriptions,
            paywallInstance.loggedInUser?.subscriptionId
        )
        val subStatusExists = billingSubscription != null || bestAvailableSub != null || userSubscription != null
        val isActiveSubscriber = bestAvailableSub != null ||
                PaywallConstants.ACTIVE == PaywallService.getConnector().iapSubscriptionStatus
        val isMetering = !isActiveSubscriber && paywallInstance.isTurnedOn

        updateDeveloperSection(paywallInstance, isMetering)
        updateUserSection(paywallInstance, isSignedIn, signedInState)
        updateSubscriptionSection(paywallInstance, isSignedIn, billingSubscription, subStatusExists)
        updateAddOnSection(paywallInstance, billingSubscription)
        updateNetworkingSection()
    }

    private fun updateDeveloperSection(
        paywallInstance: PaywallService,
        isMetering: Boolean,
    ) {
        findPreference<PreferenceCategory>(AppPreferences.PREF_DEVELOPER_CATEGORY)?.let { category ->
            category.isVisible = isDevModeEnabled
            category.removeAll()

            addPrefToCategory(category, "pref_developer_on", "Paywall On", if (paywallInstance.isTurnedOn) "Yes" else "No")
            addPrefToCategory(category, "pref_developer_metering", "Metering", if (isMetering) "Yes" else "No")
            addPrefToCategory(category, "pref_developer_ad_free", "Ad Free", if (PaywallReactive.isAdFree.value) "Yes" else "No")
            if (isMetering) {
                addPrefToCategory(category, "pref_developer_meter_cycle", "Meter Cycle", "${MeteringPrefs.getMeterCycleDays()} Days")
                addPrefToCategory(category, "pref_developer_free_count", "Free Count", MeteringPrefs.getFreeArticlesRemaining().toString())
            }
            addPrefToCategory(category, "pref_developer_meter", "Meter", "${MeteringService.getCurrentArticleCount().toInt()} of ${MeteringService.getMaxArticleLimit()}")

            Preference(requireContext()).apply {
                key = "pref_developer_clear_paywall"
                title = "Clear Paywall Data"
                layoutResource = R.layout.preference_item_with_divider
                isSelectable = true
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    PaywallService.getInstance().clearAllPaywallData()
                    settingsViewModel.startSignOutProcess()
                    settingsViewModel.signInState.value?.let { updateDebugUI(it) }
                    Toast.makeText(requireContext(), "Cleared", android.widget.Toast.LENGTH_SHORT).show()
                    true
                }
                category.addPreference(this)
            }
        }
    }

    private fun updateUserSection(
        paywallInstance: PaywallService,
        isSignedIn: Boolean,
        signedInState: SignInState.SignedIn?,
    ) {
        findPreference<PreferenceCategory>(AppPreferences.PREF_USER_CATEGORY)?.let { category ->
            category.isVisible = isDevModeEnabled
            category.removeAll()
            if (!isSignedIn) {
                addPrefToCategory(category, "pref_user_none", "User", "N/A")
                addPrefToCategory(category, "pref_user_iterable_id", "Iterable Id", paywallInstance.iterableUserId ?: "N/A", copyable = true)
            } else {
                addPrefToCategory(category, "pref_user_id", "ID", paywallInstance.loginId ?: "N/A", copyable = true)
                addPrefToCategory(category, "pref_user_iterable_id", "Iterable Id", paywallInstance.iterableUserId ?: "N/A", copyable = true)
                addPrefToCategory(category, "pref_user_name", "Name", signedInState?.name ?: "N/A")
                addPrefToCategory(category, "pref_user_email", "Email", signedInState?.email ?: "N/A")
                addPrefToCategory(category, "pref_user_provider", "Provider", paywallInstance.loggedInUser?.signedInThrough ?: "N/A")
                addPrefToCategory(category, "pref_user_session_id", "SessionID", "Bearer ${PaywallService.getAccessToken() ?: "N/A"}", copyable = true)
            }
        }
    }

    private fun updateSubscriptionSection(
        paywallInstance: PaywallService,
        isSignedIn: Boolean,
        billingSubscription: Subscription?,
        subStatusExists: Boolean,
    ) {
        val subId = paywallInstance.subscriptionID?.takeIf { it.isNotBlank() }
            ?: paywallInstance.loggedInUser?.subscriptionId?.takeIf { it.isNotBlank() }
            ?: paywallInstance.loggedInUser?.subscriptions
                ?.firstOrNull { it.sku == paywallInstance.loggedInUser?.subSku }
                ?.subscriptionId?.takeIf { it.isNotBlank() }
            ?: "N/A"

        val subscriptionName = PaywallService.getConnector().getPrefPaywallSubShortTitle()
            ?.takeIf { it.isNotBlank() } ?: "N/A"

        val product = paywallInstance.loggedInUser?.subscriptions
            ?.firstOrNull { it.sku == paywallInstance.loggedInUser?.subSku }
            ?.product?.takeIf { it.isNotBlank() }
            ?: paywallInstance.loggedInUser?.accessLevel
                ?.takeIf { it.isNotBlank() && it != PaywallConstants.WP_PRODUCT_NO }
            ?: "N/A"

        val productSku = paywallInstance.inAppSubProductId?.takeIf { it.isNotBlank() }
            ?: billingSubscription?.storeProductId?.takeIf { it.isNotBlank() }
            ?: paywallInstance.loggedInUser?.subSku?.takeIf { it.isNotBlank() }
            ?: "N/A"

        val autoRenewal = if (isSignedIn) paywallInstance.loggedInUser?.isProductRenewable else null

        val expirationDisplay = billingSubscription?.expirationDate?.takeIf { it > 0 }
            ?.let { formatEpochMillis(it) }
            ?: PaywallService.getPaywallPrefHelper().prefLastSubExpirationDate.takeIf { it > 0 }
                ?.let { formatEpochMillis(it) }
            ?: paywallInstance.loggedInUser?.accessExpiry?.takeIf { it.isNotBlank() }
                ?.let { parseAndFormatServerDate(it) ?: it }
            ?: "N/A"

        val status = PaywallService.getConnector().iapSubscriptionStatus
            ?.takeIf { it.isNotBlank() && it != PaywallConstants.EMPTY }
            ?: paywallInstance.subStatus
            ?: "N/A"

        findPreference<PreferenceCategory>(AppPreferences.PREF_SUBSCRIPTION_CATEGORY)?.let { category ->
            category.isVisible = isDevModeEnabled
            category.removeAll()
            if (!subStatusExists) {
                addPrefToCategory(category, "pref_subscription_none", "Subscription", "N/A")
            } else {
                addPrefToCategory(category, "pref_subscription_id", "ID", subId, copyable = true)
                addPrefToCategory(category, "pref_subscription_name", "Name", subscriptionName)
                addPrefToCategory(category, "pref_subscription_product", "Product", product)
                addPrefToCategory(category, "pref_subscription_product_sku", "Product SKU", productSku)
                addPrefToCategory(category, "pref_subscription_source", "Source", settingsViewModel.getSubSourceText())
                addPrefToCategory(category, "pref_subscription_ad_free", "Ad Free", if (PaywallReactive.isAdFree.value) "Yes" else "No")
                addPrefToCategory(category, "pref_subscription_auto_renewal", "Auto Renewal", if (autoRenewal == true) "Yes" else "No")
                addPrefToCategory(category, "pref_subscription_status", "Status", status)
                addPrefToCategory(category, "pref_subscription_expiration", "Expiration", expirationDisplay)
                addPrefToCategory(category, "pref_subscription_state", "State", paywallInstance.subStateString ?: "N/A")
                addPrefToCategory(category, "pref_subscription_grace_period", "Grace Period", if (paywallInstance.isSubInGracePeriod()) "Yes" else "No")
            }
        }
    }

    private fun updateAddOnSection(
        paywallInstance: PaywallService,
        billingSubscription: Subscription?,
    ) {
        val addOnSubs = billingSubscription?.addonSubscriptions?.takeIf { it.isNotEmpty() }
            ?: paywallInstance.loggedInUser?.subscriptions
                ?.filter { it.sku != paywallInstance.loggedInUser?.subSku }
                ?.takeIf { it.isNotEmpty() }

        findPreference<PreferenceCategory>(AppPreferences.PREF_ADD_ON_CATEGORY)?.let { category ->
            if (addOnSubs != null) {
                category.isVisible = isDevModeEnabled
                category.removeAll()
                addOnSubs.forEachIndexed { index, addOn ->
                    val prefix = "add_on_$index"
                    addPrefToCategory(category, "${prefix}_product", "Product", addOn.product ?: "N/A")
                    addPrefToCategory(category, "${prefix}_id", "ID", addOn.subscriptionId ?: "N/A", copyable = true)
                    addPrefToCategory(category, "${prefix}_sku", "Product SKU", addOn.sku ?: "N/A")
                    addPrefToCategory(category, "${prefix}_ad_free", "Ad Free", if (PaywallReactive.isAdFreeProduct(addOn)) "Yes" else "No")
                    addPrefToCategory(category, "${prefix}_status", "Status", addOn.subStatus ?: "N/A")
                    addPrefToCategory(
                        category, "${prefix}_expiration", "Expiration",
                        addOn.expirationDate?.takeIf { it.isNotBlank() }
                            ?.let { parseAndFormatServerDate(it) ?: it } ?: "N/A"
                    )
                }
            } else {
                category.isVisible = false
            }
        }
    }

    private fun updateNetworkingSection() {
        findPreference<PreferenceCategory>(AppPreferences.PREF_NETWORKING_CATEGORY)?.let { category ->
            category.isVisible = isDevModeEnabled
            category.removeAll()

            val prefHelper = PaywallService.getPaywallPrefHelper()
            val userVerifiedTime = prefHelper.getLastVerifyUserTime()
            val subVerifiedTime = prefHelper.getLastVerifyDeviceSubTime()

            addPrefToCategory(
                category, "pref_networking_user_verified", "User Verified",
                if (userVerifiedTime > 0) formatEpochMillis(userVerifiedTime) else "Never"
            )
            addPrefToCategory(
                category, "pref_networking_sub_verified", "Subscription Verified",
                if (subVerifiedTime > 0) formatEpochMillis(subVerifiedTime) else "Never"
            )

            Preference(requireContext()).apply {
                key = "pref_networking_reset"
                title = "Reset Verification Dates"
                layoutResource = R.layout.preference_item_with_divider
                isSelectable = true
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    prefHelper.setPrefVerifyUserLastTime(0)
                    prefHelper.setPrefVerifyDeviceSubLastTime(0)
                    settingsViewModel.signInState.value?.let { updateDebugUI(it) }
                    Toast.makeText(requireContext(), "Reset", Toast.LENGTH_SHORT).show()
                    true
                }
                category.addPreference(this)
            }
        }
    }
}