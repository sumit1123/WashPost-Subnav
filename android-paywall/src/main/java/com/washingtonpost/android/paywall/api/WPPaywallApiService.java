package com.washingtonpost.android.paywall.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.util.Base64;

import com.wapo.android.commons.util.Logger;

import com.auth0.android.jwt.JWT;
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor;
import com.wapo.android.commons.util.DateUtilsKt;
import com.wapo.android.commons.util.Utils;
import com.washingtonpost.android.config.domain.models.config.paywall.OAuthConfigStub;
import com.washingtonpost.android.paywall.PaywallConnector;
import com.wapo.android.commons.logs.EventLog;
import com.washingtonpost.android.paywall.PaywallReactive;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.auth.AuthApplication;
import com.washingtonpost.android.paywall.auth.AuthHelper;
import com.washingtonpost.android.paywall.auth.AuthStateManager;
import com.washingtonpost.android.paywall.features.casettlement.CaSettlementValues;
import com.washingtonpost.android.paywall.features.ccpa.CCPA;
import com.washingtonpost.android.paywall.features.ccpa.CCPAUtils;
import com.washingtonpost.android.paywall.features.ccpa.IdentityPreferences;
import com.washingtonpost.android.paywall.features.ccpa.IdentityPreferencesRecord;
import com.washingtonpost.android.paywall.features.ccpa.PrivacySetting;
import com.washingtonpost.android.paywall.models.PromoPurchaseType;
import com.washingtonpost.android.paywall.newdata.model.IAPSubItem;
import com.washingtonpost.android.paywall.newdata.model.IAPSubItems;
import com.washingtonpost.android.paywall.newdata.model.Subscription;
import com.google.gson.Gson;
import com.washingtonpost.android.paywall.helper.WpPaywallHelper;
import com.washingtonpost.android.paywall.newdata.model.PaywallResult;
import com.washingtonpost.android.paywall.newdata.model.WpUser;
import com.washingtonpost.android.paywall.newdata.response.LoggedInUser;
import com.washingtonpost.android.paywall.newdata.response.RevokeResponse;
import com.washingtonpost.android.paywall.newdata.response.SubItem;
import com.washingtonpost.android.paywall.features.ccpa.SaveIdentityPreferencesResponse;
import com.washingtonpost.android.paywall.newdata.response.SubVerification;
import com.washingtonpost.android.paywall.util.PaywallConstants;
import com.washingtonpost.android.paywall.util.PaywallUtil;

import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;

import static com.wapo.android.commons.constants.HeadersKt.*;
import static com.wapo.android.commons.util.Utils.removeCurrencySignFromPrice;


/**
 * Wash Post API service
 *
 * @author Bkilari
 */
public class WPPaywallApiService {

    public final static String TAG = WPPaywallApiService.class.getSimpleName();

    private String salt;

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    /**
     * Persists featureJwt from a verify response onto the cached Subscription and writes to DB.
     */
    private void persistVerifyFeatureJwt(String jwt) {
        Subscription sub = PaywallService.getBillingHelper().cachedSubscription();
        if (sub != null) {
            sub.setFeatureJwt(jwt);
            PaywallService.getBillingHelper().updateSubscriptionDetails(sub);
        } else {
            Logger.w(TAG, "persistVerifyFeatureJwt: cachedSubscription is null, cannot persist");
        }
    }

