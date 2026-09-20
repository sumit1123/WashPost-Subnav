package com.washingtonpost.android.paywall.billing.amazon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.wapo.android.commons.util.Logger;

import androidx.annotation.Nullable;

import com.amazon.device.iap.PurchasingService;
import com.washingtonpost.android.config.domain.models.config.paywall.ServiceConfigStub;
import com.washingtonpost.android.paywall.billing.AbstractStoreBillingHelper;
import com.washingtonpost.android.paywall.billing.StoreBillingHelper;

import java.util.List;

public class AmazonBillingHelper extends AbstractStoreBillingHelper implements
        AmazonIAPListener.OnReadyListener,
        AmazonIAPListener.OnPurchasedListener,
        AmazonIAPListener.OnUpdatedListener,
        AmazonIAPListener.SkuHolder{

    private static final String TAG = AmazonBillingHelper.class.getSimpleName();
    private static volatile AmazonBillingHelper instance;
    private AmazonIAPListener listener;
    private StoreHelperInitCallback initCallback;
    private StoreHelperPurchaseCallback purchaseCallback;

    private AmazonBillingHelper() {

    }

    public static AmazonBillingHelper getInstance() {
        if (instance == null) {
            synchronized (AmazonBillingHelper.class) {
                if (instance == null) {
                    instance = new AmazonBillingHelper();
                }
            }
        }
        return instance;
    }

    @Override
    public void initAndCheckSubscription(Context ctx, ServiceConfigStub serviceConfig) {
        setValidSubscriptionProductIds(serviceConfig.getValidProductIdList());
        setSubscriptionProductId(serviceConfig.getProductId());
        initIapListener(ctx);
        readSubscriptionFromDB();
        PurchasingService.getUserData();
        PurchasingService.getProductData(serviceConfig.getValidProductIdList());
    }

    @Override
    public void saveSubscriptionProducts(@Nullable StoreHelperInitCallback initCallback) {
        // No action needed here
    }

    private synchronized void initIapListener(Context ctx) {
        if(listener == null) {
            listener = new AmazonIAPListener();
            listener.setOnReadyListener(this);
            listener.setOnPurchasedListener(this);
            listener.setOnUpdatedListener(this);
        }

        PurchasingService.registerListener(ctx, listener);
    }

    @Override
    public void init(Context ctx, StoreHelperInitCallback callback) {
        if (this.isInitialized()) {
            callback.initializedWithResult(new StoreBillingHelper.InitResult(true, "initialized", false));
            return;
        }

        this.initCallback = callback;
        initIapListener(ctx);
        PurchasingService.getUserData();
    }

    @Override
    public boolean isInitialized() {
        return (listener != null &&
                listener.getState() == AmazonIAPListener.ListenerState.STATE_READY);
    }

    @Override
    public boolean isInitializing() {
        return (listener != null &&
                listener.getState() == AmazonIAPListener.ListenerState.STATE_INITIALIZED);
    }

    @Override
    public void cleanup() {
        this.initCallback = null;
    }

    @Override
    public void startPurchaseFlow(Activity parent, StoreHelperPurchaseCallback callback) {
        this.purchaseCallback = callback;
        if (PRODUCT_ID_SUBSCRIPTION != null) {
            PurchasingService.purchase(PRODUCT_ID_SUBSCRIPTION);
        }
    }

    @Override
    public void startPurchaseFlowWithAddOns(Activity parent, String baseProductId, List<String> addOnProductIds, StoreHelperPurchaseCallback callback) {

    }

    @Override
    public void removeAddOnFromSubscription(Activity parent, String baseProductId, String addOnProductId, StoreHelperPurchaseCallback callback) {

    }

    @Override
    public void startSubscriptionUpdateFlow(Activity parent, String currentProductId, String targetProductId, StoreHelperPurchaseCallback callback) {
        if (callback != null) {
            callback.purchaseFinishedWithResult(
                new PurchaseResult(PurchaseResultStatus.RESULT_ERROR, "Subscription updates are not supported")
            );
        }
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return false;
    }

    @Override
    public String getSubscriptionProductId() {
        return PRODUCT_ID_SUBSCRIPTION;
    }

    @Override
    public void setSubscriptionOfferId(String offerId) {
        //do nothing
    }

    @Override
    public String getSubscriptionOfferId() {
        return null;
    }

    @Override
    public void setSubscriptionBasePlanId(String basePlanId) {
        //do nothing
    }

    @Override
    public String getSubscriptionBasePlanId() {
        return null;
    }

    @Override
    public void setSubscriptionProductId(String productId) {
        PRODUCT_ID_SUBSCRIPTION = productId;
    }

    @Override
    public void onPurchased(PurchaseResult result) {
        readSubscriptionFromDB();
        setCachedSubscription(currentSubscription);
        if (purchaseCallback != null) {
            purchaseCallback.purchaseFinishedWithResult(result);
        }
    }

    @Override
    public void onReady(InitResult result) {
        if (initCallback != null) {
            initCallback.initializedWithResult(result);
        }
    }

    @Override
    public void onUpdated(InitResult result) {
    }

    @Override
    public String getSku() {
        return PRODUCT_ID_SUBSCRIPTION;
    }

    @Override
    public String getUserId() {
        if (listener != null) {
            Logger.d(TAG , "listener is not null - userId="+listener.getCurrentUser());
            return listener.getCurrentUser();
        }
        Logger.d(TAG , "listener is null - returning null userId");
        return null;
    }

    @Override
    public boolean isSandboxMode() {
        return PurchasingService.IS_SANDBOX_MODE;
    }

    @Override
    public String getStoreAccountType() {
        return "com.amazon.account";
    }

    @Override
    public boolean canCallVerifyDeviceSubscription() {
        return super.canCallVerifyDeviceSubscription() && getUserId()!=null;
    }
}
