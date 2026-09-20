package com.wapo.view;

import android.content.Context;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.MetricAffectingSpan;
import android.util.AttributeSet;
import android.widget.TextView;
//import com.wapo.text.WpTextAppearanceSpan;

/**
 * This class serves as a workaround for the Android 4.1 (4.1.1 & 4.1.2 I believe)
 * @see <a href="https://code.google.com/p/android/issues/detail?id=34872">https://code.google.com/p/android/issues/detail?id=34872</a>
 * @see <a href="https://code.google.com/p/android/issues/detail?id=34872">https://code.google.com/p/android/issues/detail?id=35466</a>
 */
public class TextViewSpanFix extends TextView {
    private static final Class[] CLASSES = new Class[] { /*WpTextAppearanceSpan.class, */MetricAffectingSpan.class };

    public TextViewSpanFix(Context context) {
        super(context);
    }

    public TextViewSpanFix(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextViewSpanFix(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN_MR1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            try {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            } catch (IndexOutOfBoundsException e) {
                if (!giveItATry(widthMeasureSpec, heightMeasureSpec, CLASSES)) {
                    throw e;
                }
            }
        }
    }

    private boolean giveItATry(int widthMeasureSpec, int heightMeasureSpec, Class[] types) {
        for (int i = 0; i < types.length; i++) {
            Class kind = types[i];
            Object text = getText();
            if (!(text instanceof SpannableString)) {
                return false;
            }
            SpannableString spannable = (SpannableString) text;
            Object[] spans = spannable.getSpans(0, spannable.length(), kind);
            if (spans.length == 0) {
                continue;
            }

            for (Object span: spans) {
                spannable.removeSpan(span);
            }

            setText(spannable);
            try {
                super.measure(widthMeasureSpec, heightMeasureSpec);
                return true;
            } catch (IndexOutOfBoundsException e) {}
        }

        return false;
    }
}
