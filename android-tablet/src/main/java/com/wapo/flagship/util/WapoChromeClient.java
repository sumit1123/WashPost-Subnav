package com.wapo.flagship.util;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

/**
 * Created with IntelliJ IDEA.
 * User: maxx
 * Date: 7/15/13
 * Time: 8:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class WapoChromeClient extends WebChromeClient {
    private View customView;
    private CustomViewCallback customViewCallback;
    private int originalOrientation;
    private int originalSystemUiVisibility;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    public final static int FILE_CHOOSER_RESULT_CODE = 10;
    private Activity activity;

    public WapoChromeClient(Activity activity) {
        this.activity = activity;
    }

    public interface ProgressNotificator {
        void onProgress(WebView view, int newProgress);
    }

    private ProgressNotificator progressNotificator;

    public void setProgressNotificator(ProgressNotificator progressNotificator) {
        this.progressNotificator = progressNotificator;
    }

    public ValueCallback<Uri[]> getUploadMessageAboveL() {
        return uploadMessageAboveL;
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        uploadMessageAboveL = filePathCallback;
        openImageChooserActivity();
        return true;
    }

    private void openImageChooserActivity() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        if(activity != null && !activity.isFinishing()) {
            activity.startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILE_CHOOSER_RESULT_CODE);
        }
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
//            String str = consoleMessage.message();
//            Toast t = Toast.makeText(ctx, "Error: " + str, 3000);
//            t.show();
        }
        return super.onConsoleMessage(consoleMessage);

    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        if (progressNotificator != null) {
            progressNotificator.onProgress(view, newProgress);
        }
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        super.onShowCustomView(view, callback);
        ContextWrapper wrapper = (ContextWrapper) view.getContext();
        activity = (wrapper.getBaseContext() instanceof Activity) ? (Activity) wrapper.getBaseContext() : null;
        if (activity != null) {
            customView = view;
            customViewCallback = callback;
            originalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
            originalOrientation = activity.getRequestedOrientation();
            ((FrameLayout) activity.getWindow().getDecorView()).addView(customView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            activity.getWindow().getDecorView().setSystemUiVisibility(view.SYSTEM_UI_FLAG_IMMERSIVE | view.SYSTEM_UI_FLAG_FULLSCREEN
                    | view.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | view.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    view.SYSTEM_UI_FLAG_LAYOUT_STABLE | view.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();
        if (activity != null) {
            ((FrameLayout) activity.getWindow().getDecorView()).removeView(customView);
            customView = null;
            activity.getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
            activity.setRequestedOrientation(originalOrientation);
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
            activity = null;
        }
    }
}