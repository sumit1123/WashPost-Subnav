package com.wapo.flagship.features.pagebuilder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.wapo.flagship.features.grid.SignatureDateFormat;
import com.wapo.flagship.features.grid.model.Alignment;
import com.wapo.flagship.features.grid.model.Signature;
import com.wapo.text.WpTextAppearanceSpan;
import com.wapo.view.FlowableTextView;
import com.washingtonpost.android.sections.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CellBylineView extends FlowableTextView {

    private static final TextMeta TEXT_META_EMPTY = new TextMeta("", 0);
    private static final int RESERVED_TIMESTAMP_EMS = 15;
    private int bylineStyle = R.style.homepagestory_byline_style;
    private int sectionStyle = R.style.homepagestory_byline_style_section;
    private int timestampStyle = R.style.homepagestory_byline_style_timestamp;
    private static final String DOUBLE_SPACE = "  ";
    private Calendar calendarStart;
    private Calendar calendarEnd;

    private Signature signature;
    private Integer textColor = null;
    private com.wapo.flagship.features.grid.model.Alignment signatureAlignment;

    public CellBylineView(Context context) {
        this(context, null);
    }

    public CellBylineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellBylineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CellBylineView,
                0, 0
        );

        try {
            bylineStyle = a.getResourceId(R.styleable.CellBylineView_byline_style, R.style.homepagestory_byline_style);
            sectionStyle = a.getResourceId(R.styleable.CellBylineView_section_style, R.style.homepagestory_byline_style_section);
            timestampStyle = a.getResourceId(R.styleable.CellBylineView_timestamp_style, R.style.homepagestory_byline_style_timestamp);
        } finally {
            a.recycle();
        }
        setFocusable(true);
    }

    public Signature getSignature() {
        return signature;
    }

    public void setSignature(Signature signature, boolean isGrid) {
        if (isGrid) {
            setBylineStyle(bylineStyle);
            setSectionStyle(sectionStyle);
            setTimestampStyle(timestampStyle);
        }

        boolean same = safeEquals(signature, this.signature);
        this.signature = signature;
        if (!same) {
            TextMeta textMeta = getTextFromSignature(signature);
            setText(textMeta.text);
            if (textMeta.text.length() > 0) {
                setImportantForAccessibility(CellBylineView.IMPORTANT_FOR_ACCESSIBILITY_YES);
                setContentDescription(textMeta.text);
            } else {
                setImportantForAccessibility(CellBylineView.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
//            setMinEms(textMeta.minEms);
        }
        if (signature != null) {

            if (signature.getAlignment() != null) {
                signatureAlignment = signature.getAlignment();
            }
            if (signatureAlignment != null && signatureAlignment != Alignment.INHERIT) {
                setTextGravity(ModelHelper.getGravity(signatureAlignment));
            }
        }
    }

    private TextMeta getTextFromSignature(Signature signature) {
        if (signature == null) {
            return TEXT_META_EMPTY;
        }

        Date date = ModelHelper.parseSectionFrontDate(signature.getTimestamp());
        SignatureDateFormat dateFormat = signature.getDateFormat();
        final Spanned byLine = getFormattedText(getContext(), signature.getByLine(), bylineStyle);
        Spanned timestamp = getFormattedText(getContext(), getDate(date, dateFormat), timestampStyle);
        final Spanned section = getFormattedText(getContext(), signature.getSection(), sectionStyle);
        final boolean bylineEmpty = TextUtils.isEmpty(byLine);
        final boolean timestampEmpty = TextUtils.isEmpty(timestamp);
        final boolean sectionEmpty = TextUtils.isEmpty(section);
        if (bylineEmpty && timestampEmpty && sectionEmpty) {
            return TEXT_META_EMPTY;
        }
        String dot = getResources().getString(R.string.homepage_byline_dot);
        SpannableStringBuilder totalSpan = new SpannableStringBuilder("");
        int ems = 0;
        if (!bylineEmpty) {
            totalSpan.append(byLine);
            setSpan(totalSpan, totalSpan.length() - byLine.length(), totalSpan.length(), bylineStyle);
        }

        if (!sectionEmpty) {
            addDotIfNeed(dot, totalSpan);
            totalSpan.append(section);
            setSpan(totalSpan, totalSpan.length() - section.length(), totalSpan.length(), sectionStyle);
        }

        if (!timestampEmpty) {
            addDotIfNeed(dot, totalSpan);
            ems = totalSpan.length() + RESERVED_TIMESTAMP_EMS;
            totalSpan.append(timestamp);
            setSpan(totalSpan, totalSpan.length() - timestamp.length(), totalSpan.length(), timestampStyle);
            if (isRecent(date, signature.getRecencyThreshold())) {
                totalSpan.setSpan(
                        new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.grid_recency_threshold)),
                        totalSpan.length() - timestamp.length(),
                        totalSpan.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                );
            }
        }

        return new TextMeta(totalSpan, ems);
    }

    private boolean isRecent(Date date, long recencyThreshold) {
        if (date == null) {
            return false;
        }
        if (recencyThreshold <= 0) {
            return false;
        }
        long delta = Calendar.getInstance().getTimeInMillis() - date.getTime();
        if (delta <= 0) {
            return false;
        }
        return delta <= TimeUnit.MINUTES.toMillis(recencyThreshold);
    }

    private void addDotIfNeed(String dot, SpannableStringBuilder totalSpan) {
        if (totalSpan.length() > 0) {
            int start = totalSpan.length();
            totalSpan
                    .append(DOUBLE_SPACE)
                    .append(dot)
                    .append(DOUBLE_SPACE);
            setSpan(totalSpan, start, totalSpan.length(), bylineStyle);
        }
    }

    private void setSpan(SpannableStringBuilder span, int start, int end, int style) {
        WpTextAppearanceSpan textAppearanceSpan = new WpTextAppearanceSpan(getContext(), style);
        if (textColor != null) {
            textAppearanceSpan.setTextColor(ColorStateList.valueOf(textColor));
        }
        span.setSpan(
                textAppearanceSpan,
                start,
                end,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        );
    }

    private String getDate(Date date, SignatureDateFormat dateFormat) {
        if (date == null || date.after(getNow())) {
            return "";
        }
        SimpleDateFormat extendedDateFormat;
        if (dateFormat != null) {
            switch (dateFormat) {
                case MONTH_DAY:
                    extendedDateFormat = new SimpleDateFormat("MMMM d", Locale.US);
                    break;
                case MONTH_DAY_YEAR:
                    extendedDateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
                    break;
                case EXTENDED_WITH_MINUTES:
                    extendedDateFormat = new SimpleDateFormat("EEEE, MMMM d, 'at' h:mm aaaa z", Locale.US);
                    break;
                default:
                    extendedDateFormat = new SimpleDateFormat("EEEE, MMMM d, 'at' h aaaa z", Locale.US);
                    break;
            }
            return extendedDateFormat.format(date);
        }
        return DateUtils.getRelativeTimeSpanString(date.getTime()).toString();
    }

    private Date getNow() {
        if (calendarEnd == null) {
            calendarEnd = Calendar.getInstance();
        }

        return calendarEnd.getTime();
    }

    private Date getHoursPrior(int hour) {
        if (calendarStart == null) {
            Date now = getNow();
            calendarStart = Calendar.getInstance();
            long past3hours = hour * 60 * 60 * 1000;
            calendarStart.setTimeInMillis(now.getTime() - past3hours);
        }

        return calendarStart.getTime();
    }

    private static boolean safeEquals(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    public void setSectionStyle(int sectionStyle) {
        this.sectionStyle = sectionStyle;
    }

    public void setBylineStyle(int bylineStyle) {
        this.bylineStyle = bylineStyle;
    }

    public void setTimestampStyle(int timestampStyle) {
        this.timestampStyle = timestampStyle;
    }

    public void setTextGravity(int gravity) {
        super.setTextGravity(gravity);
    }

    private static class TextMeta {
        final CharSequence text;
        final int minEms;

        private TextMeta(CharSequence text, int minEms) {
            this.text = text;
            this.minEms = minEms;
        }
    }

    private Spanned getFormattedText(Context context, String text, int style) {
        Spanned spannedText = null;
        if (text != null) {
            if (isAllCaps(context, style)) {
                text = text.toUpperCase();
            }
            spannedText = Html.fromHtml(text);
        }
        return spannedText;
    }

    private boolean isAllCaps(Context context, int resId) {
        TypedArray a = null;
        try {
            a = context.obtainStyledAttributes(resId, new int[]{android.R.attr.textAllCaps});
            return a.getBoolean(0, false);
        } finally {
            if (a != null) {
                a.recycle();
            }
        }
    }

    public void updateBylineColor(int color) {
        this.textColor = color;
        Signature signature = this.signature;
        this.signature = null;
        setSignature(signature, true);
    }
}
