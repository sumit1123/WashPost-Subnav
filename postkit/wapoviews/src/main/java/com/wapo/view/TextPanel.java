package com.wapo.view;

import android.content.Context;
import android.graphics.Rect;
import android.text.*;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import static android.view.View.MeasureSpec.*;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class TextPanel extends StreamModuleView {

    private final SpannableStringBuilder _spannedStringBuilder = new SpannableStringBuilder();
    private final Rect _lineRect = new Rect();

    private int _shortMeasuredWidth;
    private int _shortMeasuredHeight;

    public TextPanel(Context context) {
        super(context);
        create(context, null);
    }

    public TextPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        create(context, attrs);
    }

    @Override
    protected int getDefaultLayout() {
        return R.layout.view_textpanel;
    }

    private void create(Context context, AttributeSet attrs) {
        super.processAttributes(context, attrs);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(getLayout(), this, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean isLiveBlog = false;
        final int wMode = getMode(widthMeasureSpec);
        final int hMode = getMode(heightMeasureSpec);

        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        final int effectiveWidth = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        final int effectiveHeight = Math.max(0, height - getPaddingTop() - getPaddingBottom());

        final int effectiveWidthSpec = makeMeasureSpec(effectiveWidth, wMode);

        int measuredHeight = 0;
        int measuredWidth = 0;
        for (int i = 0, cnt = getChildCount(); i < cnt; i++) {
            if ((getChildAt(i) instanceof LinearLayout)) {
                LinearLayout v = (LinearLayout) getChildAt(i);
                if (v.getVisibility() == GONE) {
                    continue;
                }
                isLiveBlog = true;
                LayoutParams lp = v.getLayoutParams();
                MarginLayoutParams mlp = null;
                if (lp instanceof MarginLayoutParams) {
                    mlp = (MarginLayoutParams) lp;
                }

                if (mlp != null) {
                    measuredHeight += mlp.topMargin + mlp.bottomMargin;
                }

                int remainingHeight = Math.max(0, effectiveHeight - measuredHeight);

                v.measure(
                        getChildMeasureSpec(
                                effectiveWidthSpec,
                                mlp == null ? 0 : (mlp.leftMargin + mlp.rightMargin),
                                lp.width
                        ),
                        getChildMeasureSpec(
                                makeMeasureSpec(remainingHeight, hMode),
                                0,
                                lp.height
                        )
                );

                measuredHeight += v.getMeasuredHeight();
                measuredWidth = Math.max(measuredWidth, v.getMeasuredWidth() + (mlp == null ? 0 : (mlp.leftMargin + mlp.rightMargin)));
                continue;
            }
            TextView v = (TextView) getChildAt(i);
            if (v.getVisibility() == GONE) {
                continue;
            }

            LayoutParams lp = v.getLayoutParams();
            MarginLayoutParams mlp = null;
            if (lp instanceof MarginLayoutParams) {
                mlp = (MarginLayoutParams) lp;
            }

            if (mlp != null) {
                measuredHeight += mlp.topMargin + mlp.bottomMargin;
            }

            int remainingHeight = Math.max(0, effectiveHeight - measuredHeight);

            v.measure(
                    getChildMeasureSpec(
                            effectiveWidthSpec,
                            mlp == null ? 0 : (mlp.leftMargin + mlp.rightMargin),
                            lp.width
                    ),
                    getChildMeasureSpec(
                            makeMeasureSpec(remainingHeight, hMode),
                            0,
                            lp.height
                    )
            );

            if (hMode != UNSPECIFIED) {
                int visibleTextHeight = 0;
                int lastVisibleLine = 0;
                int linesCount = v.getLineCount();
                for (; lastVisibleLine < linesCount; lastVisibleLine++) {
                    v.getLineBounds(lastVisibleLine, _lineRect);
                    if (visibleTextHeight + _lineRect.height() > remainingHeight) {
                        break;
                    }
                    visibleTextHeight += _lineRect.height();
                }

                if (lastVisibleLine < linesCount) {
                    if (lastVisibleLine > 0) {
                        truncateView(v, lastVisibleLine - 1);
                    } else {
                        v.measure(
                                makeMeasureSpec(v.getMeasuredWidth(), EXACTLY),
                                makeMeasureSpec(0, EXACTLY)
                        );
                    }
                }
            }

            measuredHeight += v.getMeasuredHeight();
            measuredWidth = Math.max(measuredWidth, v.getMeasuredWidth() + (mlp == null ? 0 : (mlp.leftMargin + mlp.rightMargin)));

            if (getHeadlineView() == v) {
                _shortMeasuredWidth = adjustMeasuredSize(wMode, width, measuredWidth + getPaddingLeft() + getPaddingRight());
                _shortMeasuredHeight = adjustMeasuredSize(hMode, height, measuredHeight + getPaddingTop() + getPaddingBottom());
            }
        }

        measuredWidth = adjustMeasuredSize(wMode, width, measuredWidth + getPaddingLeft() + getPaddingRight());
        if (isLiveBlog) {
            measuredHeight = measuredHeight + getPaddingTop() + getPaddingBottom();
        } else {
            measuredHeight = adjustMeasuredSize(hMode, height, measuredHeight + getPaddingTop() + getPaddingBottom());
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private int adjustMeasuredSize(int mode, int size, int measured) {
        if (mode == EXACTLY) {
            return size;
        }
        if (mode == AT_MOST) {
            return Math.min(size, measured);
        }

        return measured;
    }

    private void truncateView(TextView v, int lastVisibleLine) {
        Layout layout = v.getLayout();
        int lastIdx = layout.getLineEnd(lastVisibleLine);
        int firstIdx = layout.getLineStart(lastVisibleLine);
        final CharSequence text = v.getText();
        CharSequence line = text.subSequence(firstIdx, lastIdx);
        _spannedStringBuilder.clear();
        _spannedStringBuilder.append(line).append('…');
        float lineWidth = Layout.getDesiredWidth(_spannedStringBuilder, layout.getPaint());
        _spannedStringBuilder.clear();
        if (lineWidth < v.getMeasuredWidth()) {
            //
            // if we can just add ellipsis character to the end, just do it
            _spannedStringBuilder.append(text.subSequence(0, lastIdx)).append("…");
            v.setText(_spannedStringBuilder);
        } else {
            //
            // try to find the first white space at the end to replace
            int idx = lastIdx - 1;
            for (; idx >= firstIdx && !Character.isWhitespace(text.charAt(idx)); idx--);
            if (idx < firstIdx) {
                //
                // if there are no white spaces, replace the last character
                idx = lastIdx  -1;
            }

            _spannedStringBuilder.append(text.subSequence(0, idx)).append("…");
        }
        //
        // restore spans
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            for (Object tag: spanned.getSpans(0, text.length(), Object.class)) {
                int begin = spanned.getSpanStart(tag);
                int end = spanned.getSpanEnd(tag);
                if (begin < _spannedStringBuilder.length()) {
                    _spannedStringBuilder.setSpan(tag, begin, Math.min(_spannedStringBuilder.length(), end), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }
        }
        v.setText(_spannedStringBuilder);

        v.measure(
                makeMeasureSpec(v.getMeasuredWidth(), EXACTLY),
                makeMeasureSpec(layout.getLineBottom(lastVisibleLine), EXACTLY)
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int bottom = b - t - getPaddingBottom();
        int right = r - l - getPaddingRight();

        final int cnt = getChildCount();

        for (int i = 0; i < cnt; i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == GONE) {
                continue;
            }

            LayoutParams lp = v.getLayoutParams();
            MarginLayoutParams mlp = null;
            if (lp instanceof MarginLayoutParams) {
                mlp = (MarginLayoutParams) lp;
            }

            if (mlp != null) {
                top += mlp.topMargin;
            }

            int vb = top >= bottom ? top : Math.min(bottom, top + v.getMeasuredHeight());
            v.layout(left, top, Math.min(right, left + v.getMeasuredWidth()), vb);

            top = vb + (mlp == null ? 0 : mlp.bottomMargin);
        }
    }

    public int getShortMeasuredWidth() {
        return _shortMeasuredWidth;
    }

    public int getShortMeasuredHeight() {
        return _shortMeasuredHeight;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setKickerView((TextView) findViewById(R.id.kicker));
        setBylineView((TextView) findViewById(R.id.byline));
        setHeadlineView((TextView) findViewById(R.id.headline));
        setTimeAndBlurbView((TextView) findViewById(R.id.time_and_blurb));
        setLiveBlogListView((ListView) findViewById(R.id.live_blog_list));
        setLiveBlogLayout((LinearLayout) findViewById(R.id.live_blog_layout));
        setLiveBlogLastUpdated((TextView) findViewById(R.id.tv_last_updated));
    }

    @Override
    protected MarginLayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }
}
