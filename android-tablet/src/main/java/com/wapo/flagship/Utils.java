package com.wapo.flagship;

import static androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION;

import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import com.wapo.android.commons.util.Logger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.net.MailTo;

import com.wapo.android.commons.util.AppContextUtils;
import com.wapo.flagship.features.articles2.utils.URLHelper;
import com.wapo.flagship.features.settings.AppPreferences;
import com.wapo.flagship.features.shared.activities.SimpleWebViewActivity;
import com.wapo.flagship.util.tracking.Measurement;
import com.wapo.view.FlowableLayout;
import com.wapo.view.share.ShareType;
import com.washingtonpost.android.BuildConfig;
import com.washingtonpost.android.R;

import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.TimeZone;
import java.util.regex.Pattern;

import rx.Subscription;

public class Utils {
    private static final Pattern KindleRegEx = Pattern.compile("(Kindle Fire|KF[A-Z]{2,})");
    private static final String TAG = Utils.class.getSimpleName();
    public static final int oneDayinMilliseconds = 86400000;
    public static final int oneHourinMilliseconds = 3600000;
    private static final String webviewBaseUrl = "www.washingtonpost.com";

    public static String exceptionToString(Throwable e) {
        return Log.getStackTraceString(e);
    }

    public static String inputStreamToString(InputStream inputStream) {
        InputStream stream = BufferedInputStream.class.isInstance(inputStream) ? inputStream : new BufferedInputStream(inputStream);
        Scanner scanner = new Scanner(stream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    public static String join(String[] parts, int startIdx, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIdx; i < parts.length; i++) {
            if (i > startIdx) {
                sb.append(separator);
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    public static boolean deleteFileOrFolder(File targetDir) {
        if (targetDir.isDirectory()) {
            Stack<File> stack = new Stack<File>();
            stack.push(targetDir);
            while (!stack.isEmpty()) {
                File dir = stack.lastElement();
                int count = 0;
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {
                            stack.push(f);
                            count++;
                        } else {
                            if (!f.delete()) {
                                return false;
                            }
                        }
                    }
                }
                if (count == 0) {
                    stack.pop();
                    if (!dir.delete()) {
                        return false;
                    }
                }
            }
            return true;
        }
        return targetDir.delete();
    }

    public static void pipe(InputStream is, OutputStream os) throws IOException {
        byte[] buff = new byte[8 * 1024];
        int len;
        while ((len = is.read(buff)) > 0) {
            os.write(buff, 0, len);
        }
        is.close();
    }

    public static long dateToEDTLabel(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        c.setTime(date);
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DATE);
    }

    public static Date edtLabelToDate(long label) {
        String labelStr = String.valueOf(label);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        try {
            return sdf.parse(labelStr);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String dateToDateString(Date date) {
        return labelToDateString(dateToEDTLabel(date));
    }

    public static String labelToDateString(long label) {
        int year = (int) label / 10000;
        int month = ((int) label % 10000) / 100;
        int day = (int) label % 100;
        StringBuilder sb = new StringBuilder();
        switch (month - 1) {
            case Calendar.JANUARY:
                sb.append("January ");
                break;
            case Calendar.FEBRUARY:
                sb.append("February ");
                break;
            case Calendar.MARCH:
                sb.append("March ");
                break;
            case Calendar.APRIL:
                sb.append("April ");
                break;
            case Calendar.MAY:
                sb.append("May ");
                break;
            case Calendar.JUNE:
                sb.append("June ");
                break;
            case Calendar.JULY:
                sb.append("July ");
                break;
            case Calendar.AUGUST:
                sb.append("August ");
                break;
            case Calendar.SEPTEMBER:
                sb.append("September ");
                break;
            case Calendar.OCTOBER:
                sb.append("October ");
                break;
            case Calendar.NOVEMBER:
                sb.append("November ");
                break;
            case Calendar.DECEMBER:
                sb.append("December ");
                break;
        }
        sb.append(day).append(", ").append(year);
        return sb.toString();
    }

    public static void parseContentType(String contentType, String[] result) {
        if (result == null || contentType == null) {
            return;
        }

        String[] parts = contentType.split(";");
        if (result.length > 0) {
            result[0] = parts[0].trim();
        }

        for (int i = 1; i < parts.length && result.length > 1; i++) {
            String part = parts[i].trim();
            if (part.startsWith("charset")) {
                result[1] = part.substring(part.indexOf("=") + 1);
                break;
            }
        }
    }

    //TODO we should probably consolidate these and use one check for amazon across the app
    public static boolean isKindleFire() {
        return "SD4930UR".equalsIgnoreCase(Build.MODEL) || "Amazon".equals(Build.MANUFACTURER) && KindleRegEx.matcher(Build.MODEL).matches();
    }

    //Slightly different from the above method, this casts a wider net.
    public static boolean isAmazonBuild() {
        return "SD4930UR".equalsIgnoreCase(Build.MODEL) || "Amazon".equals(Build.MANUFACTURER);
    }

    public static boolean isProductFlavorAmazon() {
        return BuildConfig.STORE_TYPE.equals("amazon");
    }

    public static boolean isProductFlavorPlayStore() {
        return BuildConfig.STORE_TYPE.equals("playstore");
    }

    public static void openNetworkSettings(Context context) {
        context.startActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    public static String getShareTypeFromIntent(Intent intent, String shareTypeExtraName) {
        String shareType = intent.getType().contains("message/rfc822") ?
                ShareType.Email.name() :
                intent.getStringExtra(shareTypeExtraName);
        return shareType == null ? "" : shareType;
    }

    public static Intent initMarketIntent(Context context) {
        Intent _goToMarketIntent = new Intent(Intent.ACTION_VIEW);
        if (Utils.isKindleFire()) {
            _goToMarketIntent.setData(Uri.parse("amzn://apps/android?p=" + context.getPackageName()));
        } else {
            _goToMarketIntent.setData(Uri.parse("market://details?id=" + context.getPackageName()));
        }
        return _goToMarketIntent;
    }

    public static void startWeb(String url, Context context) {
        startWeb(url, context, Map.of());
    }

    public static void startWeb(String url, Context context, Map<String, Boolean> extras) {
        if (context == null || TextUtils.isEmpty(url))
            return;

        if (isWapoURL(url, context)) {
            startWebActivity(url, context, extras);
        } else {
            if (AppPreferences.INSTANCE.isOpenWebInAppEnabled()) {
                startWebChromeCustomTab(url, context, false);
            } else {
                startWebBrowser(url, context);
            }
        }

        if (url.contains(Measurement.TRACKING_ID)) {
            Measurement.trackExternalLink(url);
        }
    }

    public static void startWebActivity(String url, Context context, Map<String, Boolean> extras) {
        startWebActivity(
                url,
                context,
                getOrDefault(extras, SimpleWebViewActivity.CAN_PAYWALL, false),
                getOrDefault(extras, SimpleWebViewActivity.NATIVE_GO_BACK, false),
                getOrDefault(extras, SimpleWebViewActivity.PARAM_SUPPORT_SHARE, true),
                getOrDefault(extras, SimpleWebViewActivity.IS_FAILOVER_ORIGINATED, false)
        );
    }

    private static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        if (map.containsKey(key)) return map.get(key);
        return defaultValue;
    }

    public static void startWebActivity(String url, Context context, boolean canPaywall, boolean nativeGoBack, boolean canShare, boolean isFailoverOriginated) {
        Intent i = new Intent(context, SimpleWebViewActivity.class);
        i.putExtra(SimpleWebViewActivity.URL_INTENT_PARAM, setITID(applyBetaUrlIfEnabled(url)));
        i.putExtra(SimpleWebViewActivity.CAN_PAYWALL, canPaywall);
        i.putExtra(SimpleWebViewActivity.NATIVE_GO_BACK, nativeGoBack);
        i.putExtra(SimpleWebViewActivity.PARAM_SUPPORT_SHARE, canShare);
        i.putExtra(SimpleWebViewActivity.IS_FAILOVER_ORIGINATED, isFailoverOriginated);
        context.startActivity(i);
    }
    private static final String BETA_WEBVIEW_URL = "beta.washingtonpost.com";

    private static String applyBetaUrlIfEnabled(String url) {
        if (url == null) return null;
        if (!AppContextUtils.INSTANCE.isDebuggableBuild()) return url;
        try {
            if (AppPreferences.INSTANCE.isBetaWebviewOverrideEnabled()) {
                return url.replace(webviewBaseUrl, BETA_WEBVIEW_URL);
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error applying beta url override", e);
        }
        return url;
    }

    private static String setITID(String url) {
        return URLHelper.appendTrackingITIDParams(url, Measurement.getNavigationBehaviorCache());
    }

    public static void startWebActivity(String url, Context context, boolean canPaywall, boolean nativeGoBack, boolean canShare) {
        startWebActivity(url, context, canPaywall, nativeGoBack, canShare, false);
    }

    public static void startWebActivity(String url, Context context, boolean canPaywall, boolean nativeGoBack) {
        startWebActivity(url, context, canPaywall, nativeGoBack, true);
    }

    public static void startWebActivity(String url, Context context, boolean canPaywall) {
        startWebActivity(url, context, canPaywall, false);
    }

    public static void startWebActivity(String url, Context context) {
        startWebActivity(url, context, false);
    }

    public static void startWebActivityWithFlag(String url, Context context, boolean canPaywall, int flag) {
        Intent i = new Intent(context, SimpleWebViewActivity.class);
        i.putExtra(SimpleWebViewActivity.URL_INTENT_PARAM, setITID(url));
        i.putExtra(SimpleWebViewActivity.CAN_PAYWALL, canPaywall);
        i.setFlags(flag);
        context.startActivity(i);
    }

    public static void startWebActivityWithAction(String url, Context context, boolean canPaywall, String action) {
        if (context instanceof AppCompatActivity
                && ((AppCompatActivity) context).isFinishing()) {
            return;
        }
        Intent i = new Intent(context, SimpleWebViewActivity.class);
        i.putExtra(SimpleWebViewActivity.URL_INTENT_PARAM, setITID(url));
        i.putExtra(SimpleWebViewActivity.CAN_PAYWALL, canPaywall);
        i.setAction(action);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    public static void startWebChromeCustomTab(String url, Context context, boolean newTask) {
        try {
            String supportedPackages = getChromeCustomTabsSupportedPackage(context);
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(context.getResources().getColor(android.R.color.black));
            builder.addDefaultShareMenuItem();
            CustomTabsIntent customTabsIntent = builder.build();
            if (supportedPackages != null) {
                customTabsIntent.intent.setPackage(supportedPackages);
            }
            if (newTask) {
                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            customTabsIntent.launchUrl(context, Uri.parse(url));
        } catch (Exception e) {
            startWebActivityWithFlag(url, context, false, newTask ? Intent.FLAG_ACTIVITY_NEW_TASK : 0);
        }
    }

    public static void startWebChromeCustomTab(String url, Context context) {
        startWebChromeCustomTab(url, context, false);
    }

    public static String getChromeCustomTabsSupportedPackage(Context context) {

        //Use generic URL to avoid the wapo apps to be in the options
        String generalUrl = "http://www.example.com";

        PackageManager pm = context.getPackageManager();

        // Get default VIEW intent handler
        Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(generalUrl));
        ResolveInfo defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0);

        String defaultViewHandlerPackageName = null;

        if (defaultViewHandlerInfo != null) {
            defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName;
        }

        // Get all apps that can handle VIEW intents
        List<ResolveInfo> resolvedActivityList = pm.queryIntentActivities(activityIntent, 0);
        List<String> packagesSupportingCustomTabs = new ArrayList<>();

        for (ResolveInfo info : resolvedActivityList) {
            if (context.getPackageName().equals(info.activityInfo.packageName) || context.getPackageName().equals("com.washingtonpost.rainbow")) {
                continue;
            }
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION);
            serviceIntent.setPackage(info.activityInfo.packageName);
            if (pm.resolveService(serviceIntent, 0) != null) {
                packagesSupportingCustomTabs.add(info.activityInfo.packageName);
            }
        }

        // Now our list contains all apps that can handle both VIEW intents & service calls
        if (packagesSupportingCustomTabs.size() > 0) {

            if (!TextUtils.isEmpty(defaultViewHandlerPackageName) &&
                    packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName)) {
                // Prefer the defined default
                return defaultViewHandlerPackageName;
            } else {
                return packagesSupportingCustomTabs.get(0);
            }
        }

        return null;
    }

    public static void startWebBrowser(String url, Context context) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            context.startActivity(i);
        } catch (Exception e) {
            startWebActivity(url, context);
        }
    }

