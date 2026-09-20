package com.wapo.flagship.features.pagebuilder;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.text.Html;
import com.wapo.android.commons.util.Logger;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.wapo.flagship.features.grid.model.LiveBlog;
import com.wapo.flagship.features.grid.views.CompoundLabelView;
import com.wapo.view.FlowableTextView;
import com.washingtonpost.android.androidlive.cache.AndroidLiveCache;
import com.washingtonpost.android.androidlive.liveblog.data.ContentManager;
import com.washingtonpost.android.androidlive.liveblog.model.LiveBlogFeed;
import com.washingtonpost.android.androidlive.liveblog.model.PrimeTimeUrl;
import com.washingtonpost.android.sections.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class CellLiveBlogView extends ViewGroup {

    public static final int MIN_ROW_COUNT = 2;
    private static final String TAG = "CellLiveBlogView";
    private LiveBlog liveBlog;
    //being used to create view for a live blog outside of flex feature (old PageBuilder)
    private ContentManager contentManager;
    private String proxyUrl;
    private Subscription dataFetchingSubscription;
    private CellLabelView cellLabelView;
    private TextView titleView;
    private boolean showHeadline;
    private boolean showLabel;
    private int fontStyleNormal;
    private int fontStyleThin;
    private int fontStyleHighLight;
    private int storyViewTop;
    private LiveBlogItemClickListener blogItemClickListener;
    private int constrainedHeight;
    private int widthAdjustment;
    private int heightAdjustment;
    private int floatingType;
    private boolean wrapText = false;
    Rect rect = new Rect();
    private CompoundLabelView compoundLabelView;
    private CompoundLabelView dummyCompoundLabelView = (CompoundLabelView) LayoutInflater
            .from(getContext())
            .inflate(R.layout.fusion_cell_label, this, false);
    private View dummyView = LayoutInflater.from(getContext())
            .inflate(R.layout.layout_live_blog_list_item, this, false);

    private enum LIVE_BLOG_FEATURE_NAMES {
        NORMAL_AND_SECONDARY("Liveblog-normal-and-secondary"),
        BLACK_BACKGROUND("Liveblog-black-background"),
        NO_ARROW("Liveblog-no-arrow"),
        NO_HEADLINE("Liveblog-no headline"),
        LABEL_OFF("Liveblog-label-off"),
        WITH_HEADLINE("Liveblog-with-headline"),
        WITH_MOBILE_APP_HEAD("Liveblog-with-mobile-app-head");

        private String featureName;

        LIVE_BLOG_FEATURE_NAMES(String featureName) {
            this.featureName = featureName;
        }
    }


    public CellLiveBlogView(Context context) {
        this(context, null);
    }

    public CellLiveBlogView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLiveBlogView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CellLiveBlogView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CellLiveBlogView,
                0, 0);

        try {
            fontStyleNormal = a.getResourceId(R.styleable.CellLiveBlogView_headline_normal_font_style, R.style.homepagestory_headline_style_normal);
            fontStyleThin = a.getResourceId(R.styleable.CellLiveBlogView_headline_thin_font_style, R.style.homepagestory_headline_style_thin);
            fontStyleHighLight = a.getResourceId(R.styleable.CellLiveBlogView_headline_thin_font_style, R.style.homepagestory_headline_style_highlight);
        } finally {
            a.recycle();
        }

        CompoundLabelView labelView = (CompoundLabelView) LayoutInflater.from(context)
                .inflate(R.layout.fusion_cell_label, this, false);
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(context.getResources().getDimensionPixelSize(R.dimen.sf_live_blog_label), 0, 0, 0);
        labelView.setLayoutParams(layoutParams);
        compoundLabelView = labelView;
        addView(compoundLabelView);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        inflate(getContext(), R.layout.sf_module_live_blog_view, this);
        cellLabelView = (CellLabelView) findViewById(R.id.label);
        titleView = (TextView) findViewById(R.id.live_blog_title);
    }

    public void setItem(LiveBlog liveBlog, String proxyUrl) {
        this.proxyUrl = proxyUrl;
        this.liveBlog = liveBlog;
        titleView.setVisibility(GONE);
        cellLabelView.setVisibility(GONE);
        if (liveBlog.getCompoundLabel() == null) {
            compoundLabelView.setVisibility(GONE);
        } else {
            compoundLabelView.setVisibility(VISIBLE);
            compoundLabelView.setLabel(liveBlog.getCompoundLabel(), false);
        }
        startLoading();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int maxEntries = 0;
        if (liveBlog != null) {
            maxEntries = Math.max(liveBlog.getNumToShow(), MIN_ROW_COUNT);
        }
        
        int totalH = getPaddingTop() + getPaddingBottom();
        int resolvedWidth = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = resolvedWidth - getPaddingLeft() - getPaddingRight();
        int widthChildSpec;
        if (availableWidth > 0) {
            widthChildSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST);
        } else {
            widthChildSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        int heightChildSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        if (cellLabelView.getVisibility() != View.GONE) {
            cellLabelView.measure(widthChildSpec, heightChildSpec);
            totalH += cellLabelView.getMeasuredHeight();
            ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) cellLabelView.getLayoutParams();
            totalH += lp.topMargin + lp.bottomMargin;
        }

        if (titleView.getVisibility() != View.GONE) {
            titleView.measure(widthChildSpec, heightChildSpec);
            totalH += titleView.getMeasuredHeight();
            ViewGroup.MarginLayoutParams lp = (MarginLayoutParams) titleView.getLayoutParams();
            totalH += lp.topMargin + lp.bottomMargin;
        }

        CompoundLabelView temp1 = dummyCompoundLabelView;
        addView(temp1);
        temp1.setLabel(liveBlog.getCompoundLabel(), false);
        temp1.measure(widthChildSpec, heightChildSpec);
        totalH += temp1.getMeasuredHeight();
        removeView(temp1);

        View temp2 = dummyView;
        addView(temp2);
        FlowableTextView title = (FlowableTextView) temp2.findViewById(com.washingtonpost.android.androidlive.R.id.live_blog_headline);
        title.setLines(2);
        TextView date = (TextView) temp2.findViewById(com.washingtonpost.android.androidlive.R.id.live_blog_time);
        if (liveBlog != null && liveBlog.getShowTimestamps()) {
            date.setLines(1);
            date.setVisibility(View.VISIBLE);
        } else {
            date.setVisibility(View.GONE);
        }
        temp2.measure(widthChildSpec, heightChildSpec);
        totalH += temp2.getMeasuredHeight() * maxEntries;
        removeView(temp2);

        int headlineTop = storyViewTop;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            FlowableTextView titleView = child.findViewById(com.washingtonpost.android.androidlive.R.id.live_blog_headline);
            if (titleView != null) {
                //try checking for each live blog headline top and see if it's still less than constrainedHeight(i.e. bottom of media) or if wrapText is false then continue obstructing
                if (shouldFlowObstruction()) {
                    titleView.setFlowObstruction(widthAdjustment, heightAdjustment, FlowableTextView.FLOAT_RIGHT);
                } else {
                    titleView.setFlowObstruction(0, 0, FlowableTextView.FLOAT_NONE);
                }
            }
            child.measure(widthChildSpec, heightChildSpec); // make sure all children get their sizes
            if (child.getVisibility() == VISIBLE) {
                headlineTop += child.getMeasuredHeight();
                totalH = headlineTop;
            }
        }

        setMeasuredDimension(resolvedWidth, totalH);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int top = getPaddingTop();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                if (isProgressView(child)) {
                    //draw progress at center
                    int progressLeft = (r - l) / 2 - child.getMeasuredWidth() / 2;
                    child.layout(progressLeft, top, progressLeft + child.getMeasuredWidth(), top + child.getMeasuredHeight());
                } else {
                    MarginLayoutParams layoutParams = (MarginLayoutParams) child.getLayoutParams();
                    int childLeft = layoutParams.leftMargin;
                    if (child.getId() == R.id.live_blog_item_tag) {
                        childLeft += getPaddingLeft();
                    }
                    int childTop = top + layoutParams.topMargin;
                    child.layout(childLeft, childTop, childLeft + child.getMeasuredWidth() + layoutParams.rightMargin, childTop + child.getMeasuredHeight());
                    top = child.getBottom() + layoutParams.bottomMargin;
                }
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Logger.d(TAG, "CellLiveBlogView, onAttachedToWindow()");
        startLoading();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Logger.d(TAG, "CellLiveBlogView, onDetachedFromWindow()");
        if (dataFetchingSubscription != null) {
            dataFetchingSubscription.unsubscribe();
            dataFetchingSubscription = null;
        }
        contentManager = null;
    }

    private void startLoading() {
        if (liveBlog == null) return;

        AndroidLiveCache.init();
        final String primetimeURL = liveBlog.getPrimeTimeURL();
        int maxEntries = Math.max(liveBlog.getNumToShow(), MIN_ROW_COUNT);
        List<String> subtypes = null;
        if (liveBlog != null) {
            subtypes = liveBlog.getSubtypes();
        }
        final PrimeTimeUrl primeTimeUrlInfo = new PrimeTimeUrl(primetimeURL, subtypes);

        if (AndroidLiveCache.isLiveBlogFeedAvailable(primeTimeUrlInfo.getCacheKey())) {
            Logger.d(TAG, "Using the cached response");
            List<LiveBlogFeed.LiveBlogFeedItem> liveBlogCache = AndroidLiveCache.getLiveBlogCache(primeTimeUrlInfo.getCacheKey());
            showItems(liveBlogCache);
        }

        if (contentManager == null) {
            contentManager = new ContentManager(primeTimeUrlInfo, proxyUrl, maxEntries, getContext());
            if (dataFetchingSubscription == null || dataFetchingSubscription.isUnsubscribed()) {
                if (!AndroidLiveCache.isLiveBlogFeedAvailable(primeTimeUrlInfo.getCacheKey())) {
                    // do not show the progress bar if cached feed is available
                    showProgress();
                }
                dataFetchingSubscription = contentManager.startFetchingDataPeriodically()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<List<LiveBlogFeed.LiveBlogFeedItem>>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                Logger.e(TAG, "onError", e);
                                showItems(Collections.<LiveBlogFeed.LiveBlogFeedItem>emptyList());
                            }

                            @Override
                            public void onNext(List<LiveBlogFeed.LiveBlogFeedItem> liveBlogFeed) {
                                if (liveBlogFeed == null || liveBlogFeed.isEmpty()) {
                                    return;
                                }
                                Logger.d(TAG, "onNext :: Size of response feed is " + liveBlogFeed.size());
                                AndroidLiveCache.setLiveBlogFeed(primeTimeUrlInfo.getCacheKey(), liveBlogFeed);
                                showItems(liveBlogFeed);
                            }
                        });
            }
        }
    }

    private void showProgress() {
        removeDynamicViews();
        View progressView = getProgressView();
        progressView.setTag(R.id.live_blog_item_tag, "progress-view");
        addView(progressView);
    }

    private boolean isProgressView(View view) {
        return "progress-view".equals(view.getTag(R.id.live_blog_item_tag));
    }

    private void showItems(List<LiveBlogFeed.LiveBlogFeedItem> items) {
        removeDynamicViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
            LiveBlogFeed.LiveBlogFeedItem item = items.get(i);
            View view = getBlogView(item, inflater, i != itemsSize - 1);
            view.setTag(R.id.live_blog_item_tag, "blog-item-view");
            view.setId(R.id.live_blog_item_tag);
            view.setTag(item.getLink());
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getTag() instanceof String) {
                        String link = (String) v.getTag();
                        if (blogItemClickListener != null) {
                            blogItemClickListener.onLiveBlogItemClick(link);
                        }
                    }
                }
            });
            addView(view);
        }
    }

    private void removeDynamicViews() {
        int childCount = getChildCount();
        List<View> viewsToRemove = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            if (isDynamicView(view)) {
                viewsToRemove.add(view);
            }
        }
        for (View view : viewsToRemove) {
            removeView(view);
        }
    }

    private boolean isDynamicView(View view) {
        return view.getTag(R.id.live_blog_item_tag) != null;
    }

    public void setFlowObstruction(int widthAdjustment, int heightAdjustment, int floatType, int constrainedHeight, int storyViewTop, boolean wrapText) {
        this.widthAdjustment = widthAdjustment;
        this.heightAdjustment = heightAdjustment;
        this.constrainedHeight = constrainedHeight;
        this.storyViewTop = storyViewTop;
        this.floatingType = floatType;
        this.wrapText = wrapText;
    }

    private View getBlogView(LiveBlogFeed.LiveBlogFeedItem item, LayoutInflater inflater, boolean showDecorLine) {
        View v = inflater.inflate(R.layout.layout_live_blog_list_item, this, false);
        FlowableTextView title = (FlowableTextView) v.findViewById(com.washingtonpost.android.androidlive.R.id.live_blog_headline);
        TextView date = (TextView) v.findViewById(com.washingtonpost.android.androidlive.R.id.live_blog_time);
        date.setLines(1);
        View decorLine = v.findViewById(R.id.blog_decor_line);
        decorLine.setVisibility(showDecorLine ? VISIBLE : GONE);
        if (date == null) {
            if (liveBlog != null && liveBlog.getShowTimestamps()) {
                title.setText(Html.fromHtml("<font color=\"red\">" + item.getDate() + "</font>" + " " + item.getTitle()));
            } else {
                title.setText(Html.fromHtml("</font>" + item.getTitle()));
            }
        } else {
            title.setText(Html.fromHtml(TextUtils.isEmpty(item.getTitle()) ? "" : item.getTitle()));
            if (liveBlog != null && liveBlog.getShowTimestamps()) {
                date.setText(item.getDate());
                date.setTextColor(ContextCompat.getColor(title.getContext().getApplicationContext(),
                        AndroidLiveCache.IS_NIGHT_MODE ? R.color.live_blog_item_date_color_night : R.color.live_blog_item_date_color));
                date.setVisibility(View.VISIBLE);
            } else {
                date.setVisibility(View.GONE);
            }
        }

        title.setTextColor(ContextCompat.getColor(title.getContext().getApplicationContext(),
                AndroidLiveCache.IS_NIGHT_MODE ? com.washingtonpost.android.androidlive.R.color.live_blog_item_title_color_night : com.washingtonpost.android.androidlive.R.color.live_blog_item_title_color));
        return v;
    }

    private View getProgressView() {
        ProgressBar progressBar = new ProgressBar(getContext());
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progressBar.setPadding(20, 20, 20, 20);
        progressBar.setLayoutParams(lp);
        return progressBar;
    }

    public void setBlogItemClickListener(LiveBlogItemClickListener blogItemClickListener) {
        this.blogItemClickListener = blogItemClickListener;
    }

    public interface LiveBlogItemClickListener {
        void onLiveBlogItemClick(@NonNull String link);
    }

    private boolean shouldFlowObstruction() {
        return floatingType == FlowableTextView.FLOAT_RIGHT && (storyViewTop <= constrainedHeight || !wrapText);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (shouldFlowObstruction()) {
            getLocalVisibleRect(rect);
            // returning true when touch is on the blog view but outside of the blog entries
            // to be handled by the parent view.
            return ev.getX() > rect.right - widthAdjustment && ev.getY() < heightAdjustment;
        }
        return false;
    }
}
