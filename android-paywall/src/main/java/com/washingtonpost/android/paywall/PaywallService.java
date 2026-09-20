/*
 *  Copyright (c) 2018. The Washington Post. All rights reserved.
 */

package com.washingtonpost.android.paywall;

import static com.wapo.android.commons.util.Utils.isConnectedOrConnecting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.wapo.android.commons.util.Logger;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.auth0.android.jwt.JWT;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.util.LiveEvent;
import com.washingtonpost.android.config.domain.manager.ConfigManager;
import com.washingtonpost.android.config.domain.models.config.paywall.AcquisitionReminderModel;
import com.washingtonpost.android.config.domain.models.config.paywall.BottomCtaModel;
import com.washingtonpost.android.config.domain.models.config.paywall.GlobalBannerConfig;
import com.washingtonpost.android.config.domain.models.config.paywall.OAuthConfigStub;
import com.washingtonpost.android.config.domain.models.config.paywall.PaywallSheetModels;
import com.washingtonpost.android.config.domain.models.config.paywall.ReminderScreenConfig;
import com.washingtonpost.android.config.domain.models.config.paywall.ServiceConfigStub;
import com.washingtonpost.android.config.domain.models.config.paywallconf.ProductSkuEntry;
import com.washingtonpost.android.paywall.api.VerifyState;
import com.washingtonpost.android.paywall.api.WPPaywallApiService;
import com.washingtonpost.android.paywall.api.WapoAccessService;
import com.washingtonpost.android.paywall.auth.AuthHelper;
import com.washingtonpost.android.paywall.auth.AuthStateManager;
import com.washingtonpost.android.paywall.billing.AbstractStoreBillingHelper;
import com.washingtonpost.android.paywall.bottomsheet.SubState;
import com.washingtonpost.android.paywall.features.ccpa.CCPA;
import com.washingtonpost.android.paywall.features.ccpa.IdentityPreferences;
import com.washingtonpost.android.paywall.features.ccpa.IdentityPreferencesRecord;
import com.washingtonpost.android.paywall.features.ccpa.PrivacySetting;
import com.washingtonpost.android.paywall.features.ccpa.SaveIdentityPreferencesRequest;
import com.washingtonpost.android.paywall.features.ccpa.SaveIdentityPreferencesResponse;
import com.washingtonpost.android.paywall.features.tetro.local.TetroLocalServiceImpl;
import com.washingtonpost.android.paywall.features.tetro.remote.TetroApiService;
import com.washingtonpost.android.paywall.helper.PaywallCounterHelper;
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper;
import com.washingtonpost.android.paywall.features.tetro.TetroManager;
import com.washingtonpost.android.paywall.helper.WpPaywallHelper;
import com.washingtonpost.android.paywall.metering.MeteringPrefs;
import com.washingtonpost.android.paywall.metering.MeteringService;
import com.washingtonpost.android.paywall.models.PromoCode;
import com.washingtonpost.android.paywall.models.PromoCodeRequestState;
import com.washingtonpost.android.paywall.models.VerifyDeviceRequestState;
import com.washingtonpost.android.paywall.newdata.model.ArticleStub;
import com.washingtonpost.android.paywall.newdata.model.PaywallResult;
import com.washingtonpost.android.paywall.newdata.model.Subscription;
import com.washingtonpost.android.paywall.newdata.model.WpUser;
import com.washingtonpost.android.paywall.newdata.response.SubItem;
import com.washingtonpost.android.paywall.newdata.response.SubVerification;
import com.washingtonpost.android.config.domain.models.config.paywall.OnboardingReminderModel;
import com.washingtonpost.android.paywall.util.CookiesService;
import com.washingtonpost.android.paywall.util.PaywallConstants;
import com.washingtonpost.android.paywall.util.PaywallUtil;
import net.openid.appauth.TokenResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Paywall service
 *
 * @author Bkilari
 */
public class PaywallService {

    private WPPaywallApiService apiService;
    private MeteringService meteringService;
    private WapoAccessService wapoAccessService;
    private static AbstractStoreBillingHelper storeBillingHelper;
    private Context ctx;
    private PaywallConnector connector;
    private PaywallOmniture omniture;
    private PaywallPrefHelper paywallPrefHelper;
    private SubscriptionInfo subscriptionInfo;
    private OnboardingReminderModel onboardingReminderModel;
    private ReminderScreenConfig reminderScreenConfig;
    private AcquisitionReminderModel acquisitionReminderModel;
    private BottomCtaModel bottomCtaModel;
    private PaywallSheetModels paywallSheetModels;
    private CookiesService cookiesService;
    private TetroManager tetroManager;
    private String subscriptionSource;

    public GlobalBannerConfig globalBannerConfig;

    public static final int RUN_PAYWALL_SERVICE = 1;
    public static final int RUN_PAYWALL_SERVICE_RESULT = 2; //user is paywalled
    public static final int RUN_PAYWALL_SERVICE_RESULT_EXPIRED = 3; //logged in user with expired sub is paywalled
    public static final int RUN_PAYWALL_SERVICE_RESULT_NO_PAYWALL = 4; //preview or premium
    public static final int PAYWALL_RESULT_ID = 1;
    public static final int PAYWALL_RESULT_ID_SETTINGS = 2;
    private static PaywallService instance;
    private static final String TAG = PaywallService.class.getSimpleName();
    public boolean isSameSiteEnabled = false;
    private final LiveEvent<VerifyState> subVerifyLiveEvent = new LiveEvent<>();
    private final LiveEvent<PromoCodeRequestState> promocodeRequestStateLiveData = new LiveEvent<>();
    private final LiveEvent<VerifyDeviceRequestState> verifyDeviceRequestStateLiveEvent = new LiveEvent<>();

    private String currentTetroActionCodes = "";

    private int currentTetroAction = 0;

    private boolean shouldRefreshProfile = false;

    private Set<String> adFreeSKUs = new HashSet<>();
    private List<String> baseSubscriptionProducts = new ArrayList<>();

    public Set<String> getAdFreeSKUs() {
        return adFreeSKUs;
    }

    public List<String> getBaseSubscriptionProducts() {
        return baseSubscriptionProducts;
    }

    /**
     * Resolves the correct featureJwt based on user login state:
     * - Anonymous: use verify featureJwt from cached subscription
     * - Logged in: use verify featureJwt if not null, else fall back to profile featureJwt
     */
    public String getResolvedFeatureJwt() {
        Subscription cachedSub = getBillingHelper().cachedSubscription();
        String verifyFeatureJwt = cachedSub != null ? cachedSub.getFeatureJwt() : null;
        if (!isWpUserLoggedIn()) {
            return verifyFeatureJwt;
        }
        WpUser user = getLoggedInUser();
        String profileFeatureJwt = user != null ? user.getFeatureJwt() : null;
        return !TextUtils.isEmpty(verifyFeatureJwt) ? verifyFeatureJwt : profileFeatureJwt;
    }

    public PaywallService(Context ctx) {
        this.ctx = ctx;
    }

    public LiveEvent<PromoCodeRequestState> getPromoCodeRequestLiveData(){
        return promocodeRequestStateLiveData;
    }

    public LiveEvent<VerifyDeviceRequestState> getVerifyDeviceRequestStateLiveEvent() {
        return verifyDeviceRequestStateLiveEvent;
    }

    public synchronized static void initialize(
            Context ctx,
            ServiceConfigStub serviceConfig,
            String salt,
            PaywallConnector connector,
            PaywallOmniture omniture,
            AbstractStoreBillingHelper abstractStoreBillingHelper
    ) {
        Logger.d(TAG, "initialize PaywallService");
        PaywallService.storeBillingHelper = abstractStoreBillingHelper;
        if (instance == null) {
            instance = new PaywallService(ctx.getApplicationContext());
        }
        instance.connector = connector;
        instance.omniture = omniture;
        instance.getApiServiceInstance().setSalt(salt);
        instance.paywallPrefHelper = PaywallPrefHelper.getInstance(ctx.getApplicationContext());
        instance.getMeteringServiceInstance().initialize(serviceConfig);
        instance.adFreeSKUs = serviceConfig.getAdFreeSKUs();
        instance.baseSubscriptionProducts = serviceConfig.getBaseSubscriptionProducts();

        // Restore ad-free status from persisted user subscriptions (DB).
        // This runs before billing init so the UI has correct state immediately.
        WpUser wpUser = instance.isWpUserLoggedIn() ? instance.getLoggedInUser() : null;
        if (wpUser != null && wpUser.getSubscriptions() != null) {
            PaywallUtil.updateAdFreeStatusFromSubscriptions(wpUser.getSubscriptions(), PaywallUtil.AdFreeSource.PROFILE);
        }

        PaywallConstants.WP_API_URL = serviceConfig.getPaywallBaseURL();
        PaywallConstants.AUTH_PROFILE_API = instance.getOAuthConfigStub().getProfileUrl();
        PaywallConstants.AUTH_MIGRATE_API = instance.getOAuthConfigStub().getMigrateUrl();
        PaywallConstants.AUTH_REVOKE_API = instance.getOAuthConfigStub().getRevokeUrl();
        PaywallConstants.TETRO_API = serviceConfig.getTetroBaseUrl();
        PaywallConstants.METERING_PROXY_BASE_URL = serviceConfig.getMeteringProxyBaseUrl();
        PaywallConstants.USE_METERING_PROXY = serviceConfig.getUseMeteringProxy();
        PaywallConstants.AUTH_ONE_LINK_TOKEN_API = instance.getOAuthConfigStub().getOneLinkTokenUrl();
        PaywallConstants.IS_FTC_VISIBLE = serviceConfig.getFtcDialogVisibility();

        getBillingHelper().initAndCheckSubscription(ctx, serviceConfig);

        getBillingHelper().readRainbowSubscriptionFromDB();
        getBillingHelper().readAmazonClassicSubscriptionFromDB();
        instance.migrateSubscriptionIdFromLegacyPrefIfNeeded();

        if (getBillingHelper().cachedSubscription() == null && !instance.isWpUserLoggedIn()) {
            instance.clearSubscriptionInfo();
        }

        instance.reminderScreenConfig = serviceConfig.getReminderScreenConfig();
        instance.onboardingReminderModel = serviceConfig.getOnboardingReminder();
        instance.acquisitionReminderModel = serviceConfig.getAcquisitionReminder();
        instance.bottomCtaModel = serviceConfig.getBottomCtaModel();
        instance.paywallSheetModels = serviceConfig.getPaywallSheets();

        // Initialize CookiesService
        String cookiesUrl = ctx.getString(R.string.cookies_url);
        String cookiesDomain = ctx.getString(R.string.cookies_domain);
        if (instance.cookiesService == null && !TextUtils.isEmpty(cookiesDomain)) {
            try {
                instance.cookiesService = new CookiesService(cookiesUrl, cookiesDomain, ctx);
                instance.cookiesService.prepareCookieManager(instance.getLoggedInUser(), ctx, serviceConfig.getCookieConfig().getSameSiteEnabled());
            } catch (Exception e) {
                //catch webview package missing exceptions
                instance.connector.logE(new EventLog.Builder()
                        .setMessage("Cookie service init failed")
                        .setErrorMessage(e.getMessage()));
            }
        }
        instance.observeSubVerification(ProcessLifecycleOwner.get());
        instance.isSameSiteEnabled = serviceConfig.getCookieConfig().getSameSiteEnabled();
        instance.connector.onPaywallInitialize();
        instance.globalBannerConfig = serviceConfig.getGlobalBannerConfig();
    }