    public static boolean exists(String path) {
        return new File(path).exists();
    }

    public static void listFileNames(File dir) {
        try {
            if (dir.exists()) {
                File[] fileList = dir.listFiles();
                for (int i = 0; i < fileList.length; i++) {
                    // Recursive call if it's a directory
                    if (fileList[i].isDirectory()) {
                        listFileNames(fileList[i]);
                    } else {
                        Logger.d("WapoDebug", "FileName: " + fileList[i].getAbsolutePath() + " size:" + fileList[i].length());
                    }
                }
            }
        } catch (Exception e) {
            Logger.e("Utils", e.toString());
        }
    }

    public static boolean isWebpSupported() {
        /*if(Build.VERSION.SDK_INT < 15){
            return false;
        }
        return true;*/

        //
        // turn webP images for know b/c they give us too much trouble
        return false;
    }

    public static boolean isMainThread() {
        return Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId();
    }

    public static void assertMainThread() {
        if (!isMainThread()) {
            throw new IllegalStateException("Not a main thread");
        }
    }


    public static void unsubscribe(Subscription... subs) {
        for (Subscription sub : subs) {
            if (sub != null && !sub.isUnsubscribed()) {
                sub.unsubscribe();
            }
        }
    }

    /**
     * Return FLOAT_{OPTION} based on floatPosition recommended
     *
     * @param floatPosition
     * @return
     */
    public static int floatPositionToIntValue(String floatPosition) {
        if (floatPosition == null || floatPosition.equalsIgnoreCase("center")) {
            return FlowableLayout.FLOAT_NONE;
        } else if (floatPosition.equalsIgnoreCase("left")) {
            return FlowableLayout.FLOAT_LEFT;
        } else if (floatPosition.equalsIgnoreCase("right")) {
            return FlowableLayout.FLOAT_RIGHT;
        } else {
            //If it's unknown, we handle as FLOAT_NONE
            return FlowableLayout.FLOAT_NONE;
        }
    }

