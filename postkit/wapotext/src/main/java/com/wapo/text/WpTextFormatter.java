package com.wapo.text;

import android.content.res.TypedArray;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class WpTextFormatter {

    public final static void applyLineSpacing(@NonNull TextView textView, int appearance) {
        TypedArray a = textView.getContext().obtainStyledAttributes(appearance, R.styleable.WPTextAppearance);
        assert a != null;

        int lineSpacingExtra = a.getDimensionPixelSize(R.styleable.WPTextAppearance_android_lineSpacingExtra, 0);
        float lineSpacingMultiplier = a.getFloat(R.styleable.WPTextAppearance_android_lineSpacingMultiplier, 0f);

        a.recycle();

        textView.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
    }
}
