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

package com.wapo.flagship.util;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.gson.Gson;
import com.wapo.adsinf.AdManager;
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider;
import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.logs.LogKeys;
import com.wapo.android.commons.logs.LogModules;
import com.wapo.android.commons.util.AppContextUtils;
import com.wapo.android.commons.util.DeviceUtils;
import com.wapo.android.commons.util.Logger;
import com.wapo.android.commons.util.URLParser;
import com.wapo.android.remotelog.logger.RemoteLog;
import com.wapo.flagship.AppContext;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import com.wapo.flagship.features.deeplinks.AirshipAttributes;
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor;
import com.wapo.flagship.features.onboarding2.activity.PostLoginActivity;
import com.wapo.flagship.features.onetrust.OneTrustHelper;
import com.wapo.flagship.features.preferencesapi.repo.TopicNotificationsRepo;
import com.wapo.flagship.features.settings.contactus.ContactUsActivity;
import com.wapo.flagship.features.shared.activities.BaseActivity;
import com.wapo.flagship.features.signin.LoginRegFragment;
import com.wapo.flagship.model.LwaProfile;
import com.wapo.flagship.sdk.iterable.IterableUtils;
import com.wapo.flagship.util.tracking.Measurement;
import com.wapo.flagship.util.tracking.states.NavigationBehavior;
import com.wapo.flagship.wrappers.CrashWrapper;
import com.washingtonpost.android.BuildConfig;
import com.washingtonpost.android.config.domain.manager.ConfigManager;
import com.washingtonpost.android.config.domain.models.config.Config;
import com.washingtonpost.android.config.domain.models.config.paywall.ServiceConfigStub;
import com.washingtonpost.android.config.domain.models.config.paywallconf.Blocker;
import com.washingtonpost.android.config.domain.models.config.paywallconf.BlockerVersion;
import com.washingtonpost.android.config.domain.models.config.paywallconf.Component;
import com.washingtonpost.android.config.domain.models.config.paywallconf.Product;
import com.washingtonpost.android.config.domain.models.config.paywallconf.WallMap2;
import com.washingtonpost.android.paywall.PaywallConnector;
import com.washingtonpost.android.paywall.PaywallReactive;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.auth.PaywallLoginActivity;
import com.washingtonpost.android.paywall.features.casettlement.CaSettlementValues;
import com.washingtonpost.android.paywall.features.ccpa.CCPAUtils;
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper;
import com.washingtonpost.android.paywall.models.BlockerPaywallMessage;
import com.washingtonpost.android.paywall.newdata.model.DeviceProfile;
import com.washingtonpost.android.paywall.newdata.model.IAPSubItems;
import com.washingtonpost.android.paywall.newdata.model.StoreReceipt;
import com.washingtonpost.android.paywall.newdata.model.Subscription;
import com.washingtonpost.android.paywall.newdata.response.SubLink;
import com.washingtonpost.android.paywall.util.PaywallConstants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Created by maxx on 8/7/13.
 */
public class FlagshipPaywallConnector extends PaywallConnector {

    private final String storeType;
    private final String storeEnv;
    private final FlagshipApplication flagshipApplication;
    private String cachedKey;
    private static final String TAG = "process=\"paywall\",";
    private String UUID = null;
    private String userIpAddress;
    private CaSettlementValues caSettlementValues;
    private Map<String, BlockerPaywallMessage> blockerMessagesMap = new HashMap<>();
    private String billingCountryCode;

    private Config getConfig() {
        return ConfigManager.Companion.getInstance().getConfig();
    }

    public FlagshipPaywallConnector(FlagshipApplication flagshipApplication, String storeType, String storeEnv) {
        super(flagshipApplication);
        this.storeType = storeType;
        this.storeEnv = storeEnv;
        this.flagshipApplication = flagshipApplication;
    }

    @Override
    public void setCaSettlementValues(CaSettlementValues caSettlementValues) {
        this.caSettlementValues = caSettlementValues;
    }

    @Override
    public CaSettlementValues getCaSettlementValues() {
        return caSettlementValues;
    }

    @Override
    public void breadcrumb(String breadcrumb) {
        CrashWrapper.logExtras(breadcrumb);
    }

    @Override
    public void logHandledException(Exception e) {
        CrashWrapper.sendException(e);
    }

    @Override
    public boolean isOnline() {
        return ReachabilityUtil.isConnected(com.wapo.flagship.FlagshipApplication.getInstance());
    }

    @Override
    public String billingEncryptedKey() {
        if (cachedKey != null) {
            return cachedKey;
        }
        cachedKey = getConfig().getPlayLicense();
        return cachedKey;
    }

