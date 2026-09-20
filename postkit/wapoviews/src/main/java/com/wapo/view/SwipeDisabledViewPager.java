package com.wapo.view;

import android.content.Context;
import androidx.core.view.GestureDetectorCompat;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class SwipeDisabledViewPager extends ViewPager {

    private PagerOnClickListener onclickListener;
    private GestureDetectorCompat gestureDetector;

    public SwipeDisabledViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (onclickListener != null && gestureDetector != null && gestureDetector.onTouchEvent(ev)) {
            onclickListener.onClick();
        }
        return true;
    }

    public void setOnclickListener(PagerOnClickListener onclickListener) {
        this.onclickListener = onclickListener;
    }

    public interface PagerOnClickListener {
        void onClick();
    }
}
