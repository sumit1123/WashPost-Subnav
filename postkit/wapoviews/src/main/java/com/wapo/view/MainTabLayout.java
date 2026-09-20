package com.wapo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.tabs.TabLayout;
import com.wapo.text.WpTextAppearanceSpan;

import java.util.Collection;

public class MainTabLayout extends TabLayout {
    private static final int STYLE_MODE_STANDARD = 0;
    private static final int STYLE_MODE_WP = 1;
    private int styleMode = STYLE_MODE_STANDARD;
    private int tabLayoutId = 0;
    private int textAppearanceId = 0;
    private int selectedTextAppearanceId = 0;

    private final LayoutInflater inflater;

    public MainTabLayout(Context context) {
        this(context, null);
    }

    public MainTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MainTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MainTabLayout);
            try {
                tabLayoutId = a.getResourceId(R.styleable.MainTabLayout_tab_view_layout, tabLayoutId);
                textAppearanceId = a.getResourceId(R.styleable.MainTabLayout_text_appearance, 0);
                selectedTextAppearanceId = a.getResourceId(R.styleable.MainTabLayout_selected_text_appearance, textAppearanceId);

                if (tabLayoutId <= 0) {
                    throw new IllegalArgumentException("'tab_view_layout' attribute is not specified");
                }

                if (textAppearanceId <= 0) {
                    throw new IllegalArgumentException("'text_appearance' attribute is not specified");
                }

                styleMode = a.getInt(R.styleable.MainTabLayout_textStyleMode, STYLE_MODE_STANDARD);
            } finally {
                a.recycle();
            }
        }

        inflater = LayoutInflater.from(context);
    }

    public void setTabs(@NonNull Collection<String> tabs) {
        removeAllTabs();
        setVisibility(View.VISIBLE);
        for (String it : tabs) {

            final TextViewSelectCallback tv = (TextViewSelectCallback) inflater.inflate(tabLayoutId, this, false);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;
            tv.setLayoutParams(layoutParams);

            if (styleMode == STYLE_MODE_STANDARD) {
                applyTextStyleStandard(tv, it, textAppearanceId, selectedTextAppearanceId);
            } else {
                applyTextStyleWP(tv, it, textAppearanceId, selectedTextAppearanceId);
            }

            setTabTextColors(Color.RED, Color.GREEN);

            TabLayout.Tab tab = newTab().setCustomView(tv);
            tab.setText(it);
            addTab(tab);

        }
    }

    public void setTabViews() {
        Tab tab;
        for (int i = 0; i < getTabCount(); i++) {
            tab = getTabAt(i);
            if (tab != null) {
                tab.setCustomView(getTabView(tab));
            }
        }
    }

    private View getTabView(Tab tab) {
        CharSequence text = tab.getText();

        final TextViewSelectCallback tv = (TextViewSelectCallback) inflater.inflate(tabLayoutId, this, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        tv.setLayoutParams(layoutParams);

        if (styleMode == STYLE_MODE_STANDARD) {
            applyTextStyleStandard(tv, text, textAppearanceId, selectedTextAppearanceId);
        } else {
            applyTextStyleWP(tv, text, textAppearanceId, selectedTextAppearanceId);
        }

        setTabTextColors(Color.RED, Color.GREEN);
        return tv;
    }

    private void setTextAppearance(TextView tv, int styleId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tv.setTextAppearance(styleId);
        } else {
            tv.setTextAppearance(getContext(), styleId);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ViewGroup viewGroup = (ViewGroup) getChildAt(0);
        int actualWidth = 0;
        int desiredWidth = 0;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            actualWidth += child.getMeasuredWidth();
            child.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            child.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            if (child.getMeasuredWidth() > 0) {
                child.getLayoutParams().width = child.getMeasuredWidth();
                desiredWidth += child.getMeasuredWidth();
            }
        }
        int parentWidth = getMeasuredWidth();
        if (desiredWidth < parentWidth) {
            if (desiredWidth < actualWidth) {
                ((MarginLayoutParams) getLayoutParams()).leftMargin = (actualWidth - desiredWidth) >> 1;
            }
        } else {
            ((MarginLayoutParams) getLayoutParams()).leftMargin = 0;
        }
    }

    public int getStyleMode() {
        return styleMode;
    }

    public void setStyleMode(int styleMode) {
        this.styleMode = styleMode;
        requestLayout();
    }

    private void applyTextStyleStandard(final TextViewSelectCallback tv, CharSequence text, final int textAppearanceId, final int selectedTextAppearanceId) {
        setTextAppearance(tv, textAppearanceId);

        if (textAppearanceId != selectedTextAppearanceId) {
            tv.setSelectedCallback(new TextViewSelectCallback.SelectedCallback() {
                @Override
                public void onSelected(boolean selected) {
                    setTextAppearance(tv, selected ? selectedTextAppearanceId : textAppearanceId);
                }
            });
        }
        tv.setText(text);
    }

    private void applyTextStyleWP(final TextViewSelectCallback tv, CharSequence text, int textAppearanceId, int selectedTextAppearanceId) {
        SpannableString normalText = new SpannableString(text);
        normalText.setSpan(new WpTextAppearanceSpan(getContext(), textAppearanceId), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        final SpannableString selectedText = new SpannableString(text);
        selectedText.setSpan(new WpTextAppearanceSpan(getContext(), selectedTextAppearanceId), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(normalText);

        if (textAppearanceId != selectedTextAppearanceId) {
            tv.setSelectedCallback(new TextViewSelectCallback.SelectedCallback() {
                @Override
                public void onSelected(boolean selected) {
                    tv.setText(selectedText);
                }
            });
        }
    }

    /**
     * Shows the notification badge in the tab if needed
     * @param show - whether or not the notification badge should be shown.
     * @param tab - The tab on/from which notification badge should be shown/removed.
     */
    public void showNotificationBadge(Boolean show, Tab tab){
        View view = tab.getCustomView();
        if(view instanceof TextViewSelectCallback){
            TextViewSelectCallback tv = (TextViewSelectCallback) view;
            if(show){
                tv.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(getResources(), R.drawable.blue_circle, null),null,null,null);
            }else{
                tv.setCompoundDrawablesWithIntrinsicBounds(null,null,null,null);
            }
        }
    }
}
