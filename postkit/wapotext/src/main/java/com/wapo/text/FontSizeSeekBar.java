package com.wapo.text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.SeekBar;


public class FontSizeSeekBar extends SeekBar implements SeekBar.OnSeekBarChangeListener {

    private final static int STEPS_COUNT = 4;
    private int stepCount = STEPS_COUNT;
    private OnFontSizeChangedListener fontSizeChangedListener;
    private int originFontSize;
    private int progress;
    private Paint horLinePaint;
    private Paint verLinePaint;
    private int verLineHeight;
    private int lineColor = Color.LTGRAY;

    public FontSizeSeekBar(Context context) {
        super(context);
        init();
    }

    public FontSizeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FontSizeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        super.setOnSeekBarChangeListener(this);
        setMax(stepCount - 1);
        setProgressDrawable(null);

        horLinePaint = new Paint();
        horLinePaint.setColor(lineColor);
        horLinePaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.seek_bar_hor_line_width));

        verLinePaint = new Paint();
        verLinePaint.setColor(lineColor);
        verLinePaint.setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.seek_bar_ver_line_width));
        verLineHeight = getResources().getDimensionPixelSize(R.dimen.seek_bar_ver_line_height);
    }

    @Override
    public void draw(Canvas canvas) {
        if (getProgressDrawable() == null) {
            float y = canvas.getHeight() / 2f;
            int availableWidth = canvas.getWidth() - getPaddingRight() - getPaddingLeft();
            canvas.drawLine(0, y, canvas.getWidth(), y, horLinePaint);
            float lineStep = availableWidth / (float) (stepCount - 1);
            float offset = getPaddingLeft();
            float startY = (canvas.getHeight() - verLineHeight) / 2f;
            float endY = startY + verLineHeight;
            for (int i = 0; i < stepCount; i++) {
                float x = i * lineStep + offset;
                canvas.drawLine(x, startY, x, endY, verLinePaint);
            }
        }
        super.draw(canvas);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        throw new UnsupportedOperationException("OnSeekBarChangeListener is set internally");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        changeFontSize(progress - this.progress, false);
        this.progress = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        progress = seekBar.getProgress();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void changeFontSize(int step, boolean updateSeekBar) {
        int fontSize = (originFontSize + step) % stepCount;

        if (updateSeekBar) {
            setProgress(fontSize);
        }

        originFontSize = fontSize;

        if (step != 0 && fontSizeChangedListener != null) {
            fontSizeChangedListener.onFontSizeChanged(fontSize);
        }
    }

    public void setOriginFontSize(int originFontSize) {
        this.originFontSize = originFontSize;
        changeFontSize(0, true);
    }

    public void setFontSizeChangedListener(OnFontSizeChangedListener fontSizeChangedListener) {
        this.fontSizeChangedListener = fontSizeChangedListener;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }
}