    private enum SubscriptionActionType {
        VERIFY_FREE_TRIAL,
        VERIFY_SUB,
        LINK
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public PaywallResult verifyFreeTrialSubscription() {
        PaywallResult res = new PaywallResult();
        SubVerification deviceSubVerification = null;
        PaywallResult.State state = PaywallResult.State.FAIL;
        try {
            Map<String, Object> paramsMap = getDefaultPostParamsMap();
            addStoreSubscriptionParameters(paramsMap, SubscriptionActionType.VERIFY_FREE_TRIAL);
            Map<String, String> headerMap = getDefaultPostHeaderMap();
            String url = "";
            if (PaywallService.getConnector().getStoreType().equals(PaywallConstants.AMAZON_STORE)) {
                url = PaywallConstants.WP_API_URL + PaywallConstants.WP_API_AMAZON_RAINBOW_VERIFY_DEVICE;
            } else {
                url = PaywallConstants.WP_API_URL + PaywallConstants.WP_API_VERIFY_DEVICE;
            }
            Logger.d(TAG, "verifyFreeTrialSubscription ");
            String result = postDataWithObjects(url, paramsMap, headerMap);
            if (result != null) {
                Gson gson = new Gson();
                deviceSubVerification = gson.fromJson(result, SubVerification.class);
                if (deviceSubVerification != null &&
                        (PaywallConstants.WP_API_STATUS_OK.equals(deviceSubVerification.getStatus()) ||
                                PaywallConstants.WP_API_STATUS_EXPIRED.equals(deviceSubVerification.getStatus()))) {
                    state = PaywallResult.State.SUCCESS;
                    PaywallService.getInstance().setVerifySubUUID(deviceSubVerification.getUuid());
                    SubItem baseSub = PaywallUtil.getBestAvailableBaseSubscription(
                            deviceSubVerification.getSubscriptions(),
                            deviceSubVerification.getSubscriptionID());
                    if (baseSub != null) {
                        PaywallService.getConnector().setPaywallSubCurrentRateID(baseSub.getCurrentRateId() != null ? baseSub.getCurrentRateId() : deviceSubVerification.getCurrentRateID());
                        PaywallService.getConnector().setPaywallSubProduct(baseSub.getProduct() != null ? baseSub.getProduct() : deviceSubVerification.getProduct());
                        PaywallService.getConnector().setPaywallSubscriberType(baseSub.getSourceType() != null ? baseSub.getSourceType() : deviceSubVerification.getSubscriberType());
                        PaywallService.getConnector().setPaywallSource(baseSub.getSource() != null ? baseSub.getSource() : deviceSubVerification.getSource());
                    } else {
                        // Fallback to top-level fields if no matching base subscription found
                        PaywallService.getConnector().setPaywallSubCurrentRateID(deviceSubVerification.getCurrentRateID());
                        PaywallService.getConnector().setPaywallSubProduct(deviceSubVerification.getProduct());
                        PaywallService.getConnector().setPaywallSubscriberType(deviceSubVerification.getSubscriberType());
                        PaywallService.getConnector().setPaywallSource(deviceSubVerification.getSource());
                    }
                    PaywallService.getConnector().setPriceFlag(deviceSubVerification.getPriceFlag());
                    PaywallService.getConnector().setSubAcctMgmt(deviceSubVerification.getSubAcctMgmt());
                    PaywallService.getConnector().setSubAccountAnalytics(deviceSubVerification.getSubAccountAnalytics());
                    persistVerifyFeatureJwt(deviceSubVerification.getFeatureJwt());
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "verifyFreeTrialSubscription exception", e);
        }
        if (state.equals(PaywallResult.State.FAIL)) {
            PaywallService.getConnector().setPrefDeviceProfileSent(false);
        }
        res.setState(state);
        return res;
    }

    public PaywallResult verifyDeviceSubscription(boolean requestPromoCode, boolean isPeriodicCheck) {
        SubVerification deviceSubVerification = null;
        PaywallResult res = new PaywallResult();
        PaywallResult.State state = null;
        String resMessage = "";
        Subscription existingSub = PaywallService.getBillingHelper().cachedSubscription();
        Subscription lastSub = PaywallService.getBillingHelper().getLastActiveSubscription();
        PaywallConnector connector = PaywallService.getConnector();

        try {

            Map<String, Object> paramsMap = getDefaultPostParamsMap();
            /*
                additional parameter is added if promo code is requested in the request.
             */
            if(requestPromoCode){
                paramsMap.put("promoCode", "true");
            }
            // Get subscription info
            addStoreSubscriptionParameters(paramsMap, SubscriptionActionType.VERIFY_SUB);

            Map<String, String> headerMap = getDefaultPostHeaderMap();

            String url = "";
            if (connector.getStoreType().equals(PaywallConstants.AMAZON_STORE)) {
                url = PaywallConstants.WP_API_URL + PaywallConstants.WP_API_AMAZON_RAINBOW_VERIFY_DEVICE;
            } else {
                url = PaywallConstants.WP_API_URL + PaywallConstants.WP_API_VERIFY_DEVICE;
            }

            Logger.d(TAG, "verifyDevice REQUEST url=" + url
                    + ", params=" + paramsMap
                    + ", headers=" + headerMap
                    + ", existingSub=" + existingSub
                    + ", lastSub=" + lastSub
                    + ", requestPromoCode=" + requestPromoCode
                    + ", isPeriodicCheck=" + isPeriodicCheck);

            String result = postDataWithObjects(url, paramsMap, headerMap);

            if (result != null) {
                Gson gson = new Gson();
                deviceSubVerification = gson.fromJson(result, SubVerification.class);
                SubItem baseSub = PaywallUtil.getBestAvailableBaseSubscription(
                        deviceSubVerification.getSubscriptions(),
                        deviceSubVerification.getSubscriptionID());
                String baseSubState = baseSub != null && baseSub.getSubState() != null ? baseSub.getSubState() : deviceSubVerification.getSubState();
                logVerifyDebug("verifyDevice response=" + result, baseSubState, isPeriodicCheck);
                if(existingSub != null) {
                    showCASettlementDialogIfNeeded(deviceSubVerification, existingSub, baseSub);
                }
                if (deviceSubVerification.getMessages() != null
                        && !deviceSubVerification.getMessages().isEmpty()) {
                    resMessage = deviceSubVerification.getMessages().get(0).getBody();
                    logVerifyError(deviceSubVerification.getMessages().get(0).getBody(), baseSubState, isPeriodicCheck);
                }
                Subscription subscription = PaywallService.getBillingHelper().cachedSubscription();
                boolean promoRedeemed = deviceSubVerification.isPromoCodeRedeemed();
                if(subscription != null && promoRedeemed){
                    switch (subscription.getPromoCodePurchaseType()){
                        case UNDEFINED_IN_APP:
                            PaywallService.getBillingHelper().setPromoCodePurchaseType(PromoPurchaseType.PROMO_IN_APP);
                            PaywallService.getOmniture().trackPromoCodeRedeemedEvent(PromoPurchaseType.PROMO_IN_APP);
                            break;
                        case UNDEFINED_OUT_OF_APP:
                            PaywallService.getBillingHelper().setPromoCodePurchaseType(PromoPurchaseType.PROMO_OUT_OF_APP);
                            PaywallService.getOmniture().trackPromoCodeRedeemedEvent(PromoPurchaseType.PROMO_OUT_OF_APP);
                            break;
                        default:
                    }
                }
                String subStatus = baseSub != null && baseSub.getSubStatus() != null ? baseSub.getSubStatus() : deviceSubVerification.getSubStatus();
                String subState = baseSub != null && baseSub.getSubState() != null ? baseSub.getSubState() : deviceSubVerification.getSubState();
                connector.setIapSubscriptionStatus(subStatus);

                long expirationDateMillis = DateUtilsKt.formattedDateToMillis(
                        deviceSubVerification.getExpirationDate(),
                        PaywallConstants.VERIFY_EXPIRATION_DATE_FORMAT
                );
                long baseSubExpirationDateMillis = baseSub != null && baseSub.getExpirationDate() != null ? DateUtilsKt.formattedDateToMillis(
                        baseSub.getExpirationDate(),
                        PaywallConstants.PROFILE_EXPIRATION_DATE_FORMAT
                ) : expirationDateMillis;
                PaywallService.getPaywallPrefHelper().setPrefLastSubExpirationDate(baseSubExpirationDateMillis);
                PaywallService.getConnector().setPaywallSubShortTitle(baseSub != null && baseSub.getShortTitle() != null ? baseSub.getShortTitle() : deviceSubVerification.getShortTitle());

                if (PaywallConstants.PAUSE_SCHEDULED.equals(subState)) {
                    connector.setPauseTime(expirationDateMillis);
                } else if ( PaywallConstants.PAUSED.equals(subStatus)) {
                    connector.setAutoResumeTime(expirationDateMillis);
                }

                persistVerifyFeatureJwt(deviceSubVerification.getFeatureJwt());

                // Persist add-on subscriptions (non-base items) on the cached subscription
                PaywallUtil.persistAddonSubscriptions(deviceSubVerification.getSubscriptions(),
                        deviceSubVerification.getSubscriptionID());

            } else {
                logVerifyDebug("verifyDevice response=null", "N/A", isPeriodicCheck);
            }
        } catch (Exception e) {
            if (deviceSubVerification != null) {
                SubItem errBaseSub = PaywallUtil.getBestAvailableBaseSubscription(
                        deviceSubVerification.getSubscriptions(),
                        deviceSubVerification.getSubscriptionID());
                String errSubState = errBaseSub != null && errBaseSub.getSubState() != null ? errBaseSub.getSubState() : deviceSubVerification.getSubState();
                logVerifyError(e.getMessage(), errSubState, isPeriodicCheck);
            } else {
                logVerifyDebug(e.getMessage(), "N/A", isPeriodicCheck);
            }
            state = PaywallResult.State.ERROR;
        }

        if (state == null) {
            state = PaywallResult.State.FAIL;
            if (existingSub != null) {
                state = getVerifyDeviceState(deviceSubVerification, existingSub);
            }
            if (deviceSubVerification!=null) {
                SubItem stateBaseSub = PaywallUtil.getBestAvailableBaseSubscription(
                        deviceSubVerification.getSubscriptions(),
                        deviceSubVerification.getSubscriptionID());
                String lastSubState = stateBaseSub != null && stateBaseSub.getSubState() != null ? stateBaseSub.getSubState() : deviceSubVerification.getSubState();
                PaywallService.getPaywallPrefHelper().setPrefLastSubState(lastSubState);
            }
        }

        if ((PaywallResult.State.FAIL.equals(state) || PaywallResult.State.ERROR.equals(state))
                && deviceSubVerification != null) {
            SubItem cancelBaseSub = PaywallUtil.getBestAvailableBaseSubscription(
                    deviceSubVerification.getSubscriptions(),
                    deviceSubVerification.getSubscriptionID());
            String cancelSubState = cancelBaseSub != null && cancelBaseSub.getSubState() != null ? cancelBaseSub.getSubState() : deviceSubVerification.getSubState();
            if (PaywallConstants.SUB_STATE_CANCELLED.equalsIgnoreCase(cancelSubState)) {
                WpUser cachedUser = WpPaywallHelper.getLoggedInUser();
                if (cachedUser != null && cachedUser.getSubscriptions() != null) {
                    PaywallUtil.updateAdFreeStatusFromSubscriptions(cachedUser.getSubscriptions(), PaywallUtil.AdFreeSource.PROFILE);
                }
            }
        }

        if (state.equals(PaywallResult.State.ERROR)) {
            PaywallService.getConnector().setPrefDeviceProfileSent(false);
        }
        res.setMessage(resMessage);
        res.setState(state);
        /*
            if promo code was requested report the status back to paywall service so it can notify it's callers.
         */
        if (requestPromoCode) {
            PaywallService.getInstance().reportPromoCodeResultBack(deviceSubVerification);
        }
        PaywallService.getInstance().reportVerifyDeviceResultBack(deviceSubVerification);

        /*
            Every single time the device subscription status is updated, update with airship.
         */
        PaywallService.getConnector().updateAirshipUserStatus();
        return res;
    }

    private void logVerifyError(String errorMessage, String subState, boolean isPeriodicCheck) {
        PaywallService.getConnector().logE(
                new EventLog.Builder()
                        .setMessage(String.format("verifyDevice error, subState: %s, isPeriodicCheck: %b", subState, isPeriodicCheck))
                        .setErrorMessage(errorMessage)
        );
    }

    private void logVerifyDebug(String message, String subState, boolean isPeriodicCheck) {
        PaywallService.getConnector().logD(
                new EventLog.Builder()
                        .setMessage(String.format("%s, subState: %s, isPeriodicCheck: %b", message, subState, isPeriodicCheck)));
    }

    public PaywallResult verifyRainbowSub() {
        SubVerification deviceSubVerification = null;
        PaywallResult res = new PaywallResult();
        PaywallResult.State state = null;
        String resMessage = "";
        Subscription existingSub = PaywallService.getBillingHelper().getMigratedRainbowSubscription();
        try {

            Map<String, Object> paramsMap = getDefaultPostParamsMap();
            // Get subscription info
            addStoreSubscriptionParameters(paramsMap, SubscriptionActionType.VERIFY_SUB);

            Map<String, String> headerMap = getDefaultPostHeaderMap();

            String url = "";
            if (PaywallService.getConnector().getStoreType().equals(PaywallConstants.AMAZON_STORE)) {
                url = PaywallConstants.WP_API_URL + PaywallConstants.WP_API_AMAZON_RAINBOW_VERIFY_DEVICE;
            } else {
                url = PaywallConstants.WP_API_URL + PaywallConstants.WP_API_VERIFY_DEVICE;
            }

            String result = postDataWithObjects(url, paramsMap, headerMap);

            if (result != null) {
                PaywallService.getConnector().logD(new EventLog.Builder().setMessage("verifyDevice (Rainbow sub) response=" + result));
                Gson gson = new Gson();
                deviceSubVerification = gson.fromJson(result, SubVerification.class);
                SubItem rainbowBaseSub = PaywallUtil.getBestAvailableBaseSubscription(
                        deviceSubVerification.getSubscriptions(),
                        deviceSubVerification.getSubscriptionID());
                if(existingSub != null) {
                    showCASettlementDialogIfNeeded(deviceSubVerification, existingSub, rainbowBaseSub);
                }
                if (deviceSubVerification.getMessages() != null
                        && !deviceSubVerification.getMessages().isEmpty()) {
                    resMessage = deviceSubVerification.getMessages().get(0).getBody();
                    PaywallService.getConnector().logE(new EventLog.Builder()
                            .setMessage("verifyDevice (Rainbow sub) Error")
                            .setErrorMessage(deviceSubVerification.getMessages().get(0).getBody()));
                }
                persistVerifyFeatureJwt(deviceSubVerification.getFeatureJwt());
            }

        } catch (Exception e) {
            state = PaywallResult.State.ERROR;
        }
        if (state == null) {
            state = PaywallResult.State.FAIL;
            if(existingSub != null) {
                state = getRainbowVerifyDeviceState(deviceSubVerification, existingSub);
            }
            if(deviceSubVerification!=null) {
                SubItem rbStateSub = PaywallUtil.getBestAvailableBaseSubscription(
                        deviceSubVerification.getSubscriptions(),
                        deviceSubVerification.getSubscriptionID());
                String rbSubState = rbStateSub != null ? rbStateSub.getSubState() : deviceSubVerification.getSubState();
                PaywallService.getPaywallPrefHelper().setPrefLastSubState(rbSubState);
            }
        }
        if (state.equals(PaywallResult.State.ERROR)) {
            PaywallService.getConnector().setPrefDeviceProfileSent(false);
        }
        res.setMessage(resMessage);
        res.setState(state);
        /*
            Every single time the device subscription status is updated, update with airship.
         */
        PaywallService.getConnector().updateAirshipUserStatus();
        return res;
    }

    public PaywallResult verifyAmazonClassicSub() {
        SubVerification deviceSubVerification = null;
        PaywallResult res = new PaywallResult();
        PaywallResult.State state = null;
        String resMessage = "";
        Subscription existingSub = PaywallService.getBillingHelper().getMigratedAmazonClassicSubscription();
        try {

            Map<String, Object> paramsMap = getDefaultPostParamsMap();
            // Get subscription info
            addStoreSubscriptionParameters(paramsMap, SubscriptionActionType.VERIFY_SUB, true);

            Map<String, String> headerMap = getDefaultPostHeaderMap();

            String url = PaywallConstants.WP_API_URL + PaywallConstants.WP_API_AMAZON_RAINBOW_VERIFY_DEVICE;
            String result = postDataWithObjects(url, paramsMap, headerMap);

            if (result != null) {
                PaywallService.getConnector().logD(new EventLog.Builder().setMessage("verifyDevice (Amazon Classic sub) response=" + result));
                Gson gson = new Gson();
                deviceSubVerification = gson.fromJson(result, SubVerification.class);
                SubItem amazonBaseSub = PaywallUtil.getBestAvailableBaseSubscription(
                        deviceSubVerification.getSubscriptions(),
                        deviceSubVerification.getSubscriptionID());
                if(existingSub != null) {
                    showCASettlementDialogIfNeeded(deviceSubVerification, existingSub, amazonBaseSub);
                }
                if (deviceSubVerification.getMessages() != null
                        && !deviceSubVerification.getMessages().isEmpty()) {
                    resMessage = deviceSubVerification.getMessages().get(0).getBody();
                    PaywallService.getConnector().logE(new EventLog.Builder()
                            .setMessage("verifyDevice (Amazon Classic sub) Error")
                            .setErrorMessage(deviceSubVerification.getMessages().get(0).getBody()));
                }
                persistVerifyFeatureJwt(deviceSubVerification.getFeatureJwt());
            }

        } catch (Exception e) {
            state = PaywallResult.State.ERROR;
        }
        if (state == null) {
            state = PaywallResult.State.FAIL;
            if(existingSub != null) {
                state = getAmazonClassicVerifyState(deviceSubVerification, existingSub);
            }
            if(deviceSubVerification!=null) {
                SubItem amzStateSub = PaywallUtil.getBestAvailableBaseSubscription(
                        deviceSubVerification.getSubscriptions(),
                        deviceSubVerification.getSubscriptionID());
                String amzSubState = amzStateSub != null ? amzStateSub.getSubState() : deviceSubVerification.getSubState();
                PaywallService.getPaywallPrefHelper().setPrefLastSubState(amzSubState);
            }
        }
        if (state.equals(PaywallResult.State.ERROR)) {
            PaywallService.getConnector().setPrefDeviceProfileSent(false);
        }
        res.setMessage(resMessage);
        res.setState(state);
        /*
            Every single time the device subscription status is updated, update with airship.
         */
        PaywallService.getConnector().updateAirshipUserStatus();
        return res;
    }

    private void showCASettlementDialogIfNeeded(SubVerification deviceSubVerification, @NonNull Subscription existingSub, SubItem baseSub) {
        if(shouldShowCASettlementDialog(deviceSubVerification, baseSub)) {
            IAPSubItems iapSubItems = PaywallService.getConnector().getIAPSubItems();
            String price;
            if(iapSubItems.getItem(existingSub.getStoreProductId()) != null) {
                price = Objects.requireNonNull(iapSubItems.getItem(existingSub.getStoreProductId())).getBasePrice();
            }else{
                price = IAPSubItems.getFallBackPrice(existingSub.getStoreProductId(), PaywallService.getInstance().getContext());
            }
            String expirationDate = baseSub != null && baseSub.getExpirationDate() != null ? baseSub.getExpirationDate() : deviceSubVerification.getExpirationDate();
            String product = baseSub != null && baseSub.getProduct() != null ? baseSub.getProduct() : deviceSubVerification.getProduct();
            CaSettlementValues caSettlementValues = new CaSettlementValues(expirationDate, price, product);
            PaywallService.getConnector().setCaSettlementValues(caSettlementValues);
        }
    }

    private Boolean shouldShowCASettlementDialog(SubVerification verification, SubItem baseSub){
        try {
            Map<String, String> attributes = verification.getAttributes();
            String rateDuration = baseSub != null && baseSub.getRateDuration() != null ? baseSub.getRateDuration() : verification.getRateDuration();
            if (attributes != null && rateDuration != null && Integer.parseInt(rateDuration) > 360 && attributes.containsKey("l_ca") && attributes.get("l_ca").equals("1")) {
            /*
                For playstore only
             */
                if (PaywallService.getConnector().getStoreType().equals(PaywallConstants.GOOGLE_STORE)) {
                    return PaywallService.getConnector().getIapSubscriptionStatus().equals(PaywallConstants.ACTIVE);
                }
            /*
                This is for Amazon only
             */
                return true;
            }
        } catch (Exception ex) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("CA settlement validation failed")
                    .setErrorMessage(ex.getMessage()));
        }
        return false;
    }

