package com.wapo.flagship.features.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.billing.NativePaywallListenerActivity
import com.washingtonpost.android.paywall.billing.NativePaywallResultCallbacks
import com.washingtonpost.android.paywall.billing.NativePaywallListenerResultCallbackManager
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.Utils
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.wapo.android.commons.util.Utils as CommonsUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManageSubscriptionFragment : BasePreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    private val TAG = "ManageSubFragment"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_manage_subscription, rootKey)

        findPreference<Preference>("cancel_ad_free")?.onPreferenceClickListener = this
        findPreference<Preference>("manage_or_cancel_subscription")?.onPreferenceClickListener = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeSubscriptionState()
    }

    private fun observeSubscriptionState() {
        settingsViewModel.subscriptionState.observe(viewLifecycleOwner) {
            updateUI()
        }
    }

    private fun updateUI() {
        val formattedDate = settingsViewModel.getExpireRenewalDateFormatted() ?: ""
        findPreference<Preference>("manage_or_cancel_subscription")?.summary =
            getString(R.string.manage_subscription_summary, formattedDate)
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "cancel_ad_free" -> {
                if (settingsViewModel.shouldCancelAdFreeViaWeb()) {
                    Utils.startWebActivity(
                        settingsViewModel.getAdFreeManageSubUrl(),
                        activity,
                        false,
                        true,
                    )
                } else {
                    showCancelAdFreeBottomSheet()
                }
                return true
            }
            "manage_or_cancel_subscription" -> {
                if (settingsViewModel.isAmazonSub()) {
                    openAmazonStore()
                } else {
                    openPlayStore()
                }
                return true
            }
        }
        return false
    }

    private fun showCancelAdFreeBottomSheet() {
        val paywallService = PaywallService.getInstance()

        // Check cached device subscription first (covers anonymous users)
        val cachedProductId = paywallService.inAppSubProductId
        val isPremium = if (!cachedProductId.isNullOrEmpty()) {
            isPremiumSku(cachedProductId)
        } else {
            // Fall back to logged-in user's access level
            val accessLevel = paywallService.wapoAccessServiceInstance.currentSubscriptionType()
            accessLevel == PaywallConstants.WP_PREMIUM || accessLevel == PaywallConstants.WP_PRODUCT_ALL
        }
        val planName = if (isPremium) "Premium" else "Core"

        val bottomSheet = CancelAdFreeBottomSheet.newInstance(planName)
        bottomSheet.onContinue = {
            startCancelFlow()
        }
        bottomSheet.show(childFragmentManager, CancelAdFreeBottomSheet.TAG)
    }

    /**
     * Reverse-looks up the given SKU in productToSkuMap to determine if it's a premium product.
     */
    private fun isPremiumSku(sku: String): Boolean {
        val productToSkuMap = ConfigManager.getInstance().config.paywallConf.productToSkuMap
            ?: return false
        for ((productName, entry) in productToSkuMap) {
            val entrySku = if (CommonsUtils.isAmazonBuild()) entry.amazon else entry.playstore
            if (entrySku == sku) {
                return productName.startsWith("premium", ignoreCase = true)
            }
        }
        return false
    }

    private fun startCancelFlow() {
        val baseProductId = settingsViewModel.getAdFreeCancelBaseProductId()
        val adFreeSku = settingsViewModel.getSubscribedAdFreeSkuForCancel()

        if (adFreeSku != null && baseProductId != null) {
            val intent = NativePaywallListenerActivity.getRemoveAddOnIntent(
                requireContext(),
                baseProductId,
                adFreeSku,
                null
            )
            
            val requestCode = System.currentTimeMillis()
            intent.putExtra(PaywallConstants.REQUEST_CODE, requestCode)
            NativePaywallListenerResultCallbackManager.registerCallback(requestCode, object : NativePaywallResultCallbacks {

                override fun onSuccess(context: Context, responseCode: Int) {
                    activity?.runOnUiThread {
                        if (isAdded) {
                            try {
                                findNavController().popBackStack(R.id.settingsFragment, false)
                            } catch (e: Exception) {
                                Logger.e(TAG, "Error popping back to settings", e)
                            }
                        }
                    }
                }

                override fun onError(context: Context, responseCode: Int) {
                    Logger.e(TAG, "Cancel Ad Free error: $responseCode")
                }

                override fun onCanceled(
                    context: Context,
                    responseCode: Int
                ) {
                    Logger.d(TAG, "Cancel Ad Free canceled.")
                }
            })
            startActivity(intent)
        } else {
            Logger.w(TAG, "Cannot cancel Ad Free: subscribedAdFreeSku or baseProductId is null.")
        }
    }
}
