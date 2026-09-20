package com.wapo.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.wapo.text.TypefaceCache;
import com.washingtonpost.android.R;

public class ScalableTextView extends View {
    public static final float MIN_TEXT_SIZE = 20;
    private TextPaint _paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private CharSequence _text = "";
    private boolean _resizeNeeded = true;
    private float _percentPaddingTop = 0;
    private float _percentPaddingBottom = 0;
    private final static String CUSTOM_NS = "http://schemas.android.com/apk/res-auto";

    public ScalableTextView(Context context) {
        super(context);
    }

    public ScalableTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScalableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attr) {
        final Resources res = getResources();
        _paint.density = res.getDisplayMetrics().density;
        _paint.setTextSize(20);

        TypedArray a = context.obtainStyledAttributes(attr, new int[]{R.attr.font, R.attr.fontTextColor});
        if (a.hasValue(0)) {
            String font = a.getString(0);
            Typeface tp = null;
            if (font.startsWith("assets://")) {
                tp = TypefaceCache.getTypeface(context, font.substring(9));
            } else if (font.startsWith("file://")) {
                tp = Typeface.createFromFile(font.substring(7));
            } else {
                tp = Typeface.create(font, Typeface.NORMAL);
            }

            if (tp != null) {
                setTypeface(tp);
            }
        }

        if (a.hasValue(1)) {
            final int color = a.getColor(1, -1);
            if (color != -1)
                setTextColor(color);
        }
    }

    public void setText(String text) {
        _text = text == null ? "" : text;
        requestLayout();
    }

    public void setTypeface(Typeface typeface) {
        _paint.setTypeface(typeface);
        requestLayout();
    }

    public void setTextColor(int color) {
        _paint.setColor(color);
        invalidate();
    }

    public float getPercentPaddingTop() {
        return _percentPaddingTop;
    }

    public void setPercentPaddingTop(float percentPaddingTop) {
        if (percentPaddingTop < 0 || percentPaddingTop >= 1) {
            throw new IllegalArgumentException("Percent top padding should be within [0;1)");
        }
        this._percentPaddingTop = percentPaddingTop;
    }

    public float getPercentPaddingBottom() {
        return _percentPaddingBottom;
    }

    public void setPercentPaddingBottom(float percentPaddingBottom) {
        if (percentPaddingBottom < 0 || percentPaddingBottom >= 1) {
            throw new IllegalArgumentException("Percent bottom padding should be within [0;1)");
        }
        this._percentPaddingBottom = percentPaddingBottom;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(width, height);
            return;
        }

        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            setMeasuredDimension(
                    (int) _paint.measureText(_text, 0, _text.length()),
                    (int) getTotalHeight(_paint.getTextSize())
            );
            return;
        }

        if (widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST) {
            TextPaint paintClone = new TextPaint(_paint);
            paintClone.setTextSize(getTextHeight(height));
            //if (widthMode == MeasureSpec.AT_MOST) {
            //    width = Math.min(width, (int)paintClone.measureText(_text));
            //} else {
            width = (int) paintClone.measureText(_text, 0, _text.length());
            //}
            setMeasuredDimension(width, height);
        } else if (height == MeasureSpec.UNSPECIFIED) {
            setMeasuredDimension(width, (int) getTotalHeight(_paint.getTextSize()));
        } else {
            setMeasuredDimension(width, height);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        _resizeNeeded = w != oldw || h != oldh;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (_text == null || _text.length()<=0) {

            return;
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        if (_resizeNeeded) {
            resize(width, height);
            _resizeNeeded = false;
        }

        String text = _text.toString();

        if (isOutOfBounds()){
            if (!scaleDown()){
                text = compressText(_text.toString());
            }
        }

        float h = getTextHeight(height);
        float offset = height * _percentPaddingTop;
        float bl = (h - _paint.descent() - _paint.ascent()) / 2;

        canvas.drawText(text, 0, text.length(), 0, offset + bl, _paint);
    }

    /**
     * @return true if the text was successfully scaled
     */
    private boolean scaleDown() {
        float primaryTextSize = _paint.getTextSize();
        while (isOutOfBounds()){
            float textSize = _paint.getTextSize();
            if (textSize > MIN_TEXT_SIZE){
                textSize--;
                _paint.setTextSize(textSize);
            } else {
                //we scaled enough but the text still does not fit
                //so we'll better restore all as was
                _paint.setTextSize(primaryTextSize);
                return false;
            }
        }
        return true;
    }

    /**
     * @param textToCompress Input text
     * @return The same text if the input text is a one word.
     * The second word if the input text is two words.
     * The first initial of each word otherwise
     */
    private String compressText(String textToCompress) {
        if (!isOutOfBounds()){ //text length is fine
            return textToCompress;
        }
        String[] parts = textToCompress.split(" ");
        StringBuilder sb = new StringBuilder();
        switch (parts.length) {
            case 1:
                sb.append(parts[0]);
                break;
            case 2:
                sb.append(parts[1]);
                break;
            default:
                for (int i = 0; i < parts.length; i++) {
                    sb.append(parts[i].charAt(0));
                }
                break;
        }

        return sb.toString();
    }

    private boolean isOutOfBounds() {
        float textWidth = _paint.measureText(_text.toString());
        Rect rect = new Rect();
        getGlobalVisibleRect(rect);
        int viewVisibleWidth = rect.width();
        return textWidth > viewVisibleWidth;
    }

    private void resize(int width, int height) {
        if (width == 0 || height == 0) {
            return;
        }

        float tw = _paint.measureText(_text, 0, _text.length());
        float th = getTotalHeight(_paint.getTextSize());

        if (tw == 0 || th == 0) {
            return;
        }

        float k = Math.min(width / tw, height / th);
        _paint.setTextSize(getTextHeight(th * k));
    }

    private float getTotalHeight(float textHeight) {
        float s = 1 - _percentPaddingBottom - _percentPaddingTop;
        return s > 0 && s <= 1 ?
                textHeight / s :
                textHeight;
    }

    private float getTextHeight(float totalHeight) {
        float s = 1 - _percentPaddingTop - _percentPaddingBottom;
        return s > 0 && s <= 1 ?
                totalHeight * s :
                totalHeight;
    }

    public String getText() {
        return _text.toString();
    }
}