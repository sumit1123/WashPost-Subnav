package com.wapo.flagship.features.sections;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.wapo.flagship.features.grid.Tracking;
import com.wapo.flagship.features.sections.model.Section;
import com.wapo.view.ParentScroll;

import java.util.List;

public class SectionsPagerView extends ViewPager implements ParentScroll {
    @Nullable
    private SectionsFrontAdapter adapter = null;
    private FragmentManager fragmentManager;
    private boolean shouldAllowScroll = true;
    private boolean isLowDataMode = false;

    public SectionsPagerView(Context context) {
        this(context, null);
    }

    public SectionsPagerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOffscreenPageLimit(1);
    }

    public void init(@NonNull FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public void update(List<Section> sections) {
        if (fragmentManager == null) {
            throw new IllegalStateException("Not initialized. Call init method first");
        }
        if (adapter == null) {
            adapter = new SectionsFrontAdapter(getContext(), fragmentManager, sections);
            setAdapter(adapter);
        } else {
            adapter.setSections(sections);
        }
    }

    public Tracking getCurrentPageTracking() {
        return getPageTracking(getCurrentItem());
    }

    public Tracking getPageTracking(int pos) {
        if (adapter != null) {
            BaseSectionFragment ssf = adapter.getFragment(pos);
            if (ssf != null) {
                return ssf.getTracking();
            }
        }
        return null;
    }

    public BaseSectionFragment getCurrentFragment() {
        return adapter == null ? null : adapter.getFragment(getCurrentItem());
    }

    public String getSectionTitle() {
        return adapter == null ? null : adapter.getPageTitle(getCurrentItem()).toString();
    }
    
    public String getSectionDisplayName() {
        return adapter == null ? null : adapter.getItem(getCurrentItem()).getSectionDisplayName();
    }
    public String getHierarchy() {
        if (getCurrentPageTracking() != null) {
            return getCurrentPageTracking().getHierarchy();
        }
        return "";
    }

    public void onPageSelected(int pos) {
        if (adapter != null) {
            adapter.onPageSelected(pos);
        }
    }

    @Override
    public void setShouldAllowScroll(boolean shouldAllowScroll) {
        this.shouldAllowScroll = shouldAllowScroll;
    }

    public void setIsLowDataMode(boolean isLowDataMode) {
        this.isLowDataMode = isLowDataMode;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isLowDataMode) {
            return false;
        }
        // There are cases where touch has been cancelled with MotionEvent.ACTION_CANCEL action
        // at child views and then they don't get a chance to reset the shouldAllowScroll flag.
        // So resetting shouldAllowScroll flag here immediately after touch has been released.
        if (!shouldAllowScroll && (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP)) {
            setShouldAllowScroll(true);
            return false;
        }
        if (!shouldAllowScroll) {
            return false;
        }
        try {
            return super.onInterceptTouchEvent(event);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return shouldAllowScroll && super.onTouchEvent(event);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        shouldAllowScroll = !disallowIntercept;
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP ||
                event.getActionMasked() == MotionEvent.ACTION_POINTER_UP ||
                event.getActionMasked() == MotionEvent.ACTION_CANCEL
        ) {
            setShouldAllowScroll(true);
        }
        return super.dispatchTouchEvent(event);
    }
}
