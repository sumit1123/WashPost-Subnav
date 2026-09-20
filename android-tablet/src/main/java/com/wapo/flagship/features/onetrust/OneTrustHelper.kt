package com.wapo.flagship.features.onetrust

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.wapo.android.commons.util.Logger
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import com.onetrust.otpublishers.headless.Public.DataModel.OTProfileSyncParams
import com.onetrust.otpublishers.headless.Public.DataModel.OTSdkParams
import com.onetrust.otpublishers.headless.Public.DataModel.OTUXParams
import com.onetrust.otpublishers.headless.Public.OTCallback
import com.onetrust.otpublishers.headless.Public.OTConsentInteractionType
import com.onetrust.otpublishers.headless.Public.OTPublishersHeadlessSDK
import com.onetrust.otpublishers.headless.Public.Response.OTResponse
import com.onetrust.otpublishers.headless.UI.UIType
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.util.PrefUtils
import com.wapo.view.NestedScrollWebView
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.OneTrustConfig
import com.washingtonpost.android.paywall.PaywallService
import org.json.JSONObject
import java.util.Locale
import androidx.core.content.edit
import com.onetrust.otpublishers.headless.Public.Keys.OTGppKeys

object OneTrustHelper {
    private val TAG = OneTrustHelper.javaClass.simpleName
    private const val TEST_MOBILE_APP_ID = "1f5bb97a-1afd-44af-81f7-0ca0e64c9b5e-test"
    private val config: OneTrustConfig = ConfigManager.getInstance().config.oneTrustConfig

    val ot by lazy { OTPublishersHeadlessSDK(FlagshipApplication.getInstance().applicationContext) }

    private val _initializationState: LiveEvent<OneTrustInitializationState> = LiveEvent()
    val initializationState: LiveData<OneTrustInitializationState> = _initializationState

    private val _consentState: LiveEvent<OneTrustConsentState> = LiveEvent()
    val consentState: LiveData<OneTrustConsentState> = _consentState

    // Consent Category items states
    private val _performanceState: LiveEvent<Boolean> = LiveEvent()
    val performanceState: LiveData<Boolean> = _performanceState
    private val _functionalityState: LiveEvent<Boolean> = LiveEvent()
    val functionalityState: LiveData<Boolean> = _functionalityState
    private val _targetingState: LiveEvent<Boolean> = LiveEvent()
    val targetingState: LiveData<Boolean> = _targetingState
    private val _socialMediaState: LiveEvent<Boolean> = LiveEvent()
    val socialMediaState: LiveData<Boolean> = _socialMediaState

    private val oneTrustBroadcastReceivers =
        OneTrustBroadcastReceivers(
            FlagshipApplication.getInstance().applicationContext,
            _performanceState,
            _functionalityState,
            _targetingState,
            _socialMediaState,
        )

    /**
     * For checking Device Locale.
     * All EU + all EEA + UK + Switzerland.
     */
    private val gdprCountriesList =
        listOf(
            "AT",
            "BE",
            "BG",
            "HR",
            "CY",
            "CZ",
            "DK",
            "EE",
            "FI",
            "FR",
            "DE",
            "GR",
            "HU",
            "IE",
            "IS",
            "IT",
            "LI",
            "LV",
            "LT",
            "LU",
            "MT",
            "NL",
            "NO",
            "PL",
            "PT",
            "RO",
            "SK",
            "SI",
            "ES",
            "SE",
            "GB",
            "CH",
        )

    init {
        _initializationState.value = OneTrustInitializationState.UNINITIALIZED
    }

    /**
     * Gets OT sync credentials from profile.
     * Identifier is Profile's UUID for that user, encrypted.
     * ProfileAuth is the JWT from Profile needed to sync with OT.
     * It must contain a match with the Identifier or the sync will be rejected.
     */
    private fun getProfile(): OneTrustProfile? {
        if (PaywallService.initialized()) {
            PaywallService.getInstance()?.let { service ->
                if (service.isWpUserLoggedIn) {
                    val identifier = service.profileIdentifier
                    val profileAuth = service.profileAuth
                    return if (identifier != "" &&
                        identifier != null &&
                        profileAuth != "" &&
                        profileAuth != null
                    ) {
                        OneTrustProfile(identifier, profileAuth)
                    } else {
                        null
                    }
                }
            }
        }
        return null
    }

