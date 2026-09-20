package com.washingtonpost.android.volley.toolbox;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.DrawableRes;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import com.wapo.android.commons.util.Logger;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.material.imageview.ShapeableImageView;
import com.washingtonpost.android.volley.R;
import com.washingtonpost.android.volley.Request;
import com.washingtonpost.android.volley.VolleyError;

import java.io.InputStream;
import java.lang.reflect.Method;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static java.lang.Math.min;


/**
 * Extension of the basic ImageView allowing animated gifs and images over the network cached by Volley.
 *
 * Based on an net.frankbot.imageviewex by Sebastiano Poggi, Francesco Pontillo and NetworkImageView from the volley library
 *
 * Extension of the ImageView that handles any kind of image already supported
 * by ImageView, plus animated GIF images.
 * <p/>
 * <b>WARNING:</b> due to Android limitations, the android:adjustViewBounds
 * attribute is ignored on API levels < 16 (Jelly Bean 4.1). Use our own
 * adjustViewBounds attribute to obtain the same behaviour!
 *
 * @author Thad Cox
 */
@SuppressWarnings({"deprecation"})
public class NetworkAnimatedImageView extends ShapeableImageView {

    private static final String TAG = NetworkAnimatedImageView.class.getSimpleName();

    private static final int IMAGE_ALIGN_NONE = 0;

    /**
     * No fill direction. Acts just like a common
     * {@link ImageView} does.
     */
    public static final int FILL_DIRECTION_NONE = 0;

    /**
     * If the width of the {@link NetworkAnimatedImageView} is longer
     * than the width of the image it contains, the image
     * is scaled to fit the width of the view. The height
     * of the view is then adjusted to fit the height of
     * the scaled image.
     */
    public static final int FILL_DIRECTION_HORIZONTAL = 1;

    /**
     * If the height of the {@link NetworkAnimatedImageView} is longer
     * than the height of the image it contains, the image
     * is scaled to fit the height of the view. The width
     * of the view is then adjusted to fit the width of
     * the scaled image.
     */
    public static final int FILL_DIRECTION_VERTICAL = 2;

    private int errorResId = R.drawable.curtain_image;

    private float mScale = -1;
    private boolean mAdjustViewBounds = false;

    public static final int IMAGE_SOURCE_UNKNOWN = -1;
    public static final int IMAGE_SOURCE_DRAWABLE = 0;
    public static final int IMAGE_SOURCE_GIF = 1;

    @SuppressWarnings("unused")
    private int mImageSource;

    // Used by the fixed size optimizations
    private boolean mIsFixedSize = false;

    private int mOverriddenDensity = -1;
    private static int mOverriddenClassDensity = -1;

    private int mMaxHeight, mMaxWidth;

    private Movie mGif;
    private double mGifStartTime;
    private int mFrameDuration = 67;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Thread mUpdater;

    private int mImageAlign = IMAGE_ALIGN_NONE;

    private final DisplayMetrics mDm;
    private ScaleType mScaleType;

    protected Drawable mEmptyDrawable = new ColorDrawable(0x00000000);
    protected int mFillDirection = FILL_DIRECTION_NONE;


    private AnimatedImageLoader mImageLoader;
    private String mUrl;
    private AnimatedImageLoader.AnimatedImageContainer imageContainer;
    protected ImageLoadListener imageLoadListener = null;
    protected BitmapLoadListener bitmapLoadListener = null;
    private int priority;

    private int maxImgWidth = 0;
    private int maxImgHeight = 0;

