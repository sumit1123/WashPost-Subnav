package com.washingtonpost.android.paywall.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import com.wapo.android.commons.util.Logger;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.FragmentActivity;

import com.auth0.android.jwt.JWT;
import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.logs.LogModules;
import com.wapo.android.remotelog.logger.RemoteLog;
import com.washingtonpost.android.config.domain.models.config.paywall.OAuthConfigStub;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.R;
import com.washingtonpost.android.paywall.api.VerifyState;
import com.washingtonpost.android.paywall.helper.PaywallPrefHelper;
import com.washingtonpost.android.paywall.newdata.model.WpUser;
import com.washingtonpost.android.paywall.util.PaywallConstants;
import com.washingtonpost.android.paywall.util.PaywallUtil;
import com.washingtonpost.android.paywall.util.di.AuthEntryPoint;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationManagementActivity;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationService.TokenResponseCallback;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ClientSecretPost;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;
import net.openid.appauth.browser.AnyBrowserMatcher;
import net.openid.appauth.browser.BrowserMatcher;
import net.openid.appauth.connectivity.DefaultConnectionBuilder;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import dagger.hilt.android.EntryPointAccessors;
import kotlin.coroutines.Continuation;

public class AuthHelper {

    private static final String TAG = "AuthHelper";

    public static final String STATE_PARAM = "state";
    private static final String AUTH_PREFS_FILE = "AuthPrefs";
    public static final String AUTH_ENTRY_POINT_KEY = "AuthEntryPoint";

    private final Context mAppContext;
    private Context mActivityContext;
    private AuthorizationService mAuthService;
    private AuthStateManager mAuthStateManager;
    private OAuthConfigStub oAuthConfigStub;
    private AuthEntryPoint entryPoint;
    private SharedPreferences authPreferences;

    @NonNull
    private BrowserMatcher mBrowserMatcher = AnyBrowserMatcher.INSTANCE;

    private final AtomicReference<String> mClientId = new AtomicReference<>();
    private final AtomicReference<AuthorizationRequest> mAuthRequest = new AtomicReference<>();
    private final AtomicReference<Intent> mAuthIntent = new AtomicReference<>();
    private CountDownLatch mAuthIntentLatch = new CountDownLatch(1);
    private ExecutorService mExecutor;
    private AuthListener mAuthListener;
    private boolean mShouldUseCustomTab;
    private String mAuthBrowserPackage;
    private TokenRefreshListener tokenRefreshListener;
    private String mAppRedirectScheme;
    private boolean mIsMagicLinkOrSocialRedirect = false;
    private Uri mMagicLinkOrSocialRedirectResponseData;
    private String mPromoId;
    private String mTrialType;
    private boolean isSignUp = false;

    public enum AuthStateResetReason {
        AUTHORIZATION_EXCEPTION_FROM_RESP,
        AUTHORIZATION_EXCEPTION_IN_ON_RESUME,
        AUTHORIZATION_EXCEPTION_RESP_NULL,
        PROFILE_FAILURE,
        PROFILE_FAILURE_IN_AUTH_WEBVIEW,
        AUTH_TASK,
        REVOKE_USER_TASK
    }

    private static final AtomicReference<WeakReference<AuthHelper>> INSTANCE_REF =
            new AtomicReference<>(new WeakReference<AuthHelper>(null));

    public static AuthHelper getInstance(Context context) {

        AuthHelper authHelper = INSTANCE_REF.get().get();
        if (authHelper == null) {
            authHelper = new AuthHelper(context);
            INSTANCE_REF.set(new WeakReference<>(authHelper));
        }

        return authHelper;
    }

    /**
     * Checks if redirect uri has state data. Used for magic link flow.
     * @param data
     * @return
     */
    private static boolean hasStateParam(Uri data) {
        return data != null && data.getQueryParameter(STATE_PARAM) != null;
    }

    /**
     * Magiclink Authflows require the user to leave the app so the state of the app is unknown (User may have left
     * the sign-in screen, the app may be completely closed, etc.) So magiclinks work properly, the auth activity is started
     * over again and the request is created again using the state of the original request (This comes from magic link response).
     * @param activity - usually this will be the MainActivity
     * @param data - magic link url with params
     */
    public static void handleMagicLinkOrSocialRedirectAuth(FragmentActivity fragmentActivity, Uri data) {
        // If link has no stat param, don't do anything.
        if (!hasStateParam(data)) {
            if (PaywallService.getInstance() != null) {
                PaywallService.getConnector().logD(new EventLog.Builder()
                        .setMessage("No State Params found in Magic Link / Social Redirect URI : " + data.toString()));
            }
            return;
        }
        if (PaywallService.getInstance() != null) {
            PaywallService.getConnector().logD(new EventLog.Builder()
                    .setMessage("Magic Link / Social Redirect URI : " + data.toString()));
        }
        AuthIntentBuilder builder = new AuthIntentBuilder()
                .addMagicLinkData(data.toString())
                .addIsMagicLinkOrSocialRedirect(true);
        PaywallService.getConnector().showSignInScreen(fragmentActivity.getSupportFragmentManager(), builder.build(), null, null, false, null);
    }