    fun initSdk(context: Context) {
        val useTestEnvironment =
            AppContextUtils.isDebuggableBuild() && PrefUtils.getPrefOneTrustStage(context)
        Logger.d(
            TAG,
            "OTDebug, startSDK(), environment=${if (useTestEnvironment) "test" else "production"}",
        )
        val profile: OneTrustProfile? = getProfile()
        val domainIdentifier =
            if (useTestEnvironment) {
                TEST_MOBILE_APP_ID
            } else {
                WapoSecDataProvider.oneTrustDomainIdProd
            }
        try {
            if (AppContextUtils.isDebugBuild()) {
                OTPublishersHeadlessSDK.enableOTSDKLog(Log.DEBUG)
            }
            val otProfileSyncParamsBuilder =
                OTProfileSyncParams.OTProfileSyncParamsBuilder.newInstance()

            // Set profile attributes
            profile?.apply {
                identifier?.let {
                    otProfileSyncParamsBuilder.setIdentifier(it)
                }
                profileAuth?.let {
                    otProfileSyncParamsBuilder.setSyncProfileAuth(it)
                }
                otProfileSyncParamsBuilder.setSyncProfile("true")
            }

            val otProfileSyncParams = otProfileSyncParamsBuilder.build()
            val otUXParamsBuilder = OTUXParams.OTUXParamsBuilder.newInstance()
            val otStyles = context.resources.openRawResource(R.raw.onetrust)
            val oneTrustJsonString = otStyles.reader().use { it.readText() }
            otUXParamsBuilder.setUXParams(
                JSONObject(oneTrustJsonString),
            )
            val otUXParams = otUXParamsBuilder.build()
            val sdkParamsBuilder =
                OTSdkParams.SdkParamsBuilder
                    .newInstance()
                    .setAPIVersion("202604.2.0")
                    .shouldCreateProfile("true")
                    .setProfileSyncParams(otProfileSyncParams)
                    .setOTUXParams(otUXParams)
            val sdkParams = sdkParamsBuilder.build()
            ot.addEventListener(OneTrustEventListener(_consentState))
            ot.writeLogsToFile(false, false) // if true, writes logs to app device storage under files
            _initializationState.value = OneTrustInitializationState.INITIALIZING // reset initialization status before each call to OneTrust
            ot.startSDK(
                config.storageLocation,
                domainIdentifier,
                "EN",
                sdkParams,
                object :
                    OTCallback {
                    /**
                     * Receives geolocation data from OT.
                     * Downloads banner data from OT.
                     * Downloads consent profile data if sync was enabled.
                     */
                    override fun onSuccess(otSuccessResponse: OTResponse) {
                        val otData = otSuccessResponse.responseData
                        Logger.d(
                            TAG,
                            "OTDebug, startSDK, onSuccess, shouldShowBanner=${ot.shouldShowBanner()}, msg=$otData",
                        )
                        _initializationState.postValue(OneTrustInitializationState.SUCCESS)

                        // Fix: re-enable Performance for non-EU (US) users incorrectly opted out
                        // via cross-device sync. Runs at most once per install (guarded by
                        // ConsentManager.Sync.Performance timestamp). isEURegion() uses the
                        // OneTrust SDK's geo resolution to safely exclude EEA/UK/CH users.
                        // TODO : TO cleanup after the Sync stabilizes
                        val fixAlreadyApplied = getPerformanceCategoryFixDate(context) != Long.MIN_VALUE

                        if (!fixAlreadyApplied && !isEURegion() && !isPerformanceEnabled()) {
                            Logger.d(TAG, "OTDebug, applying performance category fix for non-EU user")
                            updatePerformanceConsent(OneTrustConsents.PERFORMANCE, true)
                            setPerformanceCategoryFixDate(context)
                        }

                        // Log after all initialization-time consent updates have been applied.
                        logConsentStateForDebug("initSdk.onSuccess")
                    }

                    /**
                     * Fails to receive data from OT.
                     * No network or OT error.
                     */
                    override fun onFailure(otErrorResponse: OTResponse) {
                        val errorCode = otErrorResponse.responseCode
                        val errorDetails = otErrorResponse.responseMessage
                        Logger.d(TAG, "OTDebug, startSDK, onFailure, error=$otErrorResponse")
                        _initializationState.postValue(OneTrustInitializationState.FAILURE)
                        if (isConsentUndetermined()) {
                            EventLog
                                .Builder()
                                .apply {
                                    setMessage("OneTrust Error")
                                    setModule(LogModules.PRIVACY)
                                    setErrorMessage(
                                        "OneTrust undetermined consent state on OS build version ${Build.VERSION.SDK_INT}. $errorDetails",
                                    )
                                    setErrorCode(errorCode)
                                }.run {
                                    RemoteLog.e(context, build())
                                }
                            if (isEULocale()) { // Defer to device locale if there is no local OT data and we failed to receive remote OT data
                                writeGDPRApplies(context)
                            }
                        } else if (errorCode == 2) { // Error code 2 means request was successful but response was empty -- could be OT config / version issue
                            EventLog
                                .Builder()
                                .apply {
                                    setMessage("OneTrust Error")
                                    setModule(LogModules.PRIVACY)
                                    setErrorMessage("OneTrust init error. $errorDetails")
                                    setErrorCode(errorCode)
                                }.run {
                                    RemoteLog.e(context, build())
                                }
                        }
                    }
                },
            )
        } catch (e: java.lang.Exception) {
            Logger.e(TAG, "Error", e)
        }
    }

