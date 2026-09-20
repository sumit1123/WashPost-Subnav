package com.wapo.flagship.features.pagebuilder;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.wapo.flagship.features.grid.model.ArtOverlayIcon;
import com.wapo.flagship.features.grid.model.Media;
import com.wapo.flagship.features.grid.model.Overlay;
import com.wapo.flagship.features.grid.model.OverlayStyle;
import com.wapo.flagship.features.grid.model.Video;
import com.wapo.flagship.features.posttv.PostTvPlayer2Manager;
import com.wapo.flagship.features.posttv.VideoManager2;
import com.wapo.flagship.features.posttv.listeners.PostTvApplication;
import com.wapo.flagship.features.posttv.model.PlaybackState;
import com.wapo.flagship.features.sections.SectionActivity;
import com.wapo.flagship.features.sections.SectionsPagerView;
import com.wapo.flagship.features.sections.utils.UIUtils;
import com.wapo.view.CircleImageView;
import com.washingtonpost.android.sections.R;
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader;
import com.wapo.view.ProportionalLayout;
import com.wapo.text.WpTextAppearanceSpan;
import com.washingtonpost.android.volley.toolbox.NetworkAnimatedImageView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class CellMediaView extends LinearLayout {
    private final int errorDrawableResId;
    private ProportionalLayout mediaFrame;
    private NetworkAnimatedImageView imageView;
    private CircleImageView circularImageView;
    private TextView captionView;
    private TextView overlayTextView;
    private ProgressBar progressBar;
    private boolean showCaption = true;
    private int captionStyle;
    private boolean isClickable;

    public CellMediaView(Context context) {
        this(context, null);
    }

    public CellMediaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellMediaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CellMediaView,
                0, 0
        );

        try {
            captionStyle = a.getResourceId(R.styleable.CellMediaView_caption_style, R.style.homepagestory_media_caption);
            errorDrawableResId = a.getResourceId(R.styleable.CellMediaView_error_drawable, 0);
        } finally {
            a.recycle();
        }

        setOrientation(VERTICAL);

        initViews(context);
    }

    private void initViews(Context context) {
        mediaFrame = new ProportionalLayout(context);
        mediaFrame.setLayoutParams(new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        imageView = new NetworkAnimatedImageView(context);
        circularImageView = new CircleImageView(context);
        imageView.setErrorDrawable(errorDrawableResId);
        circularImageView.setErrorDrawable(errorDrawableResId);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        circularImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setLayoutParams(new LayoutParams(MATCH_PARENT, MATCH_PARENT));
        circularImageView.setLayoutParams(new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        overlayTextView = new TextView(context);
        overlayTextView.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM));
        overlayTextView.setBackgroundResource(R.drawable.overlay_button_background);
        overlayTextView.setVisibility(GONE);

        addView(mediaFrame);

        captionView = new TextView(context);
        captionView.setFocusable(true);
        captionView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        // ProgressBar
        progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);

        addView(captionView);
        setIsClickable(false);
    }

    public void update(Media media, AnimatedImageLoader imageLoader, long itemId) {
        mediaFrame.removeAllViews();
        mediaFrame.setAspectRatio(media.getAspectRatio() <= 0 ? 1 : media.getAspectRatio());
        if (imageLoader != null) {
            if (Boolean.TRUE.equals(media.getMakeItRound())){
                circularImageView.setImageUrl(media.getUrl(), imageLoader);
                mediaFrame.addView(circularImageView);
            } else {
                imageView.setImageUrl(media.getUrl(), imageLoader);
                mediaFrame.addView(imageView);
            }
        }

        Video video = media.getVideo();
        // Hide overlay text for autoplay and looping enabled videos.
        Context baseContext = getContext();
        while (baseContext instanceof android.content.ContextWrapper
                && !(baseContext instanceof SectionActivity)) {
            baseContext = ((android.content.ContextWrapper) baseContext).getBaseContext();
        }
        boolean shouldHideOverlay = (baseContext instanceof SectionActivity)
                && ((SectionActivity) baseContext).canAutoPlayInlineVideo()
                && video != null && (video.getAutoplay() || video.isLooping())
                && !video.isLive();
        if (shouldHideOverlay) {
            overlayTextView.setVisibility(View.GONE);
        } else {
            updateOverlay(media.getOverlay());
        }
        final Context appContext = getContext().getApplicationContext();
        if (appContext instanceof PostTvApplication) {
            final VideoManager2 videoManager2 = ((PostTvApplication) appContext).getVideoManager2();
            String videoId = media.getVideoId();
            FrameLayout playerFrame = videoManager2.getPlayerFrameContainer(videoId);
            if (playerFrame != null) {
                Object curItemId = playerFrame.getTag();
                if (curItemId instanceof Long && ((long) curItemId) == itemId && playerFrame.getParent() == null) {
                    displayVideo(videoManager2, videoId, itemId);
                }
            }
        }
    }

    public void displayVideo(@NonNull VideoManager2 videoManager2, String videoId, long itemId) {
        FrameLayout playerFrame = videoManager2.getPlayerFrameContainer(videoId);
        if (playerFrame != null && playerFrame.getParent() != mediaFrame) {
            videoManager2.removePlayerFrame(videoId);
            playerFrame.setTag(itemId);
            mediaFrame.addView(playerFrame);
        }
    }

    /**
     * Method to add a playerFrame at the 0th z position to the mediaFrame and bring that
     * playerFrame to the front once after video state is ready.
     * @param videoManager2
     * @param videoId
     * @param itemId
     */
    public void displayVideoOnceReady(@NonNull VideoManager2 videoManager2, String videoId, long itemId) {
        FrameLayout playerFrame = videoManager2.getPlayerFrameContainer(videoId);
        if (playerFrame != null && playerFrame.getParent() != mediaFrame) {
            videoManager2.removePlayerFrame(videoId);
            playerFrame.setTag(itemId);
            mediaFrame.addView(playerFrame, 0);
            PostTvPlayer2Manager player2Manager = videoManager2.getPlayerManager(videoId);
            if (player2Manager != null) {
                // Try to get a LifecycleOwner associated with this view first.
                LifecycleOwner lifecycleOwner = ViewTreeLifecycleOwner.get(this);
                if (lifecycleOwner == null && getContext() instanceof LifecycleOwner) {
                    // Fallback to context if it happens to be a LifecycleOwner (Activity/FragmentContext)
                    lifecycleOwner = (LifecycleOwner) getContext();
                }

                if (lifecycleOwner != null) {
                    player2Manager.getPlaybackState().observe(lifecycleOwner, new Observer<PlaybackState>() {
                        @Override
                        public void onChanged(PlaybackState playbackState) {
                            if (playbackState instanceof PlaybackState.Ready) {
                                playerFrame.bringToFront();
                                bringOverlayToFront();
                                player2Manager.getPlaybackState().removeObserver(this);
                            }
                        }
                    });
                }
             }
         }
     }

    private void updateOverlay(@Nullable Overlay overlay) {
        if (overlay == null || (overlay.getPrefixIcon() == null && overlay.getSuffixIcon() == null)) {
            overlayTextView.setVisibility(GONE);
            return;
        }
        overlayTextView.setVisibility(VISIBLE);

        if (overlay.getPrefixIcon() != null) {
            overlayTextView.setCompoundDrawablesWithIntrinsicBounds(getIcon(overlay.getPrefixIcon()), null, null, null);
        }

        if (overlay.getSuffixIcon() != null) {
            overlayTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, getIcon(overlay.getSuffixIcon()), null);
        }

        SpannableStringBuilder span = new SpannableStringBuilder();
        if (!TextUtils.isEmpty(overlay.getText())) {
            span.append(overlay.getText());
            span.setSpan(
                    new WpTextAppearanceSpan(
                            getContext(),
                            R.style.overlay_media_caption
                    ),
                    0, span.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        if (!TextUtils.isEmpty(overlay.getSecondaryText())) {

            int startIndex = span.length();

            if (startIndex > 0) {
                span.append("  ");
            }

            span.append(overlay.getSecondaryText());

            if (overlay.getSecondaryStyle() == com.wapo.flagship.features.grid.model.OverlayStyle.SECONDARY) {
                span.setSpan(
                        new WpTextAppearanceSpan(
                                getContext(),
                                R.style.overlay_secondary_default
                        ),
                        startIndex, span.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else if (overlay.getSecondaryStyle() == OverlayStyle.LIVE) {
                span.setSpan(
                        new WpTextAppearanceSpan(
                                getContext(),
                                R.style.overlay_secondary_live
                        ),
                        startIndex, span.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        MarginLayoutParams params = (MarginLayoutParams) overlayTextView.getLayoutParams();
        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.art_overlay_bottom_padding);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.art_overlay_left_padding);
        overlayTextView.setLayoutParams(params);
        overlayTextView.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.art_overlay_icon_padding));
        overlayTextView.setText(span);
        mediaFrame.addView(overlayTextView);
    }

    public void bringOverlayToFront() {
        overlayTextView.bringToFront();
    }

    public void bringMediaFrameToFront() {
        mediaFrame.bringToFront();
    }

    private Drawable getIcon(ArtOverlayIcon artOverlayIcon) {
        switch (artOverlayIcon)
        {
            case ARROW: {
                Drawable arrow = ContextCompat.getDrawable(getContext(), R.drawable.ic_arrow_right_black).mutate();
                arrow.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                return arrow;
            }
            case CAMERA: {
                Drawable camera = VectorDrawableCompat.create(getContext().getResources(), R.drawable.ic_label_camera, getContext().getTheme()).mutate();
                camera.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                return camera;
            }
            case PLAY: return ContextCompat.getDrawable(getContext(), com.wpds.wpds.R.drawable.play);
            case MOBILE: return ContextCompat.getDrawable(getContext(), R.drawable.stamp_icon);
        }
        return null;
    }

    public TextView getCaptionView() {
        return this.captionView;
    }

    public TextView getOverylayTextView() {
        return this.overlayTextView;
    }

    /**
     * Returns current video playback position if it is playing
     * @param videoManager2
     * @param videoId
     * @return playbackPosition
     */
    public Long getPlaybackPosition(@NonNull VideoManager2 videoManager2, String videoId) {
        Long playbackPosition = -1L;
        PostTvPlayer2Manager player2Manager = videoManager2.getPlayerManager(videoId);
        if (player2Manager != null && player2Manager.isPlaying() && player2Manager.getPlaybackPosition() != null) {
            playbackPosition = player2Manager.getPlaybackPosition();
        }
        return playbackPosition;
    }

    public void setCaption(String caption) {
        CharSequence charSequence;
        if (TextUtils.isEmpty(caption)) {
            charSequence = "";
        } else {
            SpannableStringBuilder span = new SpannableStringBuilder(caption);
            span.setSpan(
                    new WpTextAppearanceSpan(
                            getContext(),
                            captionStyle
                    ),
                    0, span.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            charSequence = span;
        }
        captionView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.cell_homepagestory_caption_padding_top), 0, 0);
        captionView.setText(charSequence);
        captionView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        captionView.setContentDescription(charSequence);
        updateCaptionVisibility(charSequence);
    }

    public void setShowCaption(boolean showCaption) {
        this.showCaption = showCaption;
        updateCaptionVisibility(captionView != null ? captionView.getText() : null);
    }

    private void updateCaptionVisibility(CharSequence caption) {
        if (showCaption) {
            captionView.setVisibility(TextUtils.isEmpty(caption) ? GONE : VISIBLE);
        } else {
            captionView.setVisibility(GONE);
        }
    }

    public void setAspectRatio(float aspectRatio) {
        if (aspectRatio > 0) {
            mediaFrame.setAspectRatio(aspectRatio);
        }
    }

    public NetworkAnimatedImageView getImageView() {
        return imageView;
    }

    public ProportionalLayout getMediaFrame() { return mediaFrame; }

    public void updateCaptionColor(int color) {
        CharSequence captionText = this.captionView.getText();
        if (!TextUtils.isEmpty(captionText)) {
            SpannableString span = new SpannableString(captionText);
            span.setSpan(new ForegroundColorSpan(color), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.captionView.setText(span);
            this.captionView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
            this.captionView.setContentDescription(span);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), showCaption);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.baseState);
            setShowCaption(ss.showCaption);
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        //this hack allows scrubbing the video without also swiping the view pager.
        if (getContext() instanceof SectionActivity) {
            SectionsPagerView pagerView = ((SectionActivity) getContext()).getPager();
            if (pagerView != null) {
                pagerView.setShouldAllowScroll(!disallowIntercept);
            }
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    public void setIsClickable(boolean isClickable) {
        this.isClickable = isClickable;
        if (isClickable) {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        } else {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    public void showProgressBar() {
        if (progressBar.getParent() == null) {
            int size = UIUtils.dpToPx(16, getResources());
            FrameLayout.LayoutParams progressBarParams = new FrameLayout.LayoutParams(size, size);
            progressBarParams.gravity = Gravity.CENTER;
            mediaFrame.addView(progressBar, progressBarParams);
        }
        progressBar.setVisibility(View.VISIBLE);
        progressBar.bringToFront();
    }

    public void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isClickable) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    public static class SavedState implements Parcelable {
        public final Parcelable baseState;
        public final boolean showCaption;

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                Parcelable baseState = in.readParcelable(SavedState.class.getClassLoader());
                boolean showCaption = in.readInt() != 0;
                return new SavedState(baseState, showCaption);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public SavedState(Parcelable baseState, boolean showCaption) {
            this.baseState = baseState;
            this.showCaption = showCaption;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(baseState, flags);
            dest.writeInt(showCaption ? 1 : 0);
        }
    }
}