    public static boolean initialized() {
        return instance != null && storeBillingHelper != null;
    }

    public synchronized static void makePostInitializeVerifyCalls() {
        Logger.d(TAG, "makePostInitializeVerifyCalls");

        if (getConnector().isOnline()) {
            instance.verifySubscriptionIfRequired();
            if (instance.isWpUserLoggedIn()) {
                if (AuthHelper.getInstance(instance.ctx).shouldRecoverMissingTokenResponse()) {
                    if (PaywallPrefHelper.getInstance(instance.ctx).hasMigratedNullToken()) {
                        instance.connector.logW(new EventLog.Builder().setMessage("Migrate null token again"));
                    }
                    instance.migrateNullTokenUser();
                } else if (AuthHelper.getInstance(instance.ctx).shouldRefreshAccessToken()) {
                    instance.refreshAccessToken();
                } else {
                    instance.verifyUserSubscriptionPeriodically();
                }
            }

            if ((PaywallConstants.AMAZON_STORE).equals(getConnector().getStoreType())) {
                instance.verifyFreeTrialSubscription(false);
            }
            PaywallService.getConnector().setShouldVerifyPlayStoreResult(false); //reset flag
            PaywallService.getConnector().setShouldVerifyExternalPurchaseResult(false); //reset flag
        }
    }

    public int getCurrentTetroAction() {
        return currentTetroAction;
    }

    public String getCurrentTetroActionCodes() {
        return currentTetroActionCodes;
    }

    public void setCurrentTetroAction(int action) {
        this.currentTetroAction = action;
    }

    public void setCurrentTetroActionCodes(String codesCsv) {
        this.currentTetroActionCodes = codesCsv;
    }


    public Context getContext() {
        return ctx;
    }

    public static PaywallConnector getConnector() {
        return getInstance().connector;
    }

    public static String getAccessToken() {
        AuthStateManager authHelper = AuthStateManager.getInstance(instance.ctx);
        return authHelper.getCurrent().getAccessToken();
    }

    public static PaywallOmniture getOmniture() {
        return getInstance().omniture;
    }

    public static SharedPreferences getSharedPreferences() {
        return instance.getContext().getApplicationContext().getSharedPreferences(PaywallConstants.PREFS_NAME, 0);
    }

    public static PaywallPrefHelper getPaywallPrefHelper() {
        return instance.paywallPrefHelper;
    }

    public synchronized static PaywallService getInstance() {
        return instance;
    }

    public static synchronized AbstractStoreBillingHelper getBillingHelper() {
        return storeBillingHelper;
    }

    public SubscriptionInfo getSubscriptionInfo() {

        if (subscriptionInfo == null) {
            subscriptionInfo = new SubscriptionInfo();
        }

        return subscriptionInfo;
    }

    public String getInAppSubProductId() {
        Subscription sub = getBillingHelper().cachedSubscription();
        return sub == null ? null : sub.getStoreProductId();
    }

    /**
     * Resolve the user's current base subscription product ID using multiple fallback strategies:
     * 1. Cached billing subscription (from Play Store)
     * 2. Logged-in user's subscription SKU (from profile)
     * 3. Profile attributes (access level + duration) mapped via productToSkuMap config
     */
    public String resolveBaseProductId() {
        // 1. Try cached billing subscription
        String productId = getInAppSubProductId();
        if (!TextUtils.isEmpty(productId)) return productId;

        // 2. Try logged-in user's SKU from profile
        if (isWpUserLoggedIn() && getLoggedInUser() != null) {
            String profileSku = getLoggedInUser().getSubSku();
            if (!TextUtils.isEmpty(profileSku)) return profileSku;
        }

        // 3. Derive from access level + duration via config map
        if (isWpUserLoggedIn()) {
            String accessLevel = getWapoAccessServiceInstance().currentSubscriptionType();
            String tier = null;
            if (PaywallConstants.WP_PREMIUM.equals(accessLevel) || PaywallConstants.WP_PRODUCT_ALL.equals(accessLevel)) {
                tier = "premium";
            } else if (PaywallConstants.WP_BASIC.equals(accessLevel)) {
                tier = "basic";
            }

            String term = null;
            WpUser user = getLoggedInUser();
            if (user != null && user.getSubDuration() != null) {
                String duration = user.getSubDuration();
                int days = parseDaysFromDuration(duration);
                if (days > 360) {
                    term = "annual";
                } else if (days > 0 && days <= 31) {
                    term = "monthly";
                }
            }

            if (tier != null && term != null) {
                String tierKey = tier + "-" + term;
                Map<String, ProductSkuEntry> skuMap =
                        ConfigManager.Companion.getInstance().getConfig().getPaywallConf().getProductToSkuMap();
                if (skuMap != null) {
                    ProductSkuEntry entry = skuMap.get(tierKey);
                    if (entry != null) {
                        return entry.getResolvedSku();
                    }
                }
            }
        }

        return null;
    }

    /**
     * determines the target SKU based on the user's current subscription and product flag (e.g. PREMIUM)
     * @return The resolved target SKU or null if invalid
     */
    public String getTargetSubscriptionProductId(String targetProduct) {
        if (TextUtils.isEmpty(targetProduct)) {
            return null;
        }

        // get the user's current subscription (e.g., "basic-monthly")
        String currentSubscription = getCurrentSubscriptionPlan();

        if (TextUtils.isEmpty(currentSubscription) || !currentSubscription.contains("-")) {
            Logger.d(TAG, "getTargetSubscriptionProductId: Could not resolve current plan key.");
            return null;
        }

        // determine tier and billing cycle for user's current subscription (e.g., "basic" and "monthly")
        String[] parts = currentSubscription.split("-");
        String currentTier = parts[0];
        String currentCycle = parts[1];

        // determine target product using provided targetProduct flag (e.g. "premium" or "basic")
        String target = targetProduct.toLowerCase().trim();
        if (target.equals("basic") || target.equals("premium")) {
            currentTier = target;
        } else {
            Logger.d(TAG, "getTargetSubscriptionProductId: Unknown target dimension=" + targetProduct);
            return null;
        }

        // reconstruct the new plan (e.g., "premium-monthly")
        String targetPlan = currentTier + "-" + currentCycle;

        // using the target plan, look up the corresponding SKU in the productToSkuMap config
        Map<String, ProductSkuEntry> skuMap = ConfigManager.Companion.getInstance().getConfig().getPaywallConf().getProductToSkuMap();

        if (skuMap != null && skuMap.containsKey(targetPlan)) {
            ProductSkuEntry targetEntry = skuMap.get(targetPlan);
            if (targetEntry != null) {
                return targetEntry.getResolvedSku();
            }
        }
        Logger.d(TAG, "getTargetSubscriptionProductId: SKU not found for key=" + targetPlan);
        return null;
    }

    public String getAdFreeProductId() {
        String currentPlanKey = getCurrentSubscriptionPlan();

        // Look up the ad-free product ID from the separate adFreeProductToSkuMap
        if (currentPlanKey != null) {
            Map<String, String> adFreeMap = ConfigManager.Companion.getInstance().getConfig().getPaywallConf().getAdFreeProductToSkuMap();
            if (adFreeMap != null && adFreeMap.containsKey(currentPlanKey)) {
                return adFreeMap.get(currentPlanKey);
            }
        }

        return null;
    }

    /**
     * Resolves the user's current subscription plan (e.g., "basic-monthly", "premium-annual").
     *
     * determines the active plan by
     * 1. attempting to match the resolved base SKU against the config ProductToSku map.
     * 2. falls back to deriving the key from the logged-in user's profile attributes
     * (mapping access level to the tier, and subscription duration to the billing cycle).
     *
     * @return The derived plan key in the format "{tier}-{term}", or null if the state cannot be determined.
     */
    private String getCurrentSubscriptionPlan() {
        String baseSku = resolveBaseProductId();

        Map<String, ProductSkuEntry> skuMap = ConfigManager.Companion.getInstance().getConfig().getPaywallConf().getProductToSkuMap();
        String baseTierKey = null;

        if (skuMap != null) {
            // Try to find the tier key (e.g. "basic-monthly") by matching the resolved base SKU in the map
            if (!TextUtils.isEmpty(baseSku)) {
                for (Map.Entry<String, ProductSkuEntry> entry : skuMap.entrySet()) {
                    String key = entry.getKey();
                    ProductSkuEntry skuEntry = entry.getValue();
                    if (skuEntry != null && baseSku.equals(skuEntry.getResolvedSku())) {
                        baseTierKey = key;
                        break;
                    }
                }
            }

            // Fallback to profile attributes to derive the tier key if SKU match failed
            if (baseTierKey == null && isWpUserLoggedIn()) {
                WpUser user = getLoggedInUser();
                String accessLevel = getWapoAccessServiceInstance().currentSubscriptionType();

                String tier = null;
                if (PaywallConstants.WP_PREMIUM.equals(accessLevel) || PaywallConstants.WP_PRODUCT_ALL.equals(accessLevel)) {
                    tier = "premium";
                } else if (PaywallConstants.WP_BASIC.equals(accessLevel)) {
                    tier = "basic";
                }

                // subDuration format from profile is e.g. "1.99/30 days" or "99.99/365 days"
                String term = null;
                if (user != null && user.getSubDuration() != null) {
                    String duration = user.getSubDuration();
                    // Extract numeric days from the duration string (e.g. "1.99/30 days" -> 30)
                    int days = parseDaysFromDuration(duration);
                    if (days > 360) {
                        term = "annual";
                    } else if (days > 0 && days <= 31) {
                        term = "monthly";
                    }
                }

                if (tier != null && term != null) {
                    baseTierKey = tier + "-" + term;
                }
            }
        }
        return baseTierKey;
    }