    @Nullable
    public static String getDisplayDate(@Nullable String input, @NonNull String pattern) {
        try {
            if (input != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(Long.valueOf(input));
                return dateFormat.format(calendar.getTime());
            }
        } catch (Exception e) {
            Logger.d(TAG, "Error parsing date", e);
        }
        return null;
    }

    public static interface Func<R> {
        R run();
    }

    public static interface Func1<T, R> {
        R run(T arg);
    }

    public static interface Func2<T1, T2, R> {
        R run(T1 a1, T2 a2);
    }

    public static interface Func3<T1, T2, T3, R> {
        R run(T1 a1, T2 a2, T3 a3);
    }

    public interface Action {
        void run();
    }

    public static interface Action1<T> {
        void run(T p1);
    }


    public static interface Action2<T1, T2> {
        void run(T1 p1, T2 p2);
    }

    public static interface Action3<T1, T2, T3> {
        void run(T1 p1, T2 p2, T3 p3);
    }

    public static boolean isWapoURL(String url, Context context) {
        if (!TextUtils.isEmpty(url)) {
            String host = Uri.parse(url).getHost();
            if (host != null) {
                return (host.contains(context.getString(R.string.wp_domain)) || host.contains(context.getString(R.string.wp_domain_short)));
            }
        }
        return false;
    }

