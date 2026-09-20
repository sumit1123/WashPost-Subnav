/*
 * Copyright (C) 2014 Washington Post Android Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.washingtonpost.android.paywall.billing.amazon;

import android.os.AsyncTask;
import com.wapo.android.commons.util.Logger;
import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.internal.model.ProductBuilder;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.ProductType;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserDataResponse;
import com.wapo.android.commons.logs.EventLog;
import com.washingtonpost.android.paywall.BuildConfig;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.billing.StoreBillingHelper;
import com.washingtonpost.android.paywall.newdata.model.IAPSubItems;
import com.washingtonpost.android.paywall.newdata.model.StoreReceipt;
import com.washingtonpost.android.paywall.newdata.model.Subscription;
import com.washingtonpost.android.paywall.util.PaywallConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by muppallav on 11/24/14.
 */
public class AmazonIAPListener implements PurchasingListener {

    private static final String TAG = "AmazonIAPListener";
    private static String currentUser;
    private static ListenerState state;
    private static OnReadyListener onReadyListener;
    private static OnUpdatedListener onUpdatedListener;
    private static OnPurchasedListener onPurchasedListener;
    private static final Map<String, String> marketPlaceCurrencyMap = new HashMap();
    static {
        /**
        CA - Canada - CAD
        JP - Japan - JPY
        UK - United Kingdon - GBP
        US - US - USD
        AU - Australia - AUD
        BR - BRAZIL - BRL
        DE - Germany - EUR
        ES - Spain - EUR
        FR - France - EUR
        IN - India - INR
        IT - ITALY - EUR

        $ 197.12 CAD Amazon.ca
        ¥ 16227 JPY Amazon.co.jp
        £ 115.93 GBP Amazon.co.uk
        $ 149.99 USD Amazon.com
        $ 216.95 AUD Amazon.com.au
        $ 598.60 BRL Amazon.com.br
        € 134.33 EUR Amazon.de
        € 134.33 EUR Amazon.es
        € 134.33 EUR Amazon.fr
        ₹ 10620.79 INR Amazon.in
        € 134.33 EUR Amazon.it
         */
        marketPlaceCurrencyMap.put("ca", "CAD");
        marketPlaceCurrencyMap.put("jp", "JPY");
        marketPlaceCurrencyMap.put("gb", "GBP");
        marketPlaceCurrencyMap.put("uk", "GBP");
        marketPlaceCurrencyMap.put("us", "USD");
        marketPlaceCurrencyMap.put("au", "AUD");
        marketPlaceCurrencyMap.put("br", "BRL");
        marketPlaceCurrencyMap.put("de", "EUR");
        marketPlaceCurrencyMap.put("es", "EUR");
        marketPlaceCurrencyMap.put("fr", "EUR");
        marketPlaceCurrencyMap.put("in", "INR");
        marketPlaceCurrencyMap.put("it", "EUR");
    }
    private static String marketPlace;
    private static String currencyCode;

    public enum ListenerState {
        STATE_IDLE,
        STATE_INITIALIZED,
        STATE_FEATURE_NOT_SUPPORTED,
        STATE_ERROR,
        STATE_READY
    }

    AmazonIAPListener() {
        state=ListenerState.STATE_INITIALIZED;
    }

    @Override
    public void onUserDataResponse(UserDataResponse userDataResponse) {
        Logger.d(TAG, "onGetUserIdResponse recieved: response=" + userDataResponse);
        new GetUserDataAsyncTask().execute(userDataResponse);

    }

    @Override
    public void onProductDataResponse(ProductDataResponse productDataResponse) {
        Logger.d(TAG, "onProductDataResponse recieved: response=" + productDataResponse);
        new ProductDataAsyncTask().execute(productDataResponse);

    }

    @Override
    public void onPurchaseResponse(PurchaseResponse purchaseResponse) {
        Logger.d(TAG, "onPurchaseResponse recieved: response=" + purchaseResponse);
        new PurchaseAsyncTask().execute(purchaseResponse);

    }

