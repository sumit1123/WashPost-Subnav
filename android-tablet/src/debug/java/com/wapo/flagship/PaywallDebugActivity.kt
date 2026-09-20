package com.wapo.flagship

import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import com.washingtonpost.android.paywall.helper.WpPaywallHelper
import com.washingtonpost.android.paywall.newdata.model.PaywallResult
import com.washingtonpost.android.paywall.util.PaywallConstants
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by elamgodilj on 8/17/17.
 */

@AndroidEntryPoint
class PaywallDebugActivity :
    BaseDebugActivity(),
    View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var wrapContentLayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        var layout = LinearLayout(this)
        layout.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            )
        layout.orientation = LinearLayout.VERTICAL

        var launchPaywallButton = Button(this)
        launchPaywallButton.layoutParams = wrapContentLayoutParams
        launchPaywallButton.text = "Launch Native Paywall"
        launchPaywallButton.tag = "Launch Native Paywall"
        launchPaywallButton.setOnClickListener(this)
        layout.addView(launchPaywallButton)

        var invokeVerifyDevice = Button(this)
        invokeVerifyDevice.layoutParams = wrapContentLayoutParams
        invokeVerifyDevice.text = "Invoke Verify Device Call"
        invokeVerifyDevice.tag = "Invoke Verify Device Call"
        invokeVerifyDevice.setOnClickListener(this)
        layout.addView(invokeVerifyDevice)

        var invokeLinkDevice = Button(this)
        invokeLinkDevice.layoutParams = wrapContentLayoutParams
        invokeLinkDevice.text = "Invoke Link Device Call"
        invokeLinkDevice.tag = "Invoke Link Device Call"
        invokeLinkDevice.setOnClickListener(this)
        layout.addView(invokeLinkDevice)

        var invokeVerifyUserCall = Button(this)
        invokeVerifyUserCall.layoutParams = wrapContentLayoutParams
        invokeVerifyUserCall.text = "Invoke Verify User Call"
        invokeVerifyUserCall.tag = "Invoke Verify User Call"
        invokeVerifyUserCall.setOnClickListener(this)
        layout.addView(invokeVerifyUserCall)

        var showCachedSubscriptionResponse = Button(this)
        showCachedSubscriptionResponse.layoutParams = wrapContentLayoutParams
        showCachedSubscriptionResponse.text = "Show cached subscription's response"
        showCachedSubscriptionResponse.tag = "Show cached subscription's response"
        showCachedSubscriptionResponse.setOnClickListener(this)
        layout.addView(showCachedSubscriptionResponse)

        var showLoggedInUsersResponse = Button(this)
        showLoggedInUsersResponse.layoutParams = wrapContentLayoutParams
        showLoggedInUsersResponse.text = "Show logged in user's response"
        showLoggedInUsersResponse.tag = "Show logged in user's response"
        showLoggedInUsersResponse.setOnClickListener(this)
        layout.addView(showLoggedInUsersResponse)

        setContentView(layout)
    }

    override fun onClick(v: View?) {
        when (v?.tag) {
            "Launch Native Paywall" -> {
                showWallDialog(
                    PaywallService.getInstance().isWpUserLoggedIn,
                    PaywallConstants.getBundle(PaywallConstants.METERED, -1),
                )
            }

            "Invoke Verify Device Call" -> {
                var cachedSubscription = PaywallService.getBillingHelper().cachedSubscription()
                if (cachedSubscription == null) {
                    showNoResponseToast("null subs. Cannot call verify")
                } else {
                    var task =
                        object : AsyncTask<Void, Void, PaywallResult>() {
                            @Deprecated("Deprecated in Java")
                            override fun doInBackground(vararg params: Void?): PaywallResult =
                                PaywallService.getInstance().apiServiceInstance.verifyDeviceSubscription(
                                    false,
                                    false,
                                )

                            @Deprecated("Deprecated in Java")
                            override fun onPostExecute(result: PaywallResult?) {
                                super.onPostExecute(result)
                                if (result == null) {
                                    showNoResponseToast("null result after verify device call")
                                } else {
                                    showCachedSubscription()
                                }
                            }
                        }
                    task.execute()
                }
            }

            "Invoke Verify User Call" -> {
                val loggedInUser = WpPaywallHelper.getLoggedInUser()
                if (loggedInUser == null) {
                    showNoResponseToast("user not logged in. So no call")
                } else {
                    Thread(
                        Runnable {
                            val accessToken =
                                AuthHelper
                                    .getInstance(
                                        PaywallService.getInstance().context,
                                    ).accessToken

                            val result =
                                if (PaywallService.getConnector() != null && accessToken != null) {
                                    PaywallService.getInstance().apiServiceInstance.getUserProfile(
                                        accessToken,
                                        PaywallService.getConnector().clientId,
                                    )
                                } else {
                                    null
                                }

                            runOnUiThread {
                                if (result != null && result) {
                                    showLoggedinUserResponse()
                                } else {
                                    showNoResponseToast("get user profile failed")
                                }
                            }
                        },
                    ).start()
                }
            }

            "Show cached subscription's response" -> showCachedSubscription()

            "Show logged in user's response" -> showLoggedinUserResponse()
        }
    }

    fun showCachedSubscription() {
        var cachedSubscription = PaywallService.getBillingHelper().classicOrRainbowSubscription
        if (cachedSubscription == null) {
            showNoResponseToast()
        } else {
            showDialog(
                "storeUID: ${cachedSubscription.storeUID} " +
                    "\nstoreProductId: ${cachedSubscription.storeProductId} \ndeviceID: ${cachedSubscription.deviceID} \nstoreEnv: ${cachedSubscription.storeEnv} " +
                    "\nprovisional: ${cachedSubscription.isProvisional} \nreceiptInfo: ${cachedSubscription.receiptInfo} \ntransactionDate: ${cachedSubscription.transactionDate}" +
                    "\nstartDate: ${cachedSubscription.startDate} \nexpirationDate: ${cachedSubscription.expirationDate} \nstoreType: ${cachedSubscription.storeType} \nvalidity: ${cachedSubscription.validity}" +
                    "\nisSynced: ${cachedSubscription.isSynced} \nisVerified: ${cachedSubscription.isVerified}",
            )
        }
    }

    fun showLoggedinUserResponse() {
        var wpUser = PaywallService.getInstance().loggedInUser
        if (wpUser == null) {
            showNoResponseToast()
        } else {
            showDialog(
                "userId: ${wpUser.userId} \nuuid: ${wpUser.uuid} \naccessLevel: ${wpUser.accessLevel} \naccessExpiry: ${wpUser.accessExpiry} \n" +
                    "accessPurchaseLocation: ${wpUser.accessPurchaseLocation} " +
                    "\nsignedInThrough: ${wpUser.signedInThrough} \nisCCExpired: ${wpUser.isCCExpired} " +
                    "\npartnerId: ${wpUser.partnerId} \npartnerName: ${wpUser.partnerName} \nsubStatus: ${wpUser.subStatus}",
            )
        }
    }
}
