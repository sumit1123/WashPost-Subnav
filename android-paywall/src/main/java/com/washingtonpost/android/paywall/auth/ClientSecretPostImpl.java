package com.washingtonpost.android.paywall.auth;

import android.os.Build;
import androidx.annotation.NonNull;

import com.wapo.android.commons.logs.EventLog;
import com.washingtonpost.android.paywall.PaywallService;

import net.openid.appauth.ClientAuthentication;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.wapo.android.commons.constants.HeadersKt.*;
import static net.openid.appauth.Preconditions.checkNotNull;

/**
 * Implementation of the client authentication method 'client_secret_post'.
 *
 * @see "OpenID Connect Core 1.0, Section 9
 * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.9>"
 */
public class ClientSecretPostImpl implements ClientAuthentication {
    /**
     * Name of this authentication method.
     *
     * @see "OpenID Connect Core 1.0, Section 9
     * <https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.9>"
     */
    public static final String NAME = "client_secret_post";
    static final String PARAM_CLIENT_ID = "client_id";
    static final String PARAM_CLIENT_SECRET = "client_secret";
    public final static String TAG = ClientSecretPostImpl.class.getSimpleName();

    @NonNull
    private String mClientSecret;

    /**
     * Creates a {@link ClientAuthentication} which will use the client authentication method
     * `client_secret_post`.
     */
    public ClientSecretPostImpl(@NonNull String clientSecret) {
        mClientSecret = checkNotNull(clientSecret, "clientSecret cannot be null");
    }

    @Override
    public final Map<String, String> getRequestParameters(@NonNull String clientId) {
        Map<String, String> additionalParameters = new HashMap<>();
        additionalParameters.put(PARAM_CLIENT_ID, clientId);
        additionalParameters.put(PARAM_CLIENT_SECRET, mClientSecret);
        return additionalParameters;
    }

    @Override
    public final Map<String, String> getRequestHeaders(@NonNull String clientId) {
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put(CLIENT_IP, PaywallService.getConnector().getIpAddress());
        paramsMap.put(CLIENT_APP, PaywallService.getConnector().getAppName());

        if (PaywallService.getConnector().getAppName().equals("android-rainbow"))
            paramsMap.put(REQUEST_ID, "MAR-" + UUID.randomUUID().toString());
        else if (PaywallService.getConnector().getAppName().equals("android-classic"))
            paramsMap.put(REQUEST_ID, "MAC-" + UUID.randomUUID().toString());

        PaywallService.getConnector().logD(new EventLog.Builder().setMessage("Token Request ID=" + paramsMap.get("Request-ID")));

        paramsMap.put(DEVICE_ID, PaywallService.getConnector().getDeviceId());
        paramsMap.put(INSTALL_ID, PaywallService.getConnector().getDeviceId());
        paramsMap.put(CLIENT_USER_AGENT, PaywallService.getConnector().getUserAgent());
        paramsMap.put(CLIENT_APP_VERSION, PaywallService.getConnector().getAppVersion());
        paramsMap.put(OS_VERSION, String.valueOf(Build.VERSION.SDK_INT));
        paramsMap.put(DEVICE_NAME, Build.MANUFACTURER + "-" + Build.MODEL);

        return paramsMap;
    }
}