    public static boolean isWPIntelligenceUrl(String url, Context context) {
        if (!TextUtils.isEmpty(url)) {
            String host = Uri.parse(url).getHost();
            if (host != null) {
                return host.contains(context.getString(R.string.wp_intelligence));
            }
        }
        return false;
    }

    public static Uri getReferrerBelowSdk22(Intent intent) {
        // Returns the referrer on devices running SDK versions lower than 22.
        final String REFERRER_NAME = "android.intent.extra.REFERRER_NAME";
        Uri referrerUri = intent.getParcelableExtra(Intent.EXTRA_REFERRER);
        if (referrerUri != null) {
            return referrerUri;
        }
        String referrer = intent.getStringExtra(REFERRER_NAME);
        if (referrer != null) {
            // Try parsing the referrer URL; if it's invalid, return null
            try {
                return Uri.parse(referrer);
            } catch (android.net.ParseException e) {
                return null;
            }
        }
        return null;
    }

    public static boolean isTodayAFreshDayFromMillis(long millis) {
        if (millis == -1) {
            return true;
        }
        Calendar todayCal = Calendar.getInstance();
        Calendar millisCal = Calendar.getInstance();
        millisCal.setTimeInMillis(millis);
        if (todayCal.get(Calendar.ERA) < millisCal.get(Calendar.ERA)) return false;
        if (todayCal.get(Calendar.ERA) > millisCal.get(Calendar.ERA)) return true;
        if (todayCal.get(Calendar.YEAR) < millisCal.get(Calendar.YEAR)) return false;
        if (todayCal.get(Calendar.YEAR) > millisCal.get(Calendar.YEAR)) return true;
        return todayCal.get(Calendar.DAY_OF_YEAR) > millisCal.get(Calendar.DAY_OF_YEAR);
    }

