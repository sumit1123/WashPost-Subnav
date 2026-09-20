package com.wapo.text;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

/**
 * Span that applies custom styles to text, runs {@link android.text.style.TextAppearanceSpan} first
 * so this overrides anything that does but will include those changes if they don't conflict
 *
 * @author Thad Cox
 */
public class WpTextAppearanceSpan extends MetricAffectingSpan {
    private Typeface typeface, typefaceBI, typefaceI, typefaceB;
    private int textSize;
    private ColorStateList textColor;
    private int textStyle;
    private  float letterSpacing;

    public WpTextAppearanceSpan(Context context, int appearance) {

        TypedArray a =
                context.obtainStyledAttributes(appearance, R.styleable.WPTextAppearance);
        assert a != null;
        this.typeface = a.hasValue(R.styleable.WPTextAppearance_fontFile) ? TypefaceCache.getTypeface(context, a.getString(R.styleable.WPTextAppearance_fontFile)) : null;
        this.typefaceB = a.hasValue(R.styleable.WPTextAppearance_fontFileBold) ? TypefaceCache.getTypeface(context, a.getString(R.styleable.WPTextAppearance_fontFileBold)) : null;
        this.typefaceI = a.hasValue(R.styleable.WPTextAppearance_fontFileItalic) ? TypefaceCache.getTypeface(context, a.getString(R.styleable.WPTextAppearance_fontFileItalic)) : null;
        this.typefaceBI = a.hasValue(R.styleable.WPTextAppearance_fontFileBoldItalic) ? TypefaceCache.getTypeface(context, a.getString(R.styleable.WPTextAppearance_fontFileBoldItalic)) : null;

        this.textSize = a.getDimensionPixelSize(R.styleable.WPTextAppearance_android_textSize, 0);
        try {
            this.textColor = a.getColorStateList(R.styleable.WPTextAppearance_android_textColor);
        } catch (Exception e) {
            this.textColor = ColorStateList.valueOf(Color.BLACK);
        }
        this.textStyle = a.getInt(R.styleable.WPTextAppearance_android_textStyle, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.letterSpacing = a.getFloat(R.styleable.WPTextAppearance_android_letterSpacing, 0);
        } else {
            this.letterSpacing = 0;
        }


        a.recycle();

    }

    public WpTextAppearanceSpan(Context context, int appearance, int textSize) {
        this(context, appearance);
        this.textSize = textSize;
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
    }

    public void setTypefaceB(Typeface typefaceB) {
        this.typefaceB = typefaceB;
    }

    public void setTypefaceBI(Typeface typefaceBI) {
        this.typefaceBI = typefaceBI;
    }

    public void setTypefaceI(Typeface typefaceI) {
        this.typefaceI = typefaceI;
    }

    public void setTextColor(ColorStateList textColor) {
        this.textColor = textColor;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public void setLetterSpace(float letterSpace) {
        this.letterSpacing = letterSpace;
    }

    public void setTextStyle(int textStyle) {
        this.textStyle = textStyle;
    }

    public int getTextSize() {
        return textSize;
    }

    @Override
    public void updateMeasureState(TextPaint ds) {
        //We should at least have a base typeface if we are going to change the typeface,
        // otherwise lets save time and just skip it
        if (typeface != null) {
            final Typeface oldTypeface = ds.getTypeface();

            final int oldStyle = oldTypeface == null ? 0 : oldTypeface.getStyle();

            int newStyle = oldStyle | textStyle;

            boolean bold = (newStyle & Typeface.BOLD) != 0;

            boolean italic = (newStyle& Typeface.ITALIC) != 0;

            Typeface thisTypeface = null;

            if(italic || bold){
                if(bold && italic && typefaceBI!= null) thisTypeface =typefaceBI;
                else if(italic && typefaceI != null) thisTypeface = typefaceI;
                else if(bold && typefaceB != null) thisTypeface = typefaceB;
            }
            if(thisTypeface == null) {
                thisTypeface = typeface;
            }
            ds.setTypeface(thisTypeface);
        }

        if (textSize > 0) {
            ds.setTextSize(textSize);
        }

        updateLetterSpacing(ds);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        updateMeasureState(ds);

        if (textColor != null) {
            ds.setColor(textColor.getColorForState(ds.drawableState, 0));
        }

        updateLetterSpacing(ds);
    }

    private void updateLetterSpacing(TextPaint ds) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ds.setLetterSpacing(letterSpacing);
        }
    }
}
