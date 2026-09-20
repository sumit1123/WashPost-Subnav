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

package com.washingtonpost.android.paywall;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.wapo.android.commons.logs.EventLog;
import com.washingtonpost.android.config.domain.models.config.paywallconf.Blocker;
import com.washingtonpost.android.config.domain.models.config.paywallconf.Component;
import com.washingtonpost.android.config.domain.models.config.paywallconf.Product;
import com.washingtonpost.android.paywall.features.casettlement.CaSettlementValues;
import com.washingtonpost.android.paywall.helper.PaywallDbHelper;
import com.washingtonpost.android.paywall.models.BannerPaywallMessage;
import com.washingtonpost.android.paywall.models.BlockerPaywallMessage;
import com.washingtonpost.android.paywall.newdata.model.DeviceProfile;
import com.washingtonpost.android.paywall.newdata.model.IAPSubItems;
import com.washingtonpost.android.paywall.newdata.model.StoreReceipt;
import com.washingtonpost.android.paywall.newdata.model.Subscription;
import com.washingtonpost.android.paywall.newdata.response.SubLink;
import com.washingtonpost.android.paywall.util.PaywallConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class PaywallConnector {

    private PaywallDbHelper dbHelper;

    protected MutableLiveData<PaywallConstants.IapSubStatus> iapSubStatus = new MutableLiveData<>();

    public PaywallConnector(Context ctx) {
        dbHelper = new PaywallDbHelper(ctx);
    }

    public final SQLiteDatabase getDB() {
        return dbHelper.getWritableDatabase();
    }

    public LiveData<PaywallConstants.IapSubStatus> getIapSubStatus() {
        return iapSubStatus;
    }

    public PaywallConstants.IapSubStatus getCurrentIapStatus() {
        return iapSubStatus.getValue();
    }

    public abstract void breadcrumb(String breadcrumb);

    public abstract void setCaSettlementValues(CaSettlementValues caSettlementValues);

    public abstract CaSettlementValues getCaSettlementValues();

    public abstract void logHandledException(Exception e);

    public abstract boolean isOnline();

    public abstract String billingEncryptedKey();

    public abstract void logE(EventLog.Builder eventLogBuilder);

    public abstract void logW(EventLog.Builder eventLogBuilder);

    public abstract void logD(EventLog.Builder eventLogBuilder);

    public abstract String getStoreType();

    public abstract String getStoreEnv();

    public abstract String getDeviceId();

    public abstract void paywallClosed();

    public abstract String getPrevEntryPoint();

    public abstract String getAppName();

    public abstract String getAppVersion();

    public abstract String getUserAgent();

    public abstract String getIpAddress();
    /*
     * Users will be allowed to view the contents from these sections even if viewed-article-count >max-allowed-count
     */
    public abstract Set<String> getAllowedPwSections();

    public abstract void saveTestSubProductId(String productId, Set<String> validProductIdSet);

    public abstract void updateSixMonthsExpiry(long expDate);

    public abstract void updateTemporaryAccess(boolean shouldGiveAccess);

    public abstract void saveAllReceipts(List<StoreReceipt> receipts);

    public abstract boolean hasDuplicateSubscription();

    public abstract void saveFreeTrialSub(Subscription freeTrialSub);

    public abstract Subscription getFreeTrialSub();

    public abstract void saveFreeTrialStartTime(long time);

    public abstract void saveIAPSubItems(IAPSubItems iapSubItems);

    @NonNull
    public abstract IAPSubItems getIAPSubItems();

    public abstract void startSearchActivity(Context ctx);

    public abstract void onSubscriptionStatusChanged(boolean subscribed);

    public abstract void onSubscriptionItemChanged(boolean upgraded);

    public abstract void resetLoginAfterIapFlag();

    public abstract void setPrefDeviceProfileSent(boolean isSent);

    public abstract boolean isDeviceProfileSent();

    public abstract void setPrefAmazonUserId(String userId);

    public abstract String getPrefAmazonUserId();

    public abstract void setPriceFlag(String priceFlag);

    public abstract void setPaywallSource(String source);

    public abstract void setPaywallSubSource(String subSource);

    public abstract void setPaywallSubShortTitle(String shortTitle);

    public abstract void setPaywallSubProduct(String product);

    public abstract int getSamsungIAPMode();

    public abstract boolean isAppFrontOrVisible();

    public abstract boolean expiredFreeTrialAndNotSubscribed();

    public abstract void setPaywallSubCurrentRateID(String currentRateID);

    public abstract String getPaywallSubCurrentRateID();

    public abstract void setPaywallSubscriberType(String subscriberType);

    public abstract String getPaywallSubscriberType();

    public abstract void setPaywallSubAttributes(@Nullable Map<String, String> subAttributes);

    @Nullable
    public abstract Set<String> getPaywallSubAttributes();

    public abstract void setSubAcctMgmt(String subAcctMgmt);

    public abstract String getSubAcctMgmt();

    public abstract void setSubAccountAnalytics(String subAccountAnalytics);

    public abstract String getSubAccountAnalytics();

    public abstract long getFreeTrialExpiryDate();

    public abstract boolean hasUserSubscribedEver();

    public abstract DeviceProfile getDeviceProfile();

    public abstract String getPaywallSubSource();

    public abstract String getPrefPaywallSubShortTitle();

    public abstract void startOnboardingSubscriber(Bundle extras);
    public abstract void syncAlertTopicsWithPreferencesApi();
    public abstract void onLogoutComplete();

    public abstract String getClientId();

    public abstract String getClientSecret();

    public abstract String getJwtSecret();

    public abstract String getIapSubscriptionStatus();

    public abstract void setIapSubscriptionStatus(PaywallConstants.IapSubStatus state);

    public abstract void setIapSubscriptionStatus(String subStatus);

    public abstract void setRainbowSubscriptionStatus(String rainbowSubscriptionStatus);

    public abstract String getRainbowSubscriptionStatus();

    public abstract void setAmazonClassicSubscriptionStatus(String amazonClassicSubscriptionStatus);

    public abstract String getAmazonClassicSubscriptionStatus();

    public abstract long getPauseTime();

    public abstract void setPauseTime(long pauseTimeMillis);

    public abstract long getAutoResumeTime();

    public abstract void setAutoResumeTime(long autoResumeTimeMillis);

    public abstract boolean getShouldVerifyPlayStoreResult();

    public abstract void setShouldVerifyPlayStoreResult(boolean shouldVerify);

    public abstract boolean getShouldVerifyExternalPurchaseResult();

    public abstract void setShouldVerifyExternalPurchaseResult(boolean shouldVerify);

    public abstract void showPolicy(String type, Context context);

    public abstract void showContactUs(Context context);

    public abstract void openPlaystore(Context context);

    public abstract void openSiteSubManagement(Context context, Boolean resume, String itid);

    public abstract String getSiteSubManagementUrl(Boolean resume, String itid);

    public abstract Intent getPlaystoreIntent(Context context);

    public abstract String getPlayStoreUrl(Context context);

    public abstract void trackTetroEvent(float meterCount, int meterReason);

    public abstract void trackBackFromWall();

    public abstract void clearOneTrustData(Context context);

    public abstract boolean isTablet();

    public abstract String getBlocker();

    public abstract void updateAirshipUserStatus();

    public abstract long amazonFreeTrialDaysRemaining();

    public abstract String getOneTrustConsentToken();

    public abstract void setOneTrustConsentToken(String consent);

    public abstract boolean canVerifyOnEveryLaunch();

    public abstract void trackOnboardingSeen(String miscellany);

    public abstract void trackOneLinkSignIn();

    public abstract void setSubscriptionLinkResult(SubLink link);

    public abstract String getSubscriptionLinkStatus();

    public abstract String getSubscriptionLinkMessage();

    public abstract void showSignInScreen(FragmentManager fragmentManager, Bundle bundle, String wallName, PaywallConstants.WallType paywallType, boolean isAcquisition, String campaignEntranceType);

    public abstract void showSignUpScreen(FragmentManager fragmentManager, Bundle bundle, String wallName, PaywallConstants.WallType paywallType);

                                              /**
     * callback once paywall is initialized.
     */
    public abstract void onPaywallInitialize();

    public abstract String getAdSubscriptionStatus();

    public abstract BlockerPaywallMessage getBlockerPaywallMessage();

    public abstract BlockerPaywallMessage getBlockerPaywallMessage(String wallName);

    public abstract void setBlockerPaywallMessages(List<BlockerPaywallMessage> messages);

    public abstract String getBillingCountryCode();

    public abstract void setBillingCountryCode(String countryCode);

    public abstract void openCancelSubscriptionPage(String url, Context context);

    public abstract void openWeb(String url, Context context);

    public abstract Blocker getBlockerFromMessages(String wallName);

    public abstract int getSupportedConfigVersion();

    public abstract int getSupportedBlockerVersion(Blocker blocker, String category);

    public abstract List<Product> getProducts(Blocker blocker, String category);

    public abstract List<Component> getComponents(Blocker blocker, String category);

    public abstract void onCCPAAdsTrackingUpdated();

    public abstract void openUrl(String url);

    public void openUrl(String url, Bundle bundle) {
        openUrl(url);
    }

    public abstract String getIterableUserId();
}