    @Override
    public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse purchaseUpdatesResponse) {
        Logger.d(TAG, "onPurchaseUpdatesRecived recieved: response=" + purchaseUpdatesResponse);
        new PurchaseUpdatesAsyncTask().execute(purchaseUpdatesResponse);

    }


    public ListenerState getState() {
        return state;
    }

    public interface OnReadyListener {
        void onReady(StoreBillingHelper.InitResult result);
    }

    public interface OnUpdatedListener {
        void onUpdated(StoreBillingHelper.InitResult result);
    }

    public interface OnPurchasedListener {
        void onPurchased(StoreBillingHelper.PurchaseResult result);
    }

    public interface SkuHolder {
        String getSku();
    }

    public void setOnReadyListener(OnReadyListener onReadyListener) {
        AmazonIAPListener.onReadyListener = onReadyListener;
    }

    public void setOnUpdatedListener(OnUpdatedListener onUpdatedListener) {
        AmazonIAPListener.onUpdatedListener = onUpdatedListener;
    }

    public void setOnPurchasedListener(OnPurchasedListener onPurchasedListener) {
        AmazonIAPListener.onPurchasedListener = onPurchasedListener;
    }


    private static class GetUserDataAsyncTask extends AsyncTask<UserDataResponse, Void, Boolean> {
        UserDataResponse.RequestStatus requestStatus;


        @Override
        protected Boolean doInBackground(final UserDataResponse... params) {
            UserDataResponse getUserDataResponse = params[0];
            requestStatus = getUserDataResponse.getRequestStatus();
            if (requestStatus == UserDataResponse.RequestStatus.SUCCESSFUL) {
                if(getUserDataResponse.getUserData().getUserId() != null) {
                    currentUser = getUserDataResponse.getUserData().getUserId();
                    PaywallService.getConnector().setPrefAmazonUserId(currentUser);
                }
                if (getUserDataResponse.getUserData().getMarketplace() != null) {
                    marketPlace = getUserDataResponse.getUserData().getMarketplace();
                    if (marketPlaceCurrencyMap.containsKey(marketPlace.toLowerCase())) {
                        currencyCode = marketPlaceCurrencyMap.get(marketPlace.toLowerCase());
                    }
                }
                return true;
            } else {
                PaywallService.getConnector().logD(new EventLog.Builder()
                        .setMessage("getUserData: response=" + getUserDataResponse));
                return false;
            }
        }

        /*
         * Call getPurchaseUpdatesRequest for the returned user to sync purchases that are not yet fulfilled.
         */
        @Override
        protected void onPostExecute(final Boolean result) {
            super.onPostExecute(result);
            if (result) {
                Logger.d(TAG, "getUserData successful; calling getPurchaseUpdates");
                PaywallService.getConnector().logD(new EventLog.Builder()
                        .setMessage("AmazonPurchaseUpdatesCall=GetUserDataAsyncTask.getPurchaseUpdates()"));
                PurchasingService.getPurchaseUpdates(true);
            } else if (state == ListenerState.STATE_INITIALIZED && onReadyListener != null) {
                Logger.d(TAG, "getUserData error");
                switch (requestStatus) {
                    case NOT_SUPPORTED:
                        onReadyListener.onReady(new StoreBillingHelper.InitResult(false, "Error initializing user", true));
                        state = ListenerState.STATE_FEATURE_NOT_SUPPORTED;
                        break;
                    case FAILED:
                        onReadyListener.onReady(new StoreBillingHelper.InitResult(false, "Error initializing user", true));
                        state = ListenerState.STATE_ERROR;
                        break;
                }
            }
        }
    }

    private static class ProductDataAsyncTask extends AsyncTask<ProductDataResponse, Void, Void> {
        @Override
        protected Void doInBackground(final ProductDataResponse... params) {
            final ProductDataResponse productDataResponse = params[0];

            Logger.d(TAG, "getProductData: response=" + productDataResponse);
            switch (productDataResponse.getRequestStatus()) {
                case SUCCESSFUL:
                    // Information you'll want to display about your IAP items is here
                    // In this example we'll simply log them.
                    Map<String, Product> productData = productDataResponse.getProductData();

                    // Non-LAT debug builds will not return product data correctly from App Tester.
                    // For this reason, we will load in the product data ourselves
                    if (BuildConfig.DEBUG && productData.isEmpty()) {
                        productData = getDebugProductData();
                    }

                    final IAPSubItems iapSubItems = new IAPSubItems();
                    for (final String key : productData.keySet()) {
                        Product i = productData.get(key);
                        if (i == null) { continue; }
                        Logger.d(TAG, String.format("Item: %s\n Type: %s\n SKU: %s\n Price: %s\n Description: %s\n CurrencyCode: %s\n", i.getTitle(), i.getProductType(), i.getSku(), i.getPrice(), i.getDescription(), currencyCode));
                        iapSubItems.insertItem(
                            new IAPAmazonSubItem(
                                i.getSku(),
                                i.getTitle(),
                                i.getPrice(),
                                currencyCode,
                                null,
                                null,
                                null,
                                null,
                                0
                            )
                        );
                    }
                    PaywallService.getConnector().saveIAPSubItems(iapSubItems);
                    break;
                case FAILED:
                    PaywallService.getConnector().logW(new EventLog.Builder()
                            .setMessage("ProductDataAsyncTask Failed")
                            .set("data", productDataResponse));
                    // On failed responses will fail gracefully.
                    break;

            }

            return null;
        }
    }

    /**
     * Amazon Non-LAT Debug Builds Only: Currently App Tester does not return product data so we return it
     * here so that we have skus on the paywall.
     * @return
     */
    private static Map<String, Product> getDebugProductData() {
        ProductBuilder builder = new ProductBuilder();
        Product product = builder.setSku("m1-r")
                .setProductType(ProductType.SUBSCRIPTION)
                .setTitle("Basic Digital")
                .setDescription("Description")
                .setPrice("3.99")
                .setSmallIconUrl("https://com-amazon-mas-catalog.s3.amazonaws.com/amzn1.devportal.fileupload.9ecf19d00a784cdaa8f3bce22e2e67e9_d548712e-74be-489e-9a3d-4ad846681884_1a4a59ec0ca11d1fd27c9bf741374cac")
                .build();

        Map<String, Product> productMap = new HashMap<>();
        productMap.put("m1-r",product);
        return productMap;
    }

    private static class PurchaseAsyncTask extends AsyncTask<PurchaseResponse, Void, Boolean> {

        private String message = "";
        private StoreBillingHelper.PurchaseResultStatus status;

        @Override
        protected Boolean doInBackground(final PurchaseResponse... params) {
            final PurchaseResponse purchaseResponse = params[0];
            final String userId = currentUser;

            PaywallService.getConnector().logD(new EventLog.Builder()
                    .setMessage("getPurchase: response=" + purchaseResponse));

            if (userId!=null
                    && purchaseResponse.getUserData()!=null
                    && purchaseResponse.getUserData().getUserId()!=null
                    && !purchaseResponse.getUserData().getUserId().equals(userId)) {
                Logger.d(TAG, "PurchaseAsyncTask: purchaseResponse.getUserData().getUserId() " + purchaseResponse.getUserData().getUserId() + " currentUser(Old userId): " + currentUser);
                // currently logged in user is different than what we have so update the state
                currentUser = purchaseResponse.getUserData().getUserId();
                if(currentUser!=null){
                    PaywallService.getConnector().setPrefAmazonUserId(currentUser);
                }
                PaywallService.getConnector().logD(new EventLog.Builder()
                        .setMessage("AmazonPurchaseUpdatesCall=PurchaseAsyncTask.getPurchaseUpdates()"));
                PurchasingService.getPurchaseUpdates(true);
            }
            switch (purchaseResponse.getRequestStatus()) {
                case SUCCESSFUL:
                    Logger.d(TAG, "PurchaseAsyncTask status=SUCCESS ");
                    status = StoreBillingHelper.PurchaseResultStatus.RESULT_OK;
                    final Receipt receipt = purchaseResponse.getReceipt();
                    if (receipt.getProductType() == ProductType.SUBSCRIPTION) {
                        StoreReceipt storeReceipt = new StoreReceipt(
                            receipt.getReceiptId(),
                            receipt.getSku(),
                            receipt.getPurchaseDate() == null ? null : receipt.getPurchaseDate().getTime(),
                            receipt.getCancelDate() == null ? null : receipt.getCancelDate().getTime(),
                            Collections.singletonList(receipt.getSku())
                        );
                        Subscription subscription = PaywallService.getBillingHelper().createSubscription(storeReceipt);
                        if (subscription != null) {
                            PaywallService.getBillingHelper().updateSubscriptionDetails(subscription);
                        }
                    }
                    return true;
                case FAILED:
                    /* Since we don't know if it's an actual failure or just a cancel,
                       we're using this special result status */
                    status = StoreBillingHelper.PurchaseResultStatus.RESULT_AMAZON_GENERIC_FAILURE;
                    message = "Purchase failed or was canceled";
                    PaywallService.getConnector().logW(new EventLog.Builder()
                            .setMessage("PurchaseAsyncTask status=FAILED")
                            .set("data", purchaseResponse));
                    return false;
                case INVALID_SKU:
                    status = StoreBillingHelper.PurchaseResultStatus.RESULT_INVALID_OFFER;
                    message = "Invalid SKU";
                    PaywallService.getConnector().logW(new EventLog.Builder()
                            .setMessage("PurchaseAsyncTask status=INVALID_SKU")
                            .set("data", purchaseResponse));
                    return false;
                case ALREADY_PURCHASED:
                    status = StoreBillingHelper.PurchaseResultStatus.RESULT_INVALID_OFFER;
                    PaywallService.getConnector().logW(new EventLog.Builder()
                            .setMessage("PurchaseAsyncTask status=ALREADY_PURCHASED")
                            .set("data", purchaseResponse));
                    message = "Already Purchased";
                    return false;
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            super.onPostExecute(success);
            if (onPurchasedListener != null) {
                StoreBillingHelper.PurchaseResult result = new StoreBillingHelper.PurchaseResult(status, message);
                onPurchasedListener.onPurchased(result);
            }
            // We are adding this to updated stored receipts, since dollar one promo checks for existing m1-r in saved receipts.
            // Otherwise promo does not go away until next app launch.
            if(success) {
                PurchasingService.getPurchaseUpdates(true);
            }
        }
    }

    private static class PurchaseUpdatesAsyncTask extends AsyncTask<PurchaseUpdatesResponse, Void, Boolean> {

        private boolean lastUpdate;
        private String message = "";
        private boolean startingSubscriptionStatus;

        @Override
        protected void onPreExecute() {
            startingSubscriptionStatus = PaywallService.getInstance().isPremiumUser();
        }

        @Override
        protected Boolean doInBackground(final PurchaseUpdatesResponse... params) {
            try {
                final PurchaseUpdatesResponse purchaseUpdatesResponse = params[0];
                lastUpdate = !purchaseUpdatesResponse.hasMore();

                final String userId = currentUser;

                Logger.d(TAG, "getPurchaseUpdates: response=" + purchaseUpdatesResponse);

                if (purchaseUpdatesResponse.getUserData() == null
                        || purchaseUpdatesResponse.getUserData().getUserId() == null
                        || !purchaseUpdatesResponse.getUserData().getUserId().equals(userId)) {
                    message = "Invalid user";
                    return false;
                }

                switch (purchaseUpdatesResponse.getRequestStatus()) {
                    case SUCCESSFUL:
                        List<Receipt> receiptList = purchaseUpdatesResponse.getReceipts();

                        if (receiptList == null || receiptList.isEmpty()) {
                            //Assuming this is temporarily done by Amazon for peek traffic handling
                            PaywallService.getConnector().updateTemporaryAccess(true);
                        }
                        /*
                         * If the customer for some reason had items revoked, the skus for these items will be contained in the
                         * revoked skus set.
                         * */

                        Logger.d(TAG, "PurchaseUpdatesAsyncTask Status=SUCCESS ");
                        List<StoreReceipt> allReceipts = new ArrayList<StoreReceipt>();
                        if (receiptList != null) {
                            for (Receipt receipt : receiptList) {
                                if (isReceiptExpired(receipt)) {
                                    PaywallService.getConnector().logD(new EventLog.Builder()
                                            .setMessage("getPurchaseUpdates revokedSku=" + receipt.getSku()));
                                    PaywallService.getBillingHelper().clearSubscription(receipt.getSku());
                                }
                                if (receipt.getSku() != null) {
                                    StoreReceipt storeReceipt = new StoreReceipt(
                                            receipt.getReceiptId(),
                                            receipt.getSku(),
                                            receipt.getPurchaseDate() == null ? null : receipt.getPurchaseDate().getTime(),
                                            receipt.getCancelDate() == null ? null : receipt.getCancelDate().getTime(),
                                            Collections.singletonList(receipt.getSku())
                                    );
                                    allReceipts.add(storeReceipt);
                                }
                            }
                        }

                        if (allReceipts.size() > 0) {
                            PaywallService.getConnector().saveAllReceipts(allReceipts);
                        }

                        Receipt lastActiveReceipt = receiptList == null ? null : getActiveReceipt(receiptList);

                        PaywallService.getConnector().logD(new EventLog.Builder()
                                .setMessage("PurchaseUpdate Data")
                                .set("user_id", userId)
                                .set("active_receipt", lastActiveReceipt)
                                .set("has_more", purchaseUpdatesResponse.hasMore()));

                        Subscription subscription;
                        if (lastActiveReceipt != null) {
                            Logger.d(TAG, "Got last active receipt");
                            StoreReceipt storeReceipt = new StoreReceipt(
                                    lastActiveReceipt.getReceiptId(),
                                    lastActiveReceipt.getSku(),
                                    lastActiveReceipt.getPurchaseDate() == null ? null : lastActiveReceipt.getPurchaseDate().getTime(),
                                    lastActiveReceipt.getCancelDate() == null ? null : lastActiveReceipt.getCancelDate().getTime(),
                                    Collections.singletonList(lastActiveReceipt.getSku())
                            );
                            subscription = PaywallService.getBillingHelper().createSubscription(storeReceipt);
                            PaywallService.getBillingHelper().updateSubscriptionDetails(subscription);
                            PaywallService.getConnector().setIapSubscriptionStatus(PaywallConstants.IapSubStatus.ACTIVE);

                            // For App Tester Subs, we need to call this explicitely to purchase another sub (M1-R) when user already
                            // has the free trial sub (M6-R). Otherwise user is not allowed to buy another sub.
                            if (BuildConfig.DEBUG) {
                                PurchasingService.notifyFulfillment(lastActiveReceipt.getReceiptId(), FulfillmentResult.FULFILLED);
                            }
                        } else if (allReceipts.size() > 0) {
                            PaywallService.getConnector().setIapSubscriptionStatus(PaywallConstants.IapSubStatus.TERMINATED);
                        } else {
                            PaywallService.getConnector().setIapSubscriptionStatus(PaywallConstants.IapSubStatus.NO_SUB);
                        }

                        if (purchaseUpdatesResponse.hasMore()) {
                            Logger.d(TAG, "Initiating Another Purchase Updates with offset: ");
                            PaywallService.getConnector().logD(new EventLog.Builder().setMessage("AmazonPurchaseUpdatesCall=PurchaseUpdatesAsyncTask.getPurchaseUpdates()"));
                            PurchasingService.getPurchaseUpdates(false);
                        } else {
                            lastUpdate = true;
                        }

                        return true;
                    case FAILED:
                        /*
                         * On failed responses the application will ignore the request.
                         */
                        message = "Update failed";
                        PaywallService.getConnector().setIapSubscriptionStatus(PaywallConstants.IapSubStatus.UNKNOWN);
                        PaywallService.getConnector().logW(new EventLog.Builder().setMessage("getPurchaseUpdate Failed s: response=" + purchaseUpdatesResponse));
                        return false;
                    case NOT_SUPPORTED:
                        message = "Not supported";
                        return false;
                }
                return false;
            } catch (Exception e) {
                message = "Unknown error";
                PaywallService.getConnector().logE(new EventLog.Builder().setMessage("PurchaseUpdatesAsyncTask failed: error=" + e));
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            super.onPostExecute(success);
            StoreBillingHelper.InitResult result = new StoreBillingHelper.InitResult(success, message, false);
            if (lastUpdate && state == ListenerState.STATE_INITIALIZED && onReadyListener != null) {
                onReadyListener.onReady(result);
                state = success ? ListenerState.STATE_READY : ListenerState.STATE_ERROR;
            } else if (onUpdatedListener != null) {
                onUpdatedListener.onUpdated(result);
            }

            boolean paywallStatus = PaywallService.getInstance().isPremiumUser();

            if (startingSubscriptionStatus != paywallStatus) {
                PaywallService.getConnector().onSubscriptionStatusChanged(paywallStatus);
            }
        }
    }

    public String getCurrentUser() {
        if(currentUser==null){
            if(PaywallService.getConnector().getPrefAmazonUserId()!=null) {
                Logger.d(TAG, "currentUser is null, fetching userId from pref");
                currentUser = PaywallService.getConnector().getPrefAmazonUserId();
            }
        }
        return currentUser;
    }
    
    private static Receipt getActiveReceipt(List<Receipt> receiptList) {
        for (Receipt receipt : receiptList) {
            if (!PaywallService.getBillingHelper().isValidSubscriptionProductId(receipt.getSku()) || isReceiptExpired(receipt)) {
                continue;
            }
            return receipt;
        }
        return null;
    }

    /**
     * Use this function to check if Amazon IAP Receipt is expired.
     * Receipt.isCanceled() -> checks if Receipt.cancelDate != null (cancelDate = expiration date)
     * Receipt.getCancelDate() -> this is really the expiration date.
     */
    private static boolean isReceiptExpired(Receipt receipt) {
        Date currentDate = new Date();
        return receipt.isCanceled() && receipt.getCancelDate().before(currentDate);
    }
}
