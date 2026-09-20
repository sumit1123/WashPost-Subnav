package com.wapo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class RelativeLayoutVL extends RelativeLayout {
    private OnVisibilityChangedListener _visibiliyListener;

    public RelativeLayoutVL(Context context) {
        super(context);
    }

    public RelativeLayoutVL(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RelativeLayoutVL(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (_visibiliyListener != null) {
            _visibiliyListener.onVisibilityChanged(changedView, visibility);
        }
    }

    public void setOnVisibilityChangedListener(OnVisibilityChangedListener listener) {
        _visibiliyListener = listener;
    }

    public interface OnVisibilityChangedListener {
        void onVisibilityChanged(View changedView, int visibility);
    }
}
