package com.wapo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.content.ContextCompat;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.wapo.text.TypefaceCache;

public abstract class StreamModuleView extends ViewGroup {
    private int _layout = 0;
    private int _timeFontSize = 15;
    private int _kickerColor = Color.rgb(4, 107, 159);
    private int _timeColor = Color.rgb(105, 104, 105);
    private Typeface _timeFont = null;
    private Typeface _labelFont;

    private TextView _kickerView;
    private TextView _bylineView;
    private TextView _headlineView;
    private TextView _timeAndBlurbView;
    private ListView _liveBlogListView;
    private LinearLayout _liveBlogLayout;
    private TextView _liveBlogLastUpdated;

    private String _label;
    private String _byline;
    private String _title;
    private String _time;
    private String _blurb;
    private String _separator = "|";
    private boolean nightMode;

    public StreamModuleView(Context context) {
        super(context);
    }

    public StreamModuleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StreamModuleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        processAttributes(context, attrs);
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public void setNightMode(boolean nightMode) {
        this.nightMode = nightMode;
    }

    protected abstract int getDefaultLayout();

    protected void processAttributes(Context context, AttributeSet attrs) {
        _layout = getDefaultLayout();

        String timeFont = null;
        String labelFont = null;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs,
                    new int[] {
                            androidx.appcompat.R.attr.layout,
                            R.attr.timeFont,
                            R.attr.timeFontSize,
                            R.attr.labelFont,
                            R.attr.labelColor,
                            R.attr.timeColor
                    }
            );

