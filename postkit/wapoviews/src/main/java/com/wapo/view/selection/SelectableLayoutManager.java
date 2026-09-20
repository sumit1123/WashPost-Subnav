package com.wapo.view.selection;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class SelectableLayoutManager extends LinearLayoutManager {

    //private final ScrollBarController scrollBarController = new ScrollBarController();
    private SelectionController selectionController;
    private SelectableRecyclerView viewGroup;

    public SelectableLayoutManager(Context context) {
        super(context);
    }

    public SelectableLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public SelectableLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SelectionController getSelectionController() {
        return selectionController;
    }

    public void setSelectionController(SelectionController selectionController) {
        this.selectionController = selectionController;
    }

    @Override
    public void addView(View child, int index) {
        super.addView(child, index);
        selectionController.addViewToSelectable(child);
    }

    @Override
    public void offsetChildrenVertical(int dy) {
        super.offsetChildrenVertical(dy);
        selectionController.checkHandlesPosition();
    }

    @Override
    public void addDisappearingView(View child, int index) {
        super.addDisappearingView(child, index);
        selectionController.addViewToSelectable(child);
    }


    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        selectionController.checkSelectableList();
        selectionController.checkHandlesPosition();
    }

    @Override
    public void removeViewAt(int index) {
        super.removeViewAt(index);
        selectionController.checkSelectableList();
    }

   /*

    @Override
    public void layoutDecorated(View child, int left, int top, int right, int bottom) {
        super.layoutDecorated(child, left, top, right, bottom);
        selectionController.addViewToSelectable(child);
    }

    @Override
    public void layoutDecoratedWithMargins(View child, int left, int top, int right, int bottom) {
        super.layoutDecoratedWithMargins(child, left, top, right, bottom);
        selectionController.addViewToSelectable(child);
    }
*/

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void setViewGroup(SelectableRecyclerView viewGroup) {
        this.viewGroup = viewGroup;
    }

  /*  @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        return scrollBarController.computeVerticalScrollOffset(this, state);
    }

    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return scrollBarController.computeVerticalScrollExtent(this, state);
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        return scrollBarController.computeVerticalScrollRange(this, state);
    }*/
}
