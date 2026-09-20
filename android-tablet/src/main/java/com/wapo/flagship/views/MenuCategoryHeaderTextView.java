package com.wapo.flagship.views;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.AttributeSet;

import com.wapo.text.WpTextAppearanceSpan;
import com.washingtonpost.android.R;

public class MenuCategoryHeaderTextView extends AppCompatTextView {
    public MenuCategoryHeaderTextView(Context context) {
        super(context);
    }

    public MenuCategoryHeaderTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MenuCategoryHeaderTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {

        SpannableStringBuilder formattedText = new SpannableStringBuilder(text.toString().toUpperCase());
        formattedText.setSpan(
                new WpTextAppearanceSpan(
                        getContext(),
                        R.style.menu_category_header
                ),
                0,
                text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        super.setText(formattedText, type);
    }
}
