package com.wapo.text;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.style.LeadingMarginSpan;
/**
 * A paragraph style that draws the given prefix with the leading margin to the paragraph.
 */
public class ListSpan implements LeadingMarginSpan {

    private final String prefix;
    private int gapWidth = 60;
    private int prefixPadding = 8;

    public ListSpan(String prefix) {
        this.prefix = prefix;
    }

    public int getPrefixPadding() {
        return prefixPadding;
    }

    public void setPrefixPadding(int prefixPadding) {
        this.prefixPadding = prefixPadding;
    }

    public int getGapWidth() {
        return gapWidth;
    }

    public void setGapWidth(int gapWidth) {
        this.gapWidth = gapWidth;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return gapWidth + prefixPadding;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout l) {
        if (first) {
            SpannableString s = new SpannableString(prefix);
            if (text instanceof Spannable) {
                copySpansToPrefix(text, s);
                StaticLayout layout = new StaticLayout(s, l.getPaint(), gapWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
                c.save();
                c.translate(gapWidth - layout.getLineWidth(0) - prefixPadding, top);
                layout.draw(c);
                c.restore();
            }
        }
    }

    private void copySpansToPrefix(CharSequence text, SpannableString s) {
        Object[] spans = ((Spanned) text).getSpans(0, text.length(), Object.class);
        for (Object span : spans) {
            if (span instanceof WpTextAppearanceSpan) {
                s.setSpan(span, 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