    /**
     * Extracts the number of days from a duration string like "1.99/30 days" or "99.99/365 days".
     * Returns -1 if parsing fails.
     */
    private int parseDaysFromDuration(String duration) {
        try {
            // Expected format: "price/N days" — extract the number after '/'
            int slashIndex = duration.indexOf('/');
            if (slashIndex >= 0 && slashIndex < duration.length() - 1) {
                String afterSlash = duration.substring(slashIndex + 1).trim();
                // Extract leading digits (e.g. "30 days" -> "30", "365 days" -> "365")
                StringBuilder digits = new StringBuilder();
                for (char c : afterSlash.toCharArray()) {
                    if (Character.isDigit(c)) {
                        digits.append(c);
                    } else {
                        break;
                    }
                }
                if (digits.length() > 0) {
                    return Integer.parseInt(digits.toString());
                }
            }
        } catch (Exception e) {
            Logger.d(TAG, "parseDaysFromDuration: failed to parse duration=" + duration);
        }
        return -1;
    }

    /**
     * Determines if the current user has a Premium-tier subscription.
     */
    public boolean isPremiumTierUser() {
        return checkUserTier(PaywallConstants.WP_PREMIUM);
    }

    /**
     * Determines if the current user has a Basic-tier subscription.
     */
    public boolean isBasicTierUser() {
        return checkUserTier(PaywallConstants.WP_BASIC);
    }

