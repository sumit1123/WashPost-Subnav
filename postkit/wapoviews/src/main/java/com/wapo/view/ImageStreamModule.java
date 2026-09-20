package com.wapo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.TextView;


import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.MeasureSpec.*;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class ImageStreamModule extends StreamModuleView {
    protected static final int N = 3;

    private ProportionalLayout _imageFrame;
    private TextView _spareTextView;
    private ImageViewWithAnimatedIndicator _imageView;

    private final List<TextView> _views = new ArrayList<>();
    private final Rect _lineBoundRect = new Rect();
    private float _spareTextViewShift = 0;
    private int _imagePosition = 0;
    private boolean needSpareText = false;

    @SuppressWarnings("unused")
    public ImageStreamModule(Context context) {
        super(context);
        create(context, null);
    }

    @SuppressWarnings("unused")
    public ImageStreamModule(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageStreamModule(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        create(context, attrs);
    }

    @Override
    protected int getDefaultLayout() {
        return R.layout.view_image_stream_model;
    }

    public void setAspectRatio(float aspectRatio) {
        _imageFrame.setAspectRatio(aspectRatio);
        requestLayout();
    }

    public void setImageUrl(String url, AnimatedImageLoader imageLoader) {
        _imageView.setImageUrl(url, imageLoader, true);
    }

    public void setImageUrl(String url, AnimatedImageLoader imageLoader, boolean isBottomCropped) {
        _imageView.setImageUrl(url, imageLoader, isBottomCropped, true);
    }

    public void disableAnimatedSpinner(){
        _imageView.disableProgressBar();
    }

    public ProportionalLayout getImageFrame() {
        return _imageFrame;
    }

    protected List<TextView> getViews() {
        return _views;
    }

    protected TextView getSpareTextView() {
        return _spareTextView;
    }

    protected float getSpareTextViewShift() {
        return _spareTextViewShift;
    }

    private void create(Context context, AttributeSet attrs) {
        super.processAttributes(context ,attrs);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.ImageStreamModule
            );

            try {
                if (a.hasValue(R.styleable.ImageStreamModule_imagePosition)) {
                    _imagePosition = a.getInteger(R.styleable.ImageStreamModule_imagePosition, 0);
                }
            } finally {
                a.recycle();
            }
        }
        needSpareText = (needSpareText || _imagePosition == 1); //for phones always need spareText

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(getLayout(), this, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int wMode = MeasureSpec.getMode(widthMeasureSpec);
        final int hMode = MeasureSpec.getMode(heightMeasureSpec);

        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        final int effectiveWidth = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        final int effectiveHeight = Math.max(0, height - getPaddingTop() - getPaddingBottom());

        int measuredHeight = 0;
        int measuredWidth = 0;
        int remainingHeight = effectiveHeight;

        MarginLayoutParams imgFrameLp = (MarginLayoutParams) _imageFrame.getLayoutParams();
        if (imgFrameLp == null) {
            imgFrameLp = generateDefaultLayoutParams();
            _imageFrame.setLayoutParams(imgFrameLp);
        }

        int measuredKickerAndImgHeight = measuredHeight + imgFrameLp.topMargin + imgFrameLp.bottomMargin;

        remainingHeight = Math.max(0, effectiveHeight - measuredHeight);

        int effectiveFrameWidth = effectiveWidth / N;
        _imageFrame.measure(
                getChildMeasureSpec(
                        makeMeasureSpec(effectiveFrameWidth, wMode),
                        imgFrameLp.leftMargin + imgFrameLp.rightMargin,
                        imgFrameLp.width
                ),
                getChildMeasureSpec(
                        makeMeasureSpec(remainingHeight, hMode),
                        0,
                        imgFrameLp.height
                )
        );

        measuredKickerAndImgHeight += _imageFrame.getMeasuredHeight();

        measuredWidth = Math.max(
                measuredWidth,
                Math.max(
                        effectiveWidth,
                        (_imageFrame.getMeasuredWidth() + imgFrameLp.leftMargin + imgFrameLp.rightMargin) * N
                )
        );

        int effectiveTextWidth = Math.max(
             0,
             measuredWidth - _imageFrame.getMeasuredWidth() - imgFrameLp.leftMargin - imgFrameLp.rightMargin
        );
        int effectiveTextWidthSpec = makeMeasureSpec(effectiveTextWidth, AT_MOST);
        int effectiveWidthSpec = makeMeasureSpec(measuredWidth, AT_MOST);

        final TextView headlineView = getHeadlineView();
        _spareTextView.setVisibility(GONE);
        if (_views.remove(_spareTextView)) {
            headlineView.setText(getTitle());
            updateBlurb();
        }
        _spareTextViewShift = 0;
        for (int i = 0; i < _views.size(); i++) {
            TextView v = _views.get(i);

            if (v.getVisibility() == GONE) {
                continue;
            }

            MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
            if (lp == null) {
                lp = generateDefaultLayoutParams();
                v.setLayoutParams(lp);
            }

            measuredHeight += lp.topMargin;
// TODO to be revisited for tablets
            v.measure(
                    getChildMeasureSpec(
                            _imagePosition != 0 && v == headlineView ? effectiveWidthSpec : effectiveTextWidthSpec,
                            lp.leftMargin + lp.rightMargin,
                            lp.width
                    ),
                    getChildMeasureSpec(
                            makeMeasureSpec(
                                    Math.max(0, effectiveHeight - measuredHeight),
                                    hMode
                            ),
                            lp.bottomMargin,
                            lp.height
                    )
            );
            if (measuredHeight <= measuredKickerAndImgHeight && measuredHeight + v.getMeasuredHeight() > measuredKickerAndImgHeight) {
                Layout layout = v.getLayout();
                int lineIdx = 0;
                int h = measuredHeight;
                int lineCount = v.getLineCount();
                do {
                    if (lineIdx >= lineCount) {
                        break;
                    }
                    layout.getLineBounds(lineIdx, _lineBoundRect);
                    h += _lineBoundRect.height();
                    lineIdx++;
                } while (h <= measuredKickerAndImgHeight);
                if (0 < lineIdx && lineIdx < lineCount - (_imagePosition == 1 ? 1 : 0) && needSpareText) {
                    //
                    // lineIdx has the index of the first line below the image
                    //
                    // split text view between two image views, measure them separately
                    final int dividingPoint = layout.getLineEnd(lineIdx - 1);
                    CharSequence line = v.getText();
                    CharSequence partOne = line.subSequence(0, dividingPoint);
                    CharSequence partTwo = line.subSequence(dividingPoint, line.length());

                    v.setText(partOne);
                    v.measure(
                            makeMeasureSpec(v.getMeasuredWidth(), EXACTLY),
                            makeMeasureSpec(h - measuredHeight, EXACTLY)
                    );

                    _spareTextView.setText(partTwo);
                    _views.add(i + 1, _spareTextView);
                    _spareTextView.setTypeface(v.getTypeface());
                    _spareTextView.setVisibility(VISIBLE);
                    _spareTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, v.getTextSize());
                    _spareTextView.setTextColor(v.getTextColors());

                    MarginLayoutParams spLp = (MarginLayoutParams) _spareTextView.getLayoutParams();
                    if (spLp == null) {
                        spLp = new MarginLayoutParams(lp);
                        _spareTextView.setLayoutParams(spLp);
                    } else {
                        spLp.topMargin = 0;
                        spLp.leftMargin = lp.leftMargin;
                        spLp.bottomMargin = lp.bottomMargin;
                        spLp.rightMargin = lp.rightMargin;
                    }
                }
            }

            measuredHeight += v.getMeasuredHeight() + lp.bottomMargin;
            if ((_imagePosition != 0) && (v == getKickerView() || v == headlineView)) {
                measuredKickerAndImgHeight += v.getMeasuredHeight() + lp.bottomMargin;
            }

            if (v == _spareTextView) {
                final Paint.FontMetrics fm = v.getPaint().getFontMetrics();
                _spareTextViewShift = fm.descent - fm.ascent - (fm.bottom - fm.top);
                measuredHeight += _spareTextViewShift;
            }
        }

        measuredHeight = Math.max(measuredHeight, measuredKickerAndImgHeight);

        if (wMode == EXACTLY) {
            measuredWidth = width;
        } else if (wMode == AT_MOST) {
            measuredWidth = Math.min(width, measuredWidth + getPaddingLeft() + getPaddingRight());
        } else {
            measuredWidth += getPaddingLeft() + getPaddingRight();
        }

        /*if (hMode == EXACTLY) {
            measuredHeight = height;
        } else if (hMode == AT_MOST) {
            measuredHeight = Math.min(height, measuredHeight + getPaddingTop() + getPaddingBottom());
        } else {
            measuredHeight += getPaddingTop() + getPaddingBottom();
        }*/

        measuredHeight += getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int pt = getPaddingTop();
        int pl = getPaddingLeft();
        int pr = getPaddingRight();

        int width = r - l;
        int ew = width - pl - pr;
        int maxImgWidth = ew / N;

        MarginLayoutParams imgLp = (MarginLayoutParams) _imageFrame.getLayoutParams();
        int it = pt;
        int ir = _imagePosition == 1 ?
                    width - pr - imgLp.rightMargin :
                    pl + maxImgWidth - imgLp.rightMargin;
        int il = ir - Math.max(_imageFrame.getMeasuredWidth(), maxImgWidth - imgLp.leftMargin);
        ir = il + _imageFrame.getMeasuredWidth();

        int tt = pt;

        if (_imagePosition == 0) it += imgLp.topMargin;
        int ib = it + _imageFrame.getMeasuredHeight();

        if (_imagePosition == 0) _imageFrame.layout(il, it, ir, ib);


        int postponedBottomMargin = 0;
        for (TextView v : _views) {
            if (v.getVisibility() == GONE) {
                continue;
            }
            MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();

            if (v == _spareTextView) {
                tt += _spareTextViewShift;
            } else {
                tt += lp.topMargin + postponedBottomMargin;
                postponedBottomMargin = lp.bottomMargin;
            }

            int left = ((tt < ib && _imagePosition == 0) || !needSpareText ? ir + imgLp.rightMargin : pl) + lp.leftMargin;//pl + lp.leftMargin;
            int right = left + v.getMeasuredWidth();
            int bottom = tt + v.getMeasuredHeight();
            v.layout(left, tt, right, bottom);

            tt = bottom;

            // find top margin and bottom margin for the imageView
            if (_imagePosition != 0 && v == getHeadlineView()) {
                it = tt + lp.topMargin + postponedBottomMargin;
                ib += tt + lp.topMargin + postponedBottomMargin;
            }
        }

        if (_imagePosition != 0) _imageFrame.layout(il, it, ir, ib);
    }
    // region [draw margins for debug purposes]
    // uncomment to draw margins
    /*private final Rect clipBounds = new Rect();
    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.getClipBounds(clipBounds);
        final int left = clipBounds.left;
        final int top = clipBounds.top;

        Paint paint = new Paint();
        paint.setColor(Color.argb(100, 255, 0, 0));

        for (int i = 0, cnt = getChildCount(); i < cnt; i++) {
            View v = getChildAt(i);
            MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();
            int l = left + v.getLeft();
            int ml = l - lp.leftMargin;
            int t = top + v.getTop();
            int mt = t - lp.topMargin;
            int r = left + v.getRight();
            int mr = r + lp.rightMargin;
            int b = top + v.getBottom();
            int mb = b + lp.bottomMargin;

            canvas.drawRect(ml, mt, r, t, paint);
            canvas.drawRect(r, mt, mr, b, paint);
            canvas.drawRect(l, b, mr, mb, paint);
            canvas.drawRect(ml, t, l, mb, paint);
        }

        super.dispatchDraw(canvas);
    }*/
    //endregion

    @Override
    protected MarginLayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(WRAP_CONTENT, WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        //TextView kickerView = (TextView) findViewById(R.id.kicker);
        //setKickerView(kickerView);
        //_views.add(kickerView);

        _imageFrame = (ProportionalLayout) findViewById(R.id.image_frame);

        _imageView = (ImageViewWithAnimatedIndicator) findViewById(R.id.image);

        TextView headlineView = (TextView) findViewById(R.id.headline);
        setHeadlineView(headlineView);
        _views.add(headlineView);

        _spareTextView = (TextView) findViewById(R.id.spareTextView);

        TextView timeAndBlurbView = (TextView) findViewById(R.id.time_and_blurb);
        setTimeAndBlurbView(timeAndBlurbView);
        _views.add(timeAndBlurbView);

        TextView bylineView = (TextView) findViewById(R.id.byline);
        setBylineView(bylineView);
        _views.add(bylineView);
    }
}
