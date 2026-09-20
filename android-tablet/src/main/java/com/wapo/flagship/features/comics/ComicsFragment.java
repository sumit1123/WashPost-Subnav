package com.wapo.flagship.features.comics;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.commons.logs.LogModules;
import com.wapo.android.remotelog.logger.RemoteLog;
import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.views.ZoomOutPageTransformer;
import com.washingtonpost.android.volley.RequestQueue;
import com.washingtonpost.android.volley.Response;
import com.washingtonpost.android.volley.VolleyError;
import com.wapo.flagship.Utils;
import com.wapo.flagship.content.ContentFragment;
import com.wapo.flagship.content.ContentManager;
import com.wapo.flagship.network.request.ComicsImageRequest;
import com.wapo.android.commons.util.Logger;
import com.wapo.flagship.util.tracking.Measurement;
import com.wapo.view.RelativeLayoutVL;
import com.wapo.view.TouchImageView;
import com.washingtonpost.android.R;
import com.washingtonpost.android.comics.model.ComicStrip;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

public class ComicsFragment extends ContentFragment implements RelativeLayoutVL.OnVisibilityChangedListener {
    private static final String TAG = ComicsFragment.class.getName();
    public static final String FEED_CONFIG = "FEED_CONFIG";
    public static final int MaxFeedSize = 7;

    private static final String PositionParam = ComicsFragment.class.getName() + ".Position";
    private static final String FeedParam = ComicsFragment.class.getName() + ".Feed";

    private static final SimpleDateFormat PubDateFormat = new SimpleDateFormat("MMM. dd, yyyy");

    private ComicStrip comicStrip;
    private ViewPager _pager;
    private ViewGroup _pageIdentification;
    private View _indicator;
    private View _pagerLayout;
    private View _descriptionLayout;
    private View _curtain;
    private View _errorCurtain;
    private View _statusContainer;
    private TextView _pubDate;
    private TouchImageView _currentImageView;
    private OnAttachmentStateListener _attStateListener;
    private int currentPos;
    private boolean trackComicsOnInit;
    private ConnectivityManager _networkManager;
    private RelativeLayoutVL _view;
    private String _delayedToastMessage;
    private ComicStrip[] comicStripArray;
    private Subscription subscription;

    public static ComicsFragment newInstance(ComicStrip comicStrip, int pageNum) {
        ComicsFragment f = new ComicsFragment();
        Bundle bdl = new Bundle(1);
        bdl.putParcelable(FEED_CONFIG, comicStrip);
        bdl.putInt(PositionParam, pageNum);
        f.setArguments(bdl);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ComicStrip comicStrip = getComicStrip();
        if (comicStrip == null) {
            Toast.makeText(getActivity(), "Failed to load comics. Please try again later", Toast.LENGTH_SHORT).show();
            return null;
        }
        currentPos = 0;
        _view = (RelativeLayoutVL)inflater.inflate(R.layout.comics_layout, container, false);

        assert _view != null;

        _pager = (ViewPager) _view.findViewById(R.id.horizontal_pager);
        _pagerLayout = _view.findViewById(R.id.pager_layout);
        _descriptionLayout = _view.findViewById(R.id.description_layout);
        _curtain = _view.findViewById(R.id.loading_curtain);
        _curtain.setVisibility(View.VISIBLE);
        _errorCurtain = _view.findViewById(R.id.comics_line_error_curtain);
        _errorCurtain.setVisibility(View.GONE);
        _statusContainer = _view.findViewById(R.id.status_container);
        _statusContainer.setVisibility(View.GONE);
        _pageIdentification = (ViewGroup) _view.findViewById(R.id.page_identification);
        _indicator = _pageIdentification.findViewById(R.id.indicator);
        TextView _byline = (TextView) _descriptionLayout.findViewById(R.id.byline);
        _pubDate = (TextView)_descriptionLayout.findViewById(R.id.date);

        _view.setOnVisibilityChangedListener(this);

        _pager.setPageMargin((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()));
        _pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int posLeft, float offset, int offsetInPix) {
                moveIndicator(posLeft + offset);
            }

