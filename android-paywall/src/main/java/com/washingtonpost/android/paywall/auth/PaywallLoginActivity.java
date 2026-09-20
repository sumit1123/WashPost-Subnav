package com.washingtonpost.android.paywall.auth;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.wapo.android.commons.logs.EventLog;
import com.washingtonpost.android.paywall.PaywallService;
import com.washingtonpost.android.paywall.R;
import com.washingtonpost.android.paywall.api.VerifyState;
import com.washingtonpost.android.paywall.util.PaywallConstants;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class PaywallLoginActivity extends AppCompatActivity implements AuthHelper.AuthListener {

    private static final String TAG = "PaywallLoginActivity";
    private static final String ACCOUNT_CREATED_PARAM = "account_created";
    private AuthHelper mAuthHelper;
    private ProgressDialog progressDialog;
    private Uri magicLinkOrSocialRedirectResponseData;
    private boolean isDeeplink = false;
    private String promoId;
    private String trialType;
    private boolean isSignUp = false;
    private boolean isReturningFromUserCancel = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PaywallService.getInstance() == null) {
            finish();
            return;
        }

        if (getIntent() != null && getIntent().getExtras() != null) {
            String magicLinkData = getIntent().getExtras().getString(AuthIntentBuilder.getMAGIC_LINK_DATA());
            magicLinkOrSocialRedirectResponseData = magicLinkData != null ? Uri.parse(magicLinkData) : null;
            isDeeplink = getIntent().getExtras().getBoolean(AuthIntentBuilder.getIS_MAGICLINK_AUTH());
            promoId = getIntent().getExtras().getString(AuthIntentBuilder.getPROMO_ID());
            trialType = getIntent().getExtras().getString(AuthIntentBuilder.getTRIAL_TYPE());
            isSignUp = getIntent().getExtras().getBoolean(AuthIntentBuilder.getIS_SIGN_UP());
            isReturningFromUserCancel = getIntent().getBooleanExtra(PaywallConstants.AUTH_EXTRA_FAILED, false);
        }

        PaywallService.getConnector().logW(new EventLog.Builder().setMessage("login started"));
        progressDialog = new ProgressDialog(this,
                Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH ? android.R.style.Theme_Material_Light_Dialog_Alert : 0);

        progressDialog.setMessage("Logging you in...");

        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (!isFinishing()) {
                    finish();
                }
            }
        });

        if (!(getApplication() instanceof AuthApplication)) {
            throw new IllegalStateException("App must implement AuthApplication");
        }

        if (PaywallService.getInstance().isWpUserLoggedIn()) {
            PaywallService.getConnector().logE(new EventLog.Builder().setMessage("login launched while user logged in"));
            PaywallService.getInstance().logOutCurrentUser();
        }

        AuthApplication authApplication = (AuthApplication) getApplication();
        // initialize map empty and add params if any
        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put(AuthIntentBuilder.getPROMO_ID(), promoId);
        additionalParams.put(AuthIntentBuilder.getTRIAL_TYPE(), trialType);
        mAuthHelper = new AuthHelper(this);
        mAuthHelper.startAuthTask(this, this, authApplication.shouldUseCustomTab(),
                authApplication.getAuthBrowserPackageName(),
                authApplication.getAppRedirectScheme(),
                additionalParams,
                isDeeplink, magicLinkOrSocialRedirectResponseData, isSignUp);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAuthHelper.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        mAuthHelper.onStopped();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAuthHelper != null) {
            mAuthHelper.onDestroyed();
        }
    }

    @Override
    public void onAuthorized() {
        PaywallService.getPaywallPrefHelper().setHasMigratedToOAuth();
        //get user subscription info
        String accessToken = mAuthHelper.getAccessToken();

        if (accessToken == null || PaywallService.getConnector() == null || PaywallService.getConnector().getClientId() == null) {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Cannot complete login")
                    .set("access_token_is_null", (accessToken == null))
                    .set("paywall_connector", (PaywallService.getConnector() == null))
                    .set("client_id_is_null", (PaywallService.getConnector().getClientId() == null)));
            Toast.makeText(this, "Error logging in. Please try again", Toast.LENGTH_SHORT).show();
            finish();
        }
        //process value from auth response
        boolean accountWasCreated = false;
        if (mAuthHelper.getAuthState().getLastAuthorizationResponse() != null) {
            String booleanValue = mAuthHelper.getAuthState().getLastAuthorizationResponse().additionalParameters.get(ACCOUNT_CREATED_PARAM);
            accountWasCreated = booleanValue == null ? false : Boolean.valueOf(booleanValue);
        }

        //process user data from id_token
        boolean success = PaywallService.getInstance().getApiServiceInstance().processUserFromJWTClaim(mAuthHelper.getDecodedJWT(mAuthHelper.getIdToken()));
        if (success) {
            PaywallService.getOmniture().trackSignInComplete(accountWasCreated, true);
            Intent intent = getIntent();
            Bundle extras = intent != null ? intent.getExtras() : null;
            PaywallService.getConnector().startOnboardingSubscriber(extras);
            PaywallService.getConnector().syncAlertTopicsWithPreferencesApi();
            PaywallService.getConnector().logD(new EventLog.Builder()
                    .setMessage("User login success")
                    .set("sub_status", PaywallService.getInstance().getSubStatus()));
            if (PaywallService.getInstance().isPremiumUser()) {
                PaywallService.getInstance().dispatchVerifySub(VerifyState.NeedsVerification.INSTANCE);
            }
            setResult(RESULT_OK);
            finish();
        } else {
            PaywallService.getConnector().logE(new EventLog.Builder()
                    .setMessage("Unable to parse id_token for user info, calling profile API instead"));
            //if unable to parse id_token, call profile API instead
            progressDialog.show();
            new FetchUserProfileTask(this, accessToken, PaywallService.getConnector().getClientId(), accountWasCreated).execute();
        }
    }


    @Override
    public void onAuthorizeError(String message) {
        if (!message.isEmpty()) {
            PaywallService.getConnector().logE(new EventLog.Builder().setMessage(message));
            Toast.makeText(this, getText(R.string.pw_error_unknown), Toast.LENGTH_SHORT).show();
        } else {
            PaywallService.getConnector().logW(new EventLog.Builder().setMessage("User canceled login process"));
        }
        finish();
    }

    private static class FetchUserProfileTask extends AsyncTask<Void, Void, Boolean> {

        private WeakReference<PaywallLoginActivity> paywallLoginActivityWeakReference;
        private String accessToken;
        private String clientId;
        private boolean accountWasCreated;


        FetchUserProfileTask(PaywallLoginActivity paywallLoginActivity, String accessToken, String clientId, boolean accountWasCreated) {
            this.paywallLoginActivityWeakReference = new WeakReference<>(paywallLoginActivity);
            this.accessToken = accessToken;
            this.clientId = clientId;
            this.accountWasCreated = accountWasCreated;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            return PaywallService.getInstance().getApiServiceInstance().getUserProfile(accessToken, clientId);
        }

        @Override
        protected void onPostExecute(Boolean success) {

            if (paywallLoginActivityWeakReference != null && paywallLoginActivityWeakReference.get() != null) {
                if (!paywallLoginActivityWeakReference.get().isFinishing()) {
                    paywallLoginActivityWeakReference.get().progressDialog.dismiss();
                    if (success != null && success) {
                        PaywallService.getOmniture().trackSignInComplete(accountWasCreated, true);
                        PaywallService.getConnector().startOnboardingSubscriber(null);
                        PaywallService.getConnector().logD(new EventLog.Builder()
                                .setMessage("User login success")
                                .set("sub_status", PaywallService.getInstance().getSubStatus()));
                        if (PaywallService.getInstance().isPremiumUser()) {
                            PaywallService.getInstance().dispatchVerifySub(VerifyState.NeedsVerification.INSTANCE);
                        }
                        paywallLoginActivityWeakReference.get().setResult(RESULT_OK);
                    } else {
                        paywallLoginActivityWeakReference.get().mAuthHelper.clearAuthState(AuthHelper.AuthStateResetReason.PROFILE_FAILURE);
                        Toast.makeText(paywallLoginActivityWeakReference.get(), "Error logging in. Please try again", Toast.LENGTH_SHORT).show();
                        PaywallService.getConnector().logE(new EventLog.Builder()
                                .setMessage("Error getting user info from /profile during login"));
                    }
                    paywallLoginActivityWeakReference.get().finish();
                }
            }
        }
    }
}
