package com.wapo.text;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;


public class WpLinkAppearanceSpan extends ClickableSpan {
    private ColorStateList textColor;
    private boolean underline;

    public WpLinkAppearanceSpan(Context context, boolean underline) {
        this.underline = underline;
        try {
            this.textColor = ContextCompat.getColorStateList(context, R.color.links_color);
        } catch (Exception e) {
            this.textColor = ColorStateList.valueOf(context.getResources().getColor(R.color.links_color));
        }
    }

    @Override
    public void onClick(@NonNull View widget) {
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        if (textColor != null) {
            ds.setColor(textColor.getColorForState(ds.drawableState, 0));
        }
        ds.setUnderlineText(underline);
    }
}
