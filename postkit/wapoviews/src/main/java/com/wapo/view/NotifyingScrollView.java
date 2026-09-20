package com.wapo.view;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by a.glyzin on 18.04.2014.
 */
public class NotifyingScrollView extends ScrollView {

    public interface OnScrollChangedListener {
        void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt);
    }

    private List<OnScrollChangedListener> onScrollChangedListeners = new CopyOnWriteArrayList<>();

    public NotifyingScrollView(Context context) {
        super(context);
    }

    public NotifyingScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotifyingScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void addOnScrollChangedListener(OnScrollChangedListener listener) {
        onScrollChangedListeners.add(listener);
    }

    public void removeOnScrollChangedListener(OnScrollChangedListener listener) {
        onScrollChangedListeners.remove(listener);
    }

    public void removeAllOnScrollChangedListeners() {
        onScrollChangedListeners.clear();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super. onScrollChanged(l, t, oldl, oldt);


        // Kindle Fire stuff
        // Upper bounce
        if(t < 0) {
          return;
        }
        // Lower bounce
        View child = getChildAt(0);
        int scroll = t + getMeasuredHeight();
        if(child != null && scroll >= child.getMeasuredHeight()) {
            return;
        }
        // End of Kindle Fire stuff

        for (OnScrollChangedListener listener : onScrollChangedListeners) {
            if (listener != null) {
                listener.onScrollChanged(this, l, t, oldl, oldt);
            }
        }

        if (child instanceof OnScrollChangedListener) {
            ((OnScrollChangedListener) child).onScrollChanged(this, l, t, oldl, oldt);
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        View child = getChildCount() == 0 ? null : getChildAt(0);
        if (child instanceof OnScrollChangedListener) {
            ((OnScrollChangedListener) child).onScrollChanged(this, 0, ss.scrollPosition, 0, 0);
        }
        if(child instanceof HeightGetter){
            final double spp = ss.scrollPercentPosition;
            ((HeightGetter)child).setOnLayoutChangeListener(new FlowableLayout.OnLayoutChanged() {
                @Override
                public void onLayoutChanged(View view) {
                    int scrollYPosition = (int)((double)((HeightGetter)view).getTotalHeight()*spp);
                    setScrollY(scrollYPosition);
                }
            });
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.scrollPosition = getScrollY();
        View child = getChildCount() == 0 ? null : getChildAt(0);
        if (child instanceof HeightGetter){
            state.scrollPercentPosition = (double)getScrollY()/(double)((HeightGetter)getChildAt(0)).getTotalHeight();
        }

        return state;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int hMode = MeasureSpec.getMode(heightMeasureSpec);

        final int width  = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        int measuredHeight = height;

        if (getChildCount() > 0) {
            final View child = getChildAt(0);

            final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();

            int paddingTop = getPaddingTop();
            int paddingBottom = getPaddingBottom();
            int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                    getPaddingLeft() + getPaddingRight(), lp.width);

            int childHeight = height;
            childHeight -= paddingTop;
            childHeight -= paddingBottom;

            int childHeightMeasureSpec =
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.UNSPECIFIED);

            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

            measuredHeight = Math.min(measuredHeight, child.getMeasuredHeight() + paddingTop + paddingBottom);
        }

        if (hMode == MeasureSpec.EXACTLY) {
            measuredHeight = height;
        } else if (hMode == MeasureSpec.AT_MOST) {
            measuredHeight = Math.min(measuredHeight, height);
        }

        setMeasuredDimension(width, measuredHeight);
    }

    private static class SavedState extends BaseSavedState {
        public int scrollPosition;
        public double scrollPercentPosition;

        SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            scrollPosition = source.readInt();
            scrollPercentPosition = source.readDouble();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(scrollPosition);
            dest.writeDouble(scrollPercentPosition);
        }

        @Override
        public String toString() {
            return "NotifyingScrollView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " scrollPosition=" + scrollPosition + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public interface HeightGetter{
        public int getTotalHeight();
        public void setOnLayoutChangeListener(FlowableLayout.OnLayoutChanged onLayoutChangeListener);
    }
}