            try {
                int i = 0;
                if (a.hasValue(i)) {
                    _layout = a.getResourceId(i, getDefaultLayout());
                }
                i = 1;
                if (a.hasValue(i)) {
                    timeFont = a.getString(i);
                }
                i = 2;
                if (a.hasValue(i)) {
                    _timeFontSize = a.getDimensionPixelSize(i, _timeFontSize);
                }
                i = 3;
                if (a.hasValue(i)) {
                    labelFont = a.getString(i);
                }
                i = 4;
                if (a.hasValue(i)) {
                    _kickerColor = a.getColor(i, _kickerColor);
                }
                i = 5;
                if (a.hasValue(i)) {
                    _timeColor = a.getColor(i, _timeColor);
                }
            } finally {
                a.recycle();
            }
        }

        if (timeFont != null) {
            _timeFont = TypefaceCache.getTypeface(context, timeFont);
        }

        if (labelFont != null) {
            _labelFont = TypefaceCache.getTypeface(context, labelFont);
        }

    }

    public void setTime(String value) {
        _time = value;
        updateBlurb();
    }

    public String getTitle() {
        return _title;
    }

    public void setBlurb(String value) {
        _blurb = value;
        updateBlurb();
        _timeAndBlurbView.setTextColor(getColor(R.color.streamm_headline, R.color.streamm_headline_white));
    }

    public void setHeadline(String value) {
        _title = value;
        _headlineView.setText(value);
        _headlineView.setTextColor(getColor(R.color.streamm_headline, R.color.streamm_headline_white));
        _headlineView.setVisibility(value == null ? GONE : VISIBLE);
    }

    public void setHeadlineBold(String value) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(value);
        sb.setSpan(
                new StyleSpan(Typeface.BOLD),
                0, sb.length(),
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        );
        _headlineView.setText(sb);
        _headlineView.setTextColor(getColor(R.color.streamm_headline, R.color.streamm_headline_white));
        _headlineView.setVisibility(value == null ? GONE : VISIBLE);
    }

    public void hidePanel() {
        setViewVisibility(_headlineView, GONE);
        setViewVisibility(_kickerView, GONE);
        setViewVisibility(_bylineView, GONE);
        setViewVisibility(_timeAndBlurbView, GONE);
    }

    private void setViewVisibility(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    public void updatePanelForLiveBlog(String value, String gridId) {
        setViewVisibility(_liveBlogLayout, VISIBLE);
        if (_kickerView != null) {
            _kickerView.setText(getResources().getString(R.string.wapo_views_live));
            _kickerView.setBackgroundColor(Color.RED);
            _kickerView.setTextColor(Color.WHITE);
            _kickerView.setVisibility(VISIBLE);
            _kickerView.setGravity(Gravity.CENTER);
            _kickerView.setWidth(getResources().getDimensionPixelSize(R.dimen.live_label_width));
            _kickerView.setHeight(getResources().getDimensionPixelSize(R.dimen.streamm_button_height));
            _kickerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        }
        if (_headlineView != null) {
            _headlineView.setText(value);
            _headlineView.setVisibility(VISIBLE);
        }
        if (_liveBlogLastUpdated != null) {
            _liveBlogLastUpdated.setTag(gridId);
        }
        if (_liveBlogListView != null) {
            _liveBlogListView.setTag(gridId);
        }
    }

    public void setLiveBlogListView(ListView _liveBlogListView) {
        this._liveBlogListView = _liveBlogListView;
    }

    public ListView getLiveBlogListView() {
        return _liveBlogListView;
    }

    public TextView getLiveBlogLastUpdated() {
        return _liveBlogLastUpdated;
    }

    public void setLiveBlogLayout(LinearLayout _liveBlogLayout) {
        this._liveBlogLayout = _liveBlogLayout;
    }

    public void setLiveBlogLastUpdated(TextView _liveBlogLastUpdated) {
        this._liveBlogLastUpdated = _liveBlogLastUpdated;
    }

    public void setLabelAndByline(String source, String author) {
        _label = source;
        _byline = author;
        updateKicker();
        updateByLine();
    }

    public String setDisplayName (String type){

        switch (type){
            case "breaking-news":
                return getContext().getString(R.string.smv_breaking_news);
            case "the7_briefs" :
                return getContext().getString(R.string.smv_the7_briefs);
            case "politics":
                return getContext().getString(R.string.smv_politics);
            case "health_and_science":
                return getContext().getString(R.string.smv_health_science);
            case "entertainment":
                return getContext().getString(R.string.smv_entertainment);
            case "world":
                return getContext().getString(R.string.smv_world);
            case "local":
                return getContext().getString(R.string.smv_local);
            case "sports":
                return getContext().getString(R.string.smv_sports);
            case "business_and_tech":
                return getContext().getString(R.string.smv_business_tech);
            case "opinions":
                return getContext().getString(R.string.smv_opinions);
            case "editors_picks":
                return getContext().getString(R.string.smv_editorspicks);
            case "news_quiz":
                return getContext().getString(R.string.smv_games);
            case "advice":
                return getContext().getString(R.string.smv_advice);
            case "us":
                return getContext().getString(R.string.smv_us);
            case "business":
                return getContext().getString(R.string.smv_business);
            case "technology":
                return getContext().getString(R.string.smv_technology);
            case "climate":
                return getContext().getString(R.string.smv_climate);
            case "health":
                return getContext().getString(R.string.smv_health);
            case "food":
                return getContext().getString(R.string.smv_food);
            case "special_report":
                return getContext().getString(R.string.smv_special_report);
            case "daily_read":
                return getContext().getString(R.string.daily_read);
            default:
                return getContext().getString(R.string.smv_newsalerts);
        }

        }

    public void setByLineDateSeparator(String separator) {
        if (!TextUtils.isEmpty(separator)) {
            _separator = separator;
        }
    }

    public void setSectionType(String sectionType){
        _byline = setDisplayName(sectionType);
        updateKicker();
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean isVisible = false;

        if (!TextUtils.isEmpty(_byline)) {
            sb.append(_byline);
            sb.setSpan(new StyleSpan(Typeface.BOLD),
            0, sb.length(),Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            isVisible = true;
            }
        sb.append("\n");
        SpannableStringBuilder temp = checkForTimeAndAddToBuilder(sb, false);
        isVisible = (sb = (temp == null ? sb : temp)) == temp  | isVisible;

        _bylineView.setText(sb);
        _bylineView.setTextColor(getColor(R.color.streamm_section, R.color.streamm_byline_white));
        _bylineView.setVisibility(isVisible ? VISIBLE : GONE);
    }

    public void setLabel(String value) {
        _label = value;
        updateByLine();
    }

    public void setByline(String value) {
        _byline = value;
        updateByLine();
    }

    public int getLayout() {
        return _layout;
    }

    protected void setKickerView(TextView _kickerView) {
        this._kickerView = _kickerView;
        this._kickerView.setTextColor(getColor(R.color.streamm_label, R.color.streamm_label_white));
    }

    protected void setBylineView(TextView _bylineView) {
        this._bylineView = _bylineView;
        this._bylineView.setTextColor(getColor(R.color.streamm_byline, R.color.streamm_byline_white));
    }

    protected void setHeadlineView(TextView _headlineView) {
        this._headlineView = _headlineView;
        this._headlineView.setTextColor(getColor(R.color.streamm_headline, R.color.streamm_headline_white));
    }

    protected void setTimeAndBlurbView(TextView _timeAndBlurbView) {
        this._timeAndBlurbView = _timeAndBlurbView;
        this._timeAndBlurbView.setTextColor(getColor(R.color.streamm_blurb, R.color.streamm_blurb_white));
    }

    protected TextView getKickerView() {
        return _kickerView;
    }

    protected TextView getBylineView() {
        return _bylineView;
    }

    protected TextView getHeadlineView() {
        return _headlineView;
    }

    public TextView getTimeAndBlurbView() {
        return _timeAndBlurbView;
    }

    protected void updateKicker() {
        if (_kickerView == null) return;

        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean isVisible = false;
        final boolean hasSource = !TextUtils.isEmpty(_label);
        if (hasSource) {
            sb.append(_label.toUpperCase());
            sb.setSpan(
                    _labelFont == null ? new StyleSpan(Typeface.BOLD) : new CustomTypefaceSpan("custom", _labelFont),
                    0, sb.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            );
            _kickerColor = getColor(R.color.streamm_label, R.color.streamm_label_white);
            sb.setSpan(new ForegroundColorSpan(_kickerColor), 0, sb.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            isVisible = true;
        }

        _kickerView.setText(sb);
        _kickerView.setTextColor(getColor(R.color.streamm_label, R.color.streamm_label_white));
        _kickerView.setVisibility(isVisible ? VISIBLE : GONE);
    }

    protected void updateByLine() {
        if (_bylineView == null) return;

        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean isVisible = false;

        if (!TextUtils.isEmpty(_byline)) {
            sb.append(_byline);
            isVisible = true;
        }
        sb.append(" ");
        SpannableStringBuilder temp = checkForTimeAndAddToBuilder(sb, false);
        isVisible = (sb = (temp == null ? sb : temp)) == temp  | isVisible;

        _bylineView.setText(sb);
        _bylineView.setTextColor(getColor(R.color.streamm_byline, R.color.streamm_byline_white));
        _bylineView.setVisibility(isVisible ? VISIBLE : GONE);
    }

    protected void updateBlurb() {
        boolean isVisible = false;
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean hasTime = false;
        if (!TextUtils.isEmpty(_blurb)) {
            if (hasTime) {
                sb.append(" ").append(_separator).append(" ");
            }
            sb.append(Html.fromHtml(_blurb));
            isVisible = true;
        }

        _timeAndBlurbView.setText(sb);
        _timeAndBlurbView.setTextColor(getColor(R.color.streamm_blurb, R.color.streamm_blurb_white));
        _timeAndBlurbView.setVisibility(isVisible ? VISIBLE : GONE);
    }

    private SpannableStringBuilder checkForTimeAndAddToBuilder(SpannableStringBuilder sb, boolean setSpan) {
        boolean hasTime = !TextUtils.isEmpty(_time);
        if (hasTime) {
            sb.append(" ").append(_separator).append(" ");
            int startTime = sb.length();
            sb.append(_time);
            if(setSpan) {
                sb.setSpan(
                        _timeFont == null ? new StyleSpan(Typeface.BOLD) : new CustomTypefaceSpan("custom", _timeFont),
                        0, sb.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                );
                sb.setSpan(
                        new AbsoluteSizeSpan(_timeFontSize),
                        0, sb.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                );
            }
            else{
                _timeColor =getColor(R.color.streamm_time, R.color.streamm_time_white);
            }
            sb.setSpan(new ForegroundColorSpan(_timeColor), startTime, sb.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            return sb;
        }
        return null;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState state = new SavedState(superState);
        state.label = _label;
        state.byline = _byline;
        state.headline = _title;
        state.time = _time;
        state.blurb = _blurb;

        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable in) {
        if (!(in instanceof SavedState)) {
            super.onRestoreInstanceState(in);
            return;
        }

        SavedState state = (SavedState) in;
        super.onRestoreInstanceState(state.getSuperState());

        setLabel(state.label);
        setByline(state.byline);
        setHeadline(state.headline);
        setTime(state.time);
        setBlurb(state.blurb);
    }

    private int getColor(int dayColorId, int nightColorId) {
        return ContextCompat.getColor(
                getContext(),
                nightMode ?
                        nightColorId :
                        dayColorId
        );
    }

    public static class SavedState extends BaseSavedState {
        private String label;
        private String byline;
        private String headline;
        private String time;
        private String blurb;

        public Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public SavedState(Parcel in) {
            super(in);
            label = in.readString();
            byline = in.readString();
            headline = in.readString();
            time = in.readString();
            blurb = in.readString();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(label);
            out.writeString(byline);
            out.writeString(headline);
            out.writeString(time);
            out.writeString(blurb);
        }
    }
}
