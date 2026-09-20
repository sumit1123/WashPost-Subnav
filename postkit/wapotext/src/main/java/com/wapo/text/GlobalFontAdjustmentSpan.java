package com.wapo.text;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.MetricAffectingSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * @author Thad Cox
 */
public class GlobalFontAdjustmentSpan extends MetricAffectingSpan {

    @Override
    public void updateMeasureState(TextPaint p) {
        float size = GlobalFont.INSTANCE.getFontSizeAdjustment();
        p.setTextSize(p.getTextSize() + size * 1.6f);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        updateMeasureState(tp);
    }

    /**
     * Utility that finds TextViews (or views that implement {@link com.wapo.text.TextSizeChangedListener})
     * that are children of ViewGroups and sets requestLayout on them so they render with an adjusted size
     *
     * @param updatedSize new size of the text, passed to onTextSizeChanged method of children that
     *                    implement {@link com.wapo.text.TextSizeChangedListener}
     * @param view        view to notify of size change or to search for children to notify (if ViewGroup)
     */
    public static void onTextSizeChanged(final float updatedSize, final View view) {
        if (view == null || view.getVisibility() == View.GONE) return;// Invisible views are ignored
        if (view instanceof TextSizeChangedListener) {
            ((TextSizeChangedListener) view).onTextSizeChanged(updatedSize);
        } else if (view instanceof TextView) {
            updateTextViewLayoutWorkaround((TextView) view);
        } else if (view instanceof ViewGroup) {
            final int childCount = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < childCount; i++) {
                onTextSizeChanged(updatedSize, ((ViewGroup) view).getChildAt(i));
            }
        }
    }

    /**
     * TextView caches its' layout params so a simple "requestLayout" call does nothing
     * when you change the font by a span.
     * <p/>
     * This workaround triggers the TextView to invalidate the layout cache and re-measure the text
     */
    private static void updateTextViewLayoutWorkaround(TextView view) {
        view.setText(view.getText());
        view.requestLayout();
    }

    /**
     * Utility to apply GlobalFontAdjustmentSpan on a given string.
     *
     * @param text input text
     * @return Spannable
     */
    public synchronized static Spannable applyGlobalFontSpan(String text) {
        SpannableStringBuilder spannable = new SpannableStringBuilder("");
        if (TextUtils.isEmpty(text)) {
            return spannable;
        }
        spannable.append(text);
        spannable.setSpan(new GlobalFontAdjustmentSpan(), 0,
                text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }
}
