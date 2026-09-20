/*
 * Copyright (c) 2019. The Washington Post
 */
package com.wapo.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import com.wapo.android.commons.util.Logger;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;

import com.wapo.android.commons.util.AppContextUtils;

public class NestedScrollWebView extends WebView implements NestedScrollingChild {

    public static final String TAG = NestedScrollWebView.class.getSimpleName();
    private static final float TEXT_ZOOM_FACTOR = 0.89f;
    public static final float ANGLE_THRESHOLD = 0.45f;
    public static final int DISTANCE_THRESHOLD = 100;

    private int mLastMotionY;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedYOffset;
    private NestedScrollingChildHelper mChildHelper;
    private boolean isAtTop = true;
    private OnScrollChangedListener onScrollChangedListener;
    private PageLoadingListener pageLoadingListener;
    private boolean contentScrollHorizontally;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalOrientation;
    private int originalSystemUiVisibility;
    private Point touchDownPoint;

    public NestedScrollWebView(Context context) {
        super(context);
        initNestedScrolling();
    }

    public NestedScrollWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initNestedScrolling();
    }

    public NestedScrollWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initNestedScrolling();
    }

    private void initNestedScrolling() {
        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    public void initWebView() {
        initWebView(true, true, true, true, true, true);
    }

    public void initWebView(boolean setJavaScriptEnabled, boolean setAllowContentAccess, boolean setDomStorageEnabled, boolean setLoadWithOverviewMode, boolean setUseWideViewPort, boolean setUseTextZoom) {
        WebSettings webSettings = getSettings();
        webSettings.setJavaScriptEnabled(setJavaScriptEnabled);
        webSettings.setAllowContentAccess(setAllowContentAccess);
        webSettings.setDomStorageEnabled(setDomStorageEnabled);
        webSettings.setLoadWithOverviewMode(setLoadWithOverviewMode);
        webSettings.setUseWideViewPort(setUseWideViewPort);
        int textZoom = webSettings.getTextZoom();
        textZoom = textZoom > 100 ? 100 : textZoom;
        if (setUseTextZoom) {
            webSettings.setTextZoom(textZoom);
        }

        setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (pageLoadingListener != null) {
                    return pageLoadingListener.shouldOverrideUrlLoading(view, url, false);
                }
                return true;
            }

            @Override
            @RequiresApi(Build.VERSION_CODES.N)
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (pageLoadingListener != null) {
                    return pageLoadingListener.shouldOverrideUrlLoading(view, request.getUrl().toString(), request.isRedirect());
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (pageLoadingListener != null) {
                    pageLoadingListener.onPageStarted(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (pageLoadingListener != null) {
                    pageLoadingListener.onPageFinished(url);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (pageLoadingListener != null) {
                    pageLoadingListener.onReceiveError(errorCode, description);
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (pageLoadingListener != null) {
                    pageLoadingListener.onReceivedHttpError(view, request, errorResponse);
                }
            }
        });
        setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (pageLoadingListener != null) {
                    pageLoadingListener.onProgressChanged(newProgress);
                }
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (BuildConfig.DEBUG) {
                    Logger.d(TAG, consoleMessage.message() + " -- From line "
                            + consoleMessage.lineNumber() + " of "
                            + consoleMessage.sourceId());
                }
                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
                Activity activity = getActivity();
                if (activity != null) {
                    customView = view;
                    customViewCallback = callback;
                    originalSystemUiVisibility = activity.getWindow().getDecorView().getSystemUiVisibility();
                    originalOrientation = activity.getRequestedOrientation();
                    ((FrameLayout) activity.getWindow().getDecorView()).addView(customView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                    activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                }

            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();
                Activity activity = getActivity();
                if (activity != null) {
                    ((FrameLayout) activity.getWindow().getDecorView()).removeView(customView);
                    customView = null;
                    activity.getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
                    activity.setRequestedOrientation(originalOrientation);
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }
            }
        });

        if (AppContextUtils.INSTANCE.isDebuggableBuild() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    private Activity getActivity() {
        Context context = this.getContext();
        return (context instanceof Activity) ? (Activity) context : null;

    }

    public void setOnScrollChangedListener(OnScrollChangedListener onScrollChangedListener) {
        this.onScrollChangedListener = onScrollChangedListener;
    }

    public void setPageLoadingListener(PageLoadingListener pageLoadingListener) {
        this.pageLoadingListener = pageLoadingListener;
    }

    public void setUserAgent(String userAgent) {
        if (userAgent != null) {
            getSettings().setUserAgentString(userAgent);
        }
    }

    public void destroy() {
        stopLoading();
        clearHistory();
        super.destroy();
    }

    public boolean isAtTop() {
        return isAtTop;
    }

    public boolean canContentScrollHorizontally() {
        return contentScrollHorizontally;
    }

    private void determineScrollBlock(MotionEvent ev) {
        float deltaX = Math.abs(touchDownPoint.x - ev.getX());
        float deltaY = Math.abs(touchDownPoint.y - ev.getY());
        float tg = deltaY / deltaX;
        boolean disallowParent = false;

        if (deltaX > DISTANCE_THRESHOLD && tg < ANGLE_THRESHOLD) {
            // Handling horizontal scroll
            int directionX = (int) (touchDownPoint.x - ev.getX());
            disallowParent = canScrollHorizontally(directionX) || canContentScrollHorizontally();
        } else if (deltaY > DISTANCE_THRESHOLD && tg >= ANGLE_THRESHOLD) {
            // Handling vertical scroll
            int directionY = (int) (touchDownPoint.y - ev.getY());
            disallowParent = canScrollVertically(directionY);
        }
        getParent().requestDisallowInterceptTouchEvent(disallowParent);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
        if (clampedX) {
            // content is not scrollable.
            contentScrollHorizontally = false;
        } else {
            // // content is scrollable.
            contentScrollHorizontally = true;
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        isAtTop = t == 0;
        if (onScrollChangedListener != null) {
            onScrollChangedListener.onScrollChanged(l, t, oldl, oldt);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;

        MotionEvent trackedEvent = MotionEvent.obtain(event);

        final int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            contentScrollHorizontally = true;
            mNestedYOffset = 0;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        event.offsetLocation(0, mNestedYOffset);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = y;
                touchDownPoint = new Point(x, y);
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                result = super.onTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                    determineScrollBlock(event);
                int deltaY = mLastMotionY - y;

                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1];
                    trackedEvent.offsetLocation(0, mScrollOffset[1]);
                    mNestedYOffset += mScrollOffset[1];
                }

                mLastMotionY = y - mScrollOffset[1];

                int oldY = getScrollY();
                int newScrollY = Math.max(0, oldY + deltaY);
                int dyConsumed = newScrollY - oldY;
                int dyUnconsumed = deltaY - dyConsumed;

                if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, mScrollOffset)) {
                    mLastMotionY -= mScrollOffset[1];
                    trackedEvent.offsetLocation(0, mScrollOffset[1]);
                    mNestedYOffset += mScrollOffset[1];
                }

                result = super.onTouchEvent(trackedEvent);
                trackedEvent.recycle();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopNestedScroll();
                result = super.onTouchEvent(event);
                break;
        }
        return result;
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    public interface OnScrollChangedListener {
        void onScrollChanged(int l, int t, int oldl, int oldt);
    }

    public interface PageLoadingListener {
        void onProgressChanged(int newProgress);

        void onPageStarted(String url);

        void onPageFinished(String url);

        void onReceiveError(int errorCode, String description);

        void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse);

        boolean shouldOverrideUrlLoading(WebView view, String url, boolean isRedirect);
    }
}
