package com.wapo.flagship.features.pagebuilder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wapo.flagship.features.grid.model.Alignment;
import com.wapo.flagship.features.grid.model.Label;
import com.wapo.flagship.features.grid.model.LabelStyle;
import com.wapo.text.WpTextAppearanceSpan;
import com.washingtonpost.android.sections.R;

import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;

public class CellLabelView extends FrameLayout {
    private final int fontStyleNormal;
    private final int fontStyleSmall;
    private final int fontStyleLight;
    private final int fontStyleLightSmall;
    private final int fontSecondaryStyleNormal;
    private final int fontSecondaryStyleLight;
    private final int fontSecondaryStyleSmall;
    private final int fontSecondaryStyleLightSmall;

    private final int labelBarWidthBtn;
    private final int labelBarWidthBar;
    private final int labelBarWidthKicker;
    private final int labelBarWidthHighlight;
    private final int labelBarWidthLight;
    private final int labelBarWidthLightSmall;
    private final int labelBarWidthNormal;
    private final int labelBarWidthNormalSmall;

    private TextView textView;
    private TextView secondaryTextView;
    private View labelBar;
    private Label label;
    private LinearLayout labelTextContainer;
    private int alignment;
    private boolean nightModeEnabled;

    public CellLabelView(Context context) {
        this(context, null);
    }

