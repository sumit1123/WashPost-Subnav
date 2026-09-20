package com.wapo.flagship.util;

import static com.wapo.flagship.features.articles2.activities.ArticlesParcelKt.INLINE_LINK_ORIGINATED;
import static com.wapo.flagship.features.articles2.activities.ArticlesParcelKt.LUF_OUTCOME_POST_ORIGINATED;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.wapo.android.commons.util.URLParser;
import com.wapo.flagship.Utils;
import com.wapo.flagship.features.articles2.activities.ArticlesParcel;
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor;
import com.washingtonpost.android.R;

import java.lang.ref.WeakReference;
import java.util.regex.Pattern;

/**
 * Created by kilarib on 2/3/14.
 */
public class WPUrlAnalyser {
    private WeakReference<AnalysedUrlListener> analysedUrlListener;
    private static WPUrlAnalyser instance;

    private String acqOrCheckoutRegexPattern = "https:\\/\\/(subscribe.washingtonpost.com|www.washingtonpost.com\\/subscribe|subs-stage.washingtonpost.com|subscribe.digitalink.com)\\/(\\s*$|acq.*|checkout)";

    public static WPUrlAnalyser init() {
        instance = new WPUrlAnalyser();
        return instance;
    }

    public static WPUrlAnalyser getWPUrlAnalyser() {
        if (instance == null) {
            throw new IllegalStateException("WPUrlAnalyser must be initialized");
        }
        return instance;
    }

    public void analyseAndStartIntent(Context context, String startUrl, String source) {
        Uri uri = Uri.parse(startUrl);
        String scheme = uri.getScheme();
        if (scheme != null) {
            switch (scheme) {
                case "http":
                case "https":
                case "ftp": {
                    if (uri.getHost() != null && canTryToOpenArticleNatively(startUrl, context)) {
                        launchArticleIntent(context, startUrl, source);
                        return;
                    }
                }
                break;
                case "tel": {
                    Intent intent = new Intent(Intent.ACTION_DIAL, uri);
                    PackageManager packageManager = context.getPackageManager();
                    if (intent.resolveActivity(packageManager) != null) {
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context, R.string.feature_not_supported, Toast.LENGTH_LONG).show();
                    }
                    return;
                }
            }
        }
        Utils.startWeb(startUrl, context);
        if (analysedUrlListener != null && analysedUrlListener.get() != null) {
            analysedUrlListener.get().onCancelLoader();
        }
    }

    private void launchArticleIntent(Context context, String startUrl, String source) {
        Intent result = ArticlesParcel.builder()
                .setArticleSingleUrl(startUrl)
                .inlineLinkOriginated(TextUtils.equals(source, INLINE_LINK_ORIGINATED))
                .lufOutcomePostOriginated(TextUtils.equals(source, LUF_OUTCOME_POST_ORIGINATED))
                .buildIntent(context);
        if (analysedUrlListener != null && analysedUrlListener.get() != null) {
            analysedUrlListener.get().onModifyLaunchIntent(result);
        }
        context.startActivity(result);
    }

    /**
     * In this method use a list of conditions to determine that URL should be opened natively.
     * Note, this method is assuming that any URL with WaPo domain is an article URL that
     * can be tried to open natively unless deemed otherwise by the filters
     *
     * Do not natively open crosswords or article comment URLs
     *
     * @param url
     * @param context
     * @return true if a non-null input url has a washingtonpost domain and does not match the listed conditions
     */
    public boolean canTryToOpenArticleNatively(@NonNull String url, Context context) {
        boolean canTryToOpenNatively = true;
        boolean isWapoURL = Utils.isWapoURL(url, context);
        URLParser urlParser = new URLParser(url);
        if (!isWapoURL
                || DeepLinksProcessor.INSTANCE.shouldDelegateToAppWebView(urlParser)
        ) {
            canTryToOpenNatively = false;
        }
        return canTryToOpenNatively;
    }

    public void setAnalysedUrlListener(AnalysedUrlListener urlListener) {
        this.analysedUrlListener = new WeakReference<>(urlListener);
    }

    public boolean isAcqOrCheckoutUrl(String url) {
        return Pattern.matches(acqOrCheckoutRegexPattern, url);
    }

    public interface AnalysedUrlListener {
        void onCancelLoader();

        void onModifyLaunchIntent(@NonNull Intent intent);
    }
}