    /**
     * If OT response was successful, calls setupUI() which asks OT SDK to decide if the banner should be shown.
     * If OT response failed, checks local OT data for whether the banner should be shown and shows it if so.
     * Prevents user from circumventing banner by going offline once it has been shown.
     */
    fun showBannerIfNeeded(activity: AppCompatActivity) {
        Logger.d(
            TAG,
            "OTDebug, showBannerIfNeeded, sdkStarted=${_initializationState.value?.name}," +
                " isBannerShown=${ot.isBannerShown(activity)}, shouldShowBanner=${ot.shouldShowBanner()}",
        )
        when (_initializationState.value) {
            OneTrustInitializationState.SUCCESS -> {
                ot.setupUI(activity, UIType.BANNER)
            }
            OneTrustInitializationState.FAILURE -> {
                if (ot.shouldShowBanner()) {
                    ot.showBannerUI(activity)
                    _initializationState.postValue(OneTrustInitializationState.SUCCESS)
                }
            }
            else -> {
                // do nothing. await response
            }
        }
    }

    /**
     * Clears all local OT data, to be called on logout.
     * Prevents one account's preferences from carrying over to another account.
     */
    fun clearOneTrustData() {
        Logger.d(TAG, "OTDebug, clearOneTrustData()")
        ot.clearOTSDKData()
    }


    /**
     * Updates the consent state for the given OneTrust purpose/category and immediately
     * persists it via [OTConsentInteractionType.PC_CONFIRM] (Preference Center confirm),
     * which mirrors the SDK behavior of a user tapping "Confirm My Choices". This ensures
     * the change is written to local storage and broadcast to consent listeners so that
     * downstream trackers (e.g. Chartbeat for Performance) react right away.
     *
     * @param consent The OneTrust category to update (e.g. [OneTrustConsents.PERFORMANCE]).
     * @param enabled true to grant consent, false to withdraw it.
     */
    private fun updatePerformanceConsent(consent: OneTrustConsents, enabled: Boolean) {
        ot.updatePurposeConsent(consent.groupId, enabled)
        ot.saveConsent(OTConsentInteractionType.PC_CONFIRM)
    }

    /**
     * Keeps the OneTrust categories that permit personalized advertising in sync with the
     * app's privacy-settings switch.
     */
    fun updatePersonalizedAdvertisingConsent(enabled: Boolean) {
        if (isTargetingEnabled() == enabled && isSocialMediaEnabled() == enabled) return
        val consentSdk = OTPublishersHeadlessSDK(FlagshipApplication.getInstance().applicationContext)
        consentSdk.updatePurposeConsent(OneTrustConsents.TARGETING.groupId, enabled)
        consentSdk.updatePurposeConsent(OneTrustConsents.SOCIAL_MEDIA.groupId, enabled)
        consentSdk.saveConsent(OTConsentInteractionType.PC_CONFIRM)
    }

    /**
     * Returns the timestamp (epoch millis) at which the one-time Performance-category
     * repair for non-EU users was applied on this install, or [Long.MIN_VALUE] if it has
     * never been applied. Used as an idempotency guard so the fix in `initSdk.onSuccess`
     * runs at most once per install.
     */
    private fun getPerformanceCategoryFixDate(context: Context): Long =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getLong("ConsentManager.Sync.Performance", Long.MIN_VALUE)

    /**
     * Stamps the current time (epoch millis) into shared preferences to mark that the
     * one-time Performance-category repair for non-EU users has been applied on this
     * install. After this is written, [getPerformanceCategoryFixDate] will return a
     * non-sentinel value and the fix will be skipped on subsequent launches.
     */
    private fun setPerformanceCategoryFixDate(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit {
                putLong("ConsentManager.Sync.Performance", System.currentTimeMillis())
            }
    }

