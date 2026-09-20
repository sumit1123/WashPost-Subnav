package com.wapo.flagship.features.photos;

import static com.wapo.android.commons.util.Utils.isConnectedOrConnecting;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.wapo.android.commons.util.ReachabilityUtil;
import com.wapo.view.TouchImageView;
import com.washingtonpost.android.articles.R;
import com.washingtonpost.android.volley.VolleyError;
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader;
import com.washingtonpost.android.volley.toolbox.ImageLoaderProvider;

public class PhotoFragment extends Fragment {

    protected static final String EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL";
    protected static final String EXTRA_IMAGE_CAPTION = "EXTRA_IMAGE_CAPTION";
    protected View imageContainer;
    protected TouchImageView image;
    protected View progress;
    protected View textContainer;
    protected View captionLayout;
    protected TextView headLine;
    protected TextView caption;
    protected TextView fullCredits;
    protected View errorCurtain;
    protected TextView errorMessage;
    public boolean uiVisibility = true;
    private static int maxLines;
    float oldX;
    final float SENSITIVITY = 3.0f;

    public static PhotoFragment create(String imageUrl, String imageCaption, int lines) {
        PhotoFragment photoFragment = new PhotoFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_IMAGE_URL, imageUrl);
        args.putString(EXTRA_IMAGE_CAPTION, imageCaption);
        photoFragment.setArguments(args);
        maxLines = lines;
        return photoFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.native_photo_fragment, container, false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (image != null) {
            image.setShouldResetZoom(true);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        imageContainer = view.findViewById(R.id.gallery_item_text);
        image = (TouchImageView) view.findViewById(R.id.photo_item_image);
        progress = view.findViewById(R.id.image_progress);
        image.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);
        textContainer = view.findViewById(R.id.photo_caption_layout);
        captionLayout = view.findViewById(R.id.photo_scroll);
        headLine = (TextView) view.findViewById(R.id.photo_item_headline);
        caption = (TextView) view.findViewById(R.id.photo_item_caption);
        fullCredits = (TextView) view.findViewById(R.id.photo_credits);
        errorCurtain = view.findViewById(R.id.native_photo_item_error_curtain);
        errorMessage = (TextView) errorCurtain.findViewById(R.id.loading_failed_curtain_message);
        ImageButton backButton = (ImageButton) view.findViewById(R.id.photo_back_button);
        backButton.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });
        final CharSequence caption = getCaption();
        if (TextUtils.isEmpty(caption)) {
            captionLayout.setVisibility(View.GONE);
        } else {
            this.caption.setMaxLines(maxLines);
            this.caption.setText(caption);
            this.caption.setOnClickListener(new TextToggler(this.caption));
        }
        imageContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    oldX = event.getX();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (Math.abs(oldX - event.getX()) < SENSITIVITY) {
                        uiVisibility = !uiVisibility;
                        setUiVisibility(uiVisibility);
                    }
                }
                image.dispatchTouchEvent(event);
                return true;
            }
        });
        loadImage();
    }

    private CharSequence getCaption() {
        return getArguments().getString(EXTRA_IMAGE_CAPTION);
    }

    public void setUiVisibility(boolean uiVisibility) {
        this.uiVisibility = uiVisibility;
        if (!TextUtils.isEmpty(getCaption())) {
            captionLayout.setVisibility(uiVisibility ? View.VISIBLE : View.GONE);
        }
    }

    private void loadImage() {
        final String imageUrl = getImageUrl();
        ImageLoaderProvider provider = (ImageLoaderProvider) getActivity();
        AnimatedImageLoader imageLoader = provider.getImageLoader();
        imageLoader.get(imageUrl, new AnimatedImageLoader.AnimatedImageListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                setErrorView();
            }

            @Override
            public void onResponse(AnimatedImageLoader.AnimatedImageContainer response, boolean isImmediate) {
                if (response == null || image == null) {
                    return;
                }

                Object source = response.getData();

                if (source == null) {
                    return;
                }

                if (!imageUrl.equals(response.getRequestUrl())) {
                    return;
                }

                if (source instanceof Bitmap) {
                    setImageView((Bitmap) response.getData());
                } else if (source instanceof Movie) {
                    image.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    image.setImageDrawable(new MovieDrawable((Movie) source));
                    errorCurtain.setVisibility(View.GONE);
                    image.setVisibility(View.VISIBLE);
                    progress.setVisibility(View.GONE);
                    if (uiVisibility) {
                        textContainer.setVisibility(View.VISIBLE);
                    }
                }
            }

            private void setErrorView() {
                if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
                    String message = isConnectedOrConnecting(getActivity()) ?
                            getString(R.string.gallery_feature_is_unavailable_msg) :
                            getString(R.string.gallery_feature_is_unavailable_no_connection_msg);
                    image.setVisibility(View.GONE);
                    progress.setVisibility(View.GONE);
                    textContainer.setVisibility(View.GONE);
                    errorCurtain.setVisibility(View.VISIBLE);
                    errorMessage.setText(message);
                }
            }

            private void setImageView(Bitmap bitmap) {
                image.setImageBitmap(bitmap);
                errorCurtain.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
                if (uiVisibility) {
                    textContainer.setVisibility(View.VISIBLE);
                }
            }
        }, 0, 0);
    }

    class TextToggler implements View.OnClickListener {

        final TextView textView;
        private final int maxLines;
        private boolean expanded = false;

        TextToggler(TextView textView) {
            this.textView = textView;
            maxLines = textView.getMaxLines();
        }

        @Override
        public void onClick(View v) {
            if (expanded) {
                textView.setMaxLines(maxLines);
            } else {
                textView.setMaxLines(Integer.MAX_VALUE);
            }
            expanded = !expanded;
        }
    }

    private String getImageUrl() {
        return getArguments().getString(EXTRA_IMAGE_URL);
    }

    private static class MovieDrawable extends Drawable {
        private final Movie _movie;
        private final Paint _paint = new Paint();

        MovieDrawable(Movie movie) {
            _movie = movie;
            _movie.setTime(0);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (!canvas.isHardwareAccelerated()) {
                _movie.draw(canvas, 0, 0, _paint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            _paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            _paint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return _movie.isOpaque() ? PixelFormat.OPAQUE : PixelFormat.RGB_888;
        }

        @Override
        public int getIntrinsicWidth() {
            return _movie.width();
        }

        @Override
        public int getIntrinsicHeight() {
            return _movie.height();
        }
    }
}
