package com.wapo.flagship.features.articles;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.wapo.text.WpTextAppearanceSpan;
import com.washingtonpost.android.R;

public class TaglineView extends LinearLayout {
    private ImageView logo;
    private TextView label;

    public TaglineView(Context context) {
        this(context, null);
    }

    public TaglineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaglineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public TaglineView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        logo = (ImageView) findViewById(R.id.logo);
        label = (TextView) findViewById(R.id.label);
        applyTextStyle(R.style.article_text_span_tagline);
        logo.setImageDrawable(applyTint(logo.getDrawable(), ContextCompat.getColor(getContext(), R.color.logo_tint)));
    }

    private void applyTextStyle(int styleId) {
        CharSequence text = label.getText().toString();
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        ssb.setSpan(
                new WpTextAppearanceSpan(getContext(), styleId),
                0, ssb.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        );
        label.setText(ssb);
    }

    private static Drawable applyTint(Drawable drawable, int color) {
        if (drawable == null) {
            return drawable;
        }

        drawable.mutate();
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        return drawable;
    }
}
