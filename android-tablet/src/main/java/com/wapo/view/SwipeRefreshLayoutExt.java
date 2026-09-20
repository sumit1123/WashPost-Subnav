package com.wapo.view;

import android.content.Context;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.AttributeSet;

public class SwipeRefreshLayoutExt extends SwipeRefreshLayout {

    private Delegate delegate;

    public SwipeRefreshLayoutExt(Context context) {
        super(context);
    }

    public SwipeRefreshLayoutExt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean canChildScrollUp() {
        if (delegate == null) {
            return super.canChildScrollUp();
        }
        return delegate.canChildScrollUp();
    }

    public interface Delegate {
        boolean canChildScrollUp();
    }
}