    @DrawableRes
    private int placeHolderId = 0;

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        if(imageLoadListener != null && bm != null) {
            imageLoadListener.onImageLoad();
        }
        if(bitmapLoadListener != null && bm != null) {
            bitmapLoadListener.onBitmapLoaded(bm);
        }
    }

    public void setImageLoadListener(ImageLoadListener imageLoadListener) {
        this.imageLoadListener = imageLoadListener;
    }

    public void setBitmapLoadListener(BitmapLoadListener bitmapLoadListener) {
        this.bitmapLoadListener = bitmapLoadListener;
    }

    public void setErrorDrawable(int errorDrawableResId) {
        this.errorResId = errorDrawableResId;
    }

    public interface ImageLoadListener {
        public void onImageLoad();
    }

    public interface BitmapLoadListener {
        void onBitmapLoaded(Bitmap bitmap);
    }

    ///////////////////////////////////////////////////////////
    ///                  CONSTRUCTORS                       ///
    ///////////////////////////////////////////////////////////

    /**
     * Creates an instance for the class.
     *
     * @param context The context to instantiate the object for.
     */
    public NetworkAnimatedImageView(Context context) {
        super(context);
        mDm = context.getResources().getDisplayMetrics();
    }

    /**
     * Creates an instance for the class and initializes it with a given image.
     *
     * @param context The context to initialize the instance into.
     * @param src     InputStream containing the GIF to view.
     */
    public NetworkAnimatedImageView(Context context, InputStream src) {
        super(context);
        mGif = Movie.decodeStream(src);
        mDm = context.getResources().getDisplayMetrics();
    }

    /**
     * Creates an instance for the class.
     *
     * @param context The context to initialize the instance into.
     * @param attrs   The parameters to initialize the instance with.
     */
    public NetworkAnimatedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDm = context.getResources().getDisplayMetrics();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.NetworkAnimatedImageView, 0, 0);

        assert a != null;
        if (a.hasValue(R.styleable.NetworkAnimatedImageView_adjustViewBounds)) {
            // Prioritize our own adjustViewBounds
            setAdjustViewBounds(a.getBoolean(R.styleable.NetworkAnimatedImageView_adjustViewBounds, false));
        }
        else {
            // Fallback strategy: try to use ImageView's own adjustViewBounds
            // attribute value
            if (Build.VERSION.SDK_INT >= 16) {
                // The ImageView#getAdjustViewBounds() method only exists from
                // API Level 16+, for some reason.
                try {
                    Method m = super.getClass().getMethod("getAdjustViewBounds");
                    mAdjustViewBounds = (Boolean) m.invoke(this);
                }
                catch (Exception ignored) {
                }
            }
        }

        if (a.hasValue(R.styleable.NetworkAnimatedImageView_fillDirection)) {
            setFillDirection(a.getInt(R.styleable.NetworkAnimatedImageView_fillDirection, 0));
        }

        if (a.hasValue(R.styleable.NetworkAnimatedImageView_emptyDrawable)) {
            setEmptyDrawable(a.getDrawable(R.styleable.NetworkAnimatedImageView_emptyDrawable));
        }

        errorResId = a.getResourceId(R.styleable.NetworkAnimatedImageView_errorDrawable, 0);

        a.recycle();
    }

    public NetworkAnimatedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs);
    }

    ///////////////////////////////////////////////////////////
    ///                 PUBLIC SETTERS                      ///
    ///////////////////////////////////////////////////////////

    /** Initalizes the inner variable describing the kind of resource attached to the NetworkAnimatedImageView. */
    public void initializeDefaultValues() {
        if (isPlaying()) stop();
        mGif = null;
        setTag(null);
        mImageSource = IMAGE_SOURCE_UNKNOWN;
    }

    /**
     * Sets URL of the image that should be loaded into this view. Note that calling this will
     * immediately either set the cached image (if available) or the default image specified by
     * NetworkImageView#setDefaultImageResId(int) on the view.
     *
     * NOTE: If applicable, NetworkImageView#setDefaultImageResId(int) and
     * NetworkImageView#setErrorImageResId(int) should be called prior to calling
     * this function.
     *
     * @param url The URL that should be loaded into this ImageView.
     * @param imageLoader ImageLoader that will be used to make the request.
     */
    public void setImageUrl(String url, AnimatedImageLoader imageLoader) {
        setImageUrl(url, imageLoader, Request.Priority.LOW.ordinal());
    }

    public void setImageUrl(String url, AnimatedImageLoader imageLoader, int priority) {
        this.priority = priority;
        mUrl = url == null ? null : url.replaceAll(" ", "%20");
        mImageLoader = imageLoader;
        // The URL has potentially changed. See if we need to load it.
        String currentUrl = imageContainer == null ? null : imageContainer.getRequestUrl();
        if (currentUrl == null || !currentUrl.equals(url)) {
            requestLayout();
        }
    }


    protected void loadImageIfNecessary(final boolean isInLayoutPass){
        final int width = getMeasuredWidth() == 0 ? maxImgWidth : getMeasuredWidth();
        final int height = getMeasuredHeight() == 0 ? maxImgHeight : getMeasuredHeight();

        boolean isFullyWrapContent = getLayoutParams().height == LayoutParams.WRAP_CONTENT
                && getLayoutParams().width == LayoutParams.WRAP_CONTENT;
        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        if (width == 0 && height == 0 && !isFullyWrapContent) {
            return;
        }

        // if the URL to be loaded in this view is empty, cancel any old requests and clear the
        // currently loaded image.
        if (TextUtils.isEmpty(mUrl)) {
            cancelRequest();
            return;
        }


        // if there was an old request in this view, check if it needs to be canceled.
        if (imageContainer != null && imageContainer.getRequestUrl() != null) {
            if (imageContainer.getRequestUrl().equals(mUrl)) {
                // if the request is from the same URL, return.
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                imageContainer.cancelRequest();
                clearBitmap();
            }
        }

        imageContainer = mImageLoader.get(mUrl, new AnimatedImageLoader.AnimatedImageListener(){

            @Override
            public void onErrorResponse(VolleyError error) {
                //TODO
                Logger.e(TAG, "Error in downloading the image");
                if (error != null && error.getMessage() != null && error.getMessage().toLowerCase().contains("outofmemoryerror")) return;
                if (getResources() != null) {
                    setImageBitmap(decodeSampledBitmapFromResource(getResources(), errorResId, width, height));
                }

                onDispatchErrorResponse(error);
            }

            @Override
            public void onResponse(final AnimatedImageLoader.AnimatedImageContainer response, boolean isImmediate) {
                // If this was an immediate response that was delivered inside of a layout
                // pass do not set the image immediately as it will trigger a requestLayout
                // inside of a layout. Instead, defer setting the image by posting back to
                // the main thread.
                if (isImmediate && isInLayoutPass) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            onResponse(response, false);
                        }
                    });
                    return;
                }

                if (response.getData() != null) {
                    setSource(response.getData());
                }

                onDispatchSuccessResponse(response, isImmediate);
            }
        }, width, height, new Request.Priority(priority));


    }

    protected void onDispatchSuccessResponse(AnimatedImageLoader.AnimatedImageContainer response, boolean isImmediate) {

    }

    protected void onDispatchErrorResponse(VolleyError error) {

    }

    public void cancelRequest(){
        if (imageContainer != null) {
            imageContainer.cancelRequest();
            imageContainer = null;
        }
        clearBitmap();
    }

    public void setPlaceholder(@DrawableRes int placeholderId) {
        this.placeHolderId = placeholderId;
    }

    private void clearBitmap() {
        if (placeHolderId != 0) {
            setImageBitmap(BitmapFactory.decodeResource(getResources(), placeHolderId));
        } else {
            setImageBitmap(null);
        }
    }

    /**
     * Set the source of the image
     * @param source Source of the image to display, must be a {@link android.graphics.Movie}, a {@link android.graphics.Bitmap}, or a {@link android.graphics.drawable.Drawable}.
     *
     * @throws java.lang.IllegalArgumentException when a parameter other then a {@link android.graphics.Movie}, a {@link android.graphics.Bitmap}, or a {@link android.graphics.drawable.Drawable}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setSource(Object source){
        if(source instanceof Movie){
            initializeDefaultValues();
            mImageSource = IMAGE_SOURCE_GIF;
            mGif = (Movie) source;

            // Disables the HW acceleration when viewing a GIF on Android 3+
            if (Build.VERSION.SDK_INT >= 11) {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }


            super.setImageDrawable(null);
            requestLayout();
            play();
            if (imageLoadListener != null) {
                imageLoadListener.onImageLoad();
            }
        } else if(source instanceof Drawable){
            setImageDrawable((Drawable) source);
        } else if (source instanceof Bitmap){
            setImageBitmap((Bitmap) source);
        } else if(source != null){
            throw new IllegalArgumentException("Source must be either a Drawable or a Movie.");
        } else setImageDrawable(null);
    }




    private void prepForDrawable() {
        initializeDefaultValues();
        stop();
        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        mGif = null;
        mImageSource = IMAGE_SOURCE_DRAWABLE;
    }

    /** {@inheritDoc} */
    public void setImageDrawable(Drawable drawable) {
        prepForDrawable();
        super.setImageDrawable(drawable);
    }

    /** {@inheritDoc} */
    @Override
    public void setScaleType(ScaleType scaleType) {
        super.setScaleType(scaleType);
    }

    /**
     * Sets the fill direction for the image. This is used
     * in conjunction with {@link #setAdjustViewBounds(boolean)}.
     * If <code>adjustViewBounds</code> is not already enabled,
     * it will be automatically enabled by setting the direction
     * to anything other than {@link NetworkAnimatedImageView#FILL_DIRECTION_NONE}.
     *
     * @param direction The fill direction.
     */
    public void setFillDirection(int direction) {

        if (direction != mFillDirection) {
            mFillDirection = direction;

            if (mFillDirection != FILL_DIRECTION_NONE && !mAdjustViewBounds) {
                setAdjustViewBounds(true);
            }

            requestLayout();
        }
    }

    /**
     * Sets the duration, in milliseconds, of each frame during the GIF animation.
     * It is the refresh period.
     *
     * @param duration The duration, in milliseconds, of each frame.
     */
    public void setFramesDuration(int duration) {
        if (duration < 1) {
            throw new IllegalArgumentException
                    ("Frame duration can't be less or equal than zero.");
        }

        mFrameDuration = duration;
    }

    /**
     * Sets the number of frames per second during the GIF animation.
     *
     * @param fps The fps amount.
     */
    public void setFPS(float fps) {
        if (fps <= 0.0) {
            throw new IllegalArgumentException
                    ("FPS can't be less or equal than zero.");
        }

        mFrameDuration = Math.round(1000f / fps);
    }

    /**
     * Sets a density for every image set to any {@link NetworkAnimatedImageView}.
     * If a custom density level is set for a particular instance of {@link NetworkAnimatedImageView},
     * this will be ignored.
     *
     * @param classLevelDensity the density to apply to every instance of {@link NetworkAnimatedImageView}.
     */
    public static void setClassLevelDensity(int classLevelDensity) {
        mOverriddenClassDensity = classLevelDensity;
    }


    /**
     * Programmatically overrides this view's density.
     * The new density will be set on the next {@link #onMeasure(int, int)}.
     *
     * @param fixedDensity the new density the view has to use.
     */
    public void setDensity(int fixedDensity) {
        mOverriddenDensity = fixedDensity;
    }

    /**
     * Removes the class level density for {@link NetworkAnimatedImageView}.
     *
     * @see NetworkAnimatedImageView#setClassLevelDensity(int)
     */
    public static void removeClassLevelDensity() {
        setClassLevelDensity(-1);
    }

    /**
     * Sets a value indicating wether the image is considered as having a fixed size.
     * This will enable an optimization when assigning images to the NetworkAnimatedImageView, but
     * has to be used sparingly or it may cause artifacts if the image isn't really
     * fixed in size.
     * <p/>
     * An example of usage for this optimization is in ListViews, where items images
     * are supposed to be fixed size, and this enables buttery smoothness.
     * <p/>
     * See: https://plus.google.com/u/0/113058165720861374515/posts/iTk4PjgeAWX
     */
    public void setIsFixedSize(boolean fixedSize) {
        mIsFixedSize = fixedSize;
    }

    /**
     * Sets a new ImageAlign value and redraws the View.
     * If the NetworkAnimatedImageView has a ScaleType set too, this
     * will override it!
     *
     * @param align The new ImageAlign value.
     *
     * @deprecated Use setScaleType(ScaleType.FIT_START)
     *             and setScaleType(ScaleType.FIT_END) instead.
     */
    public void setImageAlign(int align) {
        if (align != mImageAlign) {
            mImageAlign = align;
            invalidate();
        }
    }

    /**
     * Sets the drawable used as "empty". Note that this
     * is not automatically assigned by {@link NetworkAnimatedImageView}
     * but can be used by descendants such as ImageViewNext (This was not included in the import to save on size --TBC).
     *
     * @param d The "empty" drawable
     */
    public void setEmptyDrawable(Drawable d) {
        mEmptyDrawable = d;
    }

    @Override
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (mFillDirection != FILL_DIRECTION_NONE) {
            // Just in case, shouldn't be ever necessary
            if (!mAdjustViewBounds) {
                mAdjustViewBounds = true;
                super.setAdjustViewBounds(true);
            }

            return;
        }

        mAdjustViewBounds = adjustViewBounds;
        super.setAdjustViewBounds(adjustViewBounds);
    }

    ///////////////////////////////////////////////////////////
    ///                 PUBLIC GETTERS                      ///
    ///////////////////////////////////////////////////////////

    /** Disables density ovverriding. */
    public void dontOverrideDensity() {
        mOverriddenDensity = -1;
    }

    /**
     * Returns a boolean indicating if an animation is currently playing.
     *
     * @return true if animating, false otherwise.
     */
    public boolean isPlaying() {
        return mUpdater != null && mUpdater.isAlive();
    }

    /**
     * Returns a boolean indicating if the instance was initialized and if
     * it is ready for playing the animation.
     *
     * @return true if the instance is ready for playing, false otherwise.
     */
    public boolean canPlay() {
        return mGif != null;
    }

    /**
     * Gets the frame duration, in milliseconds, of each frame during the GIF animation.
     * It is the refresh period.
     *
     * @return The duration, in milliseconds, of each frame.
     */
    public int getFramesDuration() {
        return mFrameDuration;
    }

    /**
     * Gets the number of frames per second during the GIF animation.
     *
     * @return The fps amount.
     */
    public float getFPS() {
        return 1000.0f / mFrameDuration;
    }

    /**
     * Gets the current scale value.
     *
     * @return Returns the scale value for this NetworkAnimatedImageView.
     */
    public float getScale() {
        float targetDensity = getContext().getResources().getDisplayMetrics().densityDpi;
        float displayThisDensity = getDensity();
        mScale = targetDensity / displayThisDensity;
        if (mScale < 0.1f) mScale = 0.1f;
        if (mScale > 5.0f) mScale = 5.0f;
        return mScale;
    }

    /**
     * Gets the fill direction for this NetworkAnimatedImageView.
     *
     * @return Returns the fill direction.
     */
    public int getFillDirection() {
        return mFillDirection;
    }

    /**
     * Gets the drawable used as "empty" state.
     *
     * @return Returns the drawable used ad "empty".
     */
    public Drawable getEmptyDrawable() {
        return mEmptyDrawable;
    }

    /**
     * Checks whether the class level density has been set.
     *
     * @return true if it has been set, false otherwise.
     * @see NetworkAnimatedImageView#setClassLevelDensity(int)
     */
    public static boolean isClassLevelDensitySet() {
        return mOverriddenClassDensity != -1;
    }

    /**
     * Gets the class level density has been set.
     *
     * @return int, the class level density
     * @see NetworkAnimatedImageView#setClassLevelDensity(int)
     */
    public static int getClassLevelDensity() {
        return mOverriddenClassDensity;
    }

    /**
     * Gets the set density of the view, given by the screen density or by value
     * overridden with {@link #setDensity(int)}.
     * If the density was not overridden and it can't be retrieved by the context,
     * it simply returns the DENSITY_HIGH constant.
     *
     * @return int representing the current set density of the view.
     */
    public int getDensity() {
        int density;

        // If a custom instance density was set, set the image to this density
        if (mOverriddenDensity > 0) {
            density = mOverriddenDensity;
        }
        else if (isClassLevelDensitySet()) {
            // If a class level density has been set, set every image to that density
            density = getClassLevelDensity();
        }
        else {
            // If the instance density was not overridden, get the one from the display
            DisplayMetrics metrics = new DisplayMetrics();

            if (!(getContext() instanceof Activity)) {
                density = DisplayMetrics.DENSITY_HIGH;
            }
            else {
                Activity activity = (Activity) getContext();
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                density = metrics.densityDpi;
            }
        }

        return density;
    }

    /**
     * Sets a value indicating wether the image is considered as having a fixed size.
     * See {@link #setIsFixedSize(boolean)} for further details.
     */
    public boolean getIsFixedSize() {
        return mIsFixedSize;
    }

    /**
     * Returns the current ImageAlign setting.
     *
     * @return Returns the current ImageAlign setting.
     * @deprecated Use setScaleType(ScaleType.FIT_START)
     *             and setScaleType(ScaleType.FIT_END) instead.
     */
    public int getImageAlign() {
        return mImageAlign;
    }

    ///////////////////////////////////////////////////////////
    ///                   PUBLIC METHODS                    ///
    ///////////////////////////////////////////////////////////

    /**
     * Starts playing the GIF, if it hasn't started yet.
     * FPS defaults to 15..
     */
    public void play() {
        // Do something if the animation hasn't started yet
        if (mUpdater == null || !mUpdater.isAlive()) {
            // Check id the animation is ready
            if (!canPlay()) {
                throw new IllegalStateException
                        ("Animation can't start before a GIF is loaded.");
            }

            // Initialize the thread and start it
            mUpdater = new Thread() {

                @Override
                public void run() {

                    // Infinite loop: invalidates the View.
                    // Stopped when the thread is stopped or interrupted.
                    while (mUpdater != null && !mUpdater.isInterrupted()) {

                        mHandler.post(new Runnable() {
                            public void run() {
                                invalidate();
                            }
                        });

                        // The thread sleeps until the next frame
                        try {
                            Thread.sleep(mFrameDuration);
                        }
                        catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            };

            mUpdater.start();
        }
    }

    /** Pause playing the GIF, if it has started. */
    public void pause() {
        // If the animation has started
        if (mUpdater != null && mUpdater.isAlive()) {
            mUpdater.stop();
        }
    }

    /** Stops playing the GIF, if it has started. */
    public void stop() {
        // If the animation has started
        if (mUpdater != null && mUpdater.isAlive() && canPlay()) {
            mUpdater.interrupt();
            mGifStartTime = 0;
        }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestLayout();//reload image by existing url
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelRequest();

        if(isPlaying()){
            stop();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void setMaxHeight(int maxHeight) {
        super.setMaxHeight(maxHeight);
        mMaxHeight = maxHeight;
    }

    @Override
    public void setMaxWidth(int maxWidth) {
        super.setMaxWidth(maxWidth);
        mMaxWidth = maxWidth;
    }

    ///////////////////////////////////////////////////////////
    ///                  EVENT HANDLERS                     ///
    ///////////////////////////////////////////////////////////

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    /**
     * Draws the control
     *
     * @param canvas The canvas to drow onto.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (mGif != null) {
            long now = android.os.SystemClock.uptimeMillis();

            // first time	
            if (mGifStartTime == 0) {
                mGifStartTime = now;
            }

            int dur = mGif.duration();
            if (dur == 0) {
                dur = 1000;
            }
            int relTime = (int) ((now - mGifStartTime) % dur);
            mGif.setTime(relTime);
            int saveCnt = canvas.save();

            canvas.scale(mScale, mScale);

            float[] gifDrawParams = applyScaleType(canvas);

            mGif.draw(canvas, gifDrawParams[0], gifDrawParams[1]);

            if (mImageAlign != IMAGE_ALIGN_NONE) {
                // We have an alignment override.
                // Note: at the moment we only have TOP as custom alignment,
                // so the code here is simplified. Will need refactoring
                // if other custom alignments are implemented further on.

                // ImageAlign.TOP: align top edge with the View

                canvas.translate(0.0f, calcTopAlignYDisplacement());
            }

            canvas.restoreToCount(saveCnt);
        }
        else {
            // Reset the original scale type
            super.setScaleType(getScaleType());

            if (mImageAlign == IMAGE_ALIGN_NONE) {
                // Everything is normal when there is no alignment override
                super.onDraw(canvas);
            }
            else {
                // We have an alignment override.
                // Note: at the moment we only have TOP as custom alignment,
                // so the code here is simplified. Will need refactoring
                // if other custom alignments are implemented further on.

                // ImageAlign.TOP: scaling forced to CENTER_CROP, align top edge with the View
                setScaleType(ScaleType.CENTER_CROP);

                int saveCnt = canvas.save();
                canvas.translate(0.0f, calcTopAlignYDisplacement());

                super.onDraw(canvas);

                canvas.restoreToCount(saveCnt);
            }
        }
    }

    /**
     * Applies the scale type of the NetworkAnimatedImageView to the GIF.
     * Use the returned value to draw the GIF and calculate
     * the right y-offset, if any has to be set.
     *
     * @param canvas The {@link Canvas} to apply the {@link ScaleType} to.
     *
     * @return A float array containing, for each position:
     *         - 0 The x position of the gif
     *         - 1 The y position of the gif
     *         - 2 The scaling applied to the y-axis
     */
    private float[] applyScaleType(Canvas canvas) {
        // Get the current dimensions of the view and the gif
        float vWidth = getWidth();
        float vHeight = getHeight();
        float gWidth = mGif.width() * mScale;
        float gHeight = mGif.height() * mScale;

        // Disable the default scaling, it can mess things up
        if (mScaleType == null) {
            mScaleType = getScaleType();
            setScaleType(ScaleType.MATRIX);
        }

        float x = 0;
        float y = 0;
        float s = 1;

        switch (mScaleType) {
            case CENTER:
                /* Center the image in the view, but perform no scaling. */
                x = (vWidth - gWidth) / 2 / mScale;
                y = (vHeight - gHeight) / 2 / mScale;
                break;

            case CENTER_CROP:
                /*
                 * Scale the image uniformly (maintain the image's aspect ratio)
        		 * so that both dimensions (width and height) of the image will
        		 * be equal to or larger than the corresponding dimension of the
        		 * view (minus padding). The image is then centered in the view.
        		 */
                float minDimensionCenterCrop = min(gWidth, gHeight);
                if (minDimensionCenterCrop == gWidth) {
                    s = vWidth / gWidth;
                }
                else {
                    s = vHeight / gHeight;
                }
                x = (vWidth - gWidth * s) / 2 / (s * mScale);
                y = (vHeight - gHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case CENTER_INSIDE:
        		/*
        		 * Scale the image uniformly (maintain the image's aspect ratio)
        		 * so that both dimensions (width and height) of the image will
        		 * be equal to or less than the corresponding dimension of the
        		 * view (minus padding). The image is then centered in the view.
        		 */
                // Scaling only applies if the gif is larger than the container!
                if (gWidth > vWidth || gHeight > vHeight) {
                    float maxDimensionCenterInside = Math.max(gWidth, gHeight);
                    if (maxDimensionCenterInside == gWidth) {
                        s = vWidth / gWidth;
                    }
                    else {
                        s = vHeight / gHeight;
                    }
                }
                x = (vWidth - gWidth * s) / 2 / (s * mScale);
                y = (vHeight - gHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case FIT_CENTER:
        		/*
        		 * Compute a scale that will maintain the original src aspect ratio,
        		 * but will also ensure that src fits entirely inside dst.
        		 * At least one axis (X or Y) will fit exactly.
        		 * The result is centered inside dst.
        		 */
                // This scale type always scales the gif to the exact dimension of the View
                float maxDimensionFitCenter = Math.max(gWidth, gHeight);
                if (maxDimensionFitCenter == gWidth) {
                    s = vWidth / gWidth;
                }
                else {
                    s = vHeight / gHeight;
                }
                x = (vWidth - gWidth * s) / 2 / (s * mScale);
                y = (vHeight - gHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case FIT_START:
        		/*
        		 * Compute a scale that will maintain the original src aspect ratio,
        		 * but will also ensure that src fits entirely inside dst.
        		 * At least one axis (X or Y) will fit exactly.
        		 * The result is centered inside dst.
        		 */
                // This scale type always scales the gif to the exact dimension of the View
                float maxDimensionFitStart = Math.max(gWidth, gHeight);
                if (maxDimensionFitStart == gWidth) {
                    s = vWidth / gWidth;
                }
                else {
                    s = vHeight / gHeight;
                }
                x = 0;
                y = 0;
                canvas.scale(s, s);
                break;

            case FIT_END:
        		/*
        		 * Compute a scale that will maintain the original src aspect ratio,
        		 * but will also ensure that src fits entirely inside dst.
        		 * At least one axis (X or Y) will fit exactly.
        		 * END aligns the result to the right and bottom edges of dst.
        		 */
                // This scale type always scales the gif to the exact dimension of the View
                float maxDimensionFitEnd = Math.max(gWidth, gHeight);
                if (maxDimensionFitEnd == gWidth) {
                    s = vWidth / gWidth;
                }
                else {
                    s = vHeight / gHeight;
                }
                x = (vWidth - gWidth * s) / mScale / s;
                y = (vHeight - gHeight * s) / mScale / s;
                canvas.scale(s, s);
                break;

            case FIT_XY:
        		/*
        		 * Scale in X and Y independently, so that src matches dst exactly.
        		 * This may change the aspect ratio of the src.
        		 */
                float sFitX = vWidth / gWidth;
                s = vHeight / gHeight;
                x = 0;
                y = 0;
                canvas.scale(sFitX, s);
                break;
            default:
                break;
        }

        return new float[] {x, y, s};
    }

    private final Point size = new Point();
    /** @see android.view.View#measure(int, int) */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        maxImgWidth = getSize(widthMeasureSpec);
        maxImgHeight = getSize(heightMeasureSpec);

        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        if ((getContext() instanceof Activity)) {
            Activity activity = (Activity) getContext();
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getSize(size);
            if (widthMeasureSpec != EXACTLY) {
                maxImgWidth = maxImgHeight <= 0 ? size.x : min(size.x, maxImgWidth);
            }
            if (heightMeasureSpec != EXACTLY) {
                maxImgHeight = maxImgHeight <= 0 ? size.y : min(size.y, maxImgHeight);
            }
        }

        mScale = getScale();

        int w;
        int h;

        // Desired aspect ratio of the view's contents (not including padding)
        float desiredAspect = 0.0f;

        // We are allowed to change the view's width
        boolean resizeWidth = false;

        // We are allowed to change the view's height
        boolean resizeHeight = false;

        final Drawable drawable = getDrawable();

        if (drawable == null || (drawable instanceof BitmapDrawable && ((BitmapDrawable) drawable).getBitmap() == null)) {
            w = 0;
            h = 0;
        } else if (mGif != null) {
            w = mGif.width();
            h = mGif.height();
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;
        } else {
            w = drawable.getIntrinsicWidth();
            h = drawable.getIntrinsicHeight();
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;
        }



        // We are supposed to adjust view bounds to match the aspect
        // ratio of our drawable. See if that is possible.
        if (w > 0 && h > 0) {
            if (mAdjustViewBounds) {
                resizeWidth = widthSpecMode != MeasureSpec.EXACTLY && mFillDirection != FILL_DIRECTION_HORIZONTAL;
                resizeHeight = heightSpecMode != MeasureSpec.EXACTLY && mFillDirection != FILL_DIRECTION_VERTICAL;

                desiredAspect = (float) w / (float) h;
            }
        }

        int pleft = getPaddingLeft();
        int pright = getPaddingRight();
        int ptop = getPaddingTop();
        int pbottom = getPaddingBottom();

        int widthSize;
        int heightSize;

        if (resizeWidth || resizeHeight) {
            // If we get here, it means we want to resize to match the
            // drawables aspect ratio, and we have the freedom to change at
            // least one dimension.

            // Get the max possible width given our constraints
            widthSize = resolveAdjustedSize(w + pleft + pright, mMaxWidth, widthMeasureSpec);

            // Get the max possible height given our constraints
            heightSize = resolveAdjustedSize(h + ptop + pbottom, mMaxHeight, heightMeasureSpec);

            if (desiredAspect != 0.0f) {
                // See what our actual aspect ratio is
                float actualAspect = (float) (widthSize - pleft - pright) /
                        (heightSize - ptop - pbottom);

                if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {

                    boolean done = false;

                    // Try adjusting width to be proportional to height
                    if (resizeWidth) {
                        int newWidth = (int) (desiredAspect * (heightSize - ptop - pbottom)) +
                                pleft + pright;
                        if (newWidth <= widthSize || mFillDirection == FILL_DIRECTION_VERTICAL) {
                            widthSize = newWidth;
                            done = true;
                        }
                    }

                    // Try adjusting height to be proportional to width
                    if (!done && resizeHeight) {
                        int newHeight = (int) ((widthSize - pleft - pright) / desiredAspect) +
                                ptop + pbottom;
                        if (newHeight <= heightSize || mFillDirection == FILL_DIRECTION_HORIZONTAL) {
                            heightSize = newHeight;
                        }
                    }
                }
            }
        }
        else {
            /* We either don't want to preserve the drawables aspect ratio,
               or we are not allowed to change view dimensions. Just measure in
               the normal way.
            */
            w += pleft + pright;
            h += ptop + pbottom;

            w = Math.max(w, getSuggestedMinimumWidth());
            h = Math.max(h, getSuggestedMinimumHeight());

            widthSize = resolveSize(w, widthMeasureSpec);
            heightSize = resolveSize(h, heightMeasureSpec);
        }

        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
    }

    ///////////////////////////////////////////////////////////
    ///                  PRIVATE HELPERS                    ///
    ///////////////////////////////////////////////////////////

    /** Copied from {@link ImageView}'s implementation. */
    private int resolveAdjustedSize(int desiredSize, int maxSize,
                                    int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = getSize(measureSpec);

        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                // Parent says we can be as big as we want. Just don't be larger
                // than max size imposed on ourselves.

                result = min(desiredSize, maxSize);
                break;

            case MeasureSpec.AT_MOST:
                // Parent says we can be as big as we want, up to specSize.
                // Don't be larger than specSize, and don't be larger than
                // the max size imposed on ourselves.
                result = min(min(desiredSize, specSize), maxSize);
                break;

            case MeasureSpec.EXACTLY:
                // No choice. Do what we are told.
                result = specSize;
                break;
        }
        return result;
    }

    /**
     * Calculates the top displacement for the image to make sure it
     * is aligned at the top of the NetworkAnimatedImageView.
     */
    private float calcTopAlignYDisplacement() {
        int viewHeight = getHeight();
        int imgHeight;
        float displacement = 0f;

        if (viewHeight <= 0) {
            Logger.v(TAG, "The NetworkAnimatedImageView is still initializing...");
            return displacement;
        }

        if (mGif == null) {
            final Drawable tmpDrawable = getDrawable();
            if (!(tmpDrawable instanceof BitmapDrawable) || mGif == null) {
                return 0f;     // Nothing to do here
            }

            // Retrieve the bitmap, its height and the ImageView height
            Bitmap bmp = ((BitmapDrawable) tmpDrawable).getBitmap();
            imgHeight = bmp.getScaledHeight(mDm);
        }
        else {
            // This is a GIF...
            imgHeight = mGif.height();
        }

        //noinspection IfMayBeConditional
        if (viewHeight > imgHeight) {
            displacement = -1 * (viewHeight - imgHeight);   // Just align to top edge
        }
        else {
            // Top displacement [px] = (image height / 2) - (view height / 2)
            displacement = -1 * ((imgHeight - viewHeight) / 2);        // This is in pixels...
        }
        return displacement;
    }

    private static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(res, resId, options);
            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeResource(res, resId, options);
        } catch (OutOfMemoryError error) {
            return null;
        }
    }

    /**
     * @url http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     */
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Temporarily shows the empty drawable (or empties
     * the view if none is defined). Note that this does not
     * follow all procedures {@link #setImageDrawable(android.graphics.drawable.Drawable)}
     * follows and is only intended for temporary assignments such as in
     * ImageViewNext.ImageLoadCompletionListener#onLoadStarted(ImageViewNext, ImageViewNext.CacheLevel) (Not include in the import --TBC)
     */
    public void showEmptyDrawable() {
        setScaleType(ScaleType.CENTER_CROP);
        super.setImageDrawable(mEmptyDrawable);
    }


    ///////////////////////////////////////////////////////////
    ///                  PRIVATE CLASSES                    ///
    ///////////////////////////////////////////////////////////

    /** Class that represents a saved state for the NetworkAnimatedImageView. */
    private static class SavedState extends BaseSavedState {
        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }


}