    /**
     * Checks if the user has ever responded to a OneTrust banner on this account (not necessarily this device).
     * Useful for distinguishing between users who have ever visited EU or not.
     */
    fun isBannerShown(): Boolean {
        val isBannerShown = ot.isBannerShown(FlagshipApplication.getInstance().applicationContext)
        return isBannerShown > 0
    }

    /**
     * Checks if OT banner is currently displayed.
     * Used to avoid covering banner with paywall.
     */
    fun isBannerPresent(activity: AppCompatActivity): Boolean = ot.isOTUIPresent(activity)

    /**
     * Checks whether OT SDK geolocation thinks user is in a GDPR location.
     * Replaces our old GDPR system's method of checking geolocation for purposes such as serving different ads in EU.
     */
    fun isEURegion(): Boolean {
        val isBannerShown = ot.isBannerShown(FlagshipApplication.getInstance().applicationContext)
        // https://developer.onetrust.com/sdk/mobile-apps/android/displaying-ui#shouldshowbanner
        Logger.d(
            TAG,
            "OTDebug, isEURegion(), shouldShowBanner=${ot.shouldShowBanner()}," +
                " isBannerShown=$isBannerShown",
        )
        return ot.shouldShowBanner() || isBannerShown > 0
    }

    /**
     * Checks for an EEA device locale.
     * Fallback when geolocation fails.
     */
    fun isEULocale(): Boolean = gdprCountriesList.contains(Locale.getDefault().country)

    /**
     * Directly asks the OT SDK if the user needs to be shown the banner.
     * Used to check if banner needs to be shown in offline case.
     */
    fun shouldShowBanner(): Boolean = ot.shouldShowBanner()

    /**
     * Checks if Performance consent has been given.
     * False if consent is withdrawn or in uninitialized state (off by default).
     * Used by Chartbeat.
     */
    fun isPerformanceEnabled(): Boolean =
        ot.getConsentStatusForGroupId(
            OneTrustConsents.PERFORMANCE.groupId,
        ) == 1

    /**
     * Checks if Targeting consent has been given.
     * False if consent is withdrawn or in uninitialized state (off by default).
     * Used by RTE/For You.
     */
    fun isTargetingEnabled(): Boolean =
        ot.getConsentStatusForGroupId(
            OneTrustConsents.TARGETING.groupId,
        ) == 1

    /**
     * Checks if Functionality consent has been given.
     * False if consent is withdrawn or in uninitialized state.
     * Used by RTE/For You.
     */
    fun isFunctionalityEnabled(): Boolean =
        ot.getConsentStatusForGroupId(
            OneTrustConsents.FUNCTIONALITY.groupId,
        ) == 1

    /**
     * Checks if Social Media consent has been given.
     * False if consent is withdrawn or in uninitialized state.
     * Used by RTE/For You.
     */
    fun isSocialMediaEnabled(): Boolean =
        ot.getConsentStatusForGroupId(
            OneTrustConsents.SOCIAL_MEDIA.groupId,
        ) == 1

    /**
     * Returns whether all consent categories required for personalized advertising are enabled.
     */
    fun isPersonalizedAdvertisingEnabled(): Boolean =
        isTargetingEnabled() && isSocialMediaEnabled()

    /**
     * Injects consent from app's OneTrust SDK into webview's OneTrust SDK.
     */
    fun passConsent(webView: WebView) {
        val jsToPass: String? = ot.otConsentJSForWebView
        webView.evaluateJavascript("javascript:$jsToPass", null)
    }

    fun passConsent(webView: NestedScrollWebView?) {
        val jsToPass: String? = ot.otConsentJSForWebView
        webView?.evaluateJavascript("javascript:$jsToPass", null)
    }

    fun registerBroadcastReceivers() {
        Logger.d(
            TAG,
            "OTDebug, registerBroadcastReceivers, sdkStarted=${_initializationState.value?.name}",
        )
        oneTrustBroadcastReceivers.registerOTBroadcastReceivers()
    }

    fun unregisterBroadcastReceivers() {
        Logger.d(
            TAG,
            "OTDebug, unregisterBroadcastReceivers, sdkStarted=${_initializationState.value?.name}",
        )
        oneTrustBroadcastReceivers.unregisterOTBroadcastReceivers()
    }

