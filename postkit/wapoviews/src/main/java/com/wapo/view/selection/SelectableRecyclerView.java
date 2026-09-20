package com.wapo.view.selection;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.recyclerview.widget.RecyclerView;

import com.wapo.view.R;

public class SelectableRecyclerView extends RecyclerView implements SelectableView {

    private final SelectionController selectionController = new SelectionController(this);
    private boolean selectionEnabled;

    public SelectableRecyclerView(Context context) {
        this(context, null);
    }

    public SelectableRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelectableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.SelectableRecyclerView,
                0, 0);

        try {
            selectionEnabled = a.getBoolean(R.styleable.SelectableRecyclerView_textSelectionEnabled, false);
        } finally {
            a.recycle();
        }
    }

    {
        selectionController.setSelectionEnableListener(new SelectionController.SelectionEnableListener() {
            @Override
            public boolean isEnabled() {
                return selectionEnabled;
            }
        });
    }

    public boolean isSelectionEnabled() {
        return selectionEnabled;
    }

    public void setSelectionEnabled(boolean selectionEnabled) {
        this.selectionEnabled = selectionEnabled;
    }

    protected SelectionController getSelectionController(){
        return selectionController;
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
        if (layout instanceof SelectableLayoutManager) {
            ((SelectableLayoutManager) layout).setSelectionController(selectionController);
            //((SelectableLayoutManager) layout).setViewGroup(this);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (selectionController != null) {
            if (selectionController.onTouchEvent(ev)) {
                return true;
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        selectionController.checkHandlesPosition();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        selectionController.drawHandles(canvas);
    }

    public void setSelectionCallback(SelectionCallback selectionCallback) {
        if (selectionController != null) {
            selectionController.setSelectionCallback(selectionCallback);
        }
    }

    @Override
    public void copyTextToClipboard() {
        selectionController.copyTextToClipboard();
    }

    @Override
    public String getSelectedText() {
        return selectionController.getSelectedText();
    }

    @Override
    public boolean isSelectionActive() {
        return selectionController.isSelectionActive();
    }

    @Override
    public void resetSelection() {
        selectionController.resetSelection();
    }
}
