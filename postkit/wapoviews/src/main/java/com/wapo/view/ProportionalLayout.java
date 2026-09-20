package com.wapo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;

public class ProportionalLayout extends FrameLayout {
    private float _aspectRatio = 0;

    public ProportionalLayout(Context context) {
        super(context);
    }

    public ProportionalLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProportionalLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, new int[]{R.attr.aspectRatio});
        try {
            if (a.hasValue(0)) {
                _aspectRatio = a.getFloat(0, 0);
            }
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (_aspectRatio <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);

        if (wMode == EXACTLY && hMode == EXACTLY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        int h = height;
        int w = height == 0 ? width : Math.round(h * _aspectRatio);

        if (wMode == EXACTLY) {
            w = width;
        } else if (wMode == AT_MOST) {
            w = Math.min(w, width);
        }

        h = Math.round(w / _aspectRatio);

        if (hMode == EXACTLY) {
            h = height;
        } else {
            int h1 = h;
            if (hMode == AT_MOST) {
                h1 = Math.min(h1, height);
            }
            if (h1 < h) {
                w = Math.round(h1 * _aspectRatio);
                if (wMode == AT_MOST) {
                    w = Math.min(w, width);
                }
            }
            h = h1;
        }


        super.onMeasure(MeasureSpec.makeMeasureSpec(w, EXACTLY), MeasureSpec.makeMeasureSpec(h, EXACTLY));
    }

    public float getAspectRatio() {
        return _aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        if (_aspectRatio != aspectRatio) {
            _aspectRatio = aspectRatio;
            requestLayout();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), _aspectRatio);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.superState);
            _aspectRatio = ss.aspectRatio;
        } else {
            super.onRestoreInstanceState(state);
        }
    }




    public static class SavedState implements Parcelable {
        public final Parcelable superState;
        public final float aspectRatio;

        public SavedState(Parcelable superState, float aspectRatio) {
            this.superState = superState;
            this.aspectRatio = aspectRatio;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(superState, flags);
            dest.writeFloat(aspectRatio);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(
                        source.readParcelable(SavedState.class.getClassLoader()),
                        source.readFloat()
                );
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