    /**
     * Disables Performance and Targeting category's functionality in undetermined consent state (off by default).
     */
    fun startOrStopExternalLibrariesTracking() {
        oneTrustBroadcastReceivers.apply {
            initializePerformanceTracking(isPerformanceEnabled())
            initializeTargetingTracking(isTargetingEnabled())
        }
        logConsentStateForDebug("startOrStopExternalLibrariesTracking")
    }

    /**
     * Logs current OneTrust consent state to logcat for local debugging only.
     * Values from OT SDK: 1 = granted, 0 = denied, -1 = not collected.
     * Filter with: `adb logcat -s OneTrustHelper` and search "OTDebug, ConsentState".
     */
    private fun logConsentStateForDebug(source: String) {
        if (!AppContextUtils.isDebugBuild()) return
        try {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(FlagshipApplication.getInstance())
            val gppString =
                sharedPreferences.getString(OTGppKeys.IAB_GPP_HDR_GPP_STRING, null)
            val gppSid =
                sharedPreferences.getString(OTGppKeys.IAB_GPP_GPP_SID, null)
            val strictlyNecessary =
                ot.getConsentStatusForGroupId(OneTrustConsents.STRICTLY_NECESSARY.groupId)
            val perf = ot.getConsentStatusForGroupId(OneTrustConsents.PERFORMANCE.groupId)
            val func = ot.getConsentStatusForGroupId(OneTrustConsents.FUNCTIONALITY.groupId)
            val targ = ot.getConsentStatusForGroupId(OneTrustConsents.TARGETING.groupId)
            val soc = ot.getConsentStatusForGroupId(OneTrustConsents.SOCIAL_MEDIA.groupId)
            val hasUndeterminedConsentStatus =
                listOf(strictlyNecessary, perf, func, targ, soc).any { it !in 0..1 }
            val token =
                "C0001:$strictlyNecessary-C0002:$perf-C0003:$func-C0004:$targ-C0005:$soc"
            Logger.d(
                TAG,
                "OTDebug, ConsentState [$source] " +
                    "strictlyNecessary=${statusLabel(strictlyNecessary)}, " +
                    "performance=${statusLabel(perf)}, " +
                    "functionality=${statusLabel(func)}, " +
                    "targeting=${statusLabel(targ)}, " +
                    "socialMedia=${statusLabel(soc)}, " +
                    "consent_token=$token, " +
                    "gppString=$gppString, " +
                    "gppSid=$gppSid, " +
                    "isEURegion=${isEURegion()}, " +
                    "isBannerShown=${isBannerShown()}, " +
                    "hasUndeterminedConsentStatus=$hasUndeterminedConsentStatus, " +
                    "hasPreferenceCenterData=${ot.preferenceCenterData != null}",
            )
        } catch (e: Exception) {
            Logger.e(TAG, "OTDebug, logConsentStateForDebug error", e)
        }
    }

    private fun statusLabel(status: Int): String =
        when (status) {
            1 -> "granted"
            0 -> "denied"
            else -> "not-collected"
        }

    fun isConsentUndetermined(): Boolean = ot.preferenceCenterData == null

    /**
     * Writes that GDPR applies in TC string.
     * OneTrust handles this in most cases; this method is only used when OT call fails and we have no local OT data to fall back on.
     * We call this method only if Device Locale is a GDPR region match.
     */
    private fun writeGDPRApplies(context: Context) {
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putInt("IABTCF_gdprApplies", 1)
        editor.apply()
        Logger.d(TAG, "OTDebug, IABTCF_gdprApplies: 1")
    }

    fun gdprApplies(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getInt("IABTCF_gdprApplies", 0) == 1

    fun getGDPRConsent(context: Context): String? {
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString("IABTCF_TCString", null)
    }

    /**
     * Global Privacy Platform (IAB GPP) consent string.
     * OT writes this the same way it writes the TCF string, but only once GPP is enabled
     * on this app's template in the OneTrust dashboard -- there is no SDK-side flag for it.
     * Null until that's turned on, or before OT has collected consent.
     */
    fun getGppString(context: Context): String? {
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(OTGppKeys.IAB_GPP_HDR_GPP_STRING, null)
    }


    /**
     * Comma-separated IDs of the GPP sections that apply to this user (e.g. "7" for US National,
     * "8" for California). Ad SDKs that consume the GPP string expect this alongside it.
     */
    fun getGppSid(context: Context): String? {
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getString(OTGppKeys.IAB_GPP_GPP_SID, null)
    }
}