    public CellLabelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLabelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CellLabelView,
                0, 0);

        try {
            fontStyleNormal = a.getResourceId(R.styleable.CellLabelView_font_style_normal, R.style.homepagestory_label_style);
            fontStyleSmall = a.getResourceId(R.styleable.CellLabelView_font_style_small, R.style.homepagestory_label_style_small);
            fontStyleLight = a.getResourceId(R.styleable.CellLabelView_font_style_light, R.style.homepagestory_label_style_light);
            fontStyleLightSmall = a.getResourceId(R.styleable.CellLabelView_font_style_light_small, R.style.homepagestory_label_style_light_small);
            fontSecondaryStyleNormal = a.getResourceId(R.styleable.CellLabelView_font_secondary_style_normal, R.style.homepagestory_label_secondary_style);
            fontSecondaryStyleLight = a.getResourceId(R.styleable.CellLabelView_font_secondary_style_light, R.style.homepagestory_label_secondary_style_light);
            fontSecondaryStyleSmall = a.getResourceId(R.styleable.CellLabelView_font_secondary_style_small, R.style.homepagestory_label_secondary_style_small);
            fontSecondaryStyleLightSmall = a.getResourceId(R.styleable.CellLabelView_font_secondary_style_light_small, R.style.homepagestory_label_secondary_style_light_small);

            labelBarWidthBtn = a.getResourceId(R.styleable.CellLabelView_bar_width_btn, R.dimen.cell_label_bar_normal);
            labelBarWidthBar = a.getResourceId(R.styleable.CellLabelView_bar_width_bar, R.dimen.cell_label_bar_normal);
            labelBarWidthKicker = a.getResourceId(R.styleable.CellLabelView_bar_width_kicker, R.dimen.cell_label_bar_kicker);
            labelBarWidthHighlight = a.getResourceId(R.styleable.CellLabelView_bar_width_highlight, R.dimen.cell_label_bar_highlight);
            labelBarWidthLight = a.getResourceId(R.styleable.CellLabelView_bar_width_light, R.dimen.cell_label_bar_normal);
            labelBarWidthLightSmall = a.getResourceId(R.styleable.CellLabelView_bar_width_light_small, R.dimen.cell_label_bar_normal);
            labelBarWidthNormal = a.getResourceId(R.styleable.CellLabelView_bar_width_normal, R.dimen.cell_label_bar_normal);
            labelBarWidthNormalSmall = a.getResourceId(R.styleable.CellLabelView_bar_width_normal_small, R.dimen.cell_label_bar_normal);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        textView = (TextView) findViewById(R.id.cell_label_textview);
        secondaryTextView = (TextView) findViewById(R.id.cell_secondary_label_textview);
        labelBar = findViewById(R.id.label_bar);
        labelTextContainer = (LinearLayout) findViewById(R.id.label_text_container);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (secondaryTextView.getVisibility() == VISIBLE) {
            final int totalAllocatedWidth = getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
            textView.measure(makeMeasureSpec(widthMeasureSpec, MeasureSpec.AT_MOST), makeMeasureSpec(0, UNSPECIFIED));
            int textViewWidth = textView.getMeasuredWidth() + textView.getPaddingLeft() + textView.getPaddingRight();
            secondaryTextView.measure(makeMeasureSpec(widthMeasureSpec, MeasureSpec.AT_MOST), makeMeasureSpec(0, UNSPECIFIED));
            int secTVWidth = secondaryTextView.getMeasuredWidth() + secondaryTextView.getPaddingLeft() + secondaryTextView.getPaddingRight();
            if (textViewWidth + secTVWidth > totalAllocatedWidth) {
                labelTextContainer.setOrientation(LinearLayout.VERTICAL);
                ((LinearLayout.LayoutParams)secondaryTextView.getLayoutParams()).leftMargin = 0;
            } else {
                labelTextContainer.setOrientation(LinearLayout.HORIZONTAL);
                ((LinearLayout.LayoutParams)secondaryTextView.getLayoutParams()).leftMargin = (int) getResources().getDimension(R.dimen.label_text_left_margin);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setLabel(Label label, int defaultAlignment, boolean nightModeEnabled) {
        this.label = label;
        this.nightModeEnabled = nightModeEnabled;
        this.alignment = getAlignment(label, defaultAlignment);
        setTag(label.getUrl());
        setupView();
    }

    public Label getLabel() {
        return label;
    }

    private void setupView() {
        if (label == null) {
            textView.setText("");
            secondaryTextView.setText("");
            return;
        }

        LabelStyle style = label.getStyle();
        LabelViewModel labelViewModel = new LabelViewModel();
        labelViewModel.style = style;
        switch (style) {
            case LABEL_BTN:
            case LABEL_BAR:
                fillLabelWithBackgroundStyle(labelViewModel);
                break;
            default:
                fillSimpleStyle(labelViewModel);

        }

        applyPrimaryLabelStyle(labelViewModel);
        applySecondaryLabelStyle(labelViewModel);
        applyLabelViewModel(labelViewModel);
    }

    private void applyPrimaryLabelStyle(LabelViewModel labelViewModel) {
        switch (label.getStyle()) {
            case LIGHT:
                labelViewModel.appearanceResId = fontStyleLight;
                break;
            case LIGHT_SMALL:
                labelViewModel.appearanceResId = fontStyleLightSmall;
                break;
            case NORMAL_SMALL:
                labelViewModel.appearanceResId = fontStyleSmall;
                break;
            default:
                labelViewModel.appearanceResId = fontStyleNormal;
                break;
        }
    }

    private void applyLabelViewModel(LabelViewModel lvm) {

        setLabelText(lvm);

        labelTextContainer.setGravity(alignment);
        textView.setBackground(lvm.textViewBackground);
        textView.setPadding(
                lvm.textViewPaddingHor,
                lvm.textViewPaddingVert,
                lvm.textViewPaddingHor,
                lvm.textViewPaddingVert
        );

        labelBar.setVisibility(lvm.showLabelBar ? VISIBLE : GONE);

        int barWidthRes = getBarWidthRes(lvm.style);
        int barWidth = getResources().getDimensionPixelSize(barWidthRes);
        FrameLayout.LayoutParams layoutParams = (LayoutParams) labelBar.getLayoutParams();
        layoutParams.width = barWidth;
        if (barWidth == 0) {
            labelTextContainer.setPadding(0, 0, 0, 0);
        } else {
            labelTextContainer.setPadding(0, (int) getContext().getResources().getDimension(R.dimen.label_text_top_margin), 0, 0);
        }
        if (label.getStyle() == LabelStyle.KICKER && label.getAlignment() == Alignment.CENTER) {
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        }
        labelBar.setLayoutParams(layoutParams);
        labelBar.setBackgroundColor(
                ContextCompat.getColor(
                        getContext(),
                        nightModeEnabled ?
                                R.color.cell_homepagestory_headline_night :
                                android.R.color.black
                )
        );

        if (label.getStyle() == LabelStyle.LABEL_BAR) {
            textView.getLayoutParams().width = LayoutParams.MATCH_PARENT;
        } else {
            textView.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
        }
        textView.setLayoutParams(textView.getLayoutParams());
        textView.setGravity(alignment);

    }

    private int getBarWidthRes(LabelStyle style) {
        switch (style) {
            case LABEL_BTN:
                return labelBarWidthBtn;
            case LABEL_BAR:
                return labelBarWidthBar;
            case HIGHLIGHT:
                return labelBarWidthHighlight;
            case KICKER:
                return labelBarWidthKicker;
            case LIGHT:
                return labelBarWidthLight;
            case LIGHT_SMALL:
                return labelBarWidthLightSmall;
            case NORMAL:
                return labelBarWidthNormal;
            case NORMAL_SMALL:
                return labelBarWidthNormalSmall;
            default:
                return labelBarWidthNormal;
        }
    }

    private void setLabelText(LabelViewModel lvm) {
        String labelText = (label.getText() != null) ? label.getText() : "";
        SpannableStringBuilder labelSpan = new SpannableStringBuilder(labelText);
        WpTextAppearanceSpan fontSpan = new WpTextAppearanceSpan(
                getContext(),
                lvm.appearanceResId
        );

        if (lvm.textColor != null) {
            fontSpan.setTextColor(ColorStateList.valueOf(lvm.textColor));
        }

        labelSpan.setSpan(
                fontSpan,
                0, labelSpan.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        textView.setText(labelSpan);

        if (label.getSecondaryText() == null) {
            secondaryTextView.setVisibility(GONE);
        } else {
            secondaryTextView.setVisibility(VISIBLE);
            SpannableStringBuilder secondaryLabelSpan = new SpannableStringBuilder(label.getSecondaryText());
            WpTextAppearanceSpan secondaryFontSpan = new WpTextAppearanceSpan(
                    getContext(),
                    lvm.appearanceSecondaryResId
            );

            secondaryLabelSpan.setSpan(
                    secondaryFontSpan,
                    0, secondaryLabelSpan.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            secondaryTextView.setText(secondaryLabelSpan);
            // Set the right arrow
            setRightArrow();
        }
    }

    private void setRightArrow() {
        if (label.isArrowVisible()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                secondaryTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0,
                        0,
                        nightModeEnabled ?
                                R.drawable.liveblog_arrow_white :
                                R.drawable.liveblog_arrow,
                        0
                );
            } else {
                secondaryTextView.setCompoundDrawables(
                        null,
                        null,
                        ContextCompat.getDrawable(
                                getContext().getApplicationContext(),
                                nightModeEnabled ?
                                        R.drawable.liveblog_arrow_white :
                                        R.drawable.liveblog_arrow
                        ),
                        null
                );
            }
        } else {
            secondaryTextView.setCompoundDrawables(null, null, null, null);
        }
    }

    private void fillSimpleStyle(LabelViewModel lvm) {
        lvm.showLabelBar = true;

        lvm.textColor = null;
        lvm.textViewPaddingHor = 0;
        lvm.textViewPaddingVert = 0;
        lvm.textViewBackground = null;
    }

    private void fillLabelWithBackgroundStyle(LabelViewModel lvm) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        int cornerRadius = getContext().getResources().getDimensionPixelSize(R.dimen.homepagestory_label_corner_radius);
        shape.setCornerRadii(new float[] { cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius });
        try {
            String backgroundColor = label.getBackgroundColor();
            if (!TextUtils.isEmpty(backgroundColor)) {
                shape.setColor(Color.parseColor(backgroundColor));
            } else {
                shape.setColor(Color.WHITE);
            }
        } catch (Exception e) {
            shape.setColor(Color.WHITE);
        }
        lvm.textViewBackground = shape;

        Integer textColor;
        try {
            textColor = Color.parseColor(label.getTextColor());
        } catch (Exception e) {
            textColor = null;
        }

        lvm.appearanceSecondaryResId = 0;
        lvm.showLabelBar = false;

        lvm.textViewPaddingHor = getContext().getResources().getDimensionPixelSize(R.dimen.homepagestory_label_hor_padding);
        lvm.textViewPaddingVert = getContext().getResources().getDimensionPixelSize(R.dimen.homepagestory_label_vert_padding);

        lvm.textColor = textColor;
    }

    private void applySecondaryLabelStyle(LabelViewModel lvm) {
        if (label.getSecondaryStyle() == null) {
            lvm.appearanceSecondaryResId = fontSecondaryStyleNormal;
        } else {
            switch (label.getSecondaryStyle()) {
                case LIGHT:
                    lvm.appearanceSecondaryResId = fontSecondaryStyleLight;
                    break;
                case LIGHT_SMALL:
                    lvm.appearanceSecondaryResId = fontSecondaryStyleLightSmall;
                    break;
                case NORMAL_SMALL:
                    lvm.appearanceSecondaryResId = fontSecondaryStyleSmall;
                    break;
                default:
                    lvm.appearanceSecondaryResId = fontSecondaryStyleNormal;
                    break;
            }
        }
    }

    private int getAlignment(Label label, int defaultAlignment) {
        if (label == null) {
            return ModelHelper.getGravity(Alignment.CENTER);
        }
        if (label.getAlignment() != null) {
            return ModelHelper.getGravity(label.getAlignment());
        }

        return defaultAlignment;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return new SavedState(
                super.onSaveInstanceState(),
                label,
                alignment
        );
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            super.onRestoreInstanceState(ss.parentState);
            if (ss.label != null) {
                setLabel(ss.label, ss.defaultAlignment, nightModeEnabled);
            }
        }
    }


    public static class SavedState implements Parcelable {
        private final Parcelable parentState;
        private final Label label;
        private final int defaultAlignment;

        public SavedState(Parcelable parentState, Label label, int defaultAlignment) {
            this.parentState = parentState;
            this.label = label;
            this.defaultAlignment = defaultAlignment;
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                ClassLoader cl = SavedState.class.getClassLoader();
                return new SavedState(
                        in.readParcelable(cl),
                        in.<Label>readParcelable(cl),
                        in.readInt()
                );
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(parentState, flags);
            dest.writeParcelable(label, flags);
            dest.writeInt(defaultAlignment);
        }
    }

    class LabelViewModel {
        LabelStyle style;
        Integer textColor;
        Drawable textViewBackground;
        int appearanceResId;
        int appearanceSecondaryResId;
        boolean showLabelBar;
        int textViewPaddingHor;
        int textViewPaddingVert;
    }
}
