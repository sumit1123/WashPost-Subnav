package com.washingtonpost.android.paywall.auth;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.washingtonpost.android.config.domain.models.config.paywall.OAuthConfigStub;
import com.washingtonpost.android.paywall.PaywallService;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;
import net.openid.appauth.connectivity.DefaultConnectionBuilder;

public class PaywallTokenActivity extends Activity {

    private AuthStateManager mStateManager;
    private OAuthConfigStub oAuthConfigStub = PaywallService.getInstance().getOAuthConfigStub();
    private AuthorizationService mAuthService;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progressDialog = new ProgressDialog(this,
                Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH ? android.R.style.Theme_Material_Light_Dialog_Alert : 0);

        progressDialog.setMessage("Authorizing...");
        progressDialog.setCancelable(false);

        mStateManager = AuthStateManager.getInstance(this);

        mAuthService = new AuthorizationService(
                this,
                new AppAuthConfiguration.Builder()
                        .setConnectionBuilder(DefaultConnectionBuilder.INSTANCE)
                        .build());
    }

    @Override
    protected void onStart() {
        super.onStart();
        progressDialog.show();
        receiveAuthorizationResponse(getIntent());
    }

    @Override
    protected void onDestroy() {
        dismissDialog();
        super.onDestroy();
        mAuthService.dispose();
    }


    private void receiveAuthorizationResponse(Intent data) {

        // the stored AuthState is incomplete, so check if we are currently receiving the result of
        // the authorization flow from the browser.
        AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
        AuthorizationException authException = AuthorizationException.fromIntent(data);

        if (response != null || authException != null) {
            if (authException != null && authException.type != 1) {
                //AuthState only records error type 1s, so we need to hardcode this so the app functions properly
                AuthorizationException temp = new AuthorizationException(1, authException.code, authException.error,
                        authException.errorDescription, authException.errorUri, authException.getCause());
                mStateManager.updateAfterAuthorization(response, temp);
            } else {
                mStateManager.updateAfterAuthorization(response, authException);
            }
        }

        if (response != null && response.authorizationCode != null) {
            // authorization code exchange is required
            mStateManager.updateAfterAuthorization(response, authException);
            exchangeAuthorizationCode(response);
        } else {
            dismissDialog();
            finish();
        }
    }

    @MainThread
    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        performTokenRequest(
                authorizationResponse.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException e) {
                        handleCodeExchangeResponse(tokenResponse, e);
                    }
                });
    }

    @MainThread
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

    @WorkerThread
    private void handleCodeExchangeResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {

        if (authException != null && authException.type != 2) {
            //AuthState only records error type 2s, so we need to hardcode this so the app functions properly
            AuthorizationException temp = new AuthorizationException(2, authException.code, authException.error,
                    authException.errorDescription, authException.errorUri, authException.getCause());
            mStateManager.updateAfterTokenResponse(tokenResponse, temp);
        } else {
            mStateManager.updateAfterTokenResponse(tokenResponse, authException);
        }
        dismissDialog();
        finish();
    }

    private void dismissDialog() {

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