            @Override
            public void onPageSelected(int i) {
                onPageChanged(i);
            }
        });
        _pager.setPageTransformer(true, new ZoomOutPageTransformer());
        _currentImageView = null;

        subscription = fetchComicForTheEntireWeek(comicStrip.getId(), savedInstanceState == null ? 0 : savedInstanceState.getInt(PositionParam, 0));

        _byline.setText(ComicsUtils.getByline(comicStrip.getName(), comicStrip.getAuthor()));

        return _view;
    }

    private Subscription fetchComicForTheEntireWeek(final String comicID, final int position) {
        if (TextUtils.isEmpty(comicID)) showErrorMessage("");

        Observable<ContentManager> contentManagerObs = getContentManagerObs();
        if (contentManagerObs == null) {
            return null;
        }

        return contentManagerObs
                .take(1)
                .flatMap(new Func1<ContentManager, Observable<Map<Date, ComicStrip>>>() {
                    @Override
                    public Observable<Map<Date, ComicStrip>> call(ContentManager contentManager) {
                        return contentManager.getSpecComicsListObservable(comicID);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Map<Date, ComicStrip>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Logger.e(TAG, "Error occurred in fetching comics " + e.getMessage());
                        RemoteLog.e(requireContext(), new EventLog.Builder()
                                .setMessage("Specific Comic Load Error")
                                .setModule(LogModules.COMICS)
                                .setErrorMessage(e.getMessage())
                                .set("cause", e.getCause())
                                .set("comic_id", comicID)
                                .build());
                        showErrorMessage(e.getMessage());
                    }

                    @Override
                    public void onNext(Map<Date, ComicStrip> comicsMap) {
                        if (comicsMap != null) {
                            setComicStripList(comicsMap);
                            initFeed();
                            _pager.setCurrentItem(position);
                        }
                    }
                });
    }

    private void setComicStripList(Map<Date, ComicStrip> comicStripMap) {
        if (comicStripMap == null) return;

        List<ComicStrip> comicStripList = new ArrayList<>();
        for (Map.Entry<Date, ComicStrip> h : comicStripMap.entrySet()) {
            if (h.getValue() != null) comicStripList.add(h.getValue());
        }
        comicStripArray = new ComicStrip[comicStripList.size()];
        comicStripList.toArray(comicStripArray);

        for (int i = 0; i < comicStripArray.length/2; i++) {
            ComicStrip temp = comicStripArray[i];
            comicStripArray[i] = comicStripArray[comicStripArray.length - i - 1];
            comicStripArray[comicStripArray.length - i - 1] = temp;
        }
    }

    private void showErrorMessage(String error) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        Logger.e(TAG, error);
        _errorCurtain.setVisibility(View.VISIBLE);
        _statusContainer.setVisibility(View.VISIBLE);
        _curtain.setVisibility(View.GONE);
        String message = isNetworkEnabled() ?
                getString(R.string.feature_is_unavailable_msg) :
                getString(R.string.feature_is_unavailable_no_connection_msg);
        View view = getView();
        if (view != null && view.isShown()) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
            TextView curtainMessage = view.findViewById(R.id.loading_failed_curtain_message);
            curtainMessage.setText(message);
        } else {
            _delayedToastMessage = message;
        }
    }

    @Override
    public void onDestroyView() {
        Utils.unsubscribe(subscription);
        subscription = null;

        super.onDestroyView();
        if (_view != null) {
            _view.setOnVisibilityChangedListener(null);
            _view = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isAdded()) {
            outState.putInt(PositionParam, _pager.getCurrentItem());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (_attStateListener != null) {
            _attStateListener.onAttachmentStateChanged(this, true);
        }
        _networkManager = (ConnectivityManager) activity.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (_attStateListener != null) {
            _attStateListener.onAttachmentStateChanged(this, false);
        }

        //
        // cancel all strip requests
        PageAdapter adapter = _pager == null ? null : (PageAdapter) _pager.getAdapter();
        if (adapter != null) {
            adapter.cancelRequests();
        }
        _networkManager = null;
    }

    @Override
    public void onVisibilityChanged(View changedView, int visibility) {
        if (_delayedToastMessage != null) {
            Toast.makeText(getActivity().getApplicationContext(), _delayedToastMessage, Toast.LENGTH_LONG).show();
            _delayedToastMessage = null;
        }
    }

    private void initFeed() {
        if (getActivity() == null || getActivity().isFinishing()) return;
        _pager.setAdapter(new PageAdapter(comicStripArray, getActivity()));
        _pagerLayout.setVisibility(View.VISIBLE);
        _descriptionLayout.setVisibility(View.VISIBLE);
        _curtain.setVisibility(View.INVISIBLE);
        int pos = _pager.getCurrentItem();
        int size = Math.min(comicStripArray.length, MaxFeedSize);
        if (size < 2) {
            _pageIdentification.setVisibility(View.INVISIBLE);
        } else {
            int width = getResources().getDimensionPixelSize(R.dimen.comics_pager_width) / size;
            ViewGroup.LayoutParams lp = _indicator.getLayoutParams();
            if (lp == null) {
                lp = new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
                _indicator.setLayoutParams(lp);
            } else {
                lp.width = width;
            }

            _indicator.setTranslationX(width * pos);
            _pageIdentification.setVisibility(View.VISIBLE);
        }
        Date pubDate = getComicStripPubDate(getComicStrip(comicStripArray, pos));
        _pubDate.setText(pubDate == null ? "" : PubDateFormat.format(pubDate));
        if (trackComicsOnInit){
            trackComicsOnInit = false;
            trackComics();
        }
    }

    private void onPageChanged(int position) {
        PageAdapter adapter = (PageAdapter) _pager.getAdapter();
        _currentImageView = adapter.getImageView(position);
        Date pubDate = getComicStripPubDate(getComicStrip(comicStripArray, position));
        _pubDate.setText(pubDate == null ? "" : PubDateFormat.format(pubDate));
        moveIndicator(position);
        currentPos = position;
        trackComics();
    }

    private Date getComicStripPubDate(ComicStrip comicStrip) {
        return comicStrip == null ? null : comicStrip.getPublished();
    }

    private void trackComics() {
        ComicStrip item = getComicStrip(comicStripArray, currentPos);
        if (item == null) return;
        String url = item.getUrl();
        String pubDate = null;
        if (item.getPublished() != null){
            pubDate = PubDateFormat.format(item.getPublished());
        }
        if (comicStrip != null) {
            Measurement.trackComics(
                    url,
                    comicStrip.getAuthor(),
                    comicStrip.getName(),
                    pubDate
            );
        }
    }

    public void setTrackComics(){
        if (comicStripArray == null){
            trackComicsOnInit = true;
        } else {
            trackComics();
        }
    }


    private void moveIndicator(float offset) {
        if (comicStripArray == null || comicStripArray.length < 2) {
            return;
        }
        ViewGroup.LayoutParams lp = _indicator.getLayoutParams();
        int size = Math.min(comicStripArray.length, MaxFeedSize);
        if (lp == null) {
            int width = getResources().getDimensionPixelSize(R.dimen.comics_pager_width) / size;
            lp = new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        _indicator.setTranslationX(lp.width * offset);
    }

    public ComicStrip getComicStrip() {
        if (comicStrip == null) {
            Parcelable parcelable = getArguments().getParcelable(FEED_CONFIG);
            if (parcelable instanceof ComicStrip) {
                comicStrip = (ComicStrip) parcelable;
            }
        }
        return comicStrip;
    }

    public TouchImageView getImageView() {
        if (_currentImageView == null) {
            if (_pager == null) {
                return null;
            }
            PageAdapter adapter = (PageAdapter) _pager.getAdapter();
            if (adapter == null) {
                return null;
            }
            _currentImageView = adapter.getImageView(_pager.getCurrentItem());
        }
        return _currentImageView;
    }

    public void setOnAttachmentStateListener(OnAttachmentStateListener _attStateListener) {
        this._attStateListener = _attStateListener;
    }

    private boolean isNetworkEnabled() {
        NetworkInfo ni = _networkManager == null ? null : _networkManager.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    public static synchronized ComicStrip getComicStrip(ComicStrip[] comicStripArray, int position) {
        return (comicStripArray != null && comicStripArray.length > 0 && position >= 0 && position < comicStripArray.length) ? comicStripArray[position] : null;
    }




    private static class PageAdapter extends PagerAdapter {
        private Queue<View> _disposedViews = new BoundedQueue<>(5);
        private SparseArray<View> _currentViews = new SparseArray<>();
        private ComicStrip[] comicsArray;
        private WeakReference<Activity> activityRef;

        public PageAdapter(ComicStrip[] comicStripArray, Activity activity) {
            this.comicsArray = comicStripArray;
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public int getCount() {
            if (comicsArray == null) return 0;
            return Math.min(comicsArray.length, MaxFeedSize);
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        private synchronized String getComicStripURL(ComicStrip comicStrip) {
            return comicStrip == null ? "" : comicStrip.getUrl();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final String newUrl = getComicStripURL(ComicsFragment.getComicStrip(comicsArray, position));
            View view = _currentViews.get(position);
            if (view == null) {
                view = _disposedViews.poll();
                if (view == null) {
                    LayoutInflater inflater = LayoutInflater.from(container.getContext());
                    view = inflater.inflate(R.layout.comics_iamge_layout, container, false);
                }
                _currentViews.put(position, view);
            }

            assert view != null;

            StripViewTag tag = (StripViewTag) view.getTag();
            if (tag == null) {
                tag = new StripViewTag(
                    (ImageView) view.findViewById(R.id.comic_image),
                    view.findViewById(R.id.image_progress),
                    view.findViewById(R.id.comic_error),
                    (TextView) view.findViewById(R.id.loading_failed_curtain_message)
                );
                view.setTag(tag);
            }
            final View layoutView = view;

            if (newUrl != null && !newUrl.equals(tag.url)) {
                tag.errorView.setVisibility(View.GONE);
                tag.progress.setVisibility(View.VISIBLE);
                tag.imageView.setVisibility(View.INVISIBLE);

                RequestQueue queue = FlagshipApplication.getInstance().getRequestQueue();
                if (tag.request != null) {
                    tag.request.cancel();
                }

                tag.request = new ComicsImageRequest(
                        newUrl,
                        new Response.Listener<Bitmap>() {
                            @Override
                            public void onResponse(Bitmap bitmap) {
                                if (activityRef == null || activityRef.get() == null || activityRef.get().isFinishing()) {
                                    return;
                                }

                                StripViewTag tag = (StripViewTag) layoutView.getTag();

                                if (tag == null || tag.request == null) {
                                    return;
                                }

                                tag.imageView.setImageBitmap(bitmap);
                                tag.imageView.setVisibility(View.VISIBLE);
                                tag.progress.setVisibility(View.GONE);
                                tag.errorView.setVisibility(View.GONE);
                                tag.request = null;
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                if (activityRef == null || activityRef.get() == null || activityRef.get().isFinishing()) {
                                    return;
                                }

                                StripViewTag tag = (StripViewTag) layoutView.getTag();

                                tag.imageView.setImageBitmap(null);
                                tag.progress.setVisibility(View.GONE);
                                tag.errorView.setVisibility(View.VISIBLE);
                                if (error.networkResponse == null){
                                    tag.message.setText(R.string.feature_is_unavailable_no_connection_msg);
                                } else {
                                    tag.message.setText(R.string.feature_is_unavailable_msg);
                                }
                                tag.request = null;
                            }
                        }
                );

                queue.add(tag.request);
            } else {
                if (newUrl == null) {
                    tag.imageView.setImageBitmap(null);
                }
                tag.progress.setVisibility(View.GONE);
                tag.imageView.setVisibility(View.VISIBLE);
            }
            tag.url = newUrl;

            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View)object;
            container.removeView(view);

            StripViewTag tag = (StripViewTag) view.getTag();
            if (tag != null && tag.request != null) {
                tag.request.cancel();
                tag.request = null;
            }

            _disposedViews.offer(view);

            _currentViews.remove(position);
        }

        public void cancelRequests() {
            for (int i = 0; i < _currentViews.size(); i++) {
                StripViewTag tag = (StripViewTag) _currentViews.valueAt(i).getTag();
                if (tag != null && tag.request != null) {
                    tag.request.cancel();
                    tag.request = null;
                }
            }
        }

        TouchImageView getImageView(int position) {
            View view = _currentViews.get(position);
            return view == null ?
                    null :
                    (TouchImageView)view.findViewById(R.id.comic_image);
        }
    }

    private static class BoundedQueue<T> extends LinkedList<T> {
        private final int _maxSize;

        public BoundedQueue(int size) {
            _maxSize = size;
        }

        @Override
        public boolean offer(T o) {
            return size() < _maxSize && super.offer(o);
        }
    }

    public interface OnAttachmentStateListener {
        void onAttachmentStateChanged(ComicsFragment fragment, boolean isAttached);
    }

    private static class StripViewTag {
        public final ImageView imageView;
        public final View progress;
        public final View errorView;
        public final TextView message;
        public String url;
        public ComicsImageRequest request;

        private StripViewTag(ImageView imageView, View progress, View errorView, TextView message) {
            this.imageView = imageView;
            this.progress = progress;
            this.errorView = errorView;
            this.message = message;
        }
    }
}
