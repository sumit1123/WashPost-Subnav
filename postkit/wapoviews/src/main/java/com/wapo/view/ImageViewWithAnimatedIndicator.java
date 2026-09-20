package com.wapo.view;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader;

/**
 * Created by maxx on 9/29/13.
 */
public class ImageViewWithAnimatedIndicator extends RelativeLayout implements ImageViewWithLoadCallbacks.ImageLoadCallback {

    private static final String TAG = ImageViewWithAnimatedIndicator.class.getName();

    ProgressBar progress;
    ImageViewWithLoadCallbacks imageView;
    private String _url;
    private AnimatedImageLoader _imageLoader;
    private boolean _bottomCropped;
    private boolean isFitXY;
    private boolean isProgressEnabled;

    public ImageViewWithAnimatedIndicator(Context context) {
        super(context);
        init(context);
    }

    public ImageViewWithAnimatedIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImageViewWithAnimatedIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {

        progress = new ProgressBar(context);
        isProgressEnabled = true;
        RelativeLayout.LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_VERTICAL, 1);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL, 1);
        addView(progress, lp);
        imageView = new ImageViewWithLoadCallbacks(context);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(200, 200));
        addView(imageView, new LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        imageView.setVisibility(VISIBLE);
        progress.setVisibility(INVISIBLE);
        imageView.setImageLoadedCallback(this);
    }

    public void setImageUrl(String url, AnimatedImageLoader imageLoader, boolean bottomCropped) {
        setImageUrl(url, imageLoader, bottomCropped, isFitXY);
    }

    // FitXY is used to make the image with same width and height here based on aspect ratio for phones
    public void setImageUrl(String url, AnimatedImageLoader imageLoader, boolean bottomCropped, boolean isFitXY) {
        _url = url;
        _imageLoader = imageLoader;
        _bottomCropped = bottomCropped;
        this.isFitXY = isFitXY;
        imageView.setBottomCropped(bottomCropped);
        imageView.setIsFitXY(isFitXY);
        imageView.setImageUrl(url, imageLoader);
    }

    public void disableProgressBar() {
        isProgressEnabled = false;
    }

    @Override
    public void onLoaded(boolean withError) {
        imageView.setVisibility(VISIBLE);
        if (isProgressEnabled) {
            progress.setVisibility(GONE);
        }
    }

    @Override
    public void onLoadStarted() {
        imageView.setVisibility(INVISIBLE);
        if (isProgressEnabled) {
            progress.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        if (_url == null) {
            return;
        }

        if (visibility != View.VISIBLE) {
            imageView.dropImage();
        } else {
            setImageUrl(_url, _imageLoader, _bottomCropped, isFitXY);
        }
    }
}