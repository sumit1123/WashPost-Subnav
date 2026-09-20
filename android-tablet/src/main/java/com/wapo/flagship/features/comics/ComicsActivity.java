package com.wapo.flagship.features.comics;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import com.wapo.flagship.Utils;
import com.wapo.flagship.content.ContentManager;
import com.wapo.flagship.features.shared.activities.BaseActivity;
import com.wapo.flagship.features.shared.fragments.TopBarFragment;
import com.wapo.flagship.util.UIUtil;
import com.wapo.flagship.util.tracking.Measurement;
import com.wapo.view.TouchImageView;
import com.washingtonpost.android.R;
import com.washingtonpost.android.comics.model.ComicStrip;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import dagger.hilt.android.AndroidEntryPoint;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

@AndroidEntryPoint
public class ComicsActivity extends BaseActivity {

    public static final String ComicsFeedsParam = ComicsActivity.class.getName() + ".feeds";
    public static final String ComicsAuthorNumberParam = ComicsActivity.class.getName() + ".authorNum";
    public static final String ComicsPageNumberParam = ComicsActivity.class.getName() + ".pageNum";
    private static final String TAG = ComicsActivity.class.getCanonicalName();

    private ViewPager2 _verticalPager;
    private ComicsFragment _currentFragment;
    private int [] location = new int[2];
    private TopBarFragment _fragment;
    private Subscription subscription;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_comics);

        final String requestedComic = getIntent().getStringExtra(ComicsAuthorNumberParam);
        final int requestedPageNumber = getIntent().getIntExtra(ComicsPageNumberParam, 0);

        _verticalPager = (ViewPager2) findViewById(R.id.comics_vertical_pager);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();
            _fragment = new TopBarFragment();
            transaction.add(_fragment, "top-bar-fragment");
            transaction.commit();
        }

        subscription = getContentManagerObs()
                .flatMap(new Func1<ContentManager, Observable<List<ComicStrip>>>() {
                    @Override
                    public Observable<List<ComicStrip>> call(ContentManager cm) {
                        return cm.getComicsList();
                    }
                })
                .filter(new Func1<List<ComicStrip>, Boolean>() {
                    @Override
                    public Boolean call(List<ComicStrip> comicStripList) {
                        return comicStripList != null;
                    }
                })
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<ComicStrip>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        finish();
                    }

                    @Override
                    public void onNext(List<ComicStrip> comicStripList) {
                        doOnComicsList(comicStripList, requestedComic, requestedPageNumber);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.unsubscribe(subscription);
        subscription = null;
    }

    private void doOnComicsList(List<ComicStrip> comicStripList, String requestedComic, int requestedPageNumber) {
        if (comicStripList != null && comicStripList.size() > 0) {
            comicStripList = sortComicsStrip(comicStripList);
            VerticalAdapter adapter = new VerticalAdapter(this, getSupportFragmentManager(), requestedPageNumber, comicStripList);
            _verticalPager.setAdapter(adapter);
            _verticalPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    onPageChanged(position);
                }
            });
            _verticalPager.setCurrentItem(findComicInList(comicStripList, requestedComic));
        } else {
            UIUtil.showToast(getString(R.string.feature_is_unavailable_msg));
            finish();
        }
    }

    private static List<ComicStrip> sortComicsStrip(List<ComicStrip> comicsStripList) {
        Collections.sort(comicsStripList, new ComicsStripComparator());
        return comicsStripList;
    }

    @Override
    protected void onResume() {
        Measurement.resumeCollection(this);
        if (_fragment != null) {
            View view = _fragment.getView();
            ActionBar actionBar = getSupportActionBar();
            if (view != null && actionBar != null) {
                actionBar.setCustomView(view);
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
                _fragment = null;
            }
        }
        super.onResume();
    }

    private static int findComicInList(List<ComicStrip> comicStripList, String comicsName) {
        if (TextUtils.isEmpty(comicsName)) return 0;
        for (int i = 0; i < comicStripList.size(); i++) {
            if (comicStripList.get(i) != null && comicsName.equals(comicStripList.get(i).getName())) return i;
        }
        return 0;
    }

    private boolean _isImageFocused = false;
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        float speedX = 0;
        float speedY = 0;

        int action = ev.getAction();
        if (action == MotionEvent.ACTION_MOVE && ev.getHistorySize() > 0) {
            speedX = ev.getX(0) - ev.getHistoricalX(0);
            speedY = ev.getY(0) - ev.getHistoricalY(0);
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            _isImageFocused = false;
            return super.dispatchTouchEvent(ev);
        }

        boolean isHorizontal = Math.abs(speedX) > Math.abs(speedY);

        TouchImageView imageView = _currentFragment == null ? null : _currentFragment.getImageView();
        if (imageView != null) {
            imageView.getLocationInWindow(location);
            float x = ev.getX() - location[0];
            float y = ev.getY() - location[1];
            boolean isWithinBorders = x >= 0 && x <= imageView.getWidth() && y >= 0 && y <= imageView.getHeight();

            if (ev.getPointerCount() > 1 && isWithinBorders) {
                ev.offsetLocation(-location[0], -location[1]);
                return imageView.dispatchTouchEvent(ev);
            }

            int visibility = imageView.getBordersVisibility();
            boolean isLeft = (visibility & TouchImageView.BORDERS_VISIBILITY_LEFT) == TouchImageView.BORDERS_VISIBILITY_LEFT;
            boolean isRight = (visibility & TouchImageView.BORDERS_VISIBILITY_RIGHT) == TouchImageView.BORDERS_VISIBILITY_RIGHT;
            boolean isTop = (visibility & TouchImageView.BORDERS_VISIBILITY_TOP) == TouchImageView.BORDERS_VISIBILITY_TOP;
            boolean isBottom = (visibility & TouchImageView.BORDERS_VISIBILITY_BOTTOM) == TouchImageView.BORDERS_VISIBILITY_BOTTOM;
            boolean isScroll = (isHorizontal && speedX >= 0 && isLeft) || (isHorizontal && speedX <= 0 && isRight) ||
                               (!isHorizontal && speedY >= 0 && isTop) || (!isHorizontal && speedY <= 0 && isBottom);

            if (isWithinBorders && !isScroll) {
                if (!_isImageFocused) {
                    MotionEvent upEvent = MotionEvent.obtain(
                            ev.getEventTime(),
                            ev.getEventTime(),
                            MotionEvent.ACTION_UP,
                            ev.getX(), ev.getY(), ev.getMetaState()
                    );
                    if(upEvent != null) {
                        super.dispatchTouchEvent(upEvent);
                    }
                }
                _isImageFocused = true;
                ev.offsetLocation(-location[0], -location[1]);
                return imageView.dispatchTouchEvent(ev);
            } else if (_isImageFocused) {
                _isImageFocused = false;
                MotionEvent downEvent = MotionEvent.obtain(
                        ev.getDownTime(),
                        ev.getEventTime(),
                        MotionEvent.ACTION_DOWN,
                        ev.getX(), ev.getY(), ev.getMetaState()
                );
                if (downEvent != null) {
                    return super.dispatchTouchEvent(downEvent);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void onPageChanged(int position) {
        VerticalAdapter adapter = (VerticalAdapter)_verticalPager.getAdapter();

        _currentFragment = adapter.createFragment(position);
        if (_currentFragment == null) return;
        TouchImageView imageView = _currentFragment.getImageView();
        if (imageView != null) {
            imageView.setEnabled(true);
        }

        _currentFragment.setTrackComics();
    }


    @Override
    protected boolean showOverlay() {
        return true;
    }

    @Override
    protected int getOverlayLayoutId() {
        return R.layout.activity_comics_overlay;
    }

    public static class VerticalAdapter extends FragmentStateAdapter implements ComicsFragment.OnAttachmentStateListener {
        private HashMap<String, ComicsFragment> _activeFragments = new HashMap<>();
        private boolean _isFirst = true;
        private int _initialPosition;
        private List<ComicStrip> comicStrips;

        public VerticalAdapter(FragmentActivity fragmentActivity, FragmentManager fm, int initialPosition, List<ComicStrip> comicStrips) {
            super(fragmentActivity);
            _initialPosition = initialPosition;
            this.comicStrips = comicStrips;
        }

        @NonNull
        @Override
        public ComicsFragment createFragment(int position) {
            ComicStrip comicStrip = comicStrips.get(position);
            if (comicStrip == null) return null;
            ComicsFragment fragment = _activeFragments.get(comicStrip.getId());
            if (fragment == null) {
                fragment = ComicsFragment.newInstance(comicStrip, _isFirst ? _initialPosition : 0);
                fragment.setOnAttachmentStateListener(this);
                _activeFragments.put(comicStrip.getId(), fragment);
            }
            _isFirst = false;
            return fragment;
        }


        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return comicStrips.size();
        }

        @Override
        public void onAttachmentStateChanged(ComicsFragment fragment, boolean isAttached) {
            ComicStrip comicStrip = fragment.getComicStrip();
            if (comicStrip == null) {
                return;
            }
            String key = comicStrip.getId();
            if (isAttached) {
                _activeFragments.put(key, fragment);
            } else if (_activeFragments.containsKey(key)) {
                _activeFragments.remove(key);
            }
        }
    }
}