    @Override
    public void logE(EventLog.Builder eventLogBuilder) {
        UUID = PaywallService.getInstance().getUUID();
        eventLogBuilder.setModule(LogModules.PAYWALL);
        eventLogBuilder.setUUID(UUID);
        RemoteLog.e(flagshipApplication, eventLogBuilder.build());
        Logger.e(TAG, eventLogBuilder.toString());
    }

    @Override
    public void logW(EventLog.Builder eventLogBuilder) {
        UUID = PaywallService.getInstance().getUUID();
        eventLogBuilder.setModule(LogModules.PAYWALL);
        eventLogBuilder.setUUID(UUID);
        RemoteLog.w(flagshipApplication, eventLogBuilder.build());
        Logger.w(TAG, eventLogBuilder.toString());
    }

    @Override
    public void logD(EventLog.Builder eventLogBuilder) {
        // Skip logging Iterable messages unless flag is enabled in the remote config.
        if (LogModules.ITERABLE.name().equals(eventLogBuilder.get(LogKeys.MODULE.getKeyName()))
                && !getConfig().getIterableConfig().getEnableDebugLogs()) {
            Logger.d(TAG, "Skip logging, " + eventLogBuilder);
            return;
        }
        UUID = PaywallService.getInstance().getUUID();
        eventLogBuilder.setModule(LogModules.PAYWALL);
        eventLogBuilder.setUUID(UUID);
        RemoteLog.d(flagshipApplication, eventLogBuilder.build());
        Logger.d(TAG, eventLogBuilder.toString());
    }

    private String getPrefixMessage() {
        UUID = PaywallService.getInstance().getUUID();
        if (UUID != null) {
            return TAG + "UUID=" + "\"" + UUID + "\", ";
        } else {
            return TAG;
        }
    }

    @Override
    public String getStoreType() {
        return storeType;
    }

    @Override
    public String getStoreEnv() {
        return storeEnv;
    }

    @Override
    public String getDeviceId() {
        return DeviceUtils.getUniqueDeviceId(FlagshipApplication.getInstance().getApplicationContext());
    }

    @Override
    public void paywallClosed() {

    }

    @Override
    public String getPrevEntryPoint() {
        return Measurement.getPrevEntryPoint();
    }

    @Override
    public String getAppName() {
        return Utils.isProductFlavorPlayStore() ? "android-classic" : "android-rainbow";
    }

    @Override
    public String getAppVersion() {
        return com.wapo.android.commons.util.Utils.getAppVersionName(flagshipApplication);
    }

    @Override
    public String getUserAgent() {
        return Measurement.getUserAgent();
    }

