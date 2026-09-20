/*
 * Copyright (c) 2018. The Washington Post. All right reserved. Created by Artur Glyzin.
 */

package com.wapo.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * This class layouts its children from left to right with wrapping children to a new row if there is not enough space for the next child.
 */
public class HorizontalFlexLayout extends ViewGroup {

    public HorizontalFlexLayout(Context context) {
        super(context);
    }

    public HorizontalFlexLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalFlexLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int availableWidth = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int currentAvailableWidth = availableWidth;
        int childCount = getChildCount();
        int heightTotal = 0;
        int rowStartsAt = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childWSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST);
            measureChild(child, childWSpec, heightMeasureSpec);
            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            if (currentAvailableWidth >= childWidth) {
                currentAvailableWidth -= childWidth;
                heightTotal = Math.max(heightTotal, childHeight + rowStartsAt);
            } else {
                //new row
                currentAvailableWidth = availableWidth - childWidth;
                rowStartsAt = heightTotal;
                heightTotal += childHeight;
            }
        }
        setMeasuredDimension(availableWidth, heightTotal + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = getChildCount();
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int width = getMeasuredWidth();
        int right = width - getPaddingRight();
        int bottomLine = 0;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            int spaceLeft = right - left;
            if (childWidth <= spaceLeft) {
                child.layout(left + lp.leftMargin, top + lp.topMargin, left + lp.leftMargin + childWidth, top + lp.topMargin + childHeight);
                left += lp.leftMargin + childWidth + lp.rightMargin;
                bottomLine = Math.max(bottomLine, top + childHeight + lp.bottomMargin + lp.topMargin);
            } else {
                left = getPaddingLeft();
                top = bottomLine;
                child.layout(left + lp.leftMargin, top + lp.topMargin, left + lp.leftMargin + childWidth, top + lp.topMargin +  childHeight);
                left += lp.leftMargin + childWidth + lp.rightMargin;
                bottomLine = top + childHeight + lp.bottomMargin + lp.topMargin;
            }
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new HorizontalFlexLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof MarginLayoutParams) {
            return new HorizontalFlexLayout.LayoutParams(((MarginLayoutParams) p));
        }
        return new LayoutParams(p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
