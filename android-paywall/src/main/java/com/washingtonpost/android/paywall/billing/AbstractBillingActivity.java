package com.washingtonpost.android.paywall.billing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.wapo.android.commons.iterable.AttributionKt;
import com.wapo.android.commons.util.Logger;

import com.wapo.android.commons.logs.EventLog;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.newdata.model.Subscription;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 7/17/13
 * Time: 12:11 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractBillingActivity extends FragmentActivity {
    private static final String TAG = AbstractBillingActivity.class.getName();

    public interface ResponseCode {
        int RESULT_OK = Activity.RESULT_OK;
        int RESULT_CANCELED = Activity.RESULT_CANCELED;
        int RESULT_INVALID_OFFER = 1;
        int RESULT_ERROR = 2;
    }

    protected PaywallService localPaywallService;
    protected Fragment fragment;
    protected StoreBillingHelper.InitResult billingInitResult = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!PaywallService.initialized()) {
            finish();
        } else {
            localPaywallService = new PaywallService(this);
            initBilling();
        }
    }

    protected boolean isAddOnsFlow() {
        Intent intent = getIntent();
        if (intent == null) return false;

        // Add-ons purchase flow (base+addons or addonsOnly)
        java.util.ArrayList<String> addOns =
                intent.getStringArrayListExtra(NativePaywallListenerActivity.ADD_ON_PRODUCT_IDS_TO_PURCHASE);
        if (addOns != null && !addOns.isEmpty()) return true;

        // Remove add-on flow (if you added this intent extra)
        String actionType = intent.getStringExtra(NativePaywallListenerActivity.ACTION_TYPE);
        return NativePaywallListenerActivity.ACTION_REMOVE_ADDON.equals(actionType);
    }

    protected boolean isUpdateSubscriptionFlow() {
        Intent intent = getIntent();
        if (intent == null) return false;

        String actionType = intent.getStringExtra(NativePaywallListenerActivity.ACTION_TYPE);
        return NativePaywallListenerActivity.ACTION_UPGRADE.equals(actionType)
                || NativePaywallListenerActivity.ACTION_DOWNGRADE.equals(actionType);
    }

    protected void initBilling() {
        localPaywallService.getBillingHelper().init(
                getApplicationContext(),
                new StoreBillingHelper.StoreHelperInitCallback() {
                    @Override
                    public void initializedWithResult(
                            StoreBillingHelper.InitResult result) {
                        if (!result.successfull) {
                            showErrorFragment(ResponseCode.RESULT_ERROR, "Initialization error");
                            return;
                        }
                        billingInitResult = result;
                        Subscription subs = localPaywallService.getBillingHelper().cachedSubscription();
                        PaywallService.getInstance().getBillingHelper()
                                .setCachedSubscription(subs);

                        //We should fetchLWA or call verifyDeviceSub only for device in app purchases
                        if (PaywallService.getInstance().isSubActive() && !isAddOnsFlow() && !isUpdateSubscriptionFlow()) {
                            onPurchaseFlowComplete();
                            Logger.i(TAG, "Restored purchase");
                        } else {
                            // For add-ons and update subscription flows, even if base sub is active, we must proceed to initBillingFinished()
                            runOnUiThread(() -> initBillingFinished());
                        }
                    }
                }
        );
    }

    protected void initBillingFinished() {}

    public void startAddAccountFlow() {
        localPaywallService.getBillingHelper().startAddAccountFlow(this, new StoreBillingHelper.StoreHelperAddAccountCallback() {
            @Override
            public void accountAddedWithResult(boolean result) {
                if (result) {
                    localPaywallService.getBillingHelper().cleanup();
                    onSkipNowSelectedOnPaywall();
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + ","
                + data);

        // Pass on the activity result to the helper for handling
        if (!localPaywallService.getBillingHelper()
                .handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Logger.d(TAG, "onActivityResult handled by store helper.");
        }
    }

    abstract void showErrorFragment(int errorCode, String errMsg);

    abstract void showProgressBar();

    abstract void hideProgressBar();

    abstract void onSkipNowSelectedOnPaywall();

    abstract void verifyDeviceSub();

    /**
     * Method invoked after a successful purchase operation
     */
    protected void onPurchaseComplete() {
        PaywallService.getOmniture().trackPurchaseComplete(PaywallService.getConnector().getStoreType(),
                AttributionKt.getAttributionInfo(getIntent().getExtras()),
                getIntent().getStringExtra(NativePaywallListenerActivity.MESSAGE_TRACKING_CAMPAIGN_NAME),
                getIntent().getStringExtra(NativePaywallListenerActivity.MESSAGE_TRACKING_OFFER_TYPE));
        onPurchaseFlowComplete();
    }

    /**
     * Use this method to do operations usually done after a purchase
     */
    protected void onPurchaseFlowComplete() {
        PaywallService.getConnector().resetLoginAfterIapFlag();
        verifyDeviceSub();
    }

    protected void startUpdateSubscriptionFlow(
            String currentProductId,
            String targetProductId
    ) {
        PaywallService.getConnector().breadcrumb(
                "Starting update subscription flow from =" + currentProductId + " to =" + targetProductId
        );

        final String startProductId = PaywallService.getInstance().getInAppSubProductId();

        PaywallService.getBillingHelper().startSubscriptionUpdateFlow(
                this,
                currentProductId,
                targetProductId,
                result -> {
                    switch (result.getStatus()) {
                        case RESULT_INVALID_OFFER:
                            showErrorFragment(ResponseCode.RESULT_INVALID_OFFER, result.getMessage());
                            return;
                        case RESULT_ERROR:
                            showErrorFragment(ResponseCode.RESULT_ERROR, result.getMessage());
                            return;
                        case RESULT_CANCELED:
                            onSkipNowSelectedOnPaywall();
                            return;
                        default:
                            break;
                    }

                    Subscription subs = localPaywallService.getBillingHelper().cachedSubscription();
                    PaywallService.getInstance().getBillingHelper().setCachedSubscription(subs);

                    onPurchaseComplete();

                    String updatedProductId = PaywallService.getInstance().getInAppSubProductId();
                    if (!java.util.Objects.equals(startProductId, updatedProductId)) {
                        PaywallService.getConnector().onSubscriptionItemChanged(true);
                    }
                }
        );
    }

    protected void startRemoveAddOnFlow(String baseProductId, String addOnProductId) {
        PaywallService.getConnector().breadcrumb(
                "Starting REMOVE add-on flow baseProductId=" + baseProductId + " addOn=" + addOnProductId
        );

        localPaywallService.getBillingHelper().removeAddOnFromSubscription(
                this,
                baseProductId,
                addOnProductId,
                result -> {
                    switch (result.getStatus()) {
                        case RESULT_INVALID_OFFER:
                            showErrorFragment(ResponseCode.RESULT_INVALID_OFFER, result.getMessage());
                            return;
                        case RESULT_ERROR:
                            showErrorFragment(ResponseCode.RESULT_ERROR, result.getMessage());
                            return;
                        case RESULT_CANCELED:
                            onSkipNowSelectedOnPaywall();
                            return;
                        default:
                            break;
                    }

                    // Treat as a completed flow; entitlement changes may be deferred by Play until period end.
                    onPurchaseComplete();
                }
        );
    }

    protected void startPurchaseFlowWithAddOns(
            String baseProductId,
            java.util.List<String> addOnProductIds
    ) {
        PaywallService.getConnector().breadcrumb(
                "Starting add-ons purchase flow with baseProductId=" + baseProductId
                        + " addOns=" + addOnProductIds
        );

        final boolean startSubscriptionStatus = localPaywallService.isPremiumUser();
        final boolean startAdFreeStatus = localPaywallService.shouldEnableAdfreeExperience();

        localPaywallService.getBillingHelper().startPurchaseFlowWithAddOns(
                AbstractBillingActivity.this,
                baseProductId,
                addOnProductIds,
                result -> {
                    switch (result.getStatus()) {
                        case RESULT_INVALID_OFFER:
                            Logger.d("AbstractBillingActivity", "startPurchaseFlowWithAddOns result=" + result);
                            showErrorFragment(ResponseCode.RESULT_INVALID_OFFER, result.getMessage());
                            return;
                        case RESULT_ERROR:
                            Logger.d("AbstractBillingActivity", "startPurchaseFlowWithAddOns result=" + result);
                            showErrorFragment(ResponseCode.RESULT_ERROR, result.getMessage());
                            return;
                        case RESULT_CANCELED:
                            onSkipNowSelectedOnPaywall();
                            return;
                        default: // RESULT_OK
                            Logger.d("AbstractBillingActivity", "startPurchaseFlowWithAddOns result=" + result);
                            break;
                    }

                    Subscription subs = localPaywallService.getBillingHelper().cachedSubscription();
                    PaywallService.getInstance().getBillingHelper().setCachedSubscription(subs);

                    onPurchaseComplete();

                    boolean updatedStatus = localPaywallService.isPremiumUser();
                    boolean updatedAdFreeStatus = localPaywallService.shouldEnableAdfreeExperience();
                    if (startSubscriptionStatus != updatedStatus) {
                        PaywallService.getConnector().onSubscriptionStatusChanged(updatedStatus);
                    }
                    if (startAdFreeStatus != updatedAdFreeStatus) {
                        PaywallService.getConnector().onSubscriptionItemChanged(updatedAdFreeStatus);
                    }
                }
        );
    }

    protected void startPurchaseFlow() {
        PaywallService.getConnector().breadcrumb("Starting purchase flow with Product ID: "
                + PaywallService.getInstance().getBillingHelper().getSubscriptionProductId());
      //  PaywallService.getConnector().logBuyNowEvent(PaywallService.getBillingHelper().getSubscriptionProductId());

        final boolean startSubscriptionStatus = localPaywallService.isPremiumUser();
        final boolean startAdFreeStatus = localPaywallService.shouldEnableAdfreeExperience();

            localPaywallService.getBillingHelper().startPurchaseFlow(
                    AbstractBillingActivity.this,
                    result -> {
                        switch (result.getStatus()) {
                            case RESULT_INVALID_OFFER:
                                PaywallService.getConnector().logE(new EventLog.Builder().setMessage("purchaseComplete result=" + result));
                                showErrorFragment(ResponseCode.RESULT_INVALID_OFFER, result.getMessage());
                                return;
                            case RESULT_ERROR:
                                PaywallService.getConnector().logE(new EventLog.Builder().setMessage("purchaseComplete result=" + result));
                                showErrorFragment(ResponseCode.RESULT_ERROR, result.getMessage());
                                return;
                            case RESULT_AMAZON_GENERIC_FAILURE:
                                PaywallService.getConnector().logD(new EventLog.Builder().setMessage("purchaseComplete result=" + result));
                                showErrorFragment(ResponseCode.RESULT_ERROR, result.getMessage());
                                return;
                            case RESULT_CANCELED:
                                PaywallService.getConnector().logD(new EventLog.Builder().setMessage("purchaseComplete result=" + result));
                                onSkipNowSelectedOnPaywall();
                                return;
                            default: // RESULT_OK
                                PaywallService.getConnector().logD(new EventLog.Builder().setMessage("purchaseComplete result=" + result));
                                break;
                        }

                        Subscription subs = localPaywallService.getBillingHelper().cachedSubscription();
                        PaywallService.getInstance().getBillingHelper()
                                .setCachedSubscription(subs);
                        onPurchaseComplete();
                        boolean updatedStatus = localPaywallService.isPremiumUser();
                        boolean updatedAdFreeStatus = localPaywallService.shouldEnableAdfreeExperience();
                        if (startSubscriptionStatus != updatedStatus) {
                            PaywallService.getConnector().onSubscriptionStatusChanged(updatedStatus);
                        }
                        if (startAdFreeStatus != updatedAdFreeStatus) {
                            PaywallService.getConnector().onSubscriptionItemChanged(updatedAdFreeStatus);
                        }
                    }
            );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localPaywallService.getBillingHelper().cleanup();
    }
}
