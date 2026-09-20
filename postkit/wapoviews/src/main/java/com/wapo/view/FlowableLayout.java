package com.wapo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;
import android.widget.ScrollView;


public class FlowableLayout extends ViewGroup implements NotifyingScrollView.OnScrollChangedListener, NotifyingScrollView.HeightGetter {
    private static final int PRELOADING_VIEWS_AMOUNT = 1;

    public static final int FLOAT_NONE = 0;
    public static final int FLOAT_RIGHT = 1;
    public static final int FLOAT_LEFT = -1;

    private int _parentScrollPos = 0;
    private int _viewportHeight = 0;
    private int _totalHeight;
    private int _countViews;
    private boolean needLoadOldViews = false;

    private boolean fullRenderingAllowed = false;

    private BaseAdapter adapter = null;
    private final DataSetObserver observer;
    private OnLayoutChanged onLayoutChanged;

    public FlowableLayout(Context context) {
        this(context, null);
    }

    public FlowableLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowableLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        observer = new DataSetObserver(){
            @Override
            public void onChanged() {
                refreshViews();
            }
        };
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int wm = MeasureSpec.getMode(widthMeasureSpec);
        final int ws = MeasureSpec.getSize(widthMeasureSpec);
        final int hm = MeasureSpec.getMode(heightMeasureSpec);

        //TODO Add support for at most and exactly for height
        if (hm == MeasureSpec.UNSPECIFIED) {
            _viewportHeight = MeasureSpec.getSize(heightMeasureSpec);
        }
        if (wm == MeasureSpec.UNSPECIFIED) {
            throw new IllegalArgumentException("Width Measurespec must NOT be Unspecified");
        }


        final int availableWidth = ws - getPaddingLeft() - getPaddingRight();

        int widthObstruction = 0;
        int heightObstruction = 0;
        _totalHeight = getPaddingTop() + getPaddingBottom();
        int lastFloat = FLOAT_NONE;

        int heightLimit = _parentScrollPos + _viewportHeight;

        int i = 0;
        if (!needLoadOldViews)
            _countViews = 0;
        final int childCount = getChildCount();
        final int count = adapter == null ? 0 : adapter.getCount();
        for (; fullRenderingAllowed || i < PRELOADING_VIEWS_AMOUNT; i++) {
            //
            // we are remeasure all previously added views OR
            // add a new views until they fit into screen
            //
            boolean b1 = (i >= childCount) && (i >= count || (widthObstruction == 0 && heightObstruction == 0 && _viewportHeight > 0 && _totalHeight >= heightLimit));
            boolean b2 = (i >= _countViews || i >= count) || !needLoadOldViews;
            if (b1 && b2) {
                needLoadOldViews = false;
                break;
            }
            View child = null;
            if (i == getChildCount()) {
                if (!needLoadOldViews)
                    _countViews = i+1;
                child = adapter.getView(i, null, this);
                assert child != null;
                final ViewGroup.LayoutParams lp = child.getLayoutParams();
                addViewInLayout(child, -1, lp != null ? lp : generateDefaultLayoutParams());
            } else {
                child = getChildAt(i);
            }

            if (child instanceof FlowableView) {
                FlowableView flowableChild = (FlowableView) child;

                flowableChild.setFlowObstruction(widthObstruction, heightObstruction, lastFloat);

                FlowableLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
                child.measure(MeasureSpec.makeMeasureSpec(availableWidth - lp.leftMargin - lp.rightMargin, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

                int additionalHeight = child.getMeasuredHeight() - heightObstruction;

                if(additionalHeight > 0) {
                    _totalHeight += additionalHeight;
                }
            } else {
                widthObstruction = 0;
                heightObstruction = 0;
                if (child != null) {
                    LayoutParams childParams = (LayoutParams) child.getLayoutParams();

                    int childWidthMS;
                    assert childParams != null;
                    switch (childParams.width) {
                        case ViewGroup.LayoutParams.MATCH_PARENT:
                            childWidthMS = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY);
                            break;
                        case ViewGroup.LayoutParams.WRAP_CONTENT:
                            childWidthMS = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST);
                            break;
                        default:
                            childWidthMS = MeasureSpec.makeMeasureSpec(childParams.width, MeasureSpec.EXACTLY);
                            break;
                    }

                    int childHeightMS;
                    if (childParams.height == ViewGroup.LayoutParams.MATCH_PARENT || childParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                        childHeightMS = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                    } else {
                        childHeightMS = MeasureSpec.makeMeasureSpec(childParams.height, MeasureSpec.EXACTLY);
                    }

                    child.measure(childWidthMS, childHeightMS);

                    _totalHeight += child.getMeasuredHeight();

                    if (childParams.floatType != FLOAT_NONE) {
                        heightObstruction = child.getMeasuredHeight() + childParams.topMargin + childParams.bottomMargin;
                        widthObstruction = child.getMeasuredWidth() + childParams.rightMargin + childParams.leftMargin;
                    }

                    lastFloat = childParams.floatType;
                }
            }
        }

