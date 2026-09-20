package com.wapo.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.washingtonpost.android.volley.VolleyError;
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader;
import com.washingtonpost.android.volley.toolbox.NetworkAnimatedImageView;

/**
 * Handles fetching an image from a URL as well as the life-cycle of the
 * associated request.
 */
public class ImageViewWithLoadCallbacks extends NetworkAnimatedImageView {

    public interface ImageLoadCallback {
        void onLoaded(boolean withError);
        void onLoadStarted();
    }

    private ImageLoadCallback mImageLoadedCallback;
    private final Matrix matrix;
    private boolean frameSet;
    private boolean bottomCropped;
    private boolean isFitXY;
    private volatile boolean imageDropped;

    public ImageViewWithLoadCallbacks(Context context) {
        this(context, null);
    }

    public ImageViewWithLoadCallbacks(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageViewWithLoadCallbacks(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setScaleType(ImageView.ScaleType.MATRIX);
        this.matrix = new Matrix();
    }

    public ImageLoadCallback getImageLoadedCallback() {
        return mImageLoadedCallback;
    }

    public void setImageLoadedCallback(ImageLoadCallback imageLoadedCallback) {
        this.mImageLoadedCallback = imageLoadedCallback;
    }

    public boolean isBottomCropped() {
        return bottomCropped;
    }

    public void setBottomCropped(boolean bottomCropped) {
        this.bottomCropped = bottomCropped;
        updateScaleType();
    }

    public boolean isFitXY() {
        return isFitXY;
    }

    public void setIsFitXY(boolean isFitXY) {
        this.isFitXY = isFitXY;
        updateScaleType();
    }

    private void updateScaleType() {
        setScaleType(bottomCropped ? ImageView.ScaleType.MATRIX : isFitXY ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
    }


    @Override
    public void setImageUrl(String url, AnimatedImageLoader imageLoader, int priority) {
        imageDropped = false;
        super.setImageUrl(url, imageLoader, priority);
    }

    public void dropImage() {
        imageDropped = true;
        cancelRequest();
    }

    protected void loadImageIfNecessary(final boolean isInLayoutPass) {
        if (imageDropped) {
            return;
        }

        if (mImageLoadedCallback != null) {
            mImageLoadedCallback.onLoadStarted();
        }
        super.loadImageIfNecessary(isInLayoutPass);
    }

    @Override
    protected void onDispatchErrorResponse(VolleyError error) {
        super.onDispatchErrorResponse(error);
        if (mImageLoadedCallback != null) {
            mImageLoadedCallback.onLoaded(true);
            mImageLoadedCallback = null;
        }
    }

    @Override
    protected void onDispatchSuccessResponse(AnimatedImageLoader.AnimatedImageContainer response, boolean isImmediate) {
        super.onDispatchSuccessResponse(response, isImmediate);
        if (response.getData() != null && !imageDropped) {
            if (mImageLoadedCallback != null) {
                mImageLoadedCallback.onLoaded(false);
                mImageLoadedCallback = null;
            }
        }
    }

    /**
     * Sets a drawable as the content of this ImageView.
     *
     * @param drawable The drawable to set
     */
    @Override
    public void setImageDrawable(Drawable drawable) {
        if (!bottomCropped) {
            super.setImageDrawable(drawable);
            return;
        }
        if (drawable != null && !drawable.equals(getDrawable())) {
            final float dWidth = drawable.getIntrinsicWidth();

            final float frameWidth = getWidth() - getPaddingLeft() - getPaddingRight();

            cropImage(frameWidth, dWidth);
        }
        super.setImageDrawable(drawable);
    }

    @SuppressWarnings("ConstantConditions")
    private void cropImage(float frameWidth, float dWidth) {
        //Scale is always based on the width since we are cropping the bottom
        float scaleFactor = frameWidth / dWidth;

        Matrix matrix = this.matrix;
        matrix.setScale(scaleFactor, scaleFactor, 0, 0);
        setImageMatrix(matrix);
    }

    @Override
    protected boolean setFrame(int frameLeft, int frameTop, int frameRight, int frameBottom) {
        if (!bottomCropped) {
            return super.setFrame(frameLeft, frameTop, frameRight, frameBottom);
        }

        float frameWidth = frameRight - frameLeft;
        if (!frameSet) {
            final Drawable drawable = getDrawable();
            if (drawable != null) {
                float originalImageWidth = (float) drawable.getIntrinsicWidth();

                cropImage(frameWidth, originalImageWidth);
            }
            frameSet = true;
        }
        return super.setFrame(frameLeft, frameTop, frameRight, frameBottom);
    }
}
