package com.wapo.flagship.network;

import android.os.Build;
import com.wapo.android.commons.util.TLSSocketFactory;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import com.wapo.flagship.model.ArticleMeta;
import com.wapo.android.commons.util.Logger;
import com.washingtonpost.android.config.domain.manager.ConfigManager;
import com.washingtonpost.android.config.domain.models.config.ImageServiceConfig;
import com.washingtonpost.android.volley.AuthFailureError;
import com.washingtonpost.android.volley.Request;
import com.washingtonpost.android.volley.toolbox.HttpStack;
import com.washingtonpost.android.volley.toolbox.HurlStack;
import com.washingtonpost.android.volley.toolbox.ImageRequestMarker;

import org.apache.http.HttpResponse;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;

public class HurlStackDispatcher implements HttpStack {
    private final HurlStack _hurlStack;
    private final HurlStack _hurlStackRedirect;
    private final HurlStack _hurlStackImgRedirect;

    public HurlStackDispatcher() {
        SSLSocketFactory sslSocketFactory = null;
        try {
            sslSocketFactory = (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                ? new TLSSocketFactory() : new SimpleSSLSocketFactory();
        } catch (GeneralSecurityException e) {
            Logger.e(HurlStackDispatcher.class.getName(), Utils.exceptionToString(e));
        }

        _hurlStack = new HurlStack(null, sslSocketFactory);
        _hurlStackRedirect = new HurlStack(
                new HurlStack.UrlRewriter() {
                    @Override
                    public String rewriteUrl(String url) {
                        String originalUrl = url;
                        String newUrl;
                        boolean isUUIDBased = url.startsWith(ArticleMeta.UUID_URL_PREFIX);
                        if (isUUIDBased) {
                            originalUrl = originalUrl.replace(ArticleMeta.UUID_URL_PREFIX, "");
                        }
                        newUrl = ConfigManager.Companion.getInstance().getConfig().createSingleArticleUrl(originalUrl, isUUIDBased);
                        Logger.d("[dispatch]", String.format("%s -> %s", originalUrl, newUrl));
                        return newUrl;
                    }
                },
                sslSocketFactory
        );
        _hurlStackImgRedirect = new HurlStack(
                new HurlStack.UrlRewriter() {
                    @Override
                    public String rewriteUrl(String originalUrl) {
                        Logger.d("ImageService","Rewriting Image URL");
                        String newUrl = originalUrl;
                        try {
                            final ImageServiceConfig imageServiceConfig= FlagshipApplication.getInstance().getAssignedImageServiceConfig();
                            newUrl = ConfigManager.Companion.getInstance().getConfig().createImageRequestUrl(originalUrl, imageServiceConfig);
                            Logger.d("ImageService","Old URL: "+originalUrl+", Modified URL: "+newUrl);
                        } catch (Exception e) {
                            Logger.e("ImageService", "Error with imageURL " + originalUrl, e);
                        }
                        return newUrl;
                    }
                },
                sslSocketFactory
        );
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        if (((Object)request).getClass().getAnnotation(DataServiceRequest.class) != null) {
            return _hurlStackRedirect.performRequest(request, additionalHeaders);
        } else if(((Object)request).getClass().getAnnotation(ImageRequestMarker.class) != null){
            return _hurlStackImgRedirect.performRequest(request,additionalHeaders);
        }
        return _hurlStack.performRequest(request, additionalHeaders);
    }
}