    /**
     * Common helper to check user tier across multiple sources:
     * 1. Cached IAP subscription product ID
     * 2. Logged-in user's SKU from profile
     * 3. Access level from profile
     *
     * @param tier The tier to check for (e.g., WP_PREMIUM, WP_BASIC)
     * @return true if user has the specified tier
     */
    private boolean checkUserTier(String tier) {
        // Check 1: Try cached IAP subscription product ID
        String productId = getInAppSubProductId();
        if (!TextUtils.isEmpty(productId)) {
            if (matchesTier(productId, tier)) {
                return true;
            }
        }

        // Check 2: Try logged-in user's SKU from profile
        if (isWpUserLoggedIn() && getLoggedInUser() != null) {
            String profileSku = getLoggedInUser().getSubSku();
            if (!TextUtils.isEmpty(profileSku)) {
                if (matchesTier(profileSku, tier)) {
                    return true;
                }
            }
        }

        // Check 3: Check access level from profile
        if (isWpUserLoggedIn()) {
            String accessLevel = getWapoAccessServiceInstance().currentSubscriptionType();
            if (tier.equals(accessLevel)) {
                return true;
            }
            // Also check for ALL_ACCESS which is equivalent to PREMIUM
            if (PaywallConstants.WP_PREMIUM.equals(tier) && PaywallConstants.WP_PRODUCT_ALL.equals(accessLevel)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given product ID or SKU matches the specified tier
     */
    private boolean matchesTier(String productOrSku, String tier) {
        String lowerProduct = productOrSku.toLowerCase();
        if (PaywallConstants.WP_PREMIUM.equals(tier)) {
            return lowerProduct.contains("premium") || "monthly_all_access".equals(productOrSku);
        } else if (PaywallConstants.WP_BASIC.equals(tier)) {
            return lowerProduct.contains("basic");
        }
        return false;
    }

    // Returns the ad-free product's short title from the logged-in user's subscriptions list.
    public String getAdFreeShortTitleFromSubscriptions() {
        WpUser user = getLoggedInUser();
        if (user == null || user.getSubscriptions() == null) return null;
        for (SubItem item : user.getSubscriptions()) {
            if (item == null) continue;
            if (PaywallReactive.isAdFreeProduct(item)) {
                if (!TextUtils.isEmpty(item.getShortTitle())) return item.getShortTitle();
            }
        }
        return null;
    }

    public String getUUID() {
        if (getLoggedInUser() != null) {
            return getLoggedInUser().getUuid();
        } else if (getVerifySubUUID() != null) {
            return getVerifySubUUID();
        } else {
            return getSubscriptionID();
        }
    }

    public WPPaywallApiService getApiServiceInstance() {
        if (apiService == null) {
            apiService = new WPPaywallApiService();
        }

        return apiService;
    }

    public MeteringService getMeteringServiceInstance() {
        if (meteringService == null) {
            meteringService = new MeteringService();
        }

        return meteringService;
    }

    public WapoAccessService getWapoAccessServiceInstance() {
        if (wapoAccessService == null) {
            wapoAccessService = new WapoAccessService();
        }

        return wapoAccessService;
    }

    public boolean isAtLimit(String category, ArticleStub article) {
        boolean storeSubscriptionActive = getBillingHelper().isClassicOrRainbowSubscriptionActive();
        boolean isAtLimit = !storeSubscriptionActive && getMeteringServiceInstance().isAtLimit(category, article);

        if (isAtLimit) {
            if (getCookiesService() != null) {
                getCookiesService().setMeterHitCookie(ctx);
            }
        }
        return isAtLimit;
    }

    public boolean hasBeenPaywalled() {
        return getMeteringServiceInstance().hasBeenPaywalled();
    }

    public float getCurrentArticleCount() {
        return MeteringService.getCurrentArticleCount();
    }

    public int getCurrentArticleCountForRule1() {
        return MeteringPrefs.getCurrentArticleCountForRule1(0);
    }

    public int getCurrentArticleMeterReason() {
        return MeteringPrefs.getCurrentArticleMeterReason();
    }

    public int getCurrentArticleCountForRule2() {
        return MeteringPrefs.getCurrentArticleCountForRule2();
    }

    public Message paywallMessageForArticle(Handler handler,
                                            final String categoryName,
                                            final ArticleStub article,
                                            final Bundle bundle) {
        Message m = Message.obtain();
        m.what = RUN_PAYWALL_SERVICE;
        m.obj = new PaywallServiceRunnable(handler, categoryName, article, bundle);
        return m;
    }

    public boolean shouldShowPaywallForRule1() {
        return MeteringPrefs.shouldShowPaywallFor(PaywallConstants.PW_SHOW_RULE1);
    }

    public boolean shouldShowPaywallForRule0() {
        return MeteringPrefs.shouldShowPaywallFor(PaywallConstants.PW_SHOW);
    }

    public boolean shouldShowPaywallForRule2() {
        return MeteringPrefs.shouldShowPaywallFor(PaywallConstants.PW_SHOW_RULE2);
    }


    public boolean isTurnedOn() {
        return PaywallService.getSharedPreferences().getBoolean(PaywallConstants.PW_TURNED_ON, false);
    }

    public WpUser getLoggedInUser() {
        return WpPaywallHelper.getLoggedInUser();
    }

    public void logOutCurrentUser() {
        instance.connector.clearOneTrustData(ctx);
        revokeUserSession();
        WpPaywallHelper.cleanUsers();
        PaywallPrefHelper.getInstance(ctx).clearLoginIdFromPrefs(ctx);
        if (cookiesService != null) {
            cookiesService.clearCookies();
            cookiesService.setWebViewCookie(ctx);
            if (cookiesService.getWasMeterCookieSet()) {
                cookiesService.setMeterHitCookie(ctx);
            }
        }
        PaywallPrefHelper.getInstance(ctx).setPrefIsUserMissingRefreshToken(false);
        getConnector().onLogoutComplete();
    }

    public void revokeUserSession() {
        new RevokeUserTask().execute();
    }

    public boolean isWpUserLoggedIn() {
        return getWapoAccessServiceInstance().isWpUserLoggedIn();
    }

    /**
     * Includes users on Amazon free trial.
     * Includes 'F' sub users who have a 'Free Days' trial.
     * Does not include 'F' sub users who have a 'Free Articles' trial.
     */
    public boolean isPremiumUser() {
        String lastIapSubStatus = PaywallService.getConnector().getIapSubscriptionStatus();
        return getWapoAccessServiceInstance().isPremiumUser() || isSubActive() || PaywallConstants.ACTIVE.equals(lastIapSubStatus)
                || PaywallConstants.SUSPENDED.equals(lastIapSubStatus);
    }

    public boolean isSubActive() {
        return getBillingHelper().isClassicOrRainbowSubscriptionActive();
    }

    /**
     * Returns the raw subscription state string from preferences (e.g. "PAUSE_SCHEDULED",
     * "SUB_GRACE_PERIOD", "SUB_ON_HOLD"), or null if none is stored.
     */
    public String getSubStateString() {
        return getPaywallPrefHelper().getPrefLastSubState();
    }

    /**
     * Includes 'F' sub users who have a 'Free Days' trial.
     * Does not include 'F' sub users who have a 'Free Articles' trial.
     * Used to customize the 'Free Days' experience to differentiate it from regular Subscribers.
     */
    public boolean isFreeDaysUser() {
        return getWapoAccessServiceInstance().isFreeDaysUser();
    }

    /**
     * Includes 'F' sub users who have a 'Free Days' trial.
     * Does not include 'F' sub users who have a 'Free Articles' trial.
     * Used to customize the 'Free Days' experience to differentiate it from regular Subscribers.
     * True even with 0 articles remaining.
     */
    public boolean isFreeArticlesUser() {
        return getWapoAccessServiceInstance().isFreeArticlesUser();
    }

    /**
     * 'F' sub status with 'MFA_T` attribute that indicates the user has a time based free subscription (only enabled for apps)
     */
    public boolean isMobileFreeDaysUser() {
        return getWapoAccessServiceInstance().isMobileFreeDaysUser();
    }

    public boolean isAnonymousUser() {
        return isSubActive() && !isWpUserLoggedIn();
    }

    /**
     * If the device has a non-null active subscription, use this method to check if the subscription has a billing issue
     *
     * @return
     */
    public boolean isSubInGracePeriod() {
        Subscription subs = getBillingHelper().getClassicOrRainbowSubscription();
        return subs!=null && PaywallConstants.SUB_GRACE_PERIOD.equalsIgnoreCase(subs.getSubState()) && !getWapoAccessServiceInstance().isPremiumUser();
    }

    //TODO how to handle classic or rainbow sub here
    public boolean isSubOnHold() {
        Subscription activeSub = getBillingHelper().cachedSubscription();
        String lastSubState = getPaywallPrefHelper().getPrefLastSubState();
        return activeSub==null && PaywallConstants.SUB_ON_HOLD.equalsIgnoreCase(lastSubState) && !getWapoAccessServiceInstance().isPremiumUser();
    }

    public boolean shouldEnableAdfreeExperience() {
        Context context = PaywallService.instance.ctx;
        if (context == null) {
            return false;
        }

        // Check 1: Check for a Play Store ad-free purchase via PaywallReactive.
        if (PaywallReactive.INSTANCE.isAdFree().getValue()) {
            return true;
        }

        // Check 2: Check for ad-free attributes from the user's WP profile via PaywallReactive.
        Set<String> subAttributes = PaywallReactive.INSTANCE.getSubAttributes().getValue();
        String adFreeKey = context.getResources().getString(R.string.sub_attribute_ad_free);
        String adFreeEuKey = context.getResources().getString(R.string.sub_attribute_ad_free_eu);

        return PaywallReactive.isAdFreeInAttributes(subAttributes, adFreeKey, adFreeEuKey);
    }

    public Date getAccessExpiryDate(PaywallConstants.SubscriptionType subType) {
        if (subType == PaywallConstants.SubscriptionType.STORE) {
            return getBillingHelper().getAccessExpiryDate();
        } else {
            return getWapoAccessServiceInstance().getAccessExpiryDate();
        }
    }

    public String getSubStatus() {
        String iapSubStatus = PaywallService.getConnector().getIapSubscriptionStatus();
        String rainbowSubStatus = PaywallService.getConnector().getRainbowSubscriptionStatus();
        String amazonClassicSubStatus = PaywallService.getConnector().getAmazonClassicSubscriptionStatus();
        String profileSubStatus = !isWpUserLoggedIn() ? "" : WpPaywallHelper.getLoggedInUser().getSubStatus() == null ? "" : WpPaywallHelper.getLoggedInUser().getSubStatus();

        /* Priority order for statuses:
         * 1. IAP: ACTIVE or SUSPENDED
         * 2. Rainbow: ACTIVE
         * 3. Amazon Classic: Active
         * 4. Profile: ACTIVE or SUSPENDED
         * 5. IAP: PAUSED
         * 6. Profile: PAUSED
         * 7. Profile: TERMINATED
         * 8. IAP if not EMPTY
         * 9. Rainbow if not EMPTY
         * 10. Amazon Classic if not EMPTY
         * 11. Empty
         */
        if (iapSubStatus.equals(PaywallConstants.ACTIVE) || iapSubStatus.equals(PaywallConstants.SUSPENDED)) {
            return iapSubStatus;
        } else if (rainbowSubStatus.equals(PaywallConstants.ACTIVE)) {
            return rainbowSubStatus;
        } else if (PaywallConstants.ACTIVE.equals(amazonClassicSubStatus)) {
            return amazonClassicSubStatus;
        } else if (profileSubStatus.equals(PaywallConstants.ACTIVE) || profileSubStatus.equals(PaywallConstants.SUSPENDED)) {
            return profileSubStatus;
        } else if (iapSubStatus.equals(PaywallConstants.PAUSED)) {
            return iapSubStatus;
        } else if (profileSubStatus.equals(PaywallConstants.PAUSED)) {
            return profileSubStatus;
        } else if (profileSubStatus.equals(PaywallConstants.TERMINATED)) {
            return profileSubStatus;
        } else if (!iapSubStatus.equals(PaywallConstants.EMPTY)){
            return iapSubStatus;
        } else if (!rainbowSubStatus.equals(PaywallConstants.EMPTY)) {
            return rainbowSubStatus;
        } else if (!amazonClassicSubStatus.equals(PaywallConstants.EMPTY)) {
            return amazonClassicSubStatus;
        } else {
            return PaywallConstants.EMPTY;
        }
    }

    /**
     * Checks if a given subscription source matches the current sub status.
     * Used by Airship to append subscription source information.
     * Set up to permit multiple sub sources, for example if user has multiple Terminated subs.
     */
    public boolean isSubSource(PaywallConstants.SubscriptionSource subSource) {
        String subStatus = getSubStatus();
        String sourceStatus = null;

        switch (subSource) {
            case CLASSIC_IAP:
                sourceStatus = PaywallService.getConnector().getIapSubscriptionStatus();
                break;
            case MIGRATED_RAINBOW:
                sourceStatus = PaywallService.getConnector().getRainbowSubscriptionStatus();
                break;
            case WAPO_PROFILE:
                if (isWpUserLoggedIn() && WpPaywallHelper.getLoggedInUser().getSubStatus() != null) {
                    sourceStatus = WpPaywallHelper.getLoggedInUser().getSubStatus();
                }
                break;
            default:
                break;
        }

        return subStatus.equals(sourceStatus);
    }

    public boolean isSubscriptionSuspended() {
        String subStatus = getSubStatus();
        return PaywallConstants.SUSPENDED.equals(subStatus);
    }

    public boolean isSubscriptionTerminated() {
        String subStatus = getSubStatus();
        return PaywallConstants.TERMINATED.equals(subStatus);
    }

    public boolean isAppStoreSubscriptionPaused() {
        return (isSubSource(PaywallConstants.SubscriptionSource.CLASSIC_IAP)
                || isSubSource(PaywallConstants.SubscriptionSource.MIGRATED_RAINBOW))
                && isSubscriptionPaused();
    }

    public boolean isSiteSubscriptionPaused() {
        return isSubSource(PaywallConstants.SubscriptionSource.WAPO_PROFILE) && isSubscriptionPaused();
    }

    public boolean isSubscriptionPaused() {
        String subStatus = getSubStatus();
        return PaywallConstants.PAUSED.equals(subStatus);
    }

    public boolean doesIapOrProfileHavePauseStatus() {
        String iapSubStatus = PaywallService.getConnector().getIapSubscriptionStatus();
        String profileSubStatus = !isWpUserLoggedIn() ? "" : WpPaywallHelper.getLoggedInUser().getSubStatus() == null ? "" : WpPaywallHelper.getLoggedInUser().getSubStatus();
        //check iap sub status or profile sub status to ensure we re-verify sub
        return PaywallConstants.PAUSED.equals(iapSubStatus) || PaywallConstants.PAUSED.equals(profileSubStatus);
    }

    public boolean isAppStoreSubscriptionPauseScheduled() {
        return (isSubSource(PaywallConstants.SubscriptionSource.CLASSIC_IAP)
                || isSubSource(PaywallConstants.SubscriptionSource.MIGRATED_RAINBOW))
                && isSubscriptionPauseScheduled();
    }

    public boolean isSiteSubscriptionPauseScheduled() {
        return isSubSource(PaywallConstants.SubscriptionSource.WAPO_PROFILE) && isSubscriptionPauseScheduled();
    }

    public boolean isSubscriptionPauseScheduled() {
        String subState = getPaywallPrefHelper().getPrefLastSubState();
        String subStatus = getSubStatus();
        return PaywallConstants.PAUSE_SCHEDULED.equals(subState) && PaywallConstants.ACTIVE.equals(subStatus);
    }

    public String getPaywallPartnerId() {
        String partnerId = "";
        if (PaywallService.getInstance().isWpUserLoggedIn()) {
            if (PaywallService.getInstance().getLoggedInUser().getPartnerId() != null) {
                partnerId = PaywallService.getInstance().getLoggedInUser().getPartnerId();
            }
        }
        return partnerId;
    }

    public String getLoginId() {
        String loginId = "";
        PaywallService service = PaywallService.getInstance();
        if (service.isWpUserLoggedIn()) {
            WpUser user = service.getLoggedInUser();
            loginId = user.getUuid();
        }
        return loginId;
    }

    public String getIterableUserId() {
        return connector.getIterableUserId();
    }


    public String getPaywallSource() {
        String pw_source = "";
        PaywallService service = PaywallService.getInstance();
        if (service.isWpUserLoggedIn()) {

            //TODO handle rainbow sub here
            if (PaywallService.getBillingHelper().isClassicOrRainbowSubscriptionActive()) {
                pw_source = "wapo/app_store";
            } else if (PaywallService.getInstance().isPremiumUser()) {
                pw_source = "wapo";
            }
        } else if (PaywallService.getBillingHelper().isClassicOrRainbowSubscriptionActive()) {
            pw_source = "app_store";
        }
        return pw_source;
    }

    public OAuthConfigStub getOAuthConfigStub() {
        return ConfigManager.Companion.getInstance().getConfig().getPaywallConfig().getOAuthConfigStub();
    }

    private void observeSubVerification(LifecycleOwner owner){
        subVerifyLiveEvent.observe(owner, verifyState -> {
            if(verifyState instanceof VerifyState.NeedsVerification) {
                verifyDeviceSubscription(true, false, verifyState.isPeriodic());
            } else {
                Logger.d(TAG, "verifyDeviceSubscriptionPeriodically /verify ");
            }
        });
    }

    private void verifySubscriptionIfRequired() {
        long lastTimeSubWasVerified = paywallPrefHelper.getLastVerifyDeviceSubTime();
        long currentTimeInMillis = System.currentTimeMillis();
        // Verify every 24 hours on prod build or determine if user needs to be verified immediately. On debug builds check if verifyOnEveryLaunch flag is set for testing.
        if (currentTimeInMillis >= lastTimeSubWasVerified + PaywallConstants.TWENTY_FOUR_HOURS_IN_MILLISECONDS
                || shouldVerifyDeviceImmediately() || getConnector().canVerifyOnEveryLaunch()) {
            Logger.d(TAG, "verifyDeviceSubscriptionPeriodically /verify ");
            VerifyState state = VerifyState.NeedsVerification.INSTANCE;
            state.setPeriodic(true);
            dispatchVerifySub(state);
            verifyDeviceRequestStateLiveEvent.postValue(VerifyDeviceRequestState.InProgress.INSTANCE);
        }
    }

    public void verifyDeviceSubscription(boolean forceVerify, boolean requestPromoCode, boolean isPeriodicCheck){
        wapoAccessService.verifyDeviceSubscription(forceVerify, requestPromoCode, isPeriodicCheck, result-> {
            boolean success = result != null && result.isSuccess();
            if (success) {
                getConnector().logD(new EventLog.Builder().setMessage("Device subscription verification complete /verify"));
                instance.paywallPrefHelper.setPrefVerifyDeviceSubLastTime(System.currentTimeMillis());
            }
            PaywallReactive.notifyVerifyComplete(success);
            return null;
        });
    }

    public void authenticateWithOneLinkToken(String oneLinkToken, Activity activity) {
        if (isWpUserLoggedIn()) logOutCurrentUser();
        wapoAccessService.authenticateWithOneLinkToken(oneLinkToken, tokenResponse-> {
            if (tokenResponse == null) {
                instance.connector.logE(new EventLog.Builder().setMessage("/one-link/token/ login failed: tokenResponse is null"));
            } else {
                AuthHelper.getInstance(instance.ctx).handleAccessTokenResponse(tokenResponse, null);
                if (AuthHelper.getInstance(instance.ctx).getAccessToken() != null) {
                    if (AuthHelper.getInstance(instance.ctx).getIdToken() != null) {
                        String idToken = AuthHelper.getInstance(instance.ctx).getIdToken();
                        try {
                            boolean success = PaywallService.getInstance().getApiServiceInstance().processUserFromJWTClaim(AuthHelper.getInstance(instance.ctx).getDecodedJWT(idToken));
                            if (success) {
                                instance.connector.logD(new EventLog.Builder().setMessage("/one-link/token/ login succeeded, subStatus=" + PaywallService.getInstance().getSubStatus()));
                                PaywallService.getConnector().trackOneLinkSignIn();
                                Intent activityIntent = activity != null ? activity.getIntent() : null;
                                Bundle extras = activityIntent != null ? activityIntent.getExtras() : null;
                                PaywallService.getConnector().startOnboardingSubscriber(extras);
                            } else {
                                instance.connector.logE(new EventLog.Builder().setMessage("/one-link/token/ login failed: JWT parsing failed"));
                            }
                        } catch (Exception e) {
                            if (e instanceof JsonSyntaxException) {
                                JWT jwt = AuthHelper.getInstance(instance.ctx).getDecodedJWT(idToken);
                                String json = jwt != null ? jwt.getClaim("subdata").asString() : "";
                                instance.connector.logE(new EventLog.Builder()
                                        .setMessage("/one-link/token/ login JWT malformed json")
                                        .set("data", json));
                            }
                            instance.connector.logE(new EventLog.Builder()
                                    .setMessage("/one-link/token/ login JWT other exception")
                                    .setErrorMessage(e.getMessage()));
                            new FetchUserProfileTask().execute();
                        }
                    }
                } else {
                    instance.connector.logE(new EventLog.Builder().setMessage("/one-link/token/ login failed: accessToken is null in non-null response"));
                }
            }
            return null;
        });
    }

    public void dispatchVerifySub(VerifyState state) {
        subVerifyLiveEvent.postValue(state);
    }

    public void triggerProfileRefresh() {
        fetchUserProfile();
    }

    /**
     * This method will verify a logged in user once a day.
     */
    private void verifyUserSubscriptionPeriodically() {
        boolean isLoggedInUser = PaywallService.getInstance().isWpUserLoggedIn();
        String accessToken = AuthHelper.getInstance(PaywallService.instance.ctx).getAccessToken();
        if (!isLoggedInUser || accessToken == null || PaywallService.getConnector().getClientId() == null) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Profile error")
                    .set("is_logged_in_user", isLoggedInUser)
                    .set("is_token_null", (accessToken == null))
                    .set("is_client_id_null", (PaywallService.getConnector().getClientId() == null)));
            return;
        }


        long lastTimeInMillisUserWasVerified = paywallPrefHelper.getLastVerifyUserTime();
        long currentTimeInMillis = System.currentTimeMillis();
        // Verify every 24 hours on prod build or determine if user needs to be verified immediately. On debug builds check if verifyOnEveryLaunch flag is set for testing.
        if (currentTimeInMillis >= lastTimeInMillisUserWasVerified + PaywallConstants.TWENTY_FOUR_HOURS_IN_MILLISECONDS
                || shouldVerifyUserImmediately() || getConnector().canVerifyOnEveryLaunch()) {
            instance.paywallPrefHelper.setPrefVerifyUserLastTime(System.currentTimeMillis());
            Logger.d(TAG, "verifyUserSubscriptionPeriodically /profile ");
            new FetchUserProfileTask().execute();
        }
    }

    /**
     * Making fetch profile task forceVerify
     */
    public void fetchUserProfile() {
        boolean isLoggedInUser = PaywallService.getInstance().isWpUserLoggedIn();
        if (isConnectedOrConnecting(ctx) && isLoggedInUser) {
            new FetchUserProfileTask().execute();
        }
    }

    public void verifyFreeTrialSubscription(boolean forceVerify) {
        new VerifyFreeTrialSubscriptionTask().execute(forceVerify);
    }

    public boolean shouldVerifyDeviceImmediately() {
        if (PaywallService.getConnector().getShouldVerifyPlayStoreResult()
                || PaywallService.getInstance().doesIapOrProfileHavePauseStatus()
                || PaywallService.getConnector().getShouldVerifyExternalPurchaseResult()) {
            // If user navigated to the Play Store from our app,
            // or the subscription is paused,
            // or user navigated to an external purchase link call verify immediately.
            return true;
        }
        return false;
    }

    public boolean shouldVerifyUserImmediately() {
        if ((PaywallService.getConnector().getShouldVerifyPlayStoreResult()
                || PaywallService.getInstance().doesIapOrProfileHavePauseStatus())
                || PaywallService.getConnector().getShouldVerifyExternalPurchaseResult()) {
            // If user navigated to the Play Store from our app
            // or the subscription is paused
            // or user navigated to an external purchase link,
            // or we got triggerProfileRefresh signal from SubsJSInterface
            return true;
        }
        return false;
    }

    public void migrateNullTokenUser() {
        new MigrateNullTokenUserTask().execute();
    }

    public void migrateLoggedInUser(String tokenResponseJson) {
        if (!isWpUserLoggedIn()) {
            if (tokenResponseJson != null) {
                TokenResponse tokenResponse = PaywallService.getInstance().getApiServiceInstance().migrateLoggedInUser(tokenResponseJson);
                if (tokenResponse == null) {
                    instance.connector.logE(new EventLog.Builder().setMessage("Migrate logged in user failed: tokenResponse is null"));
                } else {
                    AuthHelper.getInstance(instance.ctx).handleAccessTokenResponse(tokenResponse, null);
                    if (AuthHelper.getInstance(instance.ctx).getIdToken() != null) {
                        String idToken = AuthHelper.getInstance(instance.ctx).getIdToken();
                        try {
                            boolean success = PaywallService.getInstance().getApiServiceInstance().processUserFromJWTClaim(AuthHelper.getInstance(instance.ctx).getDecodedJWT(idToken));
                            if (success) {
                                instance.connector.logD(new EventLog.Builder().setMessage("Migrate logged in user succeeded, subStatus=" + PaywallService.getInstance().getSubStatus()));
                            } else {
                                instance.connector.logE(new EventLog.Builder().setMessage("Migrate logged in user failed: JWT parsing failed"));
                            }
                        } catch (Exception e) {
                            if(e instanceof JsonSyntaxException) {
                                JWT jwt = AuthHelper.getInstance(instance.ctx).getDecodedJWT(idToken);
                                String json = jwt != null ? jwt.getClaim("subdata").asString() : "";
                                instance.connector.logE(new EventLog.Builder()
                                        .setMessage("Migrate JWT malformed json")
                                        .set("data", json));
                            }
                            instance.connector.logE(new EventLog.Builder()
                                    .setMessage("Migrate JWT other exception")
                                    .setErrorMessage(e.getMessage()));
                            new FetchUserProfileTask().execute();
                        }
                    } else {
                        instance.connector.logE(new EventLog.Builder().setMessage("Migrate logged in user failed: id_token is null in non-null response"));
                    }
                }
            }

        }
    }

    public boolean handlePaywallActivityResult(Activity activity, int requestCode, int resultCode,
                                               Class<? extends Activity> landingActivityClass) {
        switch (requestCode) {
            case PAYWALL_RESULT_ID: {
                if (resultCode == Activity.RESULT_CANCELED) {
                    if (landingActivityClass != null) {
                        Intent intent = new Intent(activity, landingActivityClass);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                        //activity.finish();
                    } else {
                        activity.finish();
                    }
                    return true;
                }
                break;
            }
        }
        return false;
    }

    public void setVerifySubUUID(String uuid) {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        settings.edit().putString(PaywallConstants.PW_UUID_PREF, uuid).apply();
    }

    public String getVerifySubUUID() {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        return settings.getString(PaywallConstants.PW_UUID_PREF, null);
    }

    public void setSubscriptionID(String subscriptionID) {
        SharedPreferences settings = PaywallService.getSharedPreferences();
        if (settings.contains(PaywallConstants.PW_SUBSCRIPTION_ID)) {
            settings.edit().remove(PaywallConstants.PW_SUBSCRIPTION_ID).apply();
        }

        if (getBillingHelper() == null) {
            return;
        }

        Subscription classicSubscription = getBillingHelper().cachedSubscription();
        if (classicSubscription != null) {
            classicSubscription.setSubscriptionId(subscriptionID);
            getBillingHelper().updateClassicSubscriptionId(subscriptionID);
        }

        Subscription rainbowSubscription = getBillingHelper().getMigratedRainbowSubscription();
        if (rainbowSubscription != null) {
            rainbowSubscription.setSubscriptionId(subscriptionID);
            getBillingHelper().updateRainbowSubscriptionId(subscriptionID);
        }

        Subscription amazonClassicSubscription = getBillingHelper().getMigratedAmazonClassicSubscription();
        if (amazonClassicSubscription != null) {
            amazonClassicSubscription.setSubscriptionId(subscriptionID);
            getBillingHelper().updateAmazonClassicSubscriptionId(subscriptionID);
        }
    }

    public String getSubscriptionID() {
        if (getBillingHelper() == null) {
            return null;
        }

        Subscription classicSubscription = getBillingHelper().cachedSubscription();
        if (classicSubscription != null && !TextUtils.isEmpty(classicSubscription.getSubscriptionId())) {
            return classicSubscription.getSubscriptionId();
        }

        Subscription rainbowSubscription = getBillingHelper().getMigratedRainbowSubscription();
        if (rainbowSubscription != null && !TextUtils.isEmpty(rainbowSubscription.getSubscriptionId())) {
            return rainbowSubscription.getSubscriptionId();
        }

        Subscription amazonClassicSubscription = getBillingHelper().getMigratedAmazonClassicSubscription();
        if (amazonClassicSubscription != null && !TextUtils.isEmpty(amazonClassicSubscription.getSubscriptionId())) {
            return amazonClassicSubscription.getSubscriptionId();
        }

        return null;
    }

//    public void migrateLegacyUser() {
//        new MigrateLegacyUserTask().execute();
//    }

    public Boolean shouldRefreshAccessToken() {
        return AuthHelper.getInstance(ctx).shouldRefreshAccessToken();
    }

    public void refreshAccessToken() {
        AuthHelper.getInstance(ctx).refreshAccessToken(new AuthHelper.TokenRefreshListener() {
            @Override
            public void onTokenRefresh() {
                boolean success = PaywallService.getInstance().getApiServiceInstance().processUserFromJWTClaim(AuthHelper.getInstance(ctx).getDecodedJWT(AuthHelper.getInstance(ctx).getIdToken()));
                if (!success) {
                    PaywallService.getConnector().logE(new EventLog.Builder().setMessage("Refresh token error, could not parse JWT"));
                    verifyUserSubscriptionPeriodically();
                }
            }
        });
    }
    private static class RevokeUserTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {

            String accessToken = AuthHelper.getInstance(PaywallService.instance.ctx).getAccessToken();
            String refreshToken = AuthHelper.getInstance(PaywallService.instance.ctx).getRefreshToken();
            OAuthConfigStub oAuthConfigStub = PaywallService.getInstance().getOAuthConfigStub();

            if (oAuthConfigStub != null) {

                if (refreshToken != null || accessToken != null) {
                    AuthHelper.getInstance(PaywallService.instance.ctx).clearAuthState(AuthHelper.AuthStateResetReason.REVOKE_USER_TASK);
                    return PaywallService.getInstance().getApiServiceInstance().revokeUser(refreshToken == null ? accessToken : refreshToken,
                            PaywallService.getConnector().getClientId(), PaywallService.getConnector().getClientSecret(), refreshToken == null ? PaywallConstants.ACCESS_TOKEN_PARAM : PaywallConstants.REFRESH_TOKEN_PARAM);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean success) {

            if (success != null && success) {
                getConnector().logD(new EventLog.Builder().setMessage("User login session successfully revoked."));
            } else {
                getConnector().logW(new EventLog.Builder().setMessage("Error revoking user session"));
            }
        }


    }

    private static class MigrateNullTokenUserTask extends AsyncTask<Void, Void, TokenResponse> {

        @Override
        protected TokenResponse doInBackground(Void... voids) {
            WpUser wpUser = PaywallService.getInstance().getLoggedInUser();
            String publicKey = PaywallService.getInstance().ctx.getString(R.string.public_key);
            OAuthConfigStub oAuthConfigStub = PaywallService.getInstance().getOAuthConfigStub();

            if (wpUser.getUuid() != null && PaywallService.getConnector() != null) {
                instance.connector.logD(new EventLog.Builder().setMessage("Starting migrate null token task"));
                return PaywallService.getInstance().getApiServiceInstance().migrateUserToAuth(wpUser.getUuid(), publicKey, PaywallService.getConnector().getClientId());
            } else {
                instance.connector.logE(new EventLog.Builder()
                        .setMessage("Migrate null token failed")
                        .set("uuid", wpUser.getUuid())
                        .set("oAuthConfigStub", oAuthConfigStub));
            }

            return null;
        }

        @Override
        protected void onPostExecute(TokenResponse tokenResponse) {
            if (tokenResponse == null) {
                instance.connector.logE(new EventLog.Builder().setMessage("Migrate null token failed: tokenResponse is null"));
            } else {
                AuthHelper.getInstance(instance.ctx).handleAccessTokenResponse(tokenResponse, null);
                if (AuthHelper.getInstance(instance.ctx).getAccessToken() != null) {
                    PaywallPrefHelper.getInstance(instance.ctx).setHasMigratedNullToken();
                    instance.connector.logD(new EventLog.Builder().setMessage("Migrate null token succeeded"));
                } else {
                    instance.connector.logE(new EventLog.Builder().setMessage("Migrate null token failed: accessToken is null in non-null response"));
                }
            }
        }
    }

    private static class VerifyFreeTrialSubscriptionTask extends AsyncTask<Boolean, Void, PaywallResult> {

        @Override
        protected PaywallResult doInBackground(Boolean... params) {
            boolean forceVerify = params[0];
            if (PaywallService.getInstance() != null) {
                final boolean isAlreadyVerified = PaywallService.getInstance().getSharedPreferences().getBoolean(PaywallConstants.PW_PREF_FREE_TRIAL_SUB_VERIFIED, false);
                return PaywallService.getInstance().verifyFreeTrialSubscriptionImpl(isAlreadyVerified, forceVerify);
            }
            return null;
        }

        @Override
        protected void onPostExecute(PaywallResult paywallResult) {
            super.onPostExecute(paywallResult);
            if (paywallResult == null) return;

            if (PaywallService.getInstance() != null) {
                PaywallService.getInstance().setFreeTrialSubscriptionVerified(paywallResult);
            }
        }
    }

    public void setFreeTrialSubscriptionVerified(PaywallResult paywallResult) {
        Logger.d(TAG, "free trial sub verified ");
        SharedPreferences.Editor editor = PaywallService.getInstance().getSharedPreferences().edit();
        editor.putBoolean(PaywallConstants.PW_PREF_FREE_TRIAL_SUB_VERIFIED, paywallResult.isSuccess()).commit();
    }

    public PaywallResult verifyFreeTrialSubscriptionImpl(boolean alreadyVerified, boolean forceVerify) {
        if (PaywallService.getInstance() != null) {
            final Subscription sub = PaywallService.getInstance().connector.getFreeTrialSub();
            final boolean isPremiumLoggedInUser = PaywallService.getInstance().wapoAccessService.isPremiumUser();

            if (sub == null || isPremiumLoggedInUser) {
                return null;
            } else {
                final boolean isFreeTrialExpired = System.currentTimeMillis() > PaywallService.getInstance().connector.getFreeTrialExpiryDate();
                return (!forceVerify && (alreadyVerified || isFreeTrialExpired) ? null :
                        PaywallService.getInstance().getWapoAccessServiceInstance().verifyFreeTrialSubscription());
            }
        }
        return null;
    }

    public void initializeIdentityPreferencesWithDefaults() {
        IdentityPreferencesRecord record = WpPaywallHelper.getIdentityPreferences();
        if (record == null) {
            // No record found. Initialize with defaults.
            WpPaywallHelper.setIdentityPreferences(new IdentityPreferencesRecord(
                    IdentityPreferences.FLAG_NO,
                    IdentityPreferences.FLAG_YES,
                    IdentityPreferences.FLAG_YES,
                    null,
                    System.currentTimeMillis(),
                    IdentityPreferences.FLAG_YES));
        }
    }

    /**
     * Place were SaveIdentityPreferences exceute for OneTrust
     * Reset flags after the successful response
     */

    public void makeSaveIdentityPreferencesCallIfOneTrustConditionsAreMet() {
        if (isConnectedOrConnecting(ctx) && getLoggedInUser() != null) {
            IdentityPreferencesRecord record = WpPaywallHelper.getIdentityPreferences();
            boolean otSynchronized = record != null && IdentityPreferences.FLAG_YES.equals(record.getOtContentSynchronized());
            String localOTConsent = PaywallService.getConnector().getOneTrustConsentToken();

            if (otSynchronized || TextUtils.isEmpty(localOTConsent) ) {
                return;
            }

            if (AuthHelper.getInstance(instance.ctx).shouldRefreshAccessToken()) {
                AuthHelper.getInstance(ctx).refreshAccessToken(() -> new SaveIdentityPreferencesTaskForOneTrust().execute(record));
            } else {
                new SaveIdentityPreferencesTaskForOneTrust().execute(record);
            }
        }
    }

    private static class SaveIdentityPreferencesTaskForOneTrust extends AsyncTask<IdentityPreferencesRecord, Void, SaveIdentityPreferencesResponse> {

        @Override
        protected SaveIdentityPreferencesResponse doInBackground(IdentityPreferencesRecord... params) {
            SaveIdentityPreferencesResponse response = null;
            IdentityPreferencesRecord inputRecord = null;
            if (params != null && params.length > 0) {
                inputRecord = params[0];
            } else {
                return response;
            }
            PaywallService service = PaywallService.getInstance();
            String url = service.getOAuthConfigStub().getSaveIdentityPreferencesUrl();
            String accessToken = AuthHelper.getInstance(service.ctx).getAccessToken();
            String consentToken = getConnector().getOneTrustConsentToken();

            if (TextUtils.isEmpty(consentToken)){
                return null;
            }

            String requestBody = new Gson().toJson(
                    new SaveIdentityPreferencesRequest(new PrivacySetting(null, consentToken)));
            Logger.d(TAG, "onetrust: "+requestBody); // For QA to Verify

            response = service.getApiServiceInstance().saveIdentityPreferences(url,
                    accessToken,
                    PaywallService.getConnector().getClientId(),
                    requestBody);

            if (response != null && "SUCCESS".equals(response.getStatus())) {
                IdentityPreferencesRecord record = WpPaywallHelper.getIdentityPreferences();
                if (record != null) {
                    if (record.getSwitchTimestamp().equals(inputRecord.getSwitchTimestamp())) {
                        record.setServerResponse(response.getResponseJson());
                        record.setOtContentSynchronized(IdentityPreferences.FLAG_YES);
                        WpPaywallHelper.setIdentityPreferences(record);
                    } else if (IdentityPreferences.FLAG_YES.equals(record.getOtContentSynchronized())) {
                        // How this state came in? received last request's response already before this response?
                        // Just reset sync flag and it will be taken care in next sync cycle.
                        record.setOtContentSynchronized(IdentityPreferences.FLAG_NO);
                        WpPaywallHelper.setIdentityPreferences(record);
                    } else {
                        // ignore rest
                    }
                }
            }
            return response;
        }
    }


    public void makeSaveIdentityPreferencesCallIfConditionsAreMet() {
        if (isConnectedOrConnecting(ctx) && getLoggedInUser() != null) {
            IdentityPreferencesRecord record = WpPaywallHelper.getIdentityPreferences();
            boolean ccpaSynchronized = record != null && IdentityPreferences.FLAG_YES.equals(record.getDataSynchronized());
            if (ccpaSynchronized) {
                return;
            }
            if (AuthHelper.getInstance(instance.ctx).shouldRefreshAccessToken()) {
                AuthHelper.getInstance(ctx).refreshAccessToken(() -> new SaveIdentityPreferencesTask().execute(record));
            } else {
                new SaveIdentityPreferencesTask().execute(record);
            }
        }
    }

    private static class FetchUserProfileTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {

            String accessToken = AuthHelper.getInstance(PaywallService.getInstance().ctx).getAccessToken();
            OAuthConfigStub oAuthConfigStub = PaywallService.getInstance().getOAuthConfigStub();

            if (oAuthConfigStub != null && accessToken != null) {
                return PaywallService.getInstance().getApiServiceInstance().getUserProfile(accessToken, PaywallService.getConnector().getClientId());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean success) {

            if (success != null && success) {
                getConnector().logD(new EventLog.Builder().setMessage("LoggedInUser info retrieved from /profileAPI"));
                PaywallReactive.notifyPlacementChanged();
            } else {
                instance.paywallPrefHelper.setPrefVerifyUserLastTime(System.currentTimeMillis() - (24 * 60 * 60 * 1000));
                getConnector().logE(new EventLog.Builder().setMessage("Error getting user info from /profile API"));
            }
        }
    }

    private static class SaveIdentityPreferencesTask extends AsyncTask<IdentityPreferencesRecord, Void, SaveIdentityPreferencesResponse> {

        @Override
        protected SaveIdentityPreferencesResponse doInBackground(IdentityPreferencesRecord... params) {
            SaveIdentityPreferencesResponse response = null;
            IdentityPreferencesRecord inputRecord = null;
            if (params != null && params.length > 0) {
                inputRecord = params[0];
            } else {
                return response;
            }

            PaywallService service = PaywallService.getInstance();

            String url = service.getOAuthConfigStub().getSaveIdentityPreferencesUrl();
            String accessToken = AuthHelper.getInstance(service.ctx).getAccessToken();
            String requestBody = new Gson().toJson(
                    new SaveIdentityPreferencesRequest(new PrivacySetting(new CCPA(inputRecord.getAdsOptOut(),
                            inputRecord.getExplicitNotice()), null)));

            Logger.d(TAG, "oneTrust: Request = "+requestBody);
            response = service.getApiServiceInstance().saveIdentityPreferences(url,
                    accessToken,
                    PaywallService.getConnector().getClientId(),
                    requestBody);
            Logger.d(TAG, "oneTrust: response = "+response);

            if (response != null && "SUCCESS".equals(response.getStatus())) {
                IdentityPreferencesRecord record = WpPaywallHelper.getIdentityPreferences();
                if (record != null) {
                    if (record.getSwitchTimestamp().equals(inputRecord.getSwitchTimestamp())) {
                        record.setDataSynchronized(IdentityPreferences.FLAG_YES);
                        record.setServerResponse(response.getResponseJson());
                        record.setOtContentSynchronized(IdentityPreferences.FLAG_YES);
                        WpPaywallHelper.setIdentityPreferences(record);
                    } else if (IdentityPreferences.FLAG_YES.equals(record.getDataSynchronized())) {
                        // How this state came in? received last request's response already before this response?
                        // Just reset sync flag and it will be taken care in next sync cycle.
                        record.setDataSynchronized(IdentityPreferences.FLAG_NO);
                        WpPaywallHelper.setIdentityPreferences(record);
                    } else {
                        // ignore rest
                    }
                }
            }

            return response;
        }
    }

    public static class PaywallServiceRunnable implements Runnable {
        private Handler handler;
        private String categoryName;
        private ArticleStub article;
        private Bundle bundle;
        private final String PAYWALL_REASON = "paywall_reason";
        private final int FREE_NOT_METERED = 5;
        private final int WEBVIEW_HANDLED = 6;

        public PaywallServiceRunnable(Handler handler, String categoryName, ArticleStub article, Bundle bundle) {
            this.handler = handler;
            this.categoryName = categoryName;
            this.article = article;
            this.bundle = bundle;
        }

        @Override
        public void run() {
            boolean showPaywall = false;
            if (bundle == null || (bundle.getInt(PAYWALL_REASON, -1) != FREE_NOT_METERED && bundle.getInt(PAYWALL_REASON, -1) != WEBVIEW_HANDLED)) {
                showPaywall = PaywallService.getInstance().isAtLimit(categoryName, article);
            }

            if (showPaywall) {
                bundle = bundle != null ? bundle : new Bundle();
                boolean isLoggedInUser = PaywallService.getInstance().isWpUserLoggedIn();
                if (handler != null) {
                    Message m = handler.obtainMessage();
                    m.obj = bundle;
                    if (isLoggedInUser) {
                        m.what = RUN_PAYWALL_SERVICE_RESULT_EXPIRED;
                    } else {
                        m.what = RUN_PAYWALL_SERVICE_RESULT;
                    }
                    handler.sendMessage(m);
                }
            } else {
                if (handler != null) {
                    Message m = handler.obtainMessage();
                    m.obj = bundle;
                    m.what = RUN_PAYWALL_SERVICE_RESULT_NO_PAYWALL;
                    handler.sendMessage(m);
                }
            }
        }
    }

    public void clearSubscriptionInfo() {
        PaywallService.getConnector().setPaywallSubShortTitle(null);
        PaywallService.getConnector().setPaywallSubProduct(null);
        PaywallService.getConnector().setPaywallSource(null);
        PaywallService.getConnector().setPaywallSubSource(null);
        PaywallService.getConnector().setPaywallSubscriberType(null);
        PaywallService.getConnector().setPriceFlag(null);
        PaywallService.getConnector().setSubAcctMgmt(null);
        PaywallService.getConnector().setSubAccountAnalytics(null);
        PaywallService.getConnector().setPaywallSubCurrentRateID(null);
        PaywallService.getInstance().setVerifySubUUID(null);
        PaywallService.getInstance().setSubscriptionID(null);
    }

    public class SubscriptionInfo {

        public String getUsernameText() {
            String usernameText = "";
            if (isWpUserLoggedIn()) {
                WpUser user = getLoggedInUser();
                String signedInThrough = user.getSignedInThrough();
                String email = user.getUserId();
                String displayName = user.getDisplayName();
                if (!TextUtils.isEmpty(signedInThrough)) {
                    usernameText += getContext().getString(R.string.signed_in_with) + " " + (signedInThrough.equals("Washington Post") ? "email" : signedInThrough);
                    if (!TextUtils.isEmpty(email)) {
                        usernameText += " as " + email;
                    }
                } else {
                    if (!TextUtils.isEmpty(email)) {
                        usernameText = getContext().getString(R.string.signed_in_with) + " " + email;
                    } else {
                        if (!TextUtils.isEmpty(displayName)) {
                            usernameText = getContext().getString(R.string.signed_in_as) + " " + displayName;
                        }
                    }
                }
            }
            return usernameText;
        }

        public String getExpiryDate() {
            Date accessExpiryDate = getAccessExpiryDate(PaywallConstants.SubscriptionType.STORE) == null ? getAccessExpiryDate(PaywallConstants.SubscriptionType.WASHPOST) : getAccessExpiryDate(PaywallConstants.SubscriptionType.STORE);
            if (accessExpiryDate != null) {
                if (isPremiumUser() && (System.currentTimeMillis() > accessExpiryDate.getTime())) {
                    getConnector().logE(new EventLog.Builder()
                            .setMessage("settings expired date")
                            .set("date", accessExpiryDate.getTime()));
                }
                DateFormat df = new SimpleDateFormat("M/dd/yyyy", Locale.US);
                return df.format(accessExpiryDate);
            }
            return null;
        }

        public String getExpiryState() {
            if (isPremiumUser()) {
                if (PaywallService.getInstance().isSubscriptionSuspended()) {
                    return "Expires";
                }
                return "Next bill date";
            } else {
                if (PaywallService.getInstance().isSubscriptionTerminated()) {
                    return "Expired";
                } else if (getConnector().getFreeTrialExpiryDate() > System.currentTimeMillis()) {
                    return "Free Trial Ends";
                } else {
                    return "Not subscribed";
                }
            }
        }

        public String getExpiryString() {
            StringBuilder sb = new StringBuilder(getExpiryState());
            String expiryDateString = getExpiryDate();
            if (expiryDateString == null) {
                if (getConnector().getFreeTrialExpiryDate() != 0 && !isWpUserLoggedIn()) {
                    DateFormat df = new SimpleDateFormat("M/dd/yyyy", Locale.US);
                    sb.append(" ");
                    sb.append(df.format(getConnector().getFreeTrialExpiryDate()));
                } else {
                    sb.append(isPremiumUser() ? " bill date unknown" : "");
                }
            } else {
                sb.append(" ");
                sb.append(expiryDateString);
            }
            return sb.toString();
        }

        public String getSubscriptionType() {
            String pwType;
            String subSource = getConnector().getPaywallSubSource();
            String shortTitle = getConnector().getPrefPaywallSubShortTitle();
            if (subSource == null || shortTitle == null) {
                pwType = isPremiumUser() ? "Subscribed" : getConnector().getFreeTrialExpiryDate() != 0 ? "Free Trial" : "";
            } else {
                pwType = subSource + " . " + shortTitle;
            }
            return pwType;
        }
    }

    /**
     * Clears all locally-stored paywall state — user account, IAP, cached subscription data,
     * and metering state. Equivalent of iOS PaywallManager.clear().
     * Results in a logged-out first-run-like experience. Debug use only.
     */
    public void clearAllPaywallData() {
        // 1. Wipe user and subscription state
        logOutCurrentUser();
        clearSubscriptionInfo();
        PaywallReactive.reset();

        // 2. Reset local meter count and paywall gate flags
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putFloat(PaywallConstants.PW_CURRENT_ARTICLE_COUNT, 0f);
        editor.putBoolean(PaywallConstants.PW_SHOW, false);
        editor.putBoolean(PaywallConstants.PW_SHOW_RULE1, false);
        editor.putBoolean(PaywallConstants.PW_SHOW_RULE2, false);
        editor.apply();

        // 3. Clear the read article DB so Tetro re-evaluates fresh on next article view
        PaywallCounterHelper.cleanArticles();
    }

    public ReminderScreenConfig getReminderScreenConfig() {
        return reminderScreenConfig;
    }

    public AcquisitionReminderModel getAcquisitionReminderModel() {
        Logger.d("AcquisitionReminder", "model : " + acquisitionReminderModel.toString());
        return acquisitionReminderModel;
    }

    public OnboardingReminderModel getOnboardingReminderModel() {
        return onboardingReminderModel;
    }

    public BottomCtaModel getBottomCtaModel() {
        return bottomCtaModel;
    }

    public PaywallSheetModels getPaywallSheetModels() {
        return paywallSheetModels;
    }

    public CookiesService getCookiesService() {
        return cookiesService;
    }

    /*
        Reports the status back to the caller (whoever observes on promocodeRequestStateLiveData)
     */
    public void reportPromoCodeResultBack(SubVerification subVerification){
        if(subVerification != null && subVerification.getPromoCode() != null) {
            PromoCode promoCode = new PromoCode(subVerification.getPromoCode(),
                    subVerification.getPromoName(),
                    subVerification.getPromoStartDate(),
                    subVerification.getPromoEndDate(),
                    subVerification.getPromoCodeProductId(),
                    subVerification.getPromoTermType(),
                    subVerification.getPromoTerm(),
                    subVerification.getPromoDuration());
            promocodeRequestStateLiveData.postValue(new PromoCodeRequestState.Success(promoCode));
        }else{
            promocodeRequestStateLiveData.postValue(PromoCodeRequestState.Failure.INSTANCE);
        }
    }

    public void reportVerifyDeviceResultBack(SubVerification subVerification) {
        if (subVerification != null) {
            verifyDeviceRequestStateLiveEvent.postValue(VerifyDeviceRequestState.Success.INSTANCE);
        } else {
            verifyDeviceRequestStateLiveEvent.postValue(VerifyDeviceRequestState.Failure.INSTANCE);
        }
    }

    public void setSubscriptionSource(String subscriptionSource){
        this.subscriptionSource = subscriptionSource;
    }

    public String getSubscriptionSource(){
        return subscriptionSource;
    }

    public TetroManager getTetroManager() {
        if(tetroManager == null) {
            tetroManager = new TetroManager(TetroApiService.getInstance().getTetroNetwork(), new TetroLocalServiceImpl());
        }
        return tetroManager;
    }

    public void prepareCookieManagerForWebView() {
        CookiesService cookiesService = PaywallService.getInstance().getCookiesService();
        if (cookiesService != null && PaywallService.getInstance().isWpUserLoggedIn()) {
            cookiesService.prepareCookieManager(PaywallService.getInstance().getLoggedInUser(), ctx, PaywallService.getInstance().isSameSiteEnabled);
        }
    }

    public String getProfileIdentifier() {
        // return identifier from response/storage once b/e integration is done.
        if (getLoggedInUser() != null) {
            return getLoggedInUser().getUuid();
        }
        return "";
    }

    public String getProfileAuth() {
        // return auth from response/storage once b/e integration is done.
        if (getLoggedInUser() != null) {
            return getLoggedInUser().getConsentToken();
        }
        return "";
    }

    public void clearPreviousScreen() {
        paywallPrefHelper.setPreviousScreen(null);
    }

    /**
     * Get subscription state
     */
    public SubState getSubState() {
        boolean isValidFreeArticleUser =
                isFreeArticlesUser() && MeteringPrefs.getFreeArticlesRemaining() > 0;
        if (isValidFreeArticleUser || isFreeDaysUser() || isMobileFreeDaysUser()) {
            return SubState.FreeTrialSub.INSTANCE;
        } else if (isPremiumUser()) {
            return SubState.ActiveSub.INSTANCE;
        } else if (isSubscriptionTerminated()) {
            return SubState.TerminatedSub.INSTANCE;
        } else if (isSubscriptionPaused()) {
            return SubState.PausedSub.INSTANCE;
        } else {
            return SubState.NoSub.INSTANCE;
        }
    }

    public boolean isIapTerminated() {
        return Objects.equals(getConnector().getIapSubscriptionStatus(), PaywallConstants.TERMINATED);
    }

    private void migrateSubscriptionIdFromLegacyPrefIfNeeded() {
        if (getBillingHelper() == null) {
            Logger.d("PaywallLifecycle", "legacy pref migration skipped: billing helper unavailable");
            return;
        }

        SharedPreferences settings = PaywallService.getSharedPreferences();
        String legacySubscriptionId = settings.getString(PaywallConstants.PW_SUBSCRIPTION_ID, null);
        if (TextUtils.isEmpty(legacySubscriptionId)) {
            return;
        }

        boolean migrated = false;

        Subscription classicSubscription = getBillingHelper().cachedSubscription();
        if (classicSubscription != null) {
            if (TextUtils.isEmpty(classicSubscription.getSubscriptionId())) {
                classicSubscription.setSubscriptionId(legacySubscriptionId);
                getBillingHelper().updateClassicSubscriptionId(legacySubscriptionId);
            } else {
                Logger.d(TAG, "legacy pref migration skipped: classic already has subscription_id=" + classicSubscription.getSubscriptionId());
            }
            migrated = true;
        } else {
            Subscription rainbowSubscription = getBillingHelper().getMigratedRainbowSubscription();
            if (rainbowSubscription != null) {
                if (TextUtils.isEmpty(rainbowSubscription.getSubscriptionId())) {
                    rainbowSubscription.setSubscriptionId(legacySubscriptionId);
                    getBillingHelper().updateRainbowSubscriptionId(legacySubscriptionId);
                } else {
                    Logger.d(TAG, "legacy pref migration skipped: rainbow already has subscription_id=" + rainbowSubscription.getSubscriptionId());
                }
                migrated = true;
            } else {
                Subscription amazonClassicSubscription = getBillingHelper().getMigratedAmazonClassicSubscription();
                if (amazonClassicSubscription != null) {
                    if (TextUtils.isEmpty(amazonClassicSubscription.getSubscriptionId())) {
                        amazonClassicSubscription.setSubscriptionId(legacySubscriptionId);
                        getBillingHelper().updateAmazonClassicSubscriptionId(legacySubscriptionId);
                    } else {
                        Logger.d(TAG, "legacy pref migration skipped: amazonClassic already has subscription_id=" + amazonClassicSubscription.getSubscriptionId());
                    }
                    migrated = true;
                }
            }
        }

        if (migrated) {
            settings.edit().remove(PaywallConstants.PW_SUBSCRIPTION_ID).apply();
        } else {
            Logger.d(TAG, "legacy pref migration skipped: no subscription row available");
        }
    }

    /**
     * refreshes via /profile API (blocking call - must be called off main thread).
     * @return the ctoken or null if not available
     */
    @WorkerThread
    @Nullable
    public String getValidCToken() {
        // If not found, refresh via /profile API
        String accessToken = AuthHelper.getInstance(ctx).getAccessToken();
        String clientId = getConnector().getClientId();

        if (accessToken == null || clientId == null) {
            connector.logE(new EventLog.Builder()
                    .setMessage("PaywallService getValidCToken: accessToken or clientId is null")
                    .set("accessToken", accessToken == null ? "null" : "present")
                    .set("clientId", clientId == null ? "null" : "present"));
            return null;
        }

        boolean success = getApiServiceInstance().getUserProfile(accessToken, clientId);
        if (!success) {
            connector.logE(new EventLog.Builder()
                    .setMessage("[PaywallService] getUserProfile returned false — ctoken not refreshed")
                    .set("accessToken", "present")
                    .set("clientId", "present"));
            return null;
        }

        // getUserProfile stores the ctoken in DB, read it back
        WpUser wpUser = getLoggedInUser();
        String ctoken = wpUser != null ? wpUser.getCToken() : null;
        return TextUtils.isEmpty(ctoken) ? null : ctoken;
    }
}