    public AuthHelper(Context appContext) {
        mAppContext = appContext;
        mActivityContext = null;
        entryPoint = EntryPointAccessors.fromApplication(mAppContext.getApplicationContext(), AuthEntryPoint.class);
        authPreferences = appContext.getSharedPreferences(AUTH_PREFS_FILE, Context.MODE_PRIVATE);
    }

    @Nullable
    private OAuthConfigStub resolveOAuthConfigStub() {
        if (oAuthConfigStub != null) {
            return oAuthConfigStub;
        }
        PaywallService paywallService = PaywallService.getInstance();
        if (paywallService != null) {
            oAuthConfigStub = paywallService.getOAuthConfigStub();
        }
        return oAuthConfigStub;
    }

    /**
     * Initialization of AuthHelper for a Webview. Once the ExecutorService, AuthorizationServiceConfiguration,
     * AuthorizationService and AuthRequest are created, post the full url to onInitialized(url).
     */
    public void initAuthUriForWebView(AuthListener authListener, String redirectScheme, Map<String, String> additionalParams, boolean isMagicLinkOrSocialRedirect, Uri magicLinkOrSocialRedirectResponseData, boolean isSignUp, Bundle extras) {
        OAuthConfigStub configStub = resolveOAuthConfigStub();
        if (configStub == null) {
            authListener.onAuthorizeError("Unable to sign in right now. Please try again.");
            return;
        }
        mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        mAuthListener = authListener;
        mAppRedirectScheme = redirectScheme;
        mPromoId = additionalParams != null && additionalParams.containsKey(AuthIntentBuilder.getPROMO_ID()) ? additionalParams.get(AuthIntentBuilder.getPROMO_ID()) : null;
        mTrialType = additionalParams != null && additionalParams.containsKey(AuthIntentBuilder.getTRIAL_TYPE()) ? additionalParams.get(AuthIntentBuilder.getTRIAL_TYPE()) : null;
        this.isSignUp = isSignUp;
        mIsMagicLinkOrSocialRedirect = isMagicLinkOrSocialRedirect;
        mMagicLinkOrSocialRedirectResponseData = magicLinkOrSocialRedirectResponseData;

        if (mAuthStateManager.getCurrent().isAuthorized()
                && !configStub.hasConfigurationChanged(mAppContext)) {
            mAuthListener.onAuthorized();
        }

        if (configStub.hasConfigurationChanged(mAppContext)) {
            // discard any existing authorization state due to the change of configuration
            Logger.i(TAG, "Configuration change detected, discarding old state");
            mAuthStateManager.replace(new AuthState());
            configStub.acceptConfiguration(mAppContext);
        }

        mExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "th-authHelper") {
                    @Override
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        super.run();
                    }
                };
            }
        });

        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                mClientId.set(PaywallService.getConnector().getClientId());
                recreateAuthorizationService();
                setNewConfig();
                createAuthRequest();
                mAuthListener.onInitialized(
                        mAuthRequest.get().toUri(),
                        mIsMagicLinkOrSocialRedirect,
                        mMagicLinkOrSocialRedirectResponseData,
                        extras
                );
            }
        });
    }

    /**
     * Complete the auth flow for Webview. Exchange auth code, call token api, receive token, parse jwt or call profile api and get user profile,
     * post results back to webview with onAuthorized(message) or onAuthorizeError(message).
     * @param redirectUri Redirect uri containing an auth code
     */
    @MainThread
    public void startAuthForWebView(Uri redirectUri, Bundle extras) {
        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }
        Intent intent = extractResponseData(redirectUri);
        final AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException authException = AuthorizationException.fromIntent(intent);

        if (response != null || authException != null) {
            if (authException != null && authException.type != 1) {
                // AuthState only records error type 1s, so we need to hardcode this so the app functions properly
                AuthorizationException temp = new AuthorizationException(1, authException.code, authException.error,
                        authException.errorDescription, authException.errorUri, authException.getCause());
                mAuthStateManager.updateAfterAuthorization(response, temp);
            } else {
                mAuthStateManager.updateAfterAuthorization(response, authException);
            }
        }

        if (response != null) {
            ClientSecretPost clientSecretPost = new ClientSecretPost(PaywallService.getConnector().getClientSecret());
            TokenRequest.Builder tokenRequestBuilder = new TokenRequest
                    .Builder(getAuthState().getAuthorizationServiceConfiguration(), PaywallService.getConnector().getClientId());

            tokenRequestBuilder
                    .setAuthorizationCode(response.authorizationCode)
                    .setRedirectUri(redirectUri);


            TokenRequest tokenRequest = tokenRequestBuilder.build();

            mAuthService.performTokenRequest(tokenRequest, clientSecretPost, new AuthorizationService.TokenResponseCallback() {
                @Override public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException authException) {

                    if (authException != null && authException.type != 2) {
                        //AuthState only records error type 2s, so we need to hardcode this so the app functions properly
                        AuthorizationException temp = new AuthorizationException(2, authException.code, authException.error,
                                authException.errorDescription, authException.errorUri, authException.getCause());
                        mAuthStateManager.updateAfterTokenResponse(tokenResponse, temp);
                    } else {
                        mAuthStateManager.updateAfterTokenResponse(tokenResponse, authException);
                    }

                    if (mAuthStateManager.getCurrent().getAuthorizationException() != null) {
                        String error = mAuthStateManager.getCurrent().getAuthorizationException().errorDescription;
                        String exception = mAuthStateManager.getCurrent().getAuthorizationException().getCause() == null ? "null" :
                                mAuthStateManager.getCurrent().getAuthorizationException().getCause().getMessage();
                        clearAuthState(AuthStateResetReason.AUTHORIZATION_EXCEPTION_FROM_RESP);
                        mAuthListener.onAuthorizeError(error + ": " + exception);
                    } else {

                        PaywallService.getPaywallPrefHelper().setHasMigratedToOAuth();
                        //get user subscription info
                        String accessToken = getAccessToken();

                        if (accessToken == null || PaywallService.getConnector() == null || PaywallService.getConnector().getClientId() == null) {
                            PaywallService.getConnector().logE(new EventLog.Builder()
                                    .setMessage("Cannot complete login")
                                    .set("access_token_is_null", (accessToken == null))
                                    .set("paywall_connector", (PaywallService.getConnector() == null))
                                    .set("client_id_is_null", (PaywallService.getConnector().getClientId() == null)));
                            mAuthListener.onAuthorizeError("Error logging in. Please try again");
                        }
                        //process value from auth response
                        boolean accountWasCreated = false;
                        if (getAuthState().getLastAuthorizationResponse() != null) {
                            String booleanValue = getAuthState().getLastAuthorizationResponse().additionalParameters.get("account_created");
                            accountWasCreated = booleanValue == null ? false : Boolean.valueOf(booleanValue);
                        }

                        //process user data from id_token
                        boolean success = PaywallService.getInstance().getApiServiceInstance().processUserFromJWTClaim(getDecodedJWT(getIdToken()));
                        if (success) {
                            mAuthListener.onAuthorized();
                            PaywallService.getOmniture().trackSignInComplete(accountWasCreated, false);
                            PaywallService.getConnector().startOnboardingSubscriber(extras);
                            PaywallService.getConnector().logD(new EventLog.Builder()
                                    .setMessage("User login success")
                                    .set("sub_status", PaywallService.getInstance().getSubStatus()));
                            if (PaywallService.getInstance().isPremiumUser()) {
                                PaywallService.getInstance().dispatchVerifySub(VerifyState.NeedsVerification.INSTANCE);
                            }
                            PaywallPrefHelper.getInstance(mAppContext).setLoginId(mAppContext, PaywallService.getInstance().getLoginId());
                        } else {
                            PaywallService.getConnector().logE(new EventLog.Builder()
                                    .setMessage("Unable to parse id_token for user info, calling profile API instead"));
                            //if unable to parse id_token, call profile API instead
                            boolean profileSuccess = PaywallService.getInstance().getApiServiceInstance().getUserProfile(accessToken, PaywallService.getConnector().getClientId());

                            if (profileSuccess) {
                                mAuthListener.onAuthorized();
                                PaywallService.getOmniture().trackSignInComplete(accountWasCreated, false);
                                PaywallService.getConnector().startOnboardingSubscriber(extras);
                                PaywallService.getConnector().logD(new EventLog.Builder()
                                        .setMessage("User login success")
                                        .set("sub_status", PaywallService.getInstance().getSubStatus()));
                                if (PaywallService.getInstance().isPremiumUser()) {
                                    PaywallService.getInstance().dispatchVerifySub(VerifyState.NeedsVerification.INSTANCE);
                                }
                            } else {
                                clearAuthState(AuthStateResetReason.PROFILE_FAILURE_IN_AUTH_WEBVIEW);
                                mAuthListener.onAuthorizeError("Error logging in. Please try again");
                                PaywallService.getConnector().logE(new EventLog.Builder()
                                        .setMessage("Error getting user info from /profile during login"));
                            }
                        }
                    }
                }
            });
        } else if (mAuthStateManager.getCurrent().getAuthorizationException() != null) {
            String error = mAuthStateManager.getCurrent().getAuthorizationException().errorDescription;
            String exception = mAuthStateManager.getCurrent().getAuthorizationException().getCause() == null ? null :
                    mAuthStateManager.getCurrent().getAuthorizationException().getCause().getMessage();
            clearAuthState(AuthStateResetReason.AUTHORIZATION_EXCEPTION_RESP_NULL);
            String errorToastMsg = buildAuthErrorToastMsg(error, exception);
            mAuthListener.onAuthorizeError(errorToastMsg);
        }
    }

    private String buildAuthErrorToastMsg(String error, String exception) {
        if (error != null && exception != null) {
            return error + ": " + exception;
        } else if (error != null) {
            return error;
        } else if (exception != null) {
            return exception;
        } else {
            return "Login attempt failed.";
        }
    }

    private Intent extractResponseData(Uri responseUri) {
        if (mAuthRequest.get() == null) {
            EventLog.Builder builder = new EventLog.Builder();
            builder.setMessage("Authorization request is null")
                    .setModule(LogModules.PAYWALL);
            RemoteLog.e(mAppContext, builder.build());
            return AuthorizationException.AuthorizationRequestErrors.INVALID_REQUEST.toIntent();
        } else if (responseUri.getQueryParameterNames().contains(AuthorizationException.PARAM_ERROR)) {
            return AuthorizationException.fromOAuthRedirect(responseUri).toIntent();
        } else {
            AuthorizationResponse response = new AuthorizationResponse.Builder(mAuthRequest.get())
                    .fromUri(responseUri)
                    .build();

            if (mAuthRequest.get().state == null &&
                    response.state != null ||
                    (mAuthRequest.get().state != null && !mAuthRequest.get().state.equals(response.state))) {
                net.openid.appauth.internal.Logger.warn("State returned in authorization response (%s) does not match state "
                                + "from request (%s) - discarding response",
                        response.state,
                        mAuthRequest.get().state);

                return AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH.toIntent();
            }

            return response.toIntent();
        }
    }

    public void setAuthListener(AuthListener authListener) {
        mAuthListener = authListener;
    }

    /**
     * This will be called by the PaywallLoginActivity which will provide the appropriate params based on a normal auth flow or one that requires magiclink
     * @param activityContext
     * @param authListener
     * @param shouldUseCustomTab - should authflow use Chrome Custom Tabs
     * @param authBrowserPackage - legacy param
     * @param redirectScheme - The URI that will redirect user back to the app with appropriate auth code
     * @param additionalParams - Currently contains promoId and trialType used to tell WPAA which promo/free trial user is registering for
     * @param isMagicLinkOrSocialRedirect - Is this flow restarted due to a magic link click or social provider redirect
     * @param magicLinkOrSocialRedirectResponseData - Used to feed magicLinkOrSocialRedirectResponseData (redirectURI from magiclink or social provider)
     *
     */
    public void startAuthTask(Context activityContext, AuthListener authListener, boolean shouldUseCustomTab, String authBrowserPackage, String redirectScheme,
                              Map<String, String> additionalParams, boolean isMagicLinkOrSocialRedirect, Uri magicLinkOrSocialRedirectResponseData, boolean isSignUp) {
        OAuthConfigStub configStub = resolveOAuthConfigStub();
        if (configStub == null) {
            authListener.onAuthorizeError("Unable to sign in right now. Please try again.");
            return;
        }
        if (!(activityContext instanceof Activity)) {
            throw new IllegalArgumentException("Activity context must be provided to run authentication");
        }

        mActivityContext = activityContext;
        mExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "th-authHelper") {
                    @Override
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        super.run();
                    }
                };
            }
        });
        mAuthStateManager = AuthStateManager.getInstance(activityContext);
        mAuthListener = authListener;
        mShouldUseCustomTab = shouldUseCustomTab;
        mAuthBrowserPackage = authBrowserPackage;
        mAppRedirectScheme = redirectScheme;
        mPromoId = additionalParams.containsKey(AuthIntentBuilder.getPROMO_ID()) ? additionalParams.get(AuthIntentBuilder.getPROMO_ID()) : null;
        mTrialType = additionalParams.containsKey(AuthIntentBuilder.getTRIAL_TYPE()) ? additionalParams.get(AuthIntentBuilder.getTRIAL_TYPE()) : null;
        this.isSignUp = isSignUp;

        // Data used for Magic Link
        mIsMagicLinkOrSocialRedirect = isMagicLinkOrSocialRedirect;
        mMagicLinkOrSocialRedirectResponseData = magicLinkOrSocialRedirectResponseData;

        if (mAuthStateManager.getCurrent().isAuthorized()
                && !configStub.hasConfigurationChanged(mAppContext)) {
            mAuthListener.onAuthorized();
            return;
        }

        if (configStub.hasConfigurationChanged(mAppContext)) {
            // discard any existing authorization state due to the change of configuration
            Logger.i(TAG, "Configuration change detected, discarding old state");
            mAuthStateManager.replace(new AuthState());
            configStub.acceptConfiguration(mAppContext);
        }

        final Activity activity = (Activity) activityContext;
        if (activity.getIntent().getBooleanExtra(PaywallConstants.AUTH_EXTRA_FAILED, false)) {
            clearAuthState(AuthStateResetReason.AUTH_TASK);
            mAuthListener.onAuthorizeError("");
            return;
        }

        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                initializeAppAuth();
            }
        });
    }

    @MainThread
    public void startAuth() {
        if (mActivityContext == null) {
            throw new IllegalStateException("initAuthTask must be called before starting authentication");
        }
        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        if (!mExecutor.isShutdown()) {
            mExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    doAuth();
                }
            });
        }
    }

    @WorkerThread
    public void refreshAccessToken(TokenRefreshListener tokenRefreshListener) {
        if (PaywallPrefHelper.getInstance(mAppContext).isUserMissingRefreshToken()) {
            return;
        }

        this.tokenRefreshListener = tokenRefreshListener;

        if (mAuthService == null) {
            recreateAuthorizationService();
        }

        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }

        //determine reason for refresh token being called
        PaywallService paywallService = PaywallService.getInstance();
        AuthState state = mAuthStateManager.getCurrent();
        boolean isTokenNull = state.getAccessToken() == null;
        boolean hasTimeExpired = state.getAccessTokenExpirationTime() == null
                || System.currentTimeMillis() > state.getAccessTokenExpirationTime();
        boolean isRefreshTokenNull = state.getRefreshToken() == null;
        boolean isLastAuthResponseNull = state.getLastAuthorizationResponse() == null;
        PaywallService.getConnector().logW(new EventLog.Builder()
                .setMessage("mpivc refresh token")
                .set("is_token_null", isTokenNull)
                .set("has_time_expired", hasTimeExpired)
                .set("is_refresh_token_null", isRefreshTokenNull)
                .set("is_last_auth_response_null", isLastAuthResponseNull)
                .set("is_user_logged_in", (paywallService != null && paywallService.isWpUserLoggedIn())));

        if (isRefreshTokenNull || isLastAuthResponseNull) {
            PaywallService.getConnector().logW(new EventLog.Builder()
                    .setMessage("Auth data")
                    .set("current", mAuthStateManager.getCurrent().jsonSerializeString()));
            PaywallPrefHelper.getInstance(mAppContext).setPrefIsUserMissingRefreshToken(true);
            return;
        }
        HashMap<String, String> refreshTokenParams = new HashMap<>();
        WpUser wpUser = PaywallService.getInstance().getLoggedInUser();
        if (wpUser != null) {
            refreshTokenParams.put(PaywallConstants.LOGIN_ID_PARAM, wpUser.getUuid());
            refreshTokenParams.put(PaywallConstants.SECURE_LOGIN_ID_PARAM, wpUser.getSecureLoginID());
            refreshTokenParams.put(PaywallConstants.PROVIDER_NAME_PARAM, wpUser.getSignedInThrough());

            // If any of the values (uuid, loginId, or loginProvider) are null, log a handled exception
            // Fixme: AWA-4220 - Temporary resolution to figure out if one value is null or if all ar null. Find root cause of why they are null.
            if(wpUser.getUuid() == null || wpUser.getSecureLoginID() == null || wpUser.getSignedInThrough() == null) {
                String message = TAG + ": loginId=" + wpUser.getUuid() + " | secureLoginId =" + wpUser.getSecureLoginID() + " | loginProvider=" + wpUser.getSignedInThrough();
                PaywallService.getConnector().logHandledException(new NullPointerException(message));
            }
        }
        performTokenRequest(
                mAuthStateManager.getCurrent().createTokenRefreshRequest(refreshTokenParams), new TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException e) {
                        handleAccessTokenResponse(tokenResponse, e);
                    }
                });
    }

    public String getRefreshToken() {

        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }

        return mAuthStateManager.getCurrent().getRefreshToken();
    }

    public void runWithValidToken(Runnable apiCall) {
        PaywallService paywallService = PaywallService.getInstance();
        if (paywallService != null && paywallService.isWpUserLoggedIn()) {
            if (shouldRefreshAccessToken()) {
                refreshAccessToken(new TokenRefreshListener() {
                    @Override
                    public void onTokenRefresh() {
                        // Token refreshed, safe to call API now
                        apiCall.run();
                    }
                });
            } else {
                // Token is valid, call API right away
                apiCall.run();
            }
        }
    }

    public String getIdToken() {

        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }

        return mAuthStateManager.getCurrent().getIdToken();
    }

    public void clearAuthState(AuthStateResetReason reason) {

        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }
        PaywallService paywallService = PaywallService.getInstance();
        AuthState state = mAuthStateManager.getCurrent();
        boolean isTokenNull = state.getAccessToken() == null;
        boolean hasTimeExpired = state.getAccessTokenExpirationTime() == null
                || System.currentTimeMillis() > state.getAccessTokenExpirationTime();
        boolean isRefreshTokenNull = state.getRefreshToken() == null;
        boolean isLastAuthResponseNull = state.getLastAuthorizationResponse() == null;
        PaywallService.getConnector().logD(new EventLog.Builder()
                .setMessage("Auth State Cleared")
                .set("is_token_null", isTokenNull)
                .set("has_time_expired", hasTimeExpired)
                .set("is_refresh_token_null", isRefreshTokenNull)
                .set("is_last_auth_response_null", isLastAuthResponseNull)
                .set("is_user_logged_in", (paywallService != null && paywallService.isWpUserLoggedIn()))
                .set("reason", reason));

        mAuthStateManager.clearState();
    }

    public AuthState getAuthState() {
        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }

        return mAuthStateManager.getCurrent();
    }

    public JWT getDecodedJWT(String token) {
        if (token == null) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("id_token is null, cannot parse user profile"));
            return null;
        }
        try {
            return new JWT(token);
        } catch (Exception e) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Error parsing JWT")
                    .setErrorMessage(e.getMessage()));
            Logger.e(TAG, "Error parsing JWT : " + e.getMessage());
        }
        return null;
    }

    public boolean shouldRefreshAccessToken() {
        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }

        return mAuthStateManager.getCurrent().getNeedsTokenRefresh();
    }

    public boolean shouldRecoverMissingTokenResponse() {

        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }

        AuthState currentState = mAuthStateManager.getCurrent();

        if (currentState.getAccessToken() == null && currentState.getRefreshToken() == null) {
            return true;
        }

        return false;
    }

    public String getAccessToken() {

        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }

        return mAuthStateManager.getCurrent().getAccessToken();
    }


    public boolean isUserAuthorized() {
        OAuthConfigStub configStub = resolveOAuthConfigStub();
        if (configStub == null) {
            return false;
        }
        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }

        if (mAuthStateManager.getCurrent().isAuthorized()
                && !configStub.hasConfigurationChanged(mAppContext)) {
            return true;
        }

        return false;
    }

    /**
     * Performs the authorization request, using the browser selected in the spinner,
     * and a user-provided `login_hint` if available.
     */
    @WorkerThread
    private void doAuth() {
        try {
            mAuthIntentLatch.await();
        } catch (InterruptedException ex) {
            Logger.e(TAG, "Interrupted while waiting for auth intent");
        }

        final Activity activity = (Activity) mActivityContext;

        Intent completionIntent = new Intent(mActivityContext, PaywallTokenActivity.class);
        Intent cancelIntent = new Intent(mActivityContext, activity.getClass());
        cancelIntent.putExtra(PaywallConstants.AUTH_EXTRA_FAILED, true);
        cancelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Intent authIntent = prepareAuthIntent(mAuthRequest.get(),
                mAuthIntent.get());

        if (mActivityContext.getPackageManager() != null && authIntent.resolveActivity(mActivityContext.getPackageManager()) == null) {
            ((Activity) mActivityContext).runOnUiThread(() -> noValidBrowserDialog(cancelIntent));
            PaywallService.getConnector().logHandledException(new ActivityNotFoundException(mAppContext.getString(R.string.no_valid_browser_exception_message)));
            return;
        }

        performAuthorizationRequest(
                mAuthRequest.get(),
                authIntent,
                PendingIntent.getActivity(mActivityContext, 0, completionIntent, PendingIntent.FLAG_MUTABLE),
                PendingIntent.getActivity(mActivityContext, 0, cancelIntent, PendingIntent.FLAG_MUTABLE));
    }

    @MainThread
    private void noValidBrowserDialog(Intent cancelIntent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivityContext);
        AlertDialog dialog = builder.setTitle(R.string.no_valid_browser_title)
                .setMessage(R.string.no_valid_browser_message)
                .setPositiveButton(R.string.ok_button, (dialogInterface, i) -> {
                    mActivityContext.startActivity(cancelIntent);
                })
                .create();
        dialog.show();
    }


    private Intent prepareAuthIntent(AuthorizationRequest request, Intent intent) {
        Uri requestUri = request.toUri();
        intent.setData(requestUri);
        intent.putExtra(CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE, CustomTabsIntent.NO_TITLE);

        return intent;
    }

    private void performAuthorizationRequest(AuthorizationRequest request, Intent authIntent, PendingIntent completedIntent, PendingIntent canceledIntent) {

        mActivityContext.startActivity(AuthorizationManagementActivity.createStartIntent(
                mActivityContext,
                request,
                authIntent,
                completedIntent,
                canceledIntent));
    }

    /**
     * Initializes the authorization service configuration if necessary, either from the local
     * static values or by retrieving an OpenID discovery document.
     */
    @WorkerThread
    private void initializeAppAuth() {
        Logger.i(TAG, "Initializing AppAuth");
        recreateAuthorizationService();

        if (mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration() != null) {
            // configuration is already created, skip to client initialization
            initializeClient();
            return;
        }

        setNewConfig();
        initializeClient();
    }

    private void setNewConfig() {
        OAuthConfigStub configStub = resolveOAuthConfigStub();
        if (configStub == null) {
            return;
        }
        AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
                Uri.parse(configStub.getAuthorizationUrl(mPromoId, mTrialType, isSignUp, PaywallUtil.INSTANCE.getPaywallServiceConfigExtra())),
                Uri.parse(configStub.getTokenUrl()));
        mAuthStateManager.replace(new AuthState(config));
    }

    /**
     * Initiates a dynamic registration request if a client ID is not provided by the static
     * configuration.
     */
    @WorkerThread
    private void initializeClient() {
        Logger.i(TAG, "Using static client ID: " + PaywallService.getConnector().getClientId());
        // use a statically configured client ID
        mClientId.set(PaywallService.getConnector().getClientId());
        ((Activity) mActivityContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initializeAuthRequest();
            }
        });
    }

    /**
     * IsMagicLink = false : Initializes Auth Request. Launches normal flow with Chrome Custom Tab / Browser
     * IsMagicLink = true : Recreates Auth Request with data from Magic Link URI and handles the request
     */
    @MainThread
    private void initializeAuthRequest() {
        createAuthRequest();
        if (!mIsMagicLinkOrSocialRedirect) {
            warmUpBrowser();
            startAuth();
        } else {
            startMagicLinkOrSocialRedirectAuth();
        }
    }

    /**
     * Start magic link / social redirect auth. This process should log user in automatically if Auth Code is valid.
     */
    @MainThread
    public void startMagicLinkOrSocialRedirectAuth() {
        if (mMagicLinkOrSocialRedirectResponseData == null) {
            PaywallService.getConnector().logE(new EventLog.Builder().setMessage("Response Error : data from deeplink is NULL"));
            return;
        }
        Intent completionIntent = new Intent(mActivityContext, PaywallTokenActivity.class);
        Intent intent = AuthorizationManagementActivity.createStartIntent(
                mActivityContext,
                mAuthRequest.get(),
                null,
                PendingIntent.getActivity(mActivityContext, 0, completionIntent, PendingIntent.FLAG_MUTABLE),
                null);
        intent.setData(mMagicLinkOrSocialRedirectResponseData);

        intent.putExtra("authStarted", true);
        mActivityContext.startActivity(intent);
    }

    /**
     * Create Auth request for normal flow or magic link / social redirect flow
     */
    private void createAuthRequest() {
        OAuthConfigStub configStub = resolveOAuthConfigStub();
        if (configStub == null) {
            return;
        }
        // If is magic link or social redirect, use authState from previous request.
        String authState = mIsMagicLinkOrSocialRedirect ? mMagicLinkOrSocialRedirectResponseData.getQueryParameter("state") : configStub.getAuthorizationState();

        if (mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration() == null) {
            setNewConfig();
        }

        AuthorizationRequest.Builder authRequestBuilder = new AuthorizationRequest.Builder(
                mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration(),
                mClientId.get(),
                ResponseTypeValues.CODE,
                Uri.parse(mAppRedirectScheme))
                .setScope(configStub.getAuthorizationScope())
                .setState(authState);

        AuthorizationRequest authorizationRequest = authRequestBuilder.build();

        String requestId = getRequestId(authorizationRequest);

        PaywallService.getConnector().logW(new EventLog.Builder()
                .setMessage("Auth Warning")
                .set("request_id", requestId)
                .set("login_auth_url", configStub.getAuthorizationBaseUrl(isSignUp, mPromoId != null && mTrialType != null))
                .set("clientId", authorizationRequest.clientId)
                .set("state", authorizationRequest.state)
                .set("scope", authorizationRequest.scope));

        mAuthRequest.set(authorizationRequest);
    }

    private String getRequestId(AuthorizationRequest request) {
        if(request == null) {
            return "not_found";
        }
        String requestId = request.toUri().getQueryParameter("request_id");
        return requestId != null ? requestId : "not_found";
    }
    private void recreateAuthorizationService() {
        if (mAuthService != null) {
            Logger.i(TAG, "Discarding existing AuthService instance");
            mAuthService.dispose();
        }
        mAuthService = createAuthorizationService();
        mAuthRequest.set(null);
        mAuthIntent.set(null);
    }

    private AuthorizationService createAuthorizationService() {
        Logger.i(TAG, "Creating authorization service");
        AppAuthConfiguration.Builder builder = new AppAuthConfiguration.Builder();
        builder.setBrowserMatcher(mBrowserMatcher);
        builder.setConnectionBuilder(DefaultConnectionBuilder.INSTANCE);

        return new AuthorizationService(mAppContext, builder.build());
    }


    @WorkerThread
    private void performTokenRequest(
            TokenRequest request,
            AuthorizationService.TokenResponseCallback callback) {
        ClientAuthentication clientAuthentication;
        clientAuthentication = new ClientSecretPostImpl(PaywallService.getConnector().getClientSecret());

        mAuthService.performTokenRequest(
                request,
                clientAuthentication,
                callback);
    }

    public void handleAccessTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        if (mAuthStateManager == null) {
            mAuthStateManager = AuthStateManager.getInstance(mAppContext);
        }
        if (authException != null) {
            String error = authException.errorDescription;
            String exception = authException.getCause() == null ? "null" : authException.getCause().getMessage();
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("refresh token failed: " + error)
                    .setErrorMessage(exception));
            //in the case of an error from refresh token, we are going to retain the old token response as Identity will still accept the old tokens
            //TODO generally, we should not be able to use expired tokens, so revert this once we figure out why refreshToken is being called and handle error gracefully
        } else {
            PaywallService.getConnector().logD(new EventLog.Builder().setMessage("refresh token successful"));
            mAuthStateManager.updateAfterTokenResponse(tokenResponse, null);

            if (tokenRefreshListener != null) {
                tokenRefreshListener.onTokenRefresh();
            }
        }
    }

    @Nullable
    public Object fetchNonce(@Nullable String externalUrl, Continuation<? super String> continuation) {
        return entryPoint.nonceRepository().fetchNonce(externalUrl, continuation);
    }

    private void warmUpBrowser() {
        mAuthIntentLatch = new CountDownLatch(1);
        if (!mExecutor.isShutdown()) {
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Logger.i(TAG, "Warming up browser instance for auth request");
                    if (mShouldUseCustomTab) {
                        try {
                            CustomTabsIntent.Builder intentBuilder =
                                    mAuthService.createCustomTabsIntentBuilder(mAuthRequest.get().toUri());
                            intentBuilder.setToolbarColor(mAppContext.getResources().getColor(android.R.color.black));
                            mAuthIntent.set(intentBuilder.build().intent);
                        } catch (Exception e) {
                            PaywallService.getConnector().logE(new EventLog.Builder()
                                    .setMessage("Failed to initialize CustomTabsIntent for sign in")
                                    .setErrorMessage(e.getMessage()));
                            warmUpBrowserActionViewIntent();
                        }
                    } else {
                        warmUpBrowserActionViewIntent();
                    }
                    mAuthIntentLatch.countDown();
                }
            });
        }
    }

    private void warmUpBrowserActionViewIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (!TextUtils.isEmpty(mAuthBrowserPackage)) {
            intent.setPackage(mAuthBrowserPackage);
            intent.setData(mAuthRequest.get().toUri());
            try {
                List<ResolveInfo> list = mAppContext.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);
                if (list == null || list.isEmpty()) {
                    intent.setPackage(null);
                    Logger.e(TAG, "Did not find browser " + mAuthBrowserPackage);
                }
            } catch (Exception e) {
                intent.setPackage(null);
                Logger.e(TAG, "Did not find browser exception " + mAuthBrowserPackage, e);
            }
        }
        mAuthIntent.set(intent);
    }

    public void onResume() {
        if (isUserAuthorized()) {
            mAuthListener.onAuthorized();
        }

        if (mAuthStateManager.getCurrent().getAuthorizationException() != null) {
            String error = mAuthStateManager.getCurrent().getAuthorizationException().errorDescription;
            String exception = mAuthStateManager.getCurrent().getAuthorizationException().getCause() == null ? "null" :
                    mAuthStateManager.getCurrent().getAuthorizationException().getCause().getMessage();
            clearAuthState(AuthStateResetReason.AUTHORIZATION_EXCEPTION_IN_ON_RESUME);
            mAuthListener.onAuthorizeError(error + ": " + exception);
        }
    }

    public void onStopped() {
        if (mExecutor != null) mExecutor.shutdownNow();
    }

    public void onDestroyed() {
        if (mAuthService != null) mAuthService.dispose();
    }

    public void saveAuthEntryPoint(com.washingtonpost.android.paywall.auth.AuthEntryPoint entryPoint) {
        authPreferences.edit()
                .putString(AUTH_ENTRY_POINT_KEY, entryPoint.name())
                .apply();
    }

    public com.washingtonpost.android.paywall.auth.AuthEntryPoint restoreAuthEntryPoint() {
        String entryPoint = authPreferences.getString(AUTH_ENTRY_POINT_KEY, null);
        return entryPoint != null
                ? com.washingtonpost.android.paywall.auth.AuthEntryPoint.valueOf(entryPoint)
                : null;
    }

    public void removeEntryPoint() {
        authPreferences.edit()
                .remove(AUTH_ENTRY_POINT_KEY)
                .apply();
    }

    public com.washingtonpost.android.paywall.auth.AuthEntryPoint restoreAndRemoveAuthEntryPoint() {
        com.washingtonpost.android.paywall.auth.AuthEntryPoint entryPoint = restoreAuthEntryPoint();
        removeEntryPoint();
        return entryPoint;
    }



    public interface AuthListener {
        default void onInitialized(Uri uri, boolean isMagicLink, Uri magicLinkOrResponseData, Bundle extras) {}
        void onAuthorized();
        void onAuthorizeError(String errorMessage);
    }

    public interface TokenRefreshListener {
        void onTokenRefresh();
    }
}