    public static String processUserAgent(@NonNull Context context,
                                          @Nullable String url,
                                          @Nullable String webViewUserAgent) {
        String propertyName = Measurement.detectAppName();
        if (!TextUtils.isEmpty(url)
                && isWapoURL(url, context)
                && !TextUtils.isEmpty(webViewUserAgent)
                && !TextUtils.isEmpty(propertyName)) {
            return AppContextUtils.INSTANCE.getAppWebUserAgent();
        }
        return webViewUserAgent;
    }

    /**
     * Checks for supported Special Links which redirect outside of the app.
     */
    public static boolean isExternalSpecialLink(String url) {
        return (url != null)
                && (url.startsWith("tel:")
                || url.startsWith("sms:")
                || url.startsWith("smsto:")
                || url.startsWith("mailto:")
                || url.startsWith("geo:"));
    }

    public static boolean isSpecialLink(String url) {
        return (url != null)
                && (url.startsWith("tel:")
                || url.startsWith("sms:")
                || url.startsWith("smsto:")
                || url.startsWith("mailto:")
                || url.startsWith("intent:"));
    }

    public static boolean handleSpecialLink(Activity activity, String url) {
        boolean handled = false;
        if (isSpecialLink(url)) {
            if (activity != null && !activity.isFinishing()) {
                Intent intent;
                if (url.startsWith("mailto:")) {
                    MailTo mailTo = MailTo.parse(url);
                    intent = getMailIntent(mailTo);
                } else {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                }

                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    Intent chooser = Intent.createChooser(intent, "");
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(chooser);
                    handled = true;
                }
            }
        }
        return handled;
    }

    @NonNull
    private static Intent getMailIntent(MailTo mailTo) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] { mailTo.getTo() });
        intent.putExtra(Intent.EXTRA_TEXT, mailTo.getBody());
        intent.putExtra(Intent.EXTRA_SUBJECT, mailTo.getSubject());
        return intent;
    }

    public static LocalDate getLocalDate(@NonNull Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(getDefaultAppTimeZone());
        LocalDate threeTenFormatLocalDate = LocalDate.parse(sdf.format(date));
        return threeTenFormatLocalDate;
    }

    public static Date getDate(@NonNull LocalDate threeTenFormatLocalDate) {
        Date date = DateTimeUtils.toDate(threeTenFormatLocalDate.atStartOfDay(ZoneId.of("EST")).toInstant());
        return date;
    }

    public static TimeZone getDefaultAppTimeZone() {
        return TimeZone.getTimeZone("EST");
    }

    public static String getCompanyNameForPDFLibrary() {
        return "The Washington Post";
    }

    public static String getAdminEmailForPDFLibrary() {
        if (isProductFlavorAmazon()) {
            return "android.admin@washpost.com";
        } else {
            return "julia.beizer@wpost.com";
        }
    }

    public static String getKeyForPDFLibrary() {
        if (isProductFlavorAmazon()) {
            return "J8TO23-4KKOXN-11W52J-1SO588-1JVNFC-GWW4VC";
        } else {
            return "ZCUG8U-Z9H5KL-11W52J-1SO588-ULBLP6-F0EXZG";
        }
    }
}
