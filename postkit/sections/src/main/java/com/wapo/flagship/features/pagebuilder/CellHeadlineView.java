package com.wapo.flagship.features.pagebuilder;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.wapo.flagship.features.grid.model.Alignment;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.wapo.flagship.features.grid.model.BulletType;
import com.wapo.flagship.features.grid.model.FontStyle;
import com.wapo.flagship.features.grid.model.Headline;
import com.wapo.flagship.features.grid.model.HeadlineIcon;
import com.wapo.flagship.features.grid.model.Size;
import com.wapo.flagship.features.grid.model.Style;
import com.wapo.text.WpTextAppearanceSpan;
import com.wapo.view.FirstTextLineDrawable;
import com.wapo.view.FlowableTextView;
import com.washingtonpost.android.sections.R;

import static com.wapo.flagship.features.pagebuilder.ModelHelper.getGravity;

import java.util.HashMap;
import java.util.Map;

public class CellHeadlineView extends FlowableTextView {
    private final int highLightResId;
    private final int normalResId;
    private final int thinResId;
    private final int regularResId;
    private final int boldResId;
    private final int italicResId;
    private final int lightResId;
    private final int ultraResId;
    private final int ultraItalicResId;
    private final int franklinNormalResId;
    private final int franklinHighLightResId;
    private final int franklinThinResId;
    private final int franklinRegularResId;
    private final int franklinBoldResId;
    private final int franklinItalicResId;
    private final int franklinLightResId;
    private final int franklinUltraResId;
    private final int franklinUltraItalicResId;

    private Headline headline;
    private final Drawable listDrawable;
    private final Map<HeadlineIcon, Drawable> iconsMap = new HashMap<>();
    private boolean isGrid;

    public CellHeadlineView(Context context) {
        this(context, null);
    }

