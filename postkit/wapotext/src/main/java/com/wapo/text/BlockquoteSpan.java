package com.wapo.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;

/**
 * @author Thad Cox
 */
public class BlockquoteSpan implements LeadingMarginSpan, Parcelable {

    public static final float DEFAULT_MARGIN_SIZE = 4;

    private static final float DEFAULT_GAP_WIDTH = 2;

    private final int mColor;
    private final float mMarginSize;
    private final float mGapWidth;

    public BlockquoteSpan() {
        super();
        mColor = 0xff0000ff;
        mMarginSize = DEFAULT_MARGIN_SIZE;
        mGapWidth = DEFAULT_GAP_WIDTH;
    }

    public BlockquoteSpan(int color) {
        super();
        mColor = color;
        mMarginSize = DEFAULT_MARGIN_SIZE;
        mGapWidth = DEFAULT_GAP_WIDTH;
    }

    public BlockquoteSpan(int color, int margin_size) {
        super();
        mColor = color;
        mMarginSize = margin_size;
        mGapWidth = DEFAULT_GAP_WIDTH;
    }

    public BlockquoteSpan(int color, float marginSize, float gapWidth){
        super();
        mColor = color;
        mMarginSize = marginSize;
        mGapWidth = gapWidth;
    }

    public BlockquoteSpan(Parcel src) {
        mColor = src.readInt();
        mMarginSize = src.readFloat();
        mGapWidth = src.readFloat();
    }


    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mColor);
        dest.writeFloat(mMarginSize);
        dest.writeFloat(mGapWidth);
    }

    public int getColor() {
        return mColor;
    }

    public int getLeadingMargin(boolean first) {
        return (int) Math.ceil(mMarginSize + mGapWidth);
    }

    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir,
                                  int top, int baseline, int bottom,
                                  CharSequence text, int start, int end,
                                  boolean first, Layout layout) {
        Paint.Style style = p.getStyle();
        int color = p.getColor();

        p.setStyle(Paint.Style.FILL);
        p.setColor(mColor);

        c.drawRect(x, top, x + dir * mMarginSize, bottom, p);

        p.setStyle(style);
        p.setColor(color);
    }
}