    @Override
    public String getIpAddress() {

        if (userIpAddress == null) {
            WifiManager wm = (WifiManager) flagshipApplication.getSystemService(WIFI_SERVICE);
            if (wm != null) {
                int ipAddress = wm.getConnectionInfo().getIpAddress();
                userIpAddress = String.format(Locale.US, "%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
                return userIpAddress;
            }
            return userIpAddress;
        }

        return userIpAddress;
    }

    @Override
    public Set<String> getAllowedPwSections() {
        return null;
    }

    @Override
    public void saveTestSubProductId(String productId, Set<String> validProductIdSet) {
        AppContext.saveTestSubSku(productId, validProductIdSet);
    }

    @Override
    public void updateSixMonthsExpiry(long expDate) {
        PrefUtils.setPrefFreeTrialExpiry(flagshipApplication, expDate);
        //TODO-5935  - Check if this commented code below is needed.
//        AdService adService = AdService.getInstance();
//        if (adService != null) {
//            adService.setPaywallExpirationTime(
//                    PrefUtils.getFreeTrialExpiryDate(rainbowApplication));
//        }
    }

    @Override
    public void updateTemporaryAccess(boolean shouldGiveAccess) {

    }

    @Override
    public void saveAllReceipts(List<StoreReceipt> receipts) {
        PrefUtils.savePrefAllReceipts(flagshipApplication, receipts);
        //save free trial expiration dates
        StoreReceipt sixMonthsReceipt = getSixMonthsActiveReceipt(receipts);
        if (sixMonthsReceipt != null) {
            logD(new EventLog.Builder().setMessage("SixMonthActiveReceipt=\"" + sixMonthsReceipt));
            updateSixMonthsExpiry(getSixMonthsExpDate(sixMonthsReceipt));
            if (sixMonthsReceipt.transactionDate != null) {
                PaywallService.getConnector().saveFreeTrialStartTime(sixMonthsReceipt.transactionDate);
            }
            Subscription freeTrialSub = PaywallService.getBillingHelper().createSubscription(sixMonthsReceipt);
            saveFreeTrialSub(freeTrialSub);
        } else {
            //this is to overwrite already saved free trial sub
            saveFreeTrialSub(null);
        }
    }
    @Override
    public boolean hasDuplicateSubscription() {
        List<StoreReceipt> allReceipts = PrefUtils.getPrefAllReceipts(flagshipApplication);
        List<StoreReceipt> activeReceipts = new ArrayList<>();
        if (allReceipts != null) {
            for (StoreReceipt receipt: allReceipts) {
                if (PaywallService.getBillingHelper().isValidSubscriptionProductId(receipt.productId) && !receipt.isExpired()) {
                    activeReceipts.add(receipt);
                }
            }
        }
        if (activeReceipts.size() > 1) {
            PaywallService.getPaywallPrefHelper().setPrefMigratedAmazonUserHasDuplicateSub(true);
            PaywallService.getConnector().logD(new EventLog.Builder().setMessage("Duplicate amazon sub detected"));
            return true;
        }
        return false;
    }

    private StoreReceipt getSixMonthsActiveReceipt(List<StoreReceipt> receiptList) {
        if (receiptList != null) {
            for (StoreReceipt receipt : receiptList) {
                if (!isValidSixMonthsSKU(receipt.productId) || receipt.isExpired()) {
                    continue;
                }
                return receipt;
            }
        }
        Logger.d(TAG, "Six Months Free Receipt Not found");
        return null;
    }

    private long getSixMonthsExpDate(StoreReceipt receipt) {
        try {
            if (receipt != null) {
                if (receipt.expirationDate != null) {
                    Logger.d(TAG, "receipt.enddate " + receipt.expirationDate);
                    return receipt.expirationDate;
                } else if (receipt.transactionDate != null) {
                    Calendar calPurchase = Calendar.getInstance();
                    calPurchase.setTimeInMillis(receipt.transactionDate);
                    calPurchase.add(Calendar.MONTH, 6); //Add Six months
                    return calPurchase.getTimeInMillis();
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Exception : " + e.toString());
        }
        return 0;
    }

    private boolean isValidSixMonthsSKU(String sku) {
        return getConfig().getPaywallConfig().getValidSixMonthsSkuList().contains(sku);
    }


    @Override
    public void saveFreeTrialSub(Subscription freeTrialSub) {
        PrefUtils.saveFreeTrialSub(new Gson().toJson(freeTrialSub));
    }

    @Override
    public Subscription getFreeTrialSub() {
        String freeTrialSubscriptionString = PrefUtils.getFreeTrialSub();
        return freeTrialSubscriptionString == null ? null : new Gson().fromJson(freeTrialSubscriptionString, Subscription.class);
    }

    @Override
    public void saveFreeTrialStartTime(long time) {
        PrefUtils.setFreeTrialStartTime(flagshipApplication, time);
    }

    @Override
    public void saveIAPSubItems(IAPSubItems iapSubItems) {
        PrefUtils.setPrefPaywallIAPSubItems(flagshipApplication, iapSubItems);
    }

    @NonNull
    @Override
    public IAPSubItems getIAPSubItems() {
        return PrefUtils.getPrefPaywallIAPSubItems(flagshipApplication);
    }

    @Override
    public void startSearchActivity(Context ctx) {

    }

    @Override
    public void onSubscriptionStatusChanged(boolean subscribed) {
        PaywallReactive.notifyPlacementChanged();
    }

    @Override
    public void onSubscriptionItemChanged(boolean upgraded) {
        PaywallReactive.notifyPlacementChanged();
    }

    @Override
    public void resetLoginAfterIapFlag() {

    }

    @Override
    public void setPrefDeviceProfileSent(boolean isSent) {
        PrefUtils.setPrefDeviceProfileSent(flagshipApplication, isSent);
    }

    @Override
    public boolean isDeviceProfileSent() {
        return PrefUtils.isDeviceProfileSent(flagshipApplication);
    }

    @Override
    public void setPrefAmazonUserId(String userId) {
        PrefUtils.setPrefAmazonUserId(flagshipApplication, userId);
    }

    @Override
    public String getPrefAmazonUserId() {
        return PrefUtils.getPrefAmazonUserId(flagshipApplication);
    }

    @Override
    public void setPriceFlag(String priceFlag) {
        PrefUtils.setPrefPriceFlag(flagshipApplication, priceFlag);
    }

    @Override
    public void setPaywallSource(String source) {
        PrefUtils.setPrefPaywallSource(flagshipApplication, source);
    }

    @Override
    public void setPaywallSubSource(String subSource) {
        PrefUtils.setPrefPaywallSubSource(flagshipApplication, subSource);
    }

    @Override
    public void setPaywallSubShortTitle(String shortTitle) {
        PrefUtils.setPrefPaywallSubShortTitle(flagshipApplication, shortTitle);
    }

    @Override
    public void setPaywallSubAttributes(@Nullable Map<String, String> subAttributes) {
        PrefUtils.setPrefPaywallSubAttributes(flagshipApplication, subAttributes);
        // Update reactive holder so AdsPolicyRepository reacts immediately to server-side attribute changes.
        try {
            // Preserve both key and value (for example, NOADS:1) to match the
            // persisted subscription-attribute representation.
            PaywallReactive.updateSubAttributes(subAttributes);
        } catch (Exception e) {
            // Log but don't crash if Kotlin interop fails for unexpected reasons
            Logger.w(TAG, "Failed to update PaywallReactive subAttributes: " + e.getMessage());
        }
    }

    @Override
    public void setPaywallSubProduct(String product) {

    }

    @Override
    public int getSamsungIAPMode() {
        return 0;
    }

    @Override
    public boolean isAppFrontOrVisible() {
        return false;
    }

    @Override
    public boolean expiredFreeTrialAndNotSubscribed() {
        return false;
    }

    @Override
    public void setPaywallSubCurrentRateID(String currentRateID) {

    }

    @Override
    public String getPaywallSubCurrentRateID() {
        return null;
    }

    @Override
    public void setPaywallSubscriberType(String subscriberType) {

    }

    @Override
    public String getPaywallSubscriberType() {
        return null;
    }

    @Override
    public long getFreeTrialExpiryDate() {
        return 0;
    }

    @Override
    public boolean hasUserSubscribedEver() {
        return false;
    }

    /**
     * Amazon Only: This is to retrieve the LWA Profile
     * @return
     */
    @Override
    public DeviceProfile getDeviceProfile() {
        return PrefUtils.getDeviceProfile(FlagshipApplication.getInstance(), LwaProfile.class);
    }

    @Override
    public String getPaywallSubSource() {
        return PrefUtils.getPrefPaywallSubSource(FlagshipApplication.getInstance().getApplicationContext());
    }

    @Override
    public String getPrefPaywallSubShortTitle() {
        String shortTitle = PrefUtils.getPrefPaywallSubShortTitle(FlagshipApplication.getInstance().getApplicationContext());
        if (getFreeTrialSub() != null) {
            shortTitle = "Free Trial (6 Months)";
        } else if (TextUtils.isEmpty(shortTitle) && PaywallService.getInstance().isSubActive()) {
            shortTitle = "Subscription";
        }
        return shortTitle;
    }

    @Override
    public void startOnboardingSubscriber(Bundle extras) {
        PaywallPrefHelper.getInstance(flagshipApplication.getApplicationContext()).setPrefIsUserMissingRefreshToken(false);
        Context activityContext = flagshipApplication.getCurrentActivity();
        if (activityContext != null) {
            Intent intent = new Intent(activityContext, PostLoginActivity.class);
            if (extras != null) {
                intent.putExtras(extras);
            }
            activityContext.startActivity(intent);
        }
    }

    @Override
    public void syncAlertTopicsWithPreferencesApi() {
        TopicNotificationsRepo.getInstance().syncTopicsWithPreferencesApi(false);
    }

    @Override
    public void onLogoutComplete() {
        AirshipAttributes.INSTANCE.updateUserStatusAttribute();
        PrefUtils.setSelectedContentPacks(flagshipApplication, Collections.emptyList());
        Continuation continuation = new Continuation<Unit>() {
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(Object result) {
                Logger.d(TAG, "Result from Kotlin: " + result);
            }};
        DataStoreUtils.setNewsprintEngagedStatus("", continuation);
        DataStoreUtils.setNewsprintHasViewed(false, continuation);
        DataStoreUtils.setNewsprintReaderType("", continuation);
        PrefUtils.setNewsprintAttributesLmt(flagshipApplication, 0L);
        PrefUtils.setNewsprintStateLmt(flagshipApplication, 0L);
        PaywallReactive.reset();
        Measurement.onUserSignOutComplete();
        flagshipApplication.onUserSignOutComplete();
    }

    private boolean isProdSignIn() {
        return ConfigManager.Companion.getInstance().isProdSignIn();
    }

    @Override
    public String getClientId() {
        if (AppContextUtils.INSTANCE.isDebuggableBuild()) {
            return WapoSecDataProvider.INSTANCE.clientId(BuildConfig.FLAVOR, isProdSignIn());
        } else {
            return WapoSecDataProvider.INSTANCE.clientId(BuildConfig.FLAVOR, true);
        }
    }

    @Override
    public String getClientSecret() {
        if (AppContextUtils.INSTANCE.isDebuggableBuild()) {
            return WapoSecDataProvider.INSTANCE.clientSecret(BuildConfig.FLAVOR, isProdSignIn());
        } else {
            return WapoSecDataProvider.INSTANCE.clientSecret(BuildConfig.FLAVOR, true);
        }
    }

    @Override
    public String getJwtSecret() {
        return WapoSecDataProvider.INSTANCE.getJwtSecret();
    }

    /**
     * Retrieves the current status of IAP subscription
     * - "A" - Active Subscription
     * - "T" - Terminated Subscription
     * - "S" - Suspended Subscription (Only PlayStore)
     * - "P" - Paused Subscription
     * - "" - Empty String means No Subscription History
     * @return
     */
    public String getIapSubscriptionStatus() {
        return PrefUtils.getUserSubscriptionStatus(flagshipApplication);
    }

    public String getAdSubscriptionStatus(){
        if (PaywallService.getInstance().isWpUserLoggedIn() && PaywallService.getInstance().getSubStatus().equals(PaywallConstants.ACTIVE)){
            //Registered User with Active Sub
            return PaywallConstants.AD_SUBSCRIPTION_ACTIVE;
        } else if (PaywallService.getInstance().isWpUserLoggedIn() && PaywallService.getInstance().getSubStatus().equals(PaywallConstants.SUSPENDED)) {
            //Registered User with no Sub
            return PaywallConstants.AD_SUBSCRIPTION_REGISTERED;
        } else if (!PaywallService.getInstance().isWpUserLoggedIn() && Objects.equals(PaywallService.getInstance().getSubStatus(),PaywallConstants.EMPTY)) {
            //No Account
            return PaywallConstants.AD_SUBSCRIPTION_NONE;
        } else if (PaywallService.getInstance().getSubStatus().equals(PaywallConstants.TERMINATED)){
            //Terminated subscription
            return PaywallConstants.AD_SUBSCRIPTION_TERMINATED;
        } else return "";
    }

    @Override
    public BlockerPaywallMessage getBlockerPaywallMessage() {
        return getBlockerPaywallMessage(null);
    }

    @Override
    public BlockerPaywallMessage getBlockerPaywallMessage(String wallName) {
        String messageWallName = TextUtils.isEmpty(wallName) ? (Utils.isAmazonBuild() ? PaywallConstants.WALL_NAME_AMAZON_MAIN : PaywallConstants.WALL_NAME_MAIN) : wallName;
        return blockerMessagesMap.get(messageWallName);
    }

    @Override
    public void setBlockerPaywallMessages(List<BlockerPaywallMessage> messages) {
        if (messages != null) {
            blockerMessagesMap.clear();
            for (BlockerPaywallMessage message : messages) {
                if (message == null) continue;
                WallMap2 wallMap = IterableUtils.getWallMap(message.getAttributionInfo().getPlacementId());
                if (wallMap != null) {
                    blockerMessagesMap.put(wallMap.getBlocker(), message);
                }
            }
        }
    }

    @Override
    public String getBillingCountryCode() {
        return billingCountryCode;
    }

    @Override
    public void setBillingCountryCode(String countryCode) {
        billingCountryCode = countryCode;
        flagshipApplication.iterableSdk.syncDeviceAttributes();
    }

    @Override
    public void openCancelSubscriptionPage(String url, Context context) {
        Utils.startWeb(url, context);
    }

    @Override
    public void openWeb(String url, Context context) {
        Utils.startWebActivity(url, context);
    }

    @Override
    public Blocker getBlockerFromMessages(String wallName) {
        String blockerWallName = (TextUtils.isEmpty(wallName)) ? getBlocker() : wallName;
        BlockerPaywallMessage paywallMessage = blockerMessagesMap.get(blockerWallName);
        return (paywallMessage != null) ? paywallMessage.getBlocker() : null;
    }


    /// The highest config version this app supports
    @Override
    public int getSupportedConfigVersion() {
        return PaywallConstants.SUPPORTED_CONFIG_VERSION;
    }

    // Returns the highest supported version of the given blocker, taking into account A/B test overrides and app configuration.
    @Override
    public int getSupportedBlockerVersion(Blocker blocker, String category) {
        if (blocker == null || blocker.getVersions() == null || blocker.getVersions().isEmpty()) {
            return 0;
        }

        int maxSupported = getSupportedConfigVersion();
        int selectedVersion = 0;

        for (BlockerVersion version : blocker.getVersions()) {
            Integer versionNum = version.getVersion();
            if (versionNum != null && versionNum <= maxSupported && versionNum > selectedVersion) {
                selectedVersion = versionNum;
            }
        }

        return selectedVersion;
    }


    // Returns the list of products for the supported blocker version, or default products if no matching version is found.
    @Override
    public List<Product> getProducts(Blocker blocker , String category) {
        int targetVersion = getSupportedBlockerVersion(blocker, category);

        List<BlockerVersion> versions = blocker != null ? blocker.getVersions() : null;
        List<Product> products;

        if (targetVersion > 0 && versions != null && !versions.isEmpty()) {

            products = null;
            for (BlockerVersion version : versions) {
                if (version.getVersion() != null && version.getVersion() == targetVersion) {
                    products = version.getItems();
                    break;
                }
            }
            // Fallback to empty list if version not found
            if (products == null) {
                products = Collections.emptyList();
            }
        } else {
            // No versions, use default
            products = (blocker != null && blocker.getItems() != null) ? blocker.getItems() : Collections.emptyList();
        }

        return products;
    }

    // Returns the list of Components for the supported blocker version, or default products if no matching version is found.
    @Override
    public List<Component> getComponents(Blocker blocker, String category) {
        int targetVersion = getSupportedBlockerVersion(blocker , category);

        List<BlockerVersion> versions = blocker != null ? blocker.getVersions() : null;
        List<Component> components;

        if (targetVersion > 0 && versions != null && !versions.isEmpty()) {
            components = null;
            for (BlockerVersion version : versions) {
                if (version.getVersion() != null && version.getVersion() == targetVersion) {
                    components = version.getComponents();
                    break;
                }
            }
            // Fallback to default components if version not found
            if (components == null) {
                components = blocker.getComponents();
            }
        } else {
            // No versions, use default components
            components = blocker != null ? blocker.getComponents() : null;
        }

        return components;
    }

    /**
     * Set the Iap Sub Status. Only called by AmazonIAPListener and PlayStoreBillingHelper
     * classes which interface with the respective Amazon IAP and Google IAP libraries.
     * @param status
     */
    @Override
    public void setIapSubscriptionStatus(PaywallConstants.IapSubStatus status) {
        synchronized (this) {
            switch (status) {
                case ACTIVE:
                    PrefUtils.setUserSubscriptionStatus(flagshipApplication, PaywallConstants.ACTIVE);
                    break;
                case TERMINATED:
                    PrefUtils.setUserSubscriptionStatus(flagshipApplication, PaywallConstants.TERMINATED);
                    break;
                case SUSPENDED:
                    PrefUtils.setUserSubscriptionStatus(flagshipApplication, PaywallConstants.SUSPENDED);
                    break;
                case PAUSED:
                    PrefUtils.setUserSubscriptionStatus(flagshipApplication, PaywallConstants.PAUSED);
                    break;
                case NO_SUB:
                    PrefUtils.setUserSubscriptionStatus(flagshipApplication, PaywallConstants.EMPTY);
                default:
                    break;
            }
            iapSubStatus.postValue(status);
        }
        AirshipAttributes.INSTANCE.updateUserStatusAttribute();
    }

    @Override
    public void setIapSubscriptionStatus(String subStatus) {
        switch (subStatus) {
            case PaywallConstants.ACTIVE:
                iapSubStatus.postValue(PaywallConstants.IapSubStatus.ACTIVE);
                break;
            case PaywallConstants.TERMINATED:
                iapSubStatus.postValue(PaywallConstants.IapSubStatus.TERMINATED);
                break;
            case PaywallConstants.SUSPENDED:
                iapSubStatus.postValue(PaywallConstants.IapSubStatus.SUSPENDED);
                break;
            case PaywallConstants.PAUSED:
                iapSubStatus.postValue(PaywallConstants.IapSubStatus.PAUSED);
                break;
            case PaywallConstants.EMPTY:
                iapSubStatus.postValue(PaywallConstants.IapSubStatus.NO_SUB);
                break;
            default:
                break;
        }
        PrefUtils.setUserSubscriptionStatus(flagshipApplication, subStatus);
        AirshipAttributes.INSTANCE.updateUserStatusAttribute();
    }

    @Override
    public void setRainbowSubscriptionStatus(String rainbowSubscriptionStatus) {
        PrefUtils.setRainbowSubscriptionStatus(flagshipApplication, rainbowSubscriptionStatus);
        AirshipAttributes.INSTANCE.updateUserStatusAttribute();
    }

    @Override
    public String getRainbowSubscriptionStatus() {
        return PrefUtils.getRainbowSubscriptionStatus(flagshipApplication);
    }

    @Override
    public void setAmazonClassicSubscriptionStatus(String amazonClassicSubscriptionStatus) {
        PrefUtils.setAmazonClassicSubscriptionStatus(flagshipApplication, amazonClassicSubscriptionStatus);
        AirshipAttributes.INSTANCE.updateUserStatusAttribute();
    }

    @Override
    public String getAmazonClassicSubscriptionStatus() {
        return PrefUtils.getAmazonClassicSubscriptionStatus(flagshipApplication);
    }

    public long getPauseTime() {
        return PrefUtils.getPauseTime(flagshipApplication);
    }

    @Override
    public void setPauseTime(long pauseTimeMillis) {
        PrefUtils.setPauseTime(flagshipApplication, pauseTimeMillis);
    }

    @Override
    public long getAutoResumeTime() {
        return PrefUtils.getAutoResumeTime(flagshipApplication);
    }

    @Override
    public void setAutoResumeTime(long autoResumeTimeMillis) {
        PrefUtils.setAutoResumeTime(flagshipApplication, autoResumeTimeMillis);
    }

    @Override
    public boolean getShouldVerifyPlayStoreResult() {
        return PrefUtils.getShouldVerifyPlayStoreResult(flagshipApplication);
    }

    @Override
    public void setShouldVerifyPlayStoreResult(boolean shouldVerify) {
        PrefUtils.setShouldVerifyPlayStoreResult(flagshipApplication, shouldVerify);
    }

    public boolean getShouldVerifyExternalPurchaseResult() {
        return PrefUtils.getShouldVerifyExternalPurchaseResult(flagshipApplication);
    }

    public void setShouldVerifyExternalPurchaseResult(boolean shouldVerify) {
        PrefUtils.setShouldVerifyExternalPurchaseResult(flagshipApplication, shouldVerify);
    }

    @Nullable
    @Override
    public Set<String> getPaywallSubAttributes() {
        return PrefUtils.getPrefPaywallSubAttributes(flagshipApplication);
    }

    @Override
    public void setSubAcctMgmt(String subAcctMgmt) {
        Measurement.saveSubAcctMgmtIntoPrefs(flagshipApplication, subAcctMgmt);
    }

    @Override
    public String getSubAcctMgmt() {
        return Measurement.getSubAcctMgmtFromPrefs(flagshipApplication);
    }

    @Override
    public void setSubAccountAnalytics(String subAccountAnalytics) {
        PrefUtils.setPrefSubAccountAnalytics(flagshipApplication, subAccountAnalytics);
    }

    @Override
    public String getSubAccountAnalytics() {
        return PrefUtils.getPrefSubAccountAnalytics(flagshipApplication);
    }

    @Override
    public void showPolicy(String type, Context context) {
        String url = type.equals(PaywallConstants.PRIVACY_POLICY) ? getConfig().getPrivacyPolicyUrl() : getConfig().getTermsOfServiceUrl();
        Utils.startWebActivity(url, context);

    }

    @Override
    public void showContactUs(Context context) {
        Intent intent = new Intent(context, ContactUsActivity.class);
        context.startActivity(intent);
    }

    @Override
    public Intent getPlaystoreIntent(Context context) {
        PaywallService.getConnector().setShouldVerifyPlayStoreResult(true);
        return new Intent(Intent.ACTION_VIEW, Uri.parse(getPlayStoreUrl(context)));
    }

    @Override
    public String getPlayStoreUrl(Context context) {
        String currentSKU;
        String currentPackageName = context.getPackageName();
        Subscription lastSubscription = PaywallService.getBillingHelper().getLastActiveSubscription();
        if (lastSubscription != null) {
            currentSKU = lastSubscription.getStoreProductId();
        } else {
            currentSKU = PaywallService.getPaywallPrefHelper().getPrefLastSubProductId();
        }
        return "https://play.google.com/store/account/subscriptions?sku=" + currentSKU + "&package=" + currentPackageName;
    }

    @Override
    public void openPlaystore(Context context) {
        if (context instanceof BaseActivity) {
            ((BaseActivity) context).openPlaystore(context);
        }
    }

    @Override
    public void openSiteSubManagement(Context context, Boolean resume, String itid) {
        Utils.startWebActivity(getSiteSubManagementUrl(resume, itid), context, false, true);
    }

    @Override
    public String getSiteSubManagementUrl(Boolean resume, String itid) {
        String url;
        ServiceConfigStub paywallConfig = getConfig().getPaywallConfig();
        if (resume) {
            url = paywallConfig.getManageSubResumeUrl();
        } else {
            url = paywallConfig.getManageSubUrl();
        }

        return url.replace("itid=app_settings", "itid=" + itid);
    }

    @Override
    public void trackTetroEvent(float meterCount, int meterReason) {
        Measurement.updateTetroEvent(meterCount, meterReason);
    }

    @Override
    public void trackBackFromWall() {
        Measurement.setNavigationBehaviorInDefaultMap(Measurement.PATH_TO_VIEW_BACK_TO_FRONT);
    }

    @Override
    public boolean isTablet() {
        return DeviceUtils.isTablet(flagshipApplication.getApplicationContext());
    }

    @Override
    public String getBlocker() {
        return PrefUtils.getBlocker(flagshipApplication.getApplicationContext());
    }

    @Override
    public void updateAirshipUserStatus(){
        AirshipAttributes.INSTANCE.updateUserStatusAttribute();
    }

    @Override
    public long amazonFreeTrialDaysRemaining() {
        //Return the override if it's set.
        // (note the debug_build check enables a quicker app in the non debug version)
        if (BuildConfig.DEBUG) {
            Integer freeDays = PrefUtils.getDebugRemainingDaysFree(flagshipApplication);
            if (freeDays != null) return freeDays;
        }

        long expDate = PrefUtils.getFreeTrialExpiryDate(flagshipApplication);
        return (expDate - System.currentTimeMillis()) / DateUtils.DAY_IN_MILLIS;
    }

    @Override
    public void clearOneTrustData(Context context) {
        OneTrustHelper.INSTANCE.clearOneTrustData();
    }

    @Override
    public String getOneTrustConsentToken() {
        return PrefUtils.getPrefConsentToken(flagshipApplication.getApplicationContext());
    }

    @Override
    public void setOneTrustConsentToken(String consent) {
        PrefUtils.setPrefConsentToken(flagshipApplication.getApplicationContext(), consent);
    }

    @Override
    public boolean canVerifyOnEveryLaunch() {
        return BuildConfig.DEBUG && PrefUtils.getVerifyOnEachLaunch(flagshipApplication.getApplicationContext());
    }

    @Override
    public void trackOnboardingSeen(String miscellany) {
        Measurement.trackOnboardingSeen(miscellany);
    }

    @Override
    public void trackOneLinkSignIn() {
        Measurement.setNavigationBehavior(NavigationBehavior.ONELINK);
        PaywallService.getOmniture().trackSignIn(null, null, false, null);
        PaywallService.getOmniture().trackSignInComplete(false, false);
    }

    @Override
    public void setSubscriptionLinkResult(SubLink link) {
        PrefUtils.setSubscriptionLinkResult(flagshipApplication.getApplicationContext(), link);
    }

    @Override
    public @Nullable String getSubscriptionLinkStatus() {
        return PrefUtils.getSubscriptionLinkStatus(flagshipApplication.getApplicationContext());
    }

    @Override
    public @Nullable String getSubscriptionLinkMessage() {
        return PrefUtils.getSubscriptionLinkMessage(flagshipApplication.getApplicationContext());
    }

    @Override
    public void showSignInScreen(FragmentManager fragmentManager, Bundle bundle, String wallName, PaywallConstants.WallType paywallType, boolean isAcquisition, String campaignEntranceType) {
        PaywallService.getOmniture().trackSignIn(paywallType, wallName, isAcquisition, campaignEntranceType);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Context context = flagshipApplication.getCurrentActivity() == null ? flagshipApplication : flagshipApplication.getCurrentActivity();
            Intent intent = new Intent(context, PaywallLoginActivity.class);
            intent.putExtras(bundle);
            context.startActivity(intent);
        } else {
            LoginRegFragment loginRegFragment = new LoginRegFragment();
            loginRegFragment.setArguments(bundle);
            Fragment fragment = fragmentManager.findFragmentByTag(LoginRegFragment.TAG);
            if(fragment != null) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
            loginRegFragment.show(fragmentManager, LoginRegFragment.TAG);
        }
    }

    @Override
    public void showSignUpScreen(FragmentManager fragmentManager, Bundle bundle, String wallName, PaywallConstants.WallType paywallType) {
        PaywallService.getOmniture().trackSignUp(paywallType, wallName);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Context context = flagshipApplication.getCurrentActivity() == null ? flagshipApplication : flagshipApplication.getCurrentActivity();
            Intent intent = new Intent(context, PaywallLoginActivity.class);
            intent.putExtras(bundle);
            context.startActivity(intent);
        } else {
            LoginRegFragment loginRegFragment = new LoginRegFragment();
            loginRegFragment.setArguments(bundle);
            Fragment fragment = fragmentManager.findFragmentByTag(LoginRegFragment.TAG);
            if(fragment != null) {
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
            loginRegFragment.show(fragmentManager, LoginRegFragment.TAG);
        }
    }

    @Override
    public void onPaywallInitialize() {
        Measurement.onPaywallInitialize();
        flagshipApplication.iterableSdk.syncUserProfile();
    }

    @Override
    public void onCCPAAdsTrackingUpdated() {
        AdManager.Companion.getInstance().onCCPAAdsTrackingUpdated();
        boolean optOut = CCPAUtils.hasUserOptedOutCCPAAdsTracking(flagshipApplication);
        OneTrustHelper.INSTANCE.updatePersonalizedAdvertisingConsent(!optOut);
    }

    @Override
    public void openUrl(String url) {
        openUrl(url, null);
    }

    @Override
    public void openUrl(String url, Bundle bundle) {
        if (DeepLinksProcessor.INSTANCE.isExternalUrl(new URLParser(url))) {
            PaywallService.getConnector().setShouldVerifyExternalPurchaseResult(true);
            PaywallService.getOmniture().trackOpenExternalPurchase(url);
        }
        DeepLinksProcessor.INSTANCE.processAsync(url, null, null, null, null, bundle, null);
    }

    @Override
    public String getIterableUserId() {
        return AppContext.getIterableUserId(FlagshipApplication.getInstance().getApplicationContext());
    }
}