        setMeasuredDimension(ws, i == count ? _totalHeight : Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        if(!changed) return;//Early exit, we don't care if the layout hasn't changed
        int left = getPaddingLeft();
        int right = getMeasuredWidth() - getPaddingRight();
        int top = getPaddingTop();
        int currentFloatTop = top;

        int lastFloatedViewHeight = 0;  //TBC: I know this is not used, it will be used to support multiple floatable elements at some point in the future

        for(int i = 0; i < getChildCount(); i ++){
            View child = getChildAt(i);


            if(child instanceof FlowableView){
                int bottom = top + child.getMeasuredHeight();
                child.layout(left, top, right, bottom);

                top = bottom + 1; //TODO This doesn't allow for multiple floatable elements but onMeasure will not support that either yet


            } else {
                if(currentFloatTop != top) top = currentFloatTop = Math.max(top, currentFloatTop); //Non-floatable elements should always move down both to whichever is lower

                assert child != null;
                final LayoutParams childParams = (LayoutParams) child.getLayoutParams();

                assert childParams != null;
                lastFloatedViewHeight = childParams.floatType != FLOAT_NONE ? child.getMeasuredHeight() + childParams.topMargin + childParams.bottomMargin : 0;


                final int bottom = currentFloatTop + childParams.topMargin + child.getMeasuredHeight();
                if(childParams.floatType == FLOAT_NONE){
                    child.layout(left + childParams.leftMargin, currentFloatTop + childParams.topMargin, right - childParams.rightMargin, bottom);
                    top = currentFloatTop = bottom + childParams.bottomMargin + 1; //Since this takes up all the space we want to move the flowable text top to be the same as the float top
                } else {
                    final int currentLeft = childParams.floatType == FLOAT_LEFT ? left + childParams.leftMargin : right - childParams.rightMargin - child.getMeasuredWidth();

                    final int currentRight = currentLeft + child.getMeasuredWidth();

                    child.layout(currentLeft, currentFloatTop + childParams.topMargin, currentRight, bottom);

                    currentFloatTop = bottom + childParams.bottomMargin + 1;
                }

            }

        }
        if (getOnLayoutChanged() != null)
            getOnLayoutChanged().onLayoutChanged(this);
    }

    //LayoutParam Stuff Below:

    // Override to allow type-checking of LayoutParams.
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p != null ? new LayoutParams(p) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        assert getContext() != null;

        TypedArray a = getContext().obtainStyledAttributes(attrs, new int[]{R.attr.floatSide});
        assert a != null;

        final LayoutParams layoutParams = generateLayoutParams(super.generateLayoutParams(attrs));
        assert layoutParams != null;

        layoutParams.setFloatType(a.getInt(0, FLOAT_NONE));

        a.recycle();
        return layoutParams;
    }

    /**
     * Adds to the result of {@link android.widget.RelativeLayout#generateDefaultLayoutParams()}
     */
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new FlowableLayout.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, FLOAT_NONE);
    }

    @Override
    public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
        _parentScrollPos = t;
        if (_parentScrollPos + _viewportHeight > _totalHeight) {
            requestLayout();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.isFullRendering = fullRenderingAllowed;
        state.countViews = _countViews;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        super.onRestoreInstanceState(((SavedState) state).getSuperState());
        fullRenderingAllowed = ((SavedState)state).isFullRendering;
        _countViews = ((SavedState)state).countViews;
        needLoadOldViews = true;
    }

    public void setAdapter(BaseAdapter adapter){
        if (this.adapter != null) {
            this.adapter.unregisterDataSetObserver(this.observer);
        }

        this.adapter = adapter;
        if (this.adapter == null) {
            removeAllViewsInLayout();
            return;
        }

        if (this.adapter.getCount() > 0) {
            observer.onChanged();  //If the adapter already has data then we call it right away
        }
        this.adapter.registerDataSetObserver(this.observer);
    }

    public void refreshViews() {
        removeAllViewsInLayout();
        requestLayout();
    }

    public boolean isFullRenderingAllowed() {
        return fullRenderingAllowed;
    }

    public void setFullRenderingAllowed(boolean fullRenderingAllowed){
        this.fullRenderingAllowed = fullRenderingAllowed;
        requestLayout();
    }

    @Override
    public int getTotalHeight() {
        return  _totalHeight;
    }

    @Override
    public void setOnLayoutChangeListener(OnLayoutChanged onLayoutChangeListener) {
        setOnLayoutChanged(onLayoutChangeListener);
    }

    public OnLayoutChanged getOnLayoutChanged() {
        return onLayoutChanged;
    }

    public void setOnLayoutChanged(OnLayoutChanged onLayoutChanged) {
        this.onLayoutChanged = onLayoutChanged;
    }


    public static class LayoutParams extends MarginLayoutParams {

        private int floatType;

        public LayoutParams(ViewGroup.LayoutParams layoutParams, int floatType) {
            super(layoutParams);
            setFloatType(floatType);
        }

        public LayoutParams(int w, int h, int floatType) {
            super(w, h);
            setFloatType(floatType);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
            setFloatType(FLOAT_NONE);
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);

        }

        public int getFloatType() {
            return floatType;
        }

        public void setFloatType(int floatType) {
            this.floatType = floatType;
            if(floatType == FLOAT_NONE) this.width = ViewGroup.LayoutParams.MATCH_PARENT;

        }
    }

    static class SavedState extends BaseSavedState {
        public boolean isFullRendering;
        public int countViews;

        public SavedState(Parcel source) {
            super(source);
            isFullRendering = source.readInt() != 0;
            countViews = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt((isFullRendering ? 1 : 0));
            out.writeInt(countViews);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    public interface OnLayoutChanged{
        public void onLayoutChanged(View view);
    }
}