    public CellHeadlineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellHeadlineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CellHeadlineView,
                0, 0
        );

        try {
            highLightResId = a.getResourceId(R.styleable.CellHeadlineView_highlight, R.style.homepagestory_headline_style_highlight);
            normalResId = a.getResourceId(R.styleable.CellHeadlineView_normal, R.style.homepagestory_headline_style_normal);
            thinResId = a.getResourceId(R.styleable.CellHeadlineView_thin, R.style.homepagestory_headline_style_thin);
            regularResId = a.getResourceId(R.styleable.CellHeadlineView_regular, R.style.homepagestory_headline_style_regular);
            boldResId = a.getResourceId(R.styleable.CellHeadlineView_bold, R.style.homepagestory_headline_style_bold);
            italicResId = a.getResourceId(R.styleable.CellHeadlineView_italic, R.style.homepagestory_headline_style_italic);
            lightResId = a.getResourceId(R.styleable.CellHeadlineView_light, R.style.homepagestory_headline_style_light);
            ultraResId = a.getResourceId(R.styleable.CellHeadlineView_ultra, R.style.homepagestory_headline_style_ultra);
            ultraItalicResId = a.getResourceId(R.styleable.CellHeadlineView_ultra_italic, R.style.homepagestory_headline_style_ultra_italic);
            franklinNormalResId = a.getResourceId(R.styleable.CellHeadlineView_franklin_normal, R.style.homepagestory_headline_style_franklin_normal);
            franklinHighLightResId = a.getResourceId(R.styleable.CellHeadlineView_franklin_highlight, R.style.homepagestory_headline_style_franklin_highlight);
            franklinThinResId = a.getResourceId(R.styleable.CellHeadlineView_franklin_thin, R.style.homepagestory_headline_style_franklin_thin);
            franklinRegularResId = a.getResourceId(R.styleable.CellHeadlineView_franklin_regular, R.style.homepagestory_headline_style_franklin_regular);
            franklinBoldResId = a.getResourceId(R.styleable.CellHeadlineView_franklin_bold, R.style.homepagestory_headline_style_franklin_bold);
            franklinItalicResId = a.getResourceId(R.styleable.CellHeadlineView_franklin_italic, R.style.homepagestory_headline_style_franklin_italic);
            franklinLightResId = a.getResourceId(R.styleable.CellHeadlineView_light, R.style.homepagestory_headline_style_light);
            franklinUltraResId = a.getResourceId(R.styleable.CellHeadlineView_franklin_ultra, R.style.homepagestory_headline_style_franklin_ultra);
            franklinUltraItalicResId = a.getResourceId(R.styleable.CellHeadlineView_franklin_ultra_italic, R.style.homepagestory_headline_style_franklin_ultra_italic);
            Drawable listDrawable = a.getDrawable(R.styleable.CellHeadlineView_headline_list_drawable);
            if (listDrawable == null) {
                listDrawable = ContextCompat.getDrawable(getContext(), R.drawable.circle_solid);
            }
            if (listDrawable != null) {
                this.listDrawable = new FirstTextLineDrawable(listDrawable, this);
            } else {
                this.listDrawable = null;
            }
            iconsMap.put(HeadlineIcon.LOGO, ContextCompat.getDrawable(context, R.drawable.ic_wp));
            iconsMap.put(HeadlineIcon.RIPPLE, ContextCompat.getDrawable(context, R.drawable.ripple_logo));
        } finally {
            a.recycle();
        }
        setFocusable(true);
    }

    public void setHeadline(
            @Nullable Headline headline,
            @Nullable Drawable prefixIcon,
            @Nullable Drawable postfixIcon,
            boolean isGrid
    ) {
        this.headline = headline;
        if (headline == null || (TextUtils.isEmpty(headline.getText()) && prefixIcon == null && postfixIcon == null)) {
            setText("");
            return;
        }

        SpannableStringBuilder headlineText = new SpannableStringBuilder();
        Spanned text;
        switch (headline.getSize()){
            case COLOSSAL_ALL_CAPS:
            case JUMBO_ALL_CAPS:
            case GARGANTUAN_ALL_CAPS:
                text = Html.fromHtml(headline.getText().toUpperCase());
                break;
            default: text = Html.fromHtml(headline.getText());
        }
        headlineText.append(text);
        if ((prefixIcon != null && headlineText.toString().isEmpty()) || postfixIcon != null) {
            headlineText.append(" ");
        }

        com.wapo.flagship.features.grid.model.FontStyle headlineFontStyle = (headline.getFontStyle() != null) ? headline.getFontStyle() : com.wapo.flagship.features.grid.model.FontStyle.NORMAL_STYLE;
        int fontStyleResId = headline.getSize() == Size.TINY ? lightResId: getResId(headline.getStyle(), headlineFontStyle);

        //Setting font for Style section articles
        if (headline.getStyle() == Style.STYLE) {
            if (headline.getFontStyle() == FontStyle.ITALIC_STYLE) {
                fontStyleResId = getResId(headline.getStyle(), FontStyle.ULTRA_ITALIC_STYLE);
            } else {
                fontStyleResId = getResId(headline.getStyle(), FontStyle.ULTRA_STYLE);
            }
        } else if (headline.getStyle() == Style.CONVERSATIONS) {
            fontStyleResId = getResId(headline.getStyle(), headlineFontStyle);
        }

        headlineText.setSpan(
                new WpTextAppearanceSpan(
                        getContext(),
                        fontStyleResId,
                        ModelHelper.getHeadlineSize(getContext(), headline, isGrid)
                ),
                0,
                headlineText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        Drawable resolvedPrefixIcon = null;
        if (prefixIcon != null) {
            resolvedPrefixIcon = prefixIcon;
        } else if(headline.getType() == BulletType.BULLET) {
            resolvedPrefixIcon = listDrawable;
        }

        setText(headlineText, resolvedPrefixIcon, postfixIcon, true);

        com.wapo.flagship.features.grid.model.Alignment headlineAlignment = (headline.getAlignment() != null) ? headline.getAlignment() : com.wapo.flagship.features.grid.model.Alignment.LEFT;
        if (headlineAlignment != Alignment.INHERIT) {
            setTextGravity(getGravity(headlineAlignment));
        }
    }

    public void setHeadline(
            @Nullable Headline headline,
            @Nullable HeadlineIcon prefixIcon,
            @Nullable HeadlineIcon postfixIcon,
            boolean isGrid
    ) {
        Drawable resolvedPrefixIcon = null;
        Drawable resolvedPostfixIcon = null;
        if (prefixIcon != null) {
            resolvedPrefixIcon = iconsMap.get(prefixIcon);
        }
        if (postfixIcon != null) {
            resolvedPostfixIcon = iconsMap.get(postfixIcon);
        }
        setHeadline(headline, resolvedPrefixIcon, resolvedPostfixIcon, isGrid);
    }

    private int getResId(Style style, FontStyle headlineFontStyle) {
        if (style == Style.CONVERSATIONS) {
            switch (headlineFontStyle) {
                case HIGHLIGHT_STYLE:
                    return franklinHighLightResId;
                case NORMAL_STYLE:
                    return franklinNormalResId;
                case THIN_STYLE:
                    return franklinThinResId;
                case REGULAR_STYLE:
                    return franklinRegularResId;
                case BOLD_STYLE:
                    return franklinBoldResId;
                case ITALIC_STYLE:
                    return franklinItalicResId;
                case LIGHT_STYLE:
                    return franklinLightResId;
                case ULTRA_STYLE:
                    return franklinUltraResId;
                case ULTRA_ITALIC_STYLE:
                    return franklinUltraItalicResId;
            }
        } else {
            switch (headlineFontStyle) {
                case HIGHLIGHT_STYLE:
                    return highLightResId;
                case NORMAL_STYLE:
                    return normalResId;
                case THIN_STYLE:
                    return thinResId;
                case REGULAR_STYLE:
                    return regularResId;
                case BOLD_STYLE:
                    return boldResId;
                case ITALIC_STYLE:
                    return italicResId;
                case LIGHT_STYLE:
                    return lightResId;
                case ULTRA_STYLE:
                    return ultraResId;
                case ULTRA_ITALIC_STYLE:
                    return ultraItalicResId;
            }
        }
        return normalResId;
    }

    public Headline getHeadline() {
        return headline;
    }

    public void updateHeadlineColor(int color) {
        this.updateColor(color);
    }
}