    public TokenResponse authWithOneLinkToken(String oneLinkToken, String clientId) {
        Logger.d(TAG, "identity /one-link/token/ call");

        TokenResponse tokenResponse = null;

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            String postBody = new JSONObject().put(PaywallConstants.CLIENT_ID_HEADER_PARAM, clientId).
                    put(PaywallConstants.ONE_LINK_TOKEN_PARAM, oneLinkToken).toString();
            addDeviceIdentifiers(headers);

            String result = postDataString(PaywallConstants.AUTH_ONE_LINK_TOKEN_API, postBody, headers);

            if (result != null) {
                tokenResponse = new TokenResponse.Builder(getFakeTokenRequest()).fromResponseJsonString(result).build();
            }


        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("/one-link/token/ failed")
                    .setErrorMessage(e.getMessage()));
            return null;
        }

        if (tokenResponse == null) {
            PaywallService.getConnector().logE(new EventLog.Builder().setMessage("/one-link/token/ failed with a null token response"));
        }

        return tokenResponse;
    }

    public TokenResponse migrateUserToAuth(String loginId, String publicKey, String clientId) {
        return migrateUserToAuth(loginId, publicKey, clientId, PaywallConstants.AUTH_MIGRATE_API);
    }

    public TokenResponse migrateUserToAuth(String loginId, String publicKey, String clientId, String url) {
        Logger.d(TAG, "identity /migrate call");

        TokenResponse tokenResponse = null;

        try {
            Map<String, String> params = new HashMap<>();
            Map<String, String> headers = new HashMap<>();

            params.put(PaywallConstants.LOGIN_ID_PARAM, loginId);
            params.put(PaywallConstants.SECURE_LOGIN_ID_PARAM, encryptText(loginId, publicKey));
            params.put(PaywallConstants.CLIENT_ID_PARAM, clientId);
            params.put(PaywallConstants.GRANT_TYPE_PARAM, PaywallConstants.LEGACY_LOGIN);
            addDeviceIdentifiers(headers);

            String result = postData(url, params, headers);

            if (result != null) {
                tokenResponse = new TokenResponse.Builder(getFakeTokenRequest()).fromResponseJsonString(result).build();
            }


        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("/migrate failed")
                    .setErrorMessage(e.getMessage()));
            return null;
        }

        if (tokenResponse == null) {
            PaywallService.getConnector().logE(new EventLog.Builder().setMessage("/migrate failed with a null token response"));
        }

        return tokenResponse;

    }

    public TokenResponse migrateLoggedInUser(String tokenResponseJson) {
        OAuthConfigStub oAuthConfigStub = PaywallService.getInstance().getOAuthConfigStub();

        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
                Uri.parse(oAuthConfigStub.getAuthorizationUrl(null, null, false, PaywallUtil.INSTANCE.getPaywallServiceConfigExtra())),
                Uri.parse(oAuthConfigStub.getTokenUrl())
        );

        TokenRequest.Builder builder = new TokenRequest.Builder(config, PaywallService.getConnector().getClientId());
        builder.setGrantType("authorization_code");
        builder.setRedirectUri(Uri.parse(((AuthApplication) PaywallService.getInstance().getContext()).getAppRedirectScheme()));
        builder.setAuthorizationCode("vabeAK55rcyGmigeHrtoIhlwiyyiTldG");

        try {
            return new TokenResponse.Builder(builder.build()).fromResponseJsonString(tokenResponseJson).build();
        } catch (JSONException e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("migrate logged in user failed")
                    .setErrorMessage(e.getMessage()));
        }

        return null;

    }

    public boolean revokeUser(String token, String clientId, String clientSecret, String tokenType) {
        Logger.d(TAG, "identity /revoke call");
        RevokeResponse revokeResponse = null;

        try {
            Map<String, String> params = new HashMap<>();
            Map<String, String> headers = new HashMap<>();

            params.put(PaywallConstants.TOKEN_PARAM, token);
            params.put(PaywallConstants.CLIENT_ID_PARAM, clientId);
            params.put(PaywallConstants.CLIENT_SECRET_PARAM, clientSecret);
            params.put(PaywallConstants.TOKEN_TYPE_HINT, tokenType);
            addDeviceIdentifiers(headers);

            String result = postData(PaywallConstants.AUTH_REVOKE_API, params, headers);

            if (result != null) {
                Gson gson = new Gson();
                revokeResponse = gson.fromJson(result, RevokeResponse.class);
            }

        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("revoke exception")
                    .setErrorMessage(e.getMessage()));
            return false;
        }

        if (revokeResponse != null) {
            if (revokeResponse.getReqId() != null) {
                return true;
            } else if (revokeResponse.getError() != null || revokeResponse.getErrorDescription() != null) {
                PaywallService.getConnector().logW(new EventLog.Builder()
                        .setMessage("/revoke Error")
                        .setErrorMessage(revokeResponse.getError() + ", " + revokeResponse.getErrorDescription()));
                return false;
            }
        }
        return false;
    }


    public boolean getUserProfile(String accessToken, String clientId) {
        Logger.d(TAG, "identity /profile call");
        LoggedInUser loggedInUser = null;
        final boolean startSubscriptionStatus = PaywallService.getInstance().isPremiumUser();
        final boolean startAdFreeStatus = PaywallService.getInstance().shouldEnableAdfreeExperience();
        try {
            Map<String, String> params = new HashMap<>();
            Map<String, String> headers = new HashMap<>();
            headers.put(PaywallConstants.AUTHORIZATION_HEADER, PaywallConstants.BEARER_PREFIX + " " + accessToken);
            addDeviceIdentifiers(headers);
            params.put(PaywallConstants.CLIENT_ID_PARAM, clientId);
            String result = postData(PaywallConstants.AUTH_PROFILE_API, params, headers);

            if (result != null) {
                Logger.d(TAG, "/profile response: " + result);
                Gson gson = new Gson();
                loggedInUser = gson.fromJson(result, LoggedInUser.class);
                if (loggedInUser.getError() != null && loggedInUser.getErrorDescription() != null) {
                    PaywallService.getConnector().logW(new EventLog.Builder()
                            .setMessage("/profile Error")
                            .setErrorMessage(loggedInUser.getError() + ", " + loggedInUser.getErrorDescription()));
                    return false;
                }

                if (loggedInUser.getSubscriptions() != null) {
                    PaywallUtil.updateAdFreeStatusFromSubscriptions(loggedInUser.getSubscriptions(), PaywallUtil.AdFreeSource.PROFILE);
                } else {
                    WpUser cachedUser = WpPaywallHelper.getLoggedInUser();
                    if (cachedUser != null && cachedUser.getSubscriptions() != null) {
                        PaywallUtil.updateAdFreeStatusFromSubscriptions(cachedUser.getSubscriptions(), PaywallUtil.AdFreeSource.PROFILE);
                    }
                }

                SubItem profileBaseSub = PaywallUtil.getBestAvailableBaseSubscription(
                        loggedInUser.getSubscriptions(),
                        loggedInUser.getSubscriptionID());
                String subState = profileBaseSub != null && profileBaseSub.getSubState() != null ? profileBaseSub.getSubState() : loggedInUser.getSubState();
                PaywallService.getPaywallPrefHelper().setPrefLastSubState(subState);
                if (subState != null) {
                    if (PaywallConstants.PAUSE_SCHEDULED.equals(subState)) {
                        String expDate = profileBaseSub != null && profileBaseSub.getExpirationDate() != null ? profileBaseSub.getExpirationDate() : loggedInUser.getExpirationDate();
                        long expirationDateMillis = DateUtilsKt.formattedDateToMillis(
                                expDate,
                                PaywallConstants.PROFILE_EXPIRATION_DATE_FORMAT
                        );
                        PaywallService.getConnector().setPauseTime(expirationDateMillis);
                    }
                }

                String profileSubStatus = profileBaseSub != null && profileBaseSub.getSubStatus() != null ? profileBaseSub.getSubStatus() : loggedInUser.getSubStatus();
                if (PaywallConstants.PAUSED.equals(profileSubStatus)) {
                    String expDate = profileBaseSub != null && profileBaseSub.getExpirationDate() != null ? profileBaseSub.getExpirationDate() : loggedInUser.getExpirationDate();
                    long expirationDateMillis = DateUtilsKt.formattedDateToMillis(
                            expDate,
                            PaywallConstants.PROFILE_EXPIRATION_DATE_FORMAT
                    );
                    PaywallService.getConnector().setAutoResumeTime(expirationDateMillis);
                }
            }
        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("/profile exception")
                    .setErrorMessage(e.getMessage()));
            return false;
        }

        if (loggedInUser == null) {
            PaywallService.getConnector().logE(new EventLog.Builder().setMessage("no response from /profile"));
            return false;
        } else {
            return storeLoggedInUser(loggedInUser, startSubscriptionStatus, startAdFreeStatus);
        }
    }

    public SaveIdentityPreferencesResponse saveIdentityPreferences(String url, String accessToken, String clientId, String postBody) {
        Logger.d(TAG, "Identity /save-identity-preferences call");

        // Headers
        Map<String, String> headers = new HashMap<>();
        headers.put(PaywallConstants.AUTHORIZATION_HEADER, PaywallConstants.BEARER_PREFIX + " " + accessToken);
        headers.put(PaywallConstants.CLIENT_ID_HEADER_PARAM, clientId);
        headers.put("Content-Type", "application/json");
        addDeviceIdentifiers(headers);

        // Send Request
        String result = postDataString(url, postBody, headers);

        // Handle Response
        SaveIdentityPreferencesResponse response = null;
        if (!TextUtils.isEmpty(result)) {
            Logger.d(TAG, "Identity /save-identity-preferences response: " + result);
            Gson gson = new Gson();
            response = gson.fromJson(result, SaveIdentityPreferencesResponse.class);
            if (response != null) {
                response.setResponseJson(result);
            }
        } else {
            Logger.d(TAG, "Identity /save-identity-preferences  response is null");
        }
        return response;
    }

    public boolean processUserFromJWTClaim(String idToken, Context context) {
        JWT jwt = AuthHelper.getInstance(context).getDecodedJWT(idToken);
        return processUserFromJWTClaim(jwt);
    }

    public boolean processUserFromJWTClaim(JWT jwt) {
        if (jwt == null || jwt.getClaim("subdata").asString() == null || jwt.getClaim("subdata").asString().isEmpty()) {
            String errorMessage = null;
            if (jwt == null) {
                errorMessage = "JWT is null";
            } else if (jwt.getClaim("subdata").asString() == null) {
                errorMessage = "subdata payload is null";
            } else if (jwt.getClaim("subdata").asString().isEmpty()) {
                errorMessage = "subdata payload is empty";
            }
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Error parsing JWT")
                    .setErrorMessage(errorMessage));
            return false;
        }

        Gson gson = new Gson();
        LoggedInUser loggedInUser = gson.fromJson(jwt.getClaim("subdata").asString(), LoggedInUser.class);

        if (loggedInUser == null) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Error deserializing user profile from JWT")
                    .set("data", jwt.getClaim("subdata").asString()));
            return false;
        } else {
            if (loggedInUser.getSubscriptions() != null) {
                PaywallUtil.updateAdFreeStatusFromSubscriptions(loggedInUser.getSubscriptions(), PaywallUtil.AdFreeSource.PROFILE);
            }

            return storeLoggedInUser(loggedInUser, PaywallService.getInstance().isPremiumUser(), PaywallService.getInstance().shouldEnableAdfreeExperience());
        }
    }

    private boolean storeLoggedInUser(@NonNull LoggedInUser loggedInUser, boolean wasPremium, boolean wasAdFree) {

        String subData;

        Logger.d(TAG, loggedInUser.toString());

        if (loggedInUser.getSubData() == null ||
                (!loggedInUser.getSubData().equals(PaywallConstants.SUBDATA_FAILED) && !loggedInUser.getSubData().equals(PaywallConstants.SUBDATA_OK))) {
            PaywallService.getConnector().logW(new EventLog.Builder().setMessage("/profile failed to fetch sub info, retrying later"));
            //this is the only case when user data is not stored
            return false;
        } else {
            subData = loggedInUser.getSubData();
        }


        // Set the last verify user call to today to prevent an unnecessary /profile call
        PaywallService.getPaywallPrefHelper().setPrefVerifyUserLastTime(System.currentTimeMillis());

        WpUser oldUser = PaywallService.getInstance().getLoggedInUser();
        SubItem profileBaseSub = PaywallUtil.getBestAvailableBaseSubscription(
                loggedInUser.getSubscriptions(),
                loggedInUser.getSubscriptionID());
        if (PaywallConstants.SUBDATA_OK.equals(subData)) {
            String currentRateId = profileBaseSub != null && profileBaseSub.getCurrentRateId() != null ? profileBaseSub.getCurrentRateId() : loggedInUser.getCurrentRateID();
            if (currentRateId != null) {
                PaywallService.getConnector().setPaywallSubCurrentRateID(currentRateId);
            }

            if (oldUser == null) {
                WpPaywallHelper.setPaywallUser(loggedInUser);
            } else {
                WpPaywallHelper.resetPaywallUserAccess(
                        loggedInUser.getLoginId(),
                        loggedInUser.getSecureLoginID(),
                        profileBaseSub != null && profileBaseSub.getProduct() != null ? profileBaseSub.getProduct() : loggedInUser.getProduct(),
                        profileBaseSub != null && profileBaseSub.getExpirationDate() != null ? profileBaseSub.getExpirationDate() : loggedInUser.getExpirationDate(),
                        loggedInUser.getCcexpired(),
                        profileBaseSub != null && profileBaseSub.getSource() != null ? profileBaseSub.getSource() : loggedInUser.getSource(),
                        profileBaseSub != null && profileBaseSub.getSubSource() != null ? profileBaseSub.getSubSource() : loggedInUser.getSubSource(),
                        profileBaseSub != null && profileBaseSub.getShortTitle() != null ? profileBaseSub.getShortTitle() : loggedInUser.getShortTitle(),
                        profileBaseSub != null && profileBaseSub.getSubStatus() != null ? profileBaseSub.getSubStatus() : loggedInUser.getSubStatus(),
                        profileBaseSub != null && profileBaseSub.getSourceType() != null ? profileBaseSub.getSourceType() : loggedInUser.getSourceType(),
                        profileBaseSub != null && profileBaseSub.getCurrentRateId() != null ? profileBaseSub.getCurrentRateId() : loggedInUser.getCurrentRateID(),
                        profileBaseSub != null && profileBaseSub.getSubDuration() != null ? profileBaseSub.getSubDuration() : loggedInUser.getSubDuration(),
                        loggedInUser.getSubAttributes(),
                        loggedInUser.getSubAcctMgmt(),
                        loggedInUser.getSubAccountAnalytics(),
                        loggedInUser.getPriceFlag(),
                        loggedInUser.getEmail(), loggedInUser.getDisplayName(), loggedInUser.getFirstName(), loggedInUser.getProfilePhotoUrl(), loggedInUser.getLoginProvider(),
                        profileBaseSub != null && profileBaseSub.getSku() != null ? profileBaseSub.getSku() : loggedInUser.getProductId(),
                        profileBaseSub != null && profileBaseSub.getIsProductRenewable() != null ? profileBaseSub.getIsProductRenewable() : loggedInUser.getIsProductRenewable(),
                        loggedInUser.getSubscriptions(), loggedInUser.getCToken(),
                        loggedInUser.getFeatureJwt()
                );
                boolean isPremium = PaywallService.getInstance().isPremiumUser();
                boolean isAdFree = PaywallService.getInstance().shouldEnableAdfreeExperience();
                if (wasPremium != isPremium) {
                    PaywallService.getConnector().onSubscriptionStatusChanged(isPremium);
                }
                if (wasAdFree != isAdFree ) {
                    PaywallService.getConnector().onSubscriptionItemChanged(isAdFree);
                }
                PaywallService.getConnector().logW(new EventLog.Builder()
                        .setMessage("User subData")
                        .set("sub_data", subData)
                        .set("is_premium", isPremium)
                        .set("sub_status", loggedInUser.getSubStatus())
                        .set("last_sub_status", oldUser.getSubStatus()));
            }
        } else {
            //Either user has no valid subscription on their account or their subscription was terminated, but they can still have an active device subscription which is not linked to this account
            if (oldUser == null) {
                WpPaywallHelper.setPaywallUser(loggedInUser);
            } else {
                PaywallService.getConnector().logE(new EventLog.Builder()
                        .setMessage("User subData FAILED")
                        .set("sub_data", subData)
                        .set("was_premium", wasPremium)
                        .set("sub_status", loggedInUser.getSubStatus())
                        .set("last_sub_status", oldUser.getSubStatus()));
                WpPaywallHelper.resetPaywallUserAccess(
                        loggedInUser.getLoginId(),
                        loggedInUser.getSecureLoginID(),
                        PaywallConstants.WP_PRODUCT_NO,
                        "",
                        loggedInUser.getCcexpired(),
                        profileBaseSub != null && profileBaseSub.getSource() != null ? profileBaseSub.getSource() : loggedInUser.getSource(),
                        profileBaseSub != null && profileBaseSub.getSubSource() != null ? profileBaseSub.getSubSource() : loggedInUser.getSubSource(),
                        profileBaseSub != null && profileBaseSub.getShortTitle() != null ? profileBaseSub.getShortTitle() : loggedInUser.getShortTitle(),
                        profileBaseSub != null && profileBaseSub.getSubStatus() != null ? profileBaseSub.getSubStatus() : loggedInUser.getSubStatus(),
                        profileBaseSub != null && profileBaseSub.getSourceType() != null ? profileBaseSub.getSourceType() : loggedInUser.getSourceType(),
                        profileBaseSub != null && profileBaseSub.getCurrentRateId() != null ? profileBaseSub.getCurrentRateId() : loggedInUser.getCurrentRateID(),
                        profileBaseSub != null && profileBaseSub.getSubDuration() != null ? profileBaseSub.getSubDuration() : loggedInUser.getSubDuration(),
                        loggedInUser.getSubAttributes(),
                        loggedInUser.getSubAcctMgmt(),
                        loggedInUser.getSubAccountAnalytics(),
                        loggedInUser.getPriceFlag(),
                        loggedInUser.getEmail(), loggedInUser.getDisplayName(), loggedInUser.getFirstName(), loggedInUser.getProfilePhotoUrl(), loggedInUser.getLoginProvider(),
                        profileBaseSub != null && profileBaseSub.getSku() != null ? profileBaseSub.getSku() : loggedInUser.getProductId(),
                        profileBaseSub != null && profileBaseSub.getIsProductRenewable() != null ? profileBaseSub.getIsProductRenewable() : loggedInUser.getIsProductRenewable(),
                        loggedInUser.getSubscriptions(), loggedInUser.getCToken(),
                        loggedInUser.getFeatureJwt()
                );
            }
        }

        storePrivacySettings(loggedInUser);
        PaywallService.getInstance().prepareCookieManagerForWebView();
        PaywallReactive.notifyPlacementChanged();

        //user data is stored in all cases except when there is unrecognized subData
        return true;
    }

    private void storePrivacySettings(@NonNull LoggedInUser loggedInUser) {
        PrivacySetting privacySetting = loggedInUser.getPrivacySetting();
        if (privacySetting != null) {
            CCPA ccpa = privacySetting.getCcpa();
            IdentityPreferencesRecord record = WpPaywallHelper.getIdentityPreferences();
            if (ccpa != null) {
                String ccpaOptOut = ccpa.getOptOut();
                if (record != null && ccpaOptOut != null) {
                    if (IdentityPreferences.FLAG_NO.equals(record.getDataSynchronized())) {
                        // Sync is pending.
                        if (ccpaOptOut.equals(record.getAdsOptOut())) {
                            // both flags are same. no need to update Identity. Just reset sync flag.
                            record.setDataSynchronized(IdentityPreferences.FLAG_YES);
                            WpPaywallHelper.setIdentityPreferences(record);
                        } else {
                            // flags are not same. check which one has 'Y' flag, and then update the other one with 'Y'.
                            if (IdentityPreferences.FLAG_YES.equals(ccpaOptOut)) {
                                // Update record
                                record.setAdsOptOut(ccpaOptOut);
                                record.setExplicitNotice(ccpa.getExplicitNotice());
                                record.setDataSynchronized(IdentityPreferences.FLAG_YES);
                                WpPaywallHelper.setIdentityPreferences(record);
                                CCPAUtils.setHasUserOptedOutCCPAAdsTracking(PaywallService.getInstance().getContext(),
                                        IdentityPreferences.FLAG_YES.equals(ccpaOptOut));
                            } else if (IdentityPreferences.FLAG_YES.equals(record.getAdsOptOut())) {
                                // Update Identity
                                record.setSwitchTimestamp(System.currentTimeMillis());
                                WpPaywallHelper.setIdentityPreferences(record);
                                PaywallService.getInstance().makeSaveIdentityPreferencesCallIfConditionsAreMet();
                            }
                        }
                    } else {
                        // Accept whatever Profile has. Probably flag was updated in other platforms.
                        if (!ccpaOptOut.equals(record.getAdsOptOut())) {
                            record.setAdsOptOut(ccpaOptOut);
                            record.setExplicitNotice(ccpa.getExplicitNotice());
                            WpPaywallHelper.setIdentityPreferences(record);
                            CCPAUtils.setHasUserOptedOutCCPAAdsTracking(PaywallService.getInstance().getContext(),
                                    IdentityPreferences.FLAG_YES.equals(ccpaOptOut));
                        }
                    }
                }
            }
            //OneTrust check
            if (record != null) {
                String remoteOTConsent = privacySetting.getOneTrustConsent();
                String localOTConsent = PaywallService.getConnector().getOneTrustConsentToken();
                //If the Flag is No tells us the OTSynch state
                if (IdentityPreferences.FLAG_NO.equals(record.getOtContentSynchronized())) {
                    // Synch Pending
                    if (remoteOTConsent != null && remoteOTConsent.equals(localOTConsent)) {
                        record.setOtContentSynchronized(IdentityPreferences.FLAG_YES);
                        WpPaywallHelper.setIdentityPreferences(record);
                    } else {
                        // Diff in  remoteConsent and localConsent
                        record.setSwitchTimestamp(System.currentTimeMillis());
                        WpPaywallHelper.setIdentityPreferences(record);
                        if(!TextUtils.isEmpty(localOTConsent)) {
                            PaywallService.getInstance().makeSaveIdentityPreferencesCallIfOneTrustConditionsAreMet();
                        }
                    }
                }
            }
        }
    }

    public PaywallResult.State getVerifyDeviceState(SubVerification deviceSubVerification, Subscription existingSub) {
        PaywallResult.State state;
        if (deviceSubVerification != null) {
            PaywallService.getConnector().setSubscriptionLinkResult(deviceSubVerification.getLink());
            SubItem baseSub = PaywallUtil.getBestAvailableBaseSubscription(
                    deviceSubVerification.getSubscriptions(),
                    deviceSubVerification.getSubscriptionID());
            if (deviceSubVerification.getStatus() != null && deviceSubVerification.getStatus().equals(
                    PaywallConstants.WP_API_STATUS_OK)) {
                state = PaywallResult.State.SUCCESS;
                if (existingSub != null && Utils.INSTANCE.isAmazonDevice()) {
                    Subscription migratedAmazonClassicSubscription = PaywallService.getBillingHelper().getMigratedAmazonClassicSubscription();
                    if(migratedAmazonClassicSubscription != null) {
                        if (TextUtils.equals(migratedAmazonClassicSubscription.getReceiptNumber(), deviceSubVerification.getOldReceiptId())) {
                            migratedAmazonClassicSubscription.setDeprecated(true);
                            PaywallService.getBillingHelper().updateAmazonClassicSubscription(migratedAmazonClassicSubscription);
                            PaywallService.getConnector().logD(new EventLog.Builder().setMessage("Amazon store migrated sub"));
                            PaywallService.getPaywallPrefHelper().setPrefMigratedAmazonUserAppstoreMigrationComplete(true);
                        }
                    }
                    // existingSubType and isUpgrade are GAPs — stay on top-level until backend adds to SubItem
                    Logger.d(TAG, "verifyDevice existingSub=" + deviceSubVerification.getExistingSubType() + " , isUpgrade=" + deviceSubVerification.isUpgrade());
                    existingSub.setUpgrade(deviceSubVerification.isUpgrade());
                    existingSub.setExistingSubType(deviceSubVerification.getExistingSubType());
                }
                if (baseSub != null) {
                    Logger.d(TAG, "verifyDevice baseSub short title=" + baseSub.getShortTitle());
                    PaywallService.getConnector().setPaywallSubShortTitle(baseSub.getShortTitle() != null ? baseSub.getShortTitle() : deviceSubVerification.getShortTitle());
                    PaywallService.getConnector().setPaywallSubProduct(baseSub.getProduct() != null ? baseSub.getProduct() : deviceSubVerification.getProduct());
                    PaywallService.getConnector().setPaywallSubSource(baseSub.getSubSource() != null ? baseSub.getSubSource() : deviceSubVerification.getSubSource());
                    PaywallService.getConnector().setPaywallSubscriberType(baseSub.getSourceType() != null ? baseSub.getSourceType() : deviceSubVerification.getSubscriberType());
                    PaywallService.getConnector().setPaywallSource(baseSub.getSource() != null ? baseSub.getSource() : deviceSubVerification.getSource());
                    if (baseSub.getCurrentRateId() != null) {
                        PaywallService.getConnector().setPaywallSubCurrentRateID(baseSub.getCurrentRateId());
                    }
                } else {
                    // Fallback to top-level fields
                    PaywallService.getConnector().setPaywallSubShortTitle(deviceSubVerification.getShortTitle());
                    PaywallService.getConnector().setPaywallSubProduct(deviceSubVerification.getProduct());
                    PaywallService.getConnector().setPaywallSubSource(deviceSubVerification.getSubSource());
                    PaywallService.getConnector().setPaywallSubscriberType(deviceSubVerification.getSubscriberType());
                    PaywallService.getConnector().setPaywallSource(deviceSubVerification.getSource());
                    if (deviceSubVerification.getCurrentRateID() != null) {
                        PaywallService.getConnector().setPaywallSubCurrentRateID(deviceSubVerification.getCurrentRateID());
                    }
                }
                PaywallService.getConnector().setPriceFlag(deviceSubVerification.getPriceFlag());
                PaywallService.getConnector().setSubAcctMgmt(deviceSubVerification.getSubAcctMgmt());
                PaywallService.getConnector().setSubAccountAnalytics(deviceSubVerification.getSubAccountAnalytics());
                DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                try {
                    Date date = null;
                    if (baseSub != null && baseSub.getExpirationDate() != null) {
                        date = df2.parse(baseSub.getExpirationDate());
                    } else if (deviceSubVerification.getExpirationDate() != null) {
                        date = df1.parse(deviceSubVerification.getExpirationDate());
                    }
                    if (existingSub != null && date != null) {
                        existingSub.setExpirationDate(date.getTime());
                    }
                } catch (Exception e) {
                    PaywallService.getConnector().logE(new EventLog.Builder()
                            .setMessage("Error parsing date from verifyDevice receipt")
                            .setErrorMessage(e.getMessage()));
                }
                if (existingSub != null) {
                    String subStateVal = baseSub != null && baseSub.getSubState() != null ? baseSub.getSubState() : deviceSubVerification.getSubState();
                    existingSub.setSubState(subStateVal);
                    // Sync addon subscriptions from the current cached sub (which was updated by persistAddonSubscriptions)
                    // to prevent existingSub's stale addons from being written back to DB
                    Subscription currentCached = PaywallService.getBillingHelper().cachedSubscription();
                    if (currentCached != null) {
                        existingSub.setAddonSubscriptions(currentCached.getAddonSubscriptions());
                    }
                    PaywallService.getBillingHelper().updateSubscriptionDetails(existingSub);
                }
            } else if (deviceSubVerification.getStatus() != null && deviceSubVerification.getStatus().equals(PaywallConstants.WP_API_STATUS_EXPIRED)) {
                state = PaywallResult.State.FAIL;
                PaywallService.getBillingHelper().cleanSubscription();
                PaywallService.getConnector().logE(new EventLog.Builder().setMessage("Device subscription status [Expired], clearing device subscription locally"));
            } else {
                state = PaywallResult.State.FAIL;
                String subData = PaywallService.getBillingHelper().cachedSubscription() != null ? PaywallService.getBillingHelper().cachedSubscription().toString() : "No data";
                PaywallService.getConnector().logE(new EventLog.Builder()
                        .setMessage("Device subscription status [Failed]")
                        .set("data", subData));
            }

            if (deviceSubVerification.getUuid() != null) {
                PaywallService.getInstance().setVerifySubUUID(deviceSubVerification.getUuid());
            }
            String subId = baseSub != null && baseSub.getSubscriptionId() != null ? baseSub.getSubscriptionId() : deviceSubVerification.getSubscriptionID();
            if (subId != null) {
                PaywallService.getInstance().setSubscriptionID(subId);
            }
        } else {
            state = PaywallResult.State.FAIL;
        }
        return state;
    }

    public PaywallResult.State getRainbowVerifyDeviceState(SubVerification deviceSubVerification, Subscription existingSub) {
        PaywallResult.State state;
        if (deviceSubVerification != null) {
            SubItem baseSub = PaywallUtil.getBestAvailableBaseSubscription(
                    deviceSubVerification.getSubscriptions(),
                    deviceSubVerification.getSubscriptionID());
            if (deviceSubVerification.getStatus() != null && deviceSubVerification.getStatus().equals(
                    PaywallConstants.WP_API_STATUS_OK)) {
                state = PaywallResult.State.SUCCESS;
                if (existingSub != null && Utils.INSTANCE.isAmazonDevice()) {
                    Logger.d(TAG, "verifyDevice (Rainbow sub) existingSub=" + deviceSubVerification.getExistingSubType() + " , isUpgrade=" + deviceSubVerification.isUpgrade());
                    existingSub.setUpgrade(deviceSubVerification.isUpgrade());
                    existingSub.setExistingSubType(deviceSubVerification.getExistingSubType());
                }
                if (baseSub != null) {
                    PaywallService.getConnector().setPaywallSubShortTitle(baseSub.getShortTitle() != null ? baseSub.getShortTitle() : deviceSubVerification.getShortTitle());
                    PaywallService.getConnector().setPaywallSubProduct(baseSub.getProduct() != null ? baseSub.getProduct() : deviceSubVerification.getProduct());
                    PaywallService.getConnector().setPaywallSubSource(baseSub.getSubSource() != null ? baseSub.getSubSource() : deviceSubVerification.getSubSource());
                    PaywallService.getConnector().setPaywallSubscriberType(baseSub.getSourceType() != null ? baseSub.getSourceType() : deviceSubVerification.getSubscriberType());
                    PaywallService.getConnector().setPaywallSource(baseSub.getSource() != null ? baseSub.getSource() : deviceSubVerification.getSource());
                    if (baseSub.getCurrentRateId() != null) {
                        PaywallService.getConnector().setPaywallSubCurrentRateID(baseSub.getCurrentRateId());
                    } else if (deviceSubVerification.getCurrentRateID() != null) {
                        PaywallService.getConnector().setPaywallSubCurrentRateID(deviceSubVerification.getCurrentRateID());
                    }
                } else {
                    PaywallService.getConnector().setPaywallSubShortTitle(deviceSubVerification.getShortTitle());
                    PaywallService.getConnector().setPaywallSubProduct(deviceSubVerification.getProduct());
                    PaywallService.getConnector().setPaywallSubSource(deviceSubVerification.getSubSource());
                    PaywallService.getConnector().setPaywallSubscriberType(deviceSubVerification.getSubscriberType());
                    PaywallService.getConnector().setPaywallSource(deviceSubVerification.getSource());
                    if (deviceSubVerification.getCurrentRateID() != null) {
                        PaywallService.getConnector().setPaywallSubCurrentRateID(deviceSubVerification.getCurrentRateID());
                    }
                }
                PaywallService.getConnector().setPriceFlag(deviceSubVerification.getPriceFlag());
                PaywallService.getConnector().setSubAcctMgmt(deviceSubVerification.getSubAcctMgmt());
                PaywallService.getConnector().setSubAccountAnalytics(deviceSubVerification.getSubAccountAnalytics());
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                try {
                    String expDate = baseSub != null && baseSub.getExpirationDate() != null ? baseSub.getExpirationDate() : deviceSubVerification.getExpirationDate();
                    Date date = df.parse(expDate);
                    if (existingSub != null) {
                        existingSub.setExpirationDate(date.getTime());
                    }
                } catch (Exception e) {
                    PaywallService.getConnector().logE(new EventLog.Builder()
                            .setMessage("Error parsing date from verifyDevice receipt")
                            .setErrorMessage(e.getMessage()));
                }
            } else {
                state = PaywallResult.State.FAIL;
                PaywallService.getConnector().logE(new EventLog.Builder().setMessage("Device subscription verification failed"));
            }
            if (existingSub != null) {
                String rbSubState = baseSub != null && baseSub.getSubState() != null ? baseSub.getSubState() : deviceSubVerification.getSubState();
                String rbSubStatus = baseSub != null && baseSub.getSubStatus() != null ? baseSub.getSubStatus() : deviceSubVerification.getSubStatus();
                existingSub.setSubState(rbSubState);
                PaywallService.getConnector().setRainbowSubscriptionStatus(rbSubStatus);
                PaywallService.getBillingHelper().updateRainbowSubscription(existingSub);
            }

            if (deviceSubVerification.getUuid() != null) {
                PaywallService.getInstance().setVerifySubUUID(deviceSubVerification.getUuid());
            }
            String rbSubId = baseSub != null && baseSub.getSubscriptionId() != null ? baseSub.getSubscriptionId() : deviceSubVerification.getSubscriptionID();
            if (rbSubId != null) {
                PaywallService.getInstance().setSubscriptionID(rbSubId);
            }
        } else {
            state = PaywallResult.State.FAIL;
        }
        return state;
    }
    public PaywallResult.State getAmazonClassicVerifyState(SubVerification deviceSubVerification, Subscription existingSub) {
        PaywallResult.State state;
        if (deviceSubVerification != null) {
            SubItem baseSub = PaywallUtil.getBestAvailableBaseSubscription(
                    deviceSubVerification.getSubscriptions(),
                    deviceSubVerification.getSubscriptionID());
            if (deviceSubVerification.getStatus() != null && deviceSubVerification.getStatus().equals(
                    PaywallConstants.WP_API_STATUS_OK)) {
                state = PaywallResult.State.SUCCESS;
                if (existingSub != null && Utils.INSTANCE.isAmazonDevice()) {
                    Logger.d(TAG, "verifyDevice (Amazon Classic sub) existingSub=" + deviceSubVerification.getExistingSubType() + " , isUpgrade=" + deviceSubVerification.isUpgrade());
                    existingSub.setUpgrade(deviceSubVerification.isUpgrade());
                    existingSub.setExistingSubType(deviceSubVerification.getExistingSubType());
                }
                if (baseSub != null) {
                    PaywallService.getConnector().setPaywallSubShortTitle(baseSub.getShortTitle() != null ? baseSub.getShortTitle() : deviceSubVerification.getShortTitle());
                    PaywallService.getConnector().setPaywallSubProduct(baseSub.getProduct() != null ? baseSub.getProduct() : deviceSubVerification.getProduct());
                    PaywallService.getConnector().setPaywallSubSource(baseSub.getSubSource() != null ? baseSub.getSubSource() : deviceSubVerification.getSubSource());
                    PaywallService.getConnector().setPaywallSubscriberType(baseSub.getSourceType() != null ? baseSub.getSourceType() : deviceSubVerification.getSubscriberType());
                    PaywallService.getConnector().setPaywallSource(baseSub.getSource() != null ? baseSub.getSource() : deviceSubVerification.getSource());
                    if (baseSub.getCurrentRateId() != null) {
                        PaywallService.getConnector().setPaywallSubCurrentRateID(baseSub.getCurrentRateId());
                    } else if (deviceSubVerification.getCurrentRateID() != null) {
                        PaywallService.getConnector().setPaywallSubCurrentRateID(deviceSubVerification.getCurrentRateID());
                    }
                } else {
                    PaywallService.getConnector().setPaywallSubShortTitle(deviceSubVerification.getShortTitle());
                    PaywallService.getConnector().setPaywallSubProduct(deviceSubVerification.getProduct());
                    PaywallService.getConnector().setPaywallSubSource(deviceSubVerification.getSubSource());
                    PaywallService.getConnector().setPaywallSubscriberType(deviceSubVerification.getSubscriberType());
                    PaywallService.getConnector().setPaywallSource(deviceSubVerification.getSource());
                    if (deviceSubVerification.getCurrentRateID() != null) {
                        PaywallService.getConnector().setPaywallSubCurrentRateID(deviceSubVerification.getCurrentRateID());
                    }
                }
                PaywallService.getConnector().setPriceFlag(deviceSubVerification.getPriceFlag());
                PaywallService.getConnector().setSubAcctMgmt(deviceSubVerification.getSubAcctMgmt());
                PaywallService.getConnector().setSubAccountAnalytics(deviceSubVerification.getSubAccountAnalytics());
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                try {
                    String expDate = baseSub != null && baseSub.getExpirationDate() != null ? baseSub.getExpirationDate() : deviceSubVerification.getExpirationDate();
                    Date date = df.parse(expDate);
                    if (existingSub != null) {
                        existingSub.setExpirationDate(date.getTime());
                    }
                } catch (Exception e) {
                    PaywallService.getConnector().logE(new EventLog.Builder()
                            .setMessage("Error parsing date from verifyDevice (Amazon Classic) receipt")
                            .setErrorMessage(e.getMessage()));
                }
            } else {
                state = PaywallResult.State.FAIL;
                PaywallService.getConnector().logE(new EventLog.Builder().setMessage("Device subscription verification failed"));
            }
            if (existingSub != null) {
                String amzSubState = baseSub != null && baseSub.getSubState() != null ? baseSub.getSubState() : deviceSubVerification.getSubState();
                String amzSubStatus = baseSub != null && baseSub.getSubStatus() != null ? baseSub.getSubStatus() : deviceSubVerification.getSubStatus();
                existingSub.setSubState(amzSubState);
                PaywallService.getConnector().setAmazonClassicSubscriptionStatus(amzSubStatus);
                PaywallService.getBillingHelper().updateAmazonClassicSubscription(existingSub);
            }

            if (deviceSubVerification.getUuid() != null) {
                PaywallService.getInstance().setVerifySubUUID(deviceSubVerification.getUuid());
            }
            String amzSubId = baseSub != null && baseSub.getSubscriptionId() != null ? baseSub.getSubscriptionId() : deviceSubVerification.getSubscriptionID();
            if (amzSubId != null) {
                PaywallService.getInstance().setSubscriptionID(amzSubId);
            }
        } else {
            state = PaywallResult.State.FAIL;
        }
        return state;
    }

    private String encryptText(String id, String publicKey) {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(publicKey, Base64.NO_WRAP));
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(spec));
            return Base64.encodeToString(cipher.doFinal(id.getBytes("UTF-8")), Base64.NO_WRAP);
        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Exception in migration encryption")
                    .setErrorMessage(e.getMessage()));
            e.printStackTrace();
        }
        return null;
    }

    //this is a hack for migration to allow us to save the token response which requires a token request.
    private TokenRequest getFakeTokenRequest() {

        OAuthConfigStub oAuthConfigStub = PaywallService.getInstance().getOAuthConfigStub();

        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
                Uri.parse(oAuthConfigStub.getAuthorizationUrl(null, null, false, PaywallUtil.INSTANCE.getPaywallServiceConfigExtra())),
                Uri.parse(oAuthConfigStub.getTokenUrl()));

        TokenRequest.Builder builder = new TokenRequest.Builder(config, PaywallService.getConnector().getClientId());
        builder.setGrantType("authorization_code");
        builder.setRedirectUri(Uri.parse(((AuthApplication) PaywallService.getInstance().getContext()).getAppRedirectScheme()));
        builder.setAuthorizationCode("vabeAK55rcyGmigeHrtoIhlwiyyiTldG");

        return builder.build();
    }

    private Map<String, Object> getDefaultPostParamsMap() {
        Map<String, Object> paramsMap = new HashMap<>();

        paramsMap.put(PaywallConstants.OS_VERSION, Build.VERSION.RELEASE);
        paramsMap.put(PaywallConstants.HARDWARE_TYPE, Build.MANUFACTURER + "-" + Build.MODEL);
        paramsMap.put(PaywallConstants.APP_VERSION, PaywallService.getInstance().getConnector().getAppVersion());
        paramsMap.put(PaywallConstants.DEVICE_ID, PaywallService.getInstance().getConnector().getDeviceId());
        paramsMap.put(PaywallConstants.APP_NAME, PaywallService.getInstance().getConnector().getAppName());
        return paramsMap;
    }

    private Map<String, String> getDefaultPostHeaderMap() {
        Map<String, String> headerMap = new HashMap<>();

        headerMap.put(CLIENT_ID, PaywallService.getConnector().getClientId());
        headerMap.put(CLIENT_IP, PaywallService.getConnector().getIpAddress());
        headerMap.put(CLIENT_APP, PaywallService.getConnector().getAppName());
        headerMap.put(REQUEST_ID, UUID.randomUUID().toString());
        headerMap.put(DEVICE_ID, PaywallService.getConnector().getDeviceId());
        headerMap.put(CLIENT_USER_AGENT, PaywallService.getConnector().getUserAgent());
        headerMap.put(CLIENT_APP_VERSION, PaywallService.getConnector().getAppVersion());
        headerMap.put(OS_VERSION, String.valueOf(Build.VERSION.SDK_INT));
        headerMap.put(DEVICE_NAME, Build.MANUFACTURER + "-" + Build.MODEL);

        return headerMap;
    }


    private String getPostParamsString(Map<String, String> paramsMap) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }
            try {
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (Exception e) {
                // ignore
            }
        }
        return result.toString();
    }

    /**
     * Build URL-encoded form string supporting both single values and multi-value (list) parameters.
     * For list values, this generates repeated form parameters: key=value1&key=value2&key=value3
     *
     * @param paramsMap Map where values can be String or List<String>
     * @return URL-encoded form string
     */
    private String buildPostParamsString(Map<String, Object> paramsMap) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof List) {
                // Handle list values - create repeated parameters
                @SuppressWarnings("unchecked")
                List<String> listValues = (List<String>) value;
                for (String listValue : listValues) {
                    if (first) {
                        first = false;
                    } else {
                        result.append("&");
                    }
                    try {
                        result.append(URLEncoder.encode(key, "UTF-8"));
                        result.append("=");
                        result.append(URLEncoder.encode(listValue, "UTF-8"));
                    } catch (Exception e) {
                        // ignore
                    }
                }
            } else {
                // Handle single string values
                if (first) {
                    first = false;
                } else {
                    result.append("&");
                }
                try {
                    result.append(URLEncoder.encode(key, "UTF-8"));
                    result.append("=");
                    result.append(URLEncoder.encode(String.valueOf(value), "UTF-8"));
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return result.toString();
    }

    private String postData(String url, Map<String, String> paramsMap, Map<String, String> headerMap) {
        String postBody = paramsMap != null ? getPostParamsString(paramsMap) : null;
        return postDataString(url, postBody, headerMap);
    }

    /**
     * Post data with support for multi-value parameters (lists).
     * This overload accepts Map<String, Object> where values can be String or List<String>.
     */
    private String postDataWithObjects(String url, Map<String, Object> paramsMap, Map<String, String> headerMap) {
        String postBody = paramsMap != null ? buildPostParamsString(paramsMap) : null;
        return postDataString(url, postBody, headerMap);
    }

    private String postDataString(String url, String postBody, Map<String, String> headerMap) {
        String response = null;
        HttpURLConnection urlConnection = null;
        InputStream in = null;
        try {
            URL mURL = new URL(url);
            urlConnection = (HttpURLConnection) mURL.openConnection();
            urlConnection.setDoOutput(true);
            setHeader(urlConnection, headerMap);
            Map<String, String> defaultHeaders = DefaultHeadersInterceptor.Companion.getHeaders();
            Set<String> keys = defaultHeaders.keySet();
            for (String key : keys) {
                urlConnection.setRequestProperty(key, defaultHeaders.get(key));
            }
            if (!TextUtils.isEmpty(postBody)) {
                urlConnection.setFixedLengthStreamingMode(postBody.getBytes().length);
                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.write(postBody);
                writer.flush();
                writer.close();
                out.close();
                PaywallService.getConnector().logD(new EventLog.Builder().setMessage(getRequestInfo(url, postBody)));
            }

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                in = new BufferedInputStream(urlConnection.getInputStream());
                response = convertStreamToString(in);
                if (response == null) {
                    PaywallService.getConnector().logE(new EventLog.Builder().setMessage("Conversion from stream to string failed"));
                }
            } else {
                PaywallService.getConnector().logE(new EventLog.Builder()
                        .setMessage("postData Error")
                        .setErrorCode(responseCode));
                String errorResponse = convertStreamToString(urlConnection.getErrorStream());
                if (errorResponse != null) {
                    JSONObject error = new JSONObject(errorResponse);
                    if (error.has("error_description")) {
                        String errorDescription = error.getString("error_description");
                        if (errorDescription.equals(PaywallConstants.PW_GDPR_ANONYMIZED)
                                || errorDescription.equals(PaywallConstants.PW_CSR_DELETED)
                                || errorDescription.equals(PaywallConstants.PW_IOS_DELETED)) {
                            PaywallService.getInstance().logOutCurrentUser();
                            PaywallService.getConnector().logW(new EventLog.Builder()
                                    .setMessage("/profile error")
                                    .setErrorMessage(error.getString("error") + ", " + errorDescription));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("postData exception")
                    .setErrorMessage(e.getMessage()));
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
        return response;
    }

    private void setHeader(HttpURLConnection urlConnection, Map<String, String> headerMap) {
        if (urlConnection == null || headerMap == null) return;

        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }

    }

    private String getRequestInfo(String url, String params) {
        StringBuilder request = new StringBuilder();
        return request.append("request= {")
                .append("url=").append("\"").append(url).append("\"")
                .append("params=").append(params).toString();
    }

    private String convertStreamToString(InputStream inputStream)
            throws IOException {
        if (inputStream != null) {
            StringBuilder stringbuilder = new StringBuilder();
            String line;
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    stringbuilder.append(line).append("\n");
                }
            } finally {
                inputStream.close();
            }
            return stringbuilder.toString();
        } else {
            return null;
        }
    }

    public static String Sha256(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("SHA-256");
            digest.update(s.getBytes());
            byte hashed[] = digest.digest();
            byte[] encoded = Base64.encode(hashed, Base64.NO_WRAP);
            return new String(encoded);
        } catch (NoSuchAlgorithmException e) {
            Logger.e(TAG, e.getMessage());
        }
        return "";
    }

    private static String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Logger.e(TAG, e.getMessage());
        }
        return "";
    }

    private String encryptTransactionId(String transactionId) {
        return md5(transactionId + salt);
    }

    private void addDeviceIdentifiers(Map<String, String> paramsMap) {
        paramsMap.put(CLIENT_IP, PaywallService.getConnector().getIpAddress());
        paramsMap.put(CLIENT_APP, PaywallService.getConnector().getAppName());
        paramsMap.put(REQUEST_ID, UUID.randomUUID().toString());
        paramsMap.put(DEVICE_ID, PaywallService.getConnector().getDeviceId());
        paramsMap.put(CLIENT_USER_AGENT, PaywallService.getConnector().getUserAgent());
        paramsMap.put(CLIENT_APP_VERSION, PaywallService.getConnector().getAppVersion());
        paramsMap.put(OS_VERSION, String.valueOf(Build.VERSION.SDK_INT));
        paramsMap.put(DEVICE_NAME, Build.MANUFACTURER + "-" + Build.MODEL);
    }

    private void addStoreSubscriptionParameters(Map<String, Object> paramsMap, SubscriptionActionType subscriptionActionType) {
        addStoreSubscriptionParameters(paramsMap, subscriptionActionType, false);
    }

    private void addStoreSubscriptionParameters(Map<String, Object> paramsMap, SubscriptionActionType subscriptionActionType, boolean isAmazonClassic) {

        Subscription subs = null;

        if (SubscriptionActionType.VERIFY_FREE_TRIAL.equals(subscriptionActionType)) {
            subs = PaywallService.getConnector().getFreeTrialSub();
        } else if (isAmazonClassic) {
            subs = PaywallService.getBillingHelper().getMigratedAmazonClassicSubscription();
        } else if(PaywallService.getBillingHelper().getClassicOrRainbowSubscription() != null) {
            subs = PaywallService.getBillingHelper().getClassicOrRainbowSubscription();
        } else if(PaywallService.getBillingHelper().getLastActiveSubscription() != null && SubscriptionActionType.VERIFY_SUB.equals(subscriptionActionType)) {
            subs = PaywallService.getBillingHelper().getLastActiveSubscription();
        }

        if (subs == null) {
            String message = "Wrong state, no store subscription data";
            PaywallService.getConnector().logD(new EventLog.Builder().setMessage(message));
            throw new IllegalStateException(message);
        }

        WpUser loggedInUser = WpPaywallHelper.getLoggedInUser();
        String uuid = loggedInUser != null && loggedInUser.getUuid() != null ? loggedInUser.getUuid() : PaywallService.getInstance().getVerifySubUUID();
        String storeType = PaywallService.getConnector().getStoreType().equals(PaywallConstants.AMAZON_STORE) ?
                PaywallService.getConnector().getStoreType() + (Utils.INSTANCE.isAmazonDevice() ? "-kindle-v2" : "-google-v2") : PaywallService.getConnector().getStoreType();
        String storeId = subs.getStoreUID();
        String storeProductId = subs.getStoreProductId();
        String deviceId = PaywallService.getConnector().getDeviceId();
        String storeEnv = PaywallService.getConnector().getStoreEnv(); /**"playstore"*/
        String receiptDataBase64 = "";
        if (subs.getReceiptInfo() != null) {
            byte[] receiptData;
            try {
                receiptData = subs.getReceiptInfo().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            receiptDataBase64 = Base64.encodeToString(receiptData, Base64.DEFAULT);
        }
        String storeIdEncrypted = encryptTransactionId(subs.getStoreUID());
        String isProvisional = "Y"; // TODO Make configurable
        String receiptNumber = subs.getReceiptNumber();
        String formattedTransactionDate = dateFormat.format(new Date(subs.getTransactionDate()));
        String formattedExpirationDate = dateFormat.format(new Date(subs.getExpirationDate()));
        String userId = isAmazonClassic ? PaywallService.getPaywallPrefHelper().getPrefMigratedAmazonUserId() : PaywallService.getBillingHelper().getUserId();
        if (isAmazonClassic && userId == null) {
            PaywallService.getConnector().logE(
                    new EventLog.Builder()
                            .setErrorMessage("Amazon user id missing from verifyDevice request")
            );
        }
        String isSandboxMode = Boolean.toString(PaywallService.getBillingHelper().isSandboxMode());

        String subscriptionArcId = PaywallService.getOmniture().getArcId();
        String subscriptionLocation = PaywallService.getOmniture().getGenesisLocation();
        String subscriptionExperience = PaywallService.getOmniture().getGenesisExperience();
        String subscriptionOptimizeTest = PaywallService.getOmniture().getTestGroup();
        String subscriptionUpgradeLocation = PaywallService.getOmniture().getSubStartLocation();
        String subscriptionPrice = "";
        String subscriptionDevice = PaywallService.getConnector().isTablet() ? "tablet" : "mobile";
        boolean isFromPurchase = !TextUtils.isEmpty(subscriptionLocation) || !TextUtils.isEmpty(subscriptionUpgradeLocation);

        paramsMap.put("countryCode", Locale.getDefault().getCountry());
        IAPSubItems iapSubItems = PaywallService.getConnector().getIAPSubItems();
        IAPSubItem iapSubItem = iapSubItems.getItem(storeProductId);
        if (iapSubItem != null) {
            paramsMap.put("currencyCode", iapSubItem.getCurrencyCode());
            if (!TextUtils.isEmpty(iapSubItem.getBasePrice())) {
                paramsMap.put("localPrice", removeCurrencySignFromPrice(iapSubItem.getBasePrice()));
            }
            if (!TextUtils.isEmpty(iapSubItem.getOfferPrice())) {
                paramsMap.put("localIntroPrice", removeCurrencySignFromPrice(iapSubItem.getOfferPrice()));
            }
            subscriptionPrice = iapSubItems.getIapPricingInfo();
        }

        if (SubscriptionActionType.LINK.equals(subscriptionActionType)) {
            paramsMap.put("action", "link-device");
            paramsMap.put("device.storeType", storeType);
            paramsMap.put("device.storeId", storeId);
            paramsMap.put("device.sku", storeProductId);
            paramsMap.put("device.deviceId", deviceId);
            paramsMap.put("device.storeEnv", storeEnv);
            paramsMap.put("device.receiptInfo", receiptDataBase64);
            paramsMap.put("device.storeIdEnc", storeIdEncrypted);
            paramsMap.put("device.isProvisional", isProvisional);
            paramsMap.put("device.receiptNumber", receiptNumber);
            paramsMap.put("device.transactionDate", formattedTransactionDate);
            paramsMap.put("device.subscriptionStartDate", formattedTransactionDate);
            paramsMap.put("device.subscriptionEndDate", formattedExpirationDate);
            if (userId != null) {
                paramsMap.put("device.userId", userId);
            }
            if (uuid != null) {
                paramsMap.put(PaywallConstants.PW_UUID, uuid);
            }
        } else if (SubscriptionActionType.VERIFY_SUB.equals(subscriptionActionType) || SubscriptionActionType.VERIFY_FREE_TRIAL.equals(subscriptionActionType)) {
            paramsMap.put("storeType", storeType);
            paramsMap.put("storeId", storeId);
            paramsMap.put("sku", storeProductId);
            paramsMap.put("storeEnv", storeEnv);
            paramsMap.put("receiptInfo", receiptDataBase64);
            paramsMap.put("isProvisional", isProvisional);
            paramsMap.put("receiptNumber", receiptNumber);
            paramsMap.put("transactionDate", formattedTransactionDate);
            paramsMap.put("sandbox", isSandboxMode);
            if (subs.getProductSkuList() != null) {
                // Pass productSkuList as a List<String> - will be encoded as repeated form parameters:
                // productSkuList=sku1&productSkuList=sku2&productSkuList=sku3
                paramsMap.put("productSkuList", subs.getProductSkuList());
            }
            if (userId != null) {
                paramsMap.put("userId", userId);
            }
            if (uuid != null) {
                paramsMap.put(PaywallConstants.PW_UUID, uuid);
            }

            if (isFromPurchase) {
                // wall (existing subscriber) path: send only subscription_upgrade_location and skip the
                // normal five-field subsDimensions block to verify
                if (!TextUtils.isEmpty(subscriptionUpgradeLocation)) {
                    paramsMap.put("subsDimensions.subscription_upgrade_location", subscriptionUpgradeLocation);
                } else {
                    // send the full subsDimensions when it's a nonsubscriber purchase
                    paramsMap.put("subsDimensions.subscription_location", subscriptionLocation);
                    paramsMap.put("subsDimensions.subscription_optimize_test", subscriptionOptimizeTest);
                    paramsMap.put("subsDimensions.subscription_device", subscriptionDevice);
                    paramsMap.put("subsDimensions.subscription_experience", subscriptionExperience);
                    paramsMap.put("sandbox", isSandboxMode);
                    if (!TextUtils.isEmpty(subscriptionArcId)) {
                        paramsMap.put("subsDimensions.subscription_arcid", subscriptionArcId);
                    }
                    if (!TextUtils.isEmpty(subscriptionPrice)) {
                        paramsMap.put("subsDimensions.subscription_pricing", subscriptionPrice);
                    }
                }
            }

            Map<String, String> deviceProfileParams = PaywallService.getConnector().getDeviceProfile() == null ? null : PaywallService.getConnector().getDeviceProfile().profileToMap();
            if (deviceProfileParams != null) {
                paramsMap.putAll(deviceProfileParams);
            }
        }
    }

    /**
     * This function is used to process the migrate tokens while doing LWA migration
     * i.e. creating a profile for a user only signed in using LWA prior o unification
     * in the rainbow app.
     * @param jsonResponse - json representation of the token
     * @param context - Application context
     * @return - true if the processing is successful, false otherwise.
     */
    public boolean processMigrateTokens(String jsonResponse, Context context){
        try {
            // Request is required by Auth Library to store token response
            TokenResponse tokenResponse =
                    new TokenResponse.Builder(getFakeTokenRequest()).fromResponseJsonString(jsonResponse).build();
            // Get [TokenResponse] model as it is the one used by our Auth Library
            JWT decodedToken =
                    AuthHelper.getInstance(context).getDecodedJWT(tokenResponse.idToken);
            // Process the [TokenResponse] -> Will decode JWT idToken and save profile and will

            // store refresh token in Share Prefs through auth library.
            AuthStateManager.getInstance(context).updateAfterTokenResponse(tokenResponse, null);
            return processUserFromJWTClaim(decodedToken);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

    }

}