package com.wapo.flagship.features.photos;

import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import android.view.View;
import android.view.Window;

import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.features.shared.activities.BaseActivity;
import com.wapo.flagship.features.shared.fragments.TopBarFragment;
import com.wapo.flagship.util.tracking.Measurement;
import com.washingtonpost.android.R;
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader;
import com.washingtonpost.android.volley.toolbox.ImageLoaderProvider;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NativePhotoActivity extends BaseActivity implements ImageLoaderProvider {
    public static final String photoUrl = NativePhotoActivity.class.getSimpleName() + ".photoUrl";
    public static final String photoCaption = NativePhotoActivity.class.getSimpleName() + ".photoCaption";
    private static final String TOP_BAR_FRAGMENT_TAG = "top-bar-fragment";
    private TopBarFragment topFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        setContentView(R.layout.native_photo_item);

        if (savedInstanceState == null) {
            String caption = getIntent().getStringExtra(photoCaption);
            String url = getIntent().getStringExtra(photoUrl);
            int maxLines = getResources().getInteger(com.washingtonpost.android.articles.R.integer.gallery_caption_visible_lines);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.root, PhotoFragment.create(url, caption, maxLines))
                    .commit();
        }

        initActionBar();
    }

    private void initActionBar() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            topFragment = (TopBarFragment) fragmentManager.findFragmentByTag(TOP_BAR_FRAGMENT_TAG);
            if (topFragment == null) {
                topFragment = new TopBarFragment();
                transaction.add(topFragment, TOP_BAR_FRAGMENT_TAG);
            }
        }
        if (transaction != null)
            transaction.commit();
    }

    @Override
    protected void onResume() {
        if (topFragment != null) {
            View view = topFragment.getView();
            ActionBar actionBar = getSupportActionBar();
            if (view != null && actionBar != null) {
                actionBar.setCustomView(view);
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            }
        }
        super.onResume();
        Measurement.resumeCollection(this);
    }

    @Override
    public AnimatedImageLoader getImageLoader() {
        return FlagshipApplication.getInstance().getAnimatedImageLoader();
    }
}
