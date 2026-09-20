package com.wapo.view;

import android.content.Context;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.wapo.view.selection.SelectableTextView;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.wapo.view.FlowableTextView.FLOAT_LEFT;

public class ArticleFlowableView extends FrameLayout implements ITextView {

    private SelectableTextView bottomTextView;
    private SelectableTextView sideTextView;
    private View sideView;
    private CharSequence text = "";
    private float add;
    private float mult;
    private int minTextWidth;
    private int paddigSide;
    private TextFitter textFitter;
    private int floatPosition;
    private float maxObstacleWidthPercent;
    private float minObstacleWidthPercent;
    private boolean measureSideView = true;

    public ArticleFlowableView(Context context) {
        super(context);
        init();
    }

    public ArticleFlowableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArticleFlowableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ArticleFlowableView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        textFitter = new TextFitter();
        sideTextView = new SelectableTextView(getContext());
        bottomTextView = new SelectableTextView(getContext());
        minTextWidth = getContext().getResources().getDimensionPixelSize(R.dimen.flowable_text_view_default_min_text_width);
        maxObstacleWidthPercent = 0.4f;
        minObstacleWidthPercent = 0.3f;
        paddigSide = getContext().getResources().getDimensionPixelSize(R.dimen.default_article_obstruction_padding);
        addView(sideTextView);
        addView(bottomTextView);
    }

    public void addSideView(View sideView) {
        this.sideView = sideView;
        addView(sideView, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int sizeW = getSize(widthMeasureSpec);
        final int effectiveWidth = (sizeW - getPaddingLeft() - getPaddingRight());
        minTextWidth = (int) ((1 - maxObstacleWidthPercent) * effectiveWidth);
        final int minObstacleWidth = (int) (minObstacleWidthPercent * effectiveWidth);
        int widthSideView = sizeW - minTextWidth - paddigSide - getPaddingLeft() - getPaddingRight();
        if (measureSideView) {
            sideView.measure(makeMeasureSpec(widthSideView, AT_MOST), makeMeasureSpec(0, UNSPECIFIED));
            if (sideView.getMeasuredWidth() < minObstacleWidth) {
                sideView.measure(makeMeasureSpec(minObstacleWidth, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
            }
        }
        int boxHeight = sideView.getMeasuredHeight();
        int boxWidth = sideView.getMeasuredWidth();

        int sideTextViewWidth = sizeW - boxWidth - paddigSide - getPaddingLeft() - getPaddingRight();
        divideText(sideTextViewWidth, boxHeight);
        if (sideTextView.getVisibility() == VISIBLE) {
            sideTextView.measure(makeMeasureSpec(sideTextViewWidth, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
        }
        if (bottomTextView.getVisibility() == VISIBLE) {
            int sizeBottomTextView = sizeW - getPaddingLeft() - getPaddingRight();
            bottomTextView.measure(makeMeasureSpec(sizeBottomTextView, EXACTLY), makeMeasureSpec(0, UNSPECIFIED));
        }

        int width = sizeW;
        int height =
                bottomTextView.getVisibility() == VISIBLE ?
                        Math.max(sideView.getMeasuredHeight(), sideTextView.getMeasuredHeight()) + bottomTextView.getMeasuredHeight() + getPaddingTop() + getPaddingBottom() :
                        Math.max(sideView.getMeasuredHeight(), sideTextView.getMeasuredHeight()) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //        super.onLayout(changed, left, top, right, bottom);
        int topSV = getPaddingTop();
        int bottomSV = sideView.getMeasuredHeight() + topSV;
        if (floatPosition ==
                FLOAT_LEFT) {
            int leftSV = getPaddingLeft();
            int rightSV = leftSV + sideView.getMeasuredWidth();

            sideView.layout(leftSV, topSV, rightSV, bottomSV);
            if (sideTextView.getVisibility() == VISIBLE) {
                int leftSVT = rightSV + paddigSide;
                int rightSVT = leftSVT + sideTextView.getMeasuredWidth();

                sideTextView.layout(leftSVT, topSV, rightSVT, sideTextView.getMeasuredHeight());
            }
        } else {
            int leftSVT = getPaddingLeft();
            int rightSVT = sideTextView.getMeasuredWidth() + getPaddingLeft();
            int leftSV = rightSVT + paddigSide;
            int rightSV = rightSVT + paddigSide + sideView.getMeasuredWidth();

            sideView.layout(leftSV, topSV, rightSV, bottomSV);
            if (sideTextView.getVisibility() == VISIBLE) {
                sideTextView.layout(leftSVT, 0, rightSVT, sideTextView.getMeasuredHeight());
            }
        }
        if (bottomTextView.getVisibility() == VISIBLE) {
            bottomSV = bottomSV >= sideTextView.getMeasuredHeight() ? bottomSV : sideTextView.getMeasuredHeight();
            bottomTextView.layout(getPaddingLeft(), bottomSV, bottomTextView.getMeasuredWidth() + getPaddingLeft(), bottomSV + bottomTextView.getMeasuredHeight());
        }
    }


    private void divideText(int firstSideWidth, int firstBoxHeight) {
        final int firstBoxCharLength;

        if (firstSideWidth >= this.minTextWidth) {
            textFitter.setLineSpacingMultiplier(mult);
            textFitter.setLineAdditionalVerticalPadding(add);
            textFitter.setDisplayParametersMeasured(firstBoxHeight, firstSideWidth);
            textFitter.setPaint(sideTextView.getPaint());
            firstBoxCharLength = textFitter.getFittedLength(text, "");
            //Reset to make sure we don't use the same paint twice by mistake
            textFitter.reset();
        } else {
            firstBoxCharLength = 0;
        }

        if (firstBoxCharLength > 0) {
            sideTextView.setVisibility(View.VISIBLE);
            sideTextView.setText(this.text.subSequence(0, firstBoxCharLength));
        } else {
            sideTextView.setVisibility(View.INVISIBLE);
        }

        if (text.length() - firstBoxCharLength > 0) {
            bottomTextView.setText(firstBoxCharLength == 0 ? this.text : this.text.subSequence(firstBoxCharLength, text.length()));
            bottomTextView.setVisibility(View.VISIBLE);
        } else {
            bottomTextView.setVisibility(View.INVISIBLE);
        }
    }


    public void setLineSpacing(float add, float mult) {
        bottomTextView.setLineSpacing(add, mult);
        sideTextView.setLineSpacing(add, mult);
        this.add = add;
        this.mult = mult;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    public void setMovementMethod(MovementMethod method) {
        bottomTextView.setMovementMethod(method);
        sideTextView.setMovementMethod(method);
    }

    public View getSideView() {
        return sideView;
    }

    public void setKey(String key) {
        sideTextView.setKey("side:" + key);
        bottomTextView.setKey("bottom:" + key);
    }

    @Override
    public void setTextColor(int textColor) {
        sideTextView.setTextColor(textColor);
        bottomTextView.setTextColor(textColor);
    }

    public CharSequence getText() {
        return text == null ? "" : text;
    }

    public float getTextSize() {
        return sideTextView.getTextSize();
    }

    public boolean needMoreText(int maxHeight) {
        return ((sideTextView.getMeasuredHeight() + getPaddingTop() + getPaddingBottom()) < maxHeight);
    }

    public void setFloatPosition(int floatPosition) {
        this.floatPosition = floatPosition;
    }

    public void setMeasureSideView(boolean measureSideView) {
        this.measureSideView = measureSideView;
    }
}