package com.wapo.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.MovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.MetricAffectingSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.wapo.text.WpTextAppearanceSpan;
import com.wapo.view.selection.Selectable;
import com.wapo.view.selection.SelectableTextView;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.getMode;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static com.wapo.view.selection.SelectionController.getViewScreenRect;
import static java.lang.Math.max;
import static java.lang.Math.round;

/**
 * @author Thad Cox
 */
public class FlowableTextView extends ViewGroup implements FlowableView, Selectable {

    public static final int FLOAT_NONE = 0;
    public static final int FLOAT_RIGHT = 1;
    public static final int FLOAT_LEFT = -1;
    private static final boolean D = BuildConfig.DEBUG;

    private final SelectableTextView sideText;
    private final SelectableTextView centerText;
    private int staticLineGap = Integer.MIN_VALUE;
    private int boxWidth = 0;
    private int boxHeight = 0;
    private int boxSide = FLOAT_NONE;
    private CharSequence text = "";
    @Nullable
    private Drawable prefixIcon;
    @Nullable
    private Drawable postfixIcon;
    private boolean shouldScalePrefixIcon;
    private int maxLines = Integer.MAX_VALUE;
    private int obstructionPaddingSide = -1, obstructionPaddingBottom = -1;
    private int minTextWidth;

    private TextFitter textFitter;
    private float lineSpacingMulti = 1;
    private float lineSpacingAdditional = 0;
    private int centerTextVertShift;
    private TextPaint textPaintVertShift = new TextPaint();
    private String key;
    private int drawablePadding = 20;
    private Integer color;

    /**
     * {@inheritDoc}
     */
    public FlowableTextView(Context context) {
        this(context, null);
    }

    /**
     * {@inheritDoc}
     */
    public FlowableTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    /**
     * {@inheritDoc}
     */
    public FlowableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = getResources();
        assert resources != null;

        //Make sure that we don't get a background color assigned to us in parent, we should always be transparent using the Flowable Layout
        // for Background because we don't want to draw our background over top of the floated box
        setBackgroundColor(resources.getColor(android.R.color.transparent));


        textFitter = new TextFitter();

        this.sideText = new SelectableTextView(context, attrs, defStyle);
        this.centerText = new SelectableTextView(context, attrs, defStyle);

        addView(this.sideText);
        addView(this.centerText);

        //Get Defaults
        int obstructionPaddingBottom = resources.getDimensionPixelSize(R.dimen.flowable_layout_default_obsruction_padding);
        int obstructionPaddingSide = obstructionPaddingBottom;
        final int defaultMinTextWidth = resources.getDimensionPixelSize(R.dimen.flowable_text_view_default_min_text_width);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowableTextView, defStyle, 0);

            try {
                assert a != null;

                setMaxLines(a.getInt(R.styleable.FlowableTextView_android_maxLines, Integer.MAX_VALUE));
                setTextAppearance(a.getResourceId(R.styleable.FlowableTextView_android_textAppearance, 0));

                int obstructionPadding = a.getDimensionPixelSize(R.styleable.FlowableTextView_obstructionPadding, 0);
                obstructionPaddingBottom = a.getDimensionPixelSize(R.styleable.FlowableTextView_obstructionPaddingBottom, obstructionPadding);
                obstructionPaddingSide = a.getDimensionPixelSize(R.styleable.FlowableTextView_obstructionPaddingSide, obstructionPadding);

                this.minTextWidth = a.getDimensionPixelSize(R.styleable.FlowableTextView_minSideTextWidth, defaultMinTextWidth);

                lineSpacingMulti = a.getFloat(R.styleable.FlowableTextView_android_lineSpacingMultiplier, 1f);
                lineSpacingAdditional = a.getDimensionPixelSize(R.styleable.FlowableTextView_android_lineSpacingExtra, 0);
                staticLineGap = a.getDimensionPixelSize(R.styleable.FlowableTextView_staticLineGap, Integer.MIN_VALUE);
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        } else {
            this.minTextWidth = defaultMinTextWidth;
        }
        setObstructionPaddingSide(obstructionPaddingSide);
        setObstructionPaddingBottom(obstructionPaddingBottom);
    }

    /**
     * Sets the text appearance for text in this FlowableTextView.  Simply a pass through to
     * {@link android.widget.TextView#setTextAppearance(android.content.Context, int)} passing the assigned context on to the children
     *
     *
     * @see android.widget.TextView#setTextAppearance(android.content.Context, int)
     */
    @SuppressWarnings("ConstantConditions")
    public void setTextAppearance(int textAppearance) {
        this.centerText.setTextAppearance(getContext(), textAppearance);
        this.sideText.setTextAppearance(getContext(), textAppearance);
    }

    public void setLineSpacing(float add, float multi){
        lineSpacingAdditional = add;
        lineSpacingMulti = multi;
        sideText.setLineSpacing(add, multi);
        centerText.setLineSpacing(add, multi);
    }


    @Override
    public void setFlowObstruction(int widthAdjustment, int heightAdjustment, int floatType) {
        //If this is new data sign it up to be re-measured
        if(this.boxHeight != heightAdjustment || this.boxWidth != widthAdjustment || this.boxSide != floatType) requestLayout();

        this.boxHeight = heightAdjustment;
        this.boxWidth = widthAdjustment;
        this.boxSide = floatType;

        requestLayout();
    }

    public final void setMovementMethod(MovementMethod movement) {
        this.sideText.setMovementMethod(movement);
        this.centerText.setMovementMethod(movement);
    }

    public void setDrawablePadding(int value) {
        this.drawablePadding = value;
    }

    public void setText(CharSequence text, @Nullable Drawable prefixIcon, @Nullable Drawable postfixIcon, boolean shouldScalePrefixIcon) {
        this.prefixIcon = prefixIcon;
        this.postfixIcon = postfixIcon;
        this.shouldScalePrefixIcon = shouldScalePrefixIcon;
        setTextInternal(text);
    }

    public void setText(CharSequence text, @Nullable Drawable prefixIcon, @Nullable Drawable postfixIcon) {
        setText(text, prefixIcon, postfixIcon, false);
    }

    public void setText(CharSequence text) {
        setText(text, null, null);
    }

    public void updateColor(int color) {
        this.color = color;
        if (prefixIcon != null) setPrefixIcon(prefixIcon, color);
        CharSequence text = this.getText();
        if (!TextUtils.isEmpty(text)) {
            SpannableString span = new SpannableString(text);
            span.setSpan(new ForegroundColorSpan(color), 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            setTextInternal(span);
        }
    }

    private void setTextInternal(CharSequence text){
        //Create a copy of the text so it doesn't get changed on us without us knowing
        if (text == null && this.text == null) {
            return;
        }

        if (text != null && text.equals(this.text)) {
            return;
        }

        this.text = text instanceof Spanned ?
                new SpannableString(text) :
                (text == null ? "" : text.toString());

        requestLayout();
    }

    public void setTextColor(int selectionColor) {
        this.color = selectionColor;
        centerText.setTextColor(selectionColor);
        sideText.setTextColor(selectionColor);
    }

    public void setTextGravity(int gravity) {
        centerText.setGravity(gravity);
        sideText.setGravity(gravity);
    }

    public float getTextSize() {
        return centerText.getTextSize();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        centerTextVertShift = 0;

        final int wMode = getMode(widthMeasureSpec);
        final int width = getSize(widthMeasureSpec);

        final int hMode = getMode(heightMeasureSpec);
        final int height = getSize(heightMeasureSpec);

        final int availableWidth = max(0, width - getPaddingLeft() - getPaddingRight());
        final int availableHeight = max(0, height - getPaddingTop() - getPaddingBottom());

        if (boxSide == FLOAT_NONE) {
            sideText.setVisibility(GONE);
            centerText.setVisibility(VISIBLE);

            centerText.setText(text);
            addIcons(centerText);
            centerText.measure(
                    makeMeasureSpec(availableWidth, wMode),
                    makeMeasureSpec(
                            max(0, availableHeight),
                            hMode
                    )
            );

            setMeasuredDimension(
                    resolveSize(centerText.getMeasuredWidth() + getPaddingLeft() + getPaddingRight(), widthMeasureSpec),
                    resolveSize(centerText.getMeasuredHeight() + getPaddingTop() + getPaddingBottom(), heightMeasureSpec)
            );

            return;
        }

        //Divide up the text
        final int boxWidthWithPadding = this.boxWidth + this.obstructionPaddingSide;
        final int boxHeightWithPadding = this.boxHeight + this.obstructionPaddingBottom;

        //setting the sideTextWidth to -1 before passing to divideText so that if it's not float it doesn't enable the sideText view, probably a better way to do this but it'll work
        final int sideTextWidth = availableWidth - boxWidthWithPadding;
        divideText(sideTextWidth, boxHeightWithPadding);

        int stWidth = 0;
        int stHeight = 0;
        if (sideText.getVisibility() != GONE) {
            sideText.measure(
                    makeMeasureSpec(sideTextWidth, EXACTLY),
                    UNSPECIFIED
            );
            //don't add the side text to the measured height because we already accounted for it's height based on the box
            stHeight = sideText.getMeasuredHeight();
            stWidth = sideText.getMeasuredWidth();


        }

        int centerTextWidth = 0;
        int centerTextHeight = 0;
        if (centerText.getVisibility() != GONE) {
            centerText.measure(
                    makeMeasureSpec(availableWidth, widthMeasureSpec),
                    makeMeasureSpec(
                            max(0, availableHeight - boxHeightWithPadding),
                            MeasureSpec.UNSPECIFIED
                    )
            );

            centerTextWidth = centerText.getMeasuredWidth();
            centerTextHeight = centerText.getMeasuredHeight();
        }

        int totalW = max(stWidth + boxWidthWithPadding, centerTextWidth) + getPaddingLeft() + getPaddingRight();
        int totalH;
        if (sideText.getVisibility() == GONE && centerText.getVisibility() != GONE) {
            totalH = centerTextHeight + getPaddingTop() + getPaddingBottom() + boxHeight + obstructionPaddingBottom;
        } else {
            totalH = centerTextHeight + stHeight + getPaddingTop() + getPaddingBottom();
        }

        if (sideText.getVisibility() != GONE && centerText.getVisibility() != GONE) {
            textPaintVertShift.set(sideText.getPaint());
            if (text instanceof Spanned) {
                MetricAffectingSpan[] spans = ((Spanned) text).getSpans(0, text.length(), MetricAffectingSpan.class);
                if (spans != null && spans.length == 1) {
                    spans[0].updateDrawState(textPaintVertShift);
                }
            }
            if (staticLineGap == Integer.MIN_VALUE) {
                Paint.FontMetrics fm = textPaintVertShift.getFontMetrics();
                centerTextVertShift = round((fm.bottom - fm.top) * (lineSpacingMulti - 1) + lineSpacingAdditional + Math.abs((fm.bottom - fm.top) - (fm.descent - fm.ascent)));
            } else {
                centerTextVertShift = staticLineGap;
            }
        }

        totalH += centerTextVertShift;

        setMeasuredDimension(
                resolveSize(
                        totalW,
                        widthMeasureSpec
                ),
                resolveSize(
                        totalH,
                        heightMeasureSpec
                )
        );
    }

    private void divideText(int firstSideWidth, int firstBoxHeight) {
        int firstBoxCharLength;

        if (firstSideWidth >= this.minTextWidth) {
            textFitter.setLineSpacingMultiplier(lineSpacingMulti);
            textFitter.setLineAdditionalVerticalPadding(lineSpacingAdditional);
            textFitter.setDisplayParametersMeasured(firstBoxHeight, firstSideWidth);
            textFitter.setPaint(sideText.getPaint());
            firstBoxCharLength = textFitter.getFittedLength(text, "");
            //Reset to make sure we don't use the same paint twice by mistake
            textFitter.reset();
        } else {
            firstBoxCharLength = 0;
        }

        //check for a single short wrapped line
        if (firstBoxCharLength > 0 && text.length() - firstBoxCharLength > 0) {
            CharSequence wrappedText = text.subSequence(firstBoxCharLength, text.length());
            StaticLayout wrappedLayout = new StaticLayout(wrappedText, sideText.getPaint(),
                    firstSideWidth,
                    Layout.Alignment.ALIGN_NORMAL, lineSpacingMulti,
                    lineSpacingAdditional, true);
            if (wrappedLayout.getLineCount() == 1) {
                firstBoxCharLength = text.length();
            }
        }

        if (firstBoxCharLength > 0) {
            sideText.setVisibility(VISIBLE);
            sideText.setText(this.text.subSequence(0, firstBoxCharLength));
        } else {
            sideText.setVisibility(View.GONE);
        }

        if(text.length() - firstBoxCharLength > 0){
            centerText.setText(this.text.subSequence(firstBoxCharLength, text.length()));
            centerText.setVisibility(VISIBLE);
        } else {
            centerText.setVisibility(View.GONE);
        }

        if (text.length() - firstBoxCharLength > 0) {
            addIcons(centerText);
        } else if (firstBoxCharLength > 0) {
            addIcons(sideText);
        }
    }

    private void addIcons(SelectableTextView sTextView) {
        setPrefixIcon(prefixIcon, color);
        if (postfixIcon != null && !TextUtils.isEmpty(sTextView.getText())) {
            SpannableString ss = new SpannableString(sTextView.getText());
            ss.setSpan(new ImageSpan(postfixIcon, ImageSpan.ALIGN_BOTTOM), ss.length() - 1, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sTextView.setText(ss);
        }
    }

    private void setPrefixIcon(@Nullable Drawable prefixIcon, @Nullable Integer color) {
        if (prefixIcon != null) {
            if (!TextUtils.isEmpty(sideText.getText())
                    && sideText.getVisibility() == View.VISIBLE) {
                setTextViewPrefixIcon(sideText, prefixIcon, color, shouldScalePrefixIcon);
                centerText.setPadding(drawablePadding + prefixIcon.getBounds().width(), 0, 0, 0);
            } else if (!TextUtils.isEmpty(centerText.getText())
                    && centerText.getVisibility() == View.VISIBLE) {
                setTextViewPrefixIcon(centerText, prefixIcon, color, shouldScalePrefixIcon);
            }
        } else {
            setTextViewPrefixIcon(sideText, null, null, false);
            setTextViewPrefixIcon(centerText, null, null, false);
        }
    }

    private void setTextViewPrefixIcon(TextView textView, @Nullable Drawable icon, @Nullable Integer color, boolean shouldScaleIcon) {
        if (icon != null) {
            Drawable finalIcon = icon.mutate();
            if (color != null) {
                finalIcon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            }
            if (shouldScaleIcon) {
                Integer textHeight = getTextSize(textView);
                int intrinsicWidth = finalIcon.getIntrinsicWidth();
                int intrinsicHeight = finalIcon.getIntrinsicHeight();
                if (textHeight != null && intrinsicWidth > 0 && intrinsicHeight > 0) {
                    float aspectRatio = (float) intrinsicWidth / intrinsicHeight;
                    int iconHeight = (int) (textHeight * 1.3);
                    int iconWidth = (int) (iconHeight * aspectRatio);
                    finalIcon.setBounds(0, 0, iconWidth, iconHeight);
                    textView.setCompoundDrawables(finalIcon, null, null, null);
                } else {
                    textView.setCompoundDrawablesWithIntrinsicBounds(finalIcon, null, null, null);
                }
            } else {
                textView.setCompoundDrawablesWithIntrinsicBounds(finalIcon, null, null, null);
            }
            textView.setCompoundDrawablePadding(drawablePadding);
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            textView.setCompoundDrawablePadding(0);
            textView.setPadding(0, 0, 0, 0);
        }
    }

    private Integer getTextSize(TextView textView) {
        try {
            CharSequence text = textView.getText();
            if (text instanceof Spanned spanned) {
                WpTextAppearanceSpan[] spans = spanned.getSpans(
                        0,
                        spanned.length(),
                        WpTextAppearanceSpan.class
                );
                if (spans.length > 0) {
                    return spans[0].getTextSize();
                } else {
                    return null;
                }
            } else {
                return (int) textView.getTextSize();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        if(!changed) return;//Early exit, we don't care if the layout hasn't changed

        //Get actual bounds for the content
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = r - l - getPaddingRight();

        if (sideText.getVisibility() != GONE) {
            switch (boxSide) {
                case FLOAT_RIGHT:  //Text goes on left
                    sideText.layout(left, top, left + sideText.getMeasuredWidth(), top + sideText.getMeasuredHeight());
                    break;

                case FLOAT_LEFT:  //Text goes on right
                    sideText.layout(right - sideText.getMeasuredWidth(), top, right, top + sideText.getMeasuredHeight());
                    break;
            }

            top += sideText.getMeasuredHeight();
        }

        if (centerText.getVisibility() != GONE) {
            if (sideText.getVisibility() == GONE) {
                top += boxHeight + obstructionPaddingBottom;
            } else {
                top += centerTextVertShift;
            }

            centerText.layout(left, top, left + centerText.getMeasuredWidth(), top + centerText.getMeasuredHeight());
        }
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public void setObstructionPaddingSide(int obstructionPaddingSide) {
        this.obstructionPaddingSide = obstructionPaddingSide;
    }

    public void setObstructionPaddingBottom(int obstructionPaddingBottom) {
        this.obstructionPaddingBottom = obstructionPaddingBottom;
    }

    @Override
    public void setFocusable(boolean focusable) {
        super.setFocusable(focusable);
        centerText.setFocusable(focusable);
        sideText.setFocusable(focusable);
    }

    @Override
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        centerText.setClickable(clickable);
        sideText.setClickable(clickable);
    }

    @Override
    public void setFocusableInTouchMode(boolean focusableInTouchMode) {
        super.setFocusableInTouchMode(focusableInTouchMode);
        centerText.setFocusableInTouchMode(focusableInTouchMode);
        sideText.setFocusableInTouchMode(focusableInTouchMode);
    }

    public CharSequence getText() {
        return new SpannedString(text);
    }

    public boolean hasTextBelowBox() {
        return centerText.getVisibility() != GONE;
    }

    public void setKey(String key) {
        this.key = key;
        sideText.setKey("side:" + key);
        centerText.setKey("bottom:" + key);
    }


    @Override
    public void selectText(int start, int end) {
        if (sideText.getVisibility() == GONE){
            centerText.selectText(start, end);
            return;
        }
        int length  = sideText.getText().length();
        if(end < length) {
            sideText.selectText(start, end);
            centerText.selectText(0, 0);
            return;
        }

        if (end >= length && start < length) {
            sideText.selectText(start, length);
            centerText.selectText(0, end-length);
            return;
        }
        int length2 = length + centerText.getText().length();
        if (start >= length && end <= length2 ){
            sideText.selectText(0, 0);
            centerText.selectText(start - length, end - length);
        }


    }

    @Override
    public CharSequence getSelectedText() {
        SpannableStringBuilder ssb = new SpannableStringBuilder(sideText.getSelectedText());
        return  ssb.append(centerText.getSelectedText());
    }

    @Override
    public void setColor(int selectionColor) {
        centerText.setColor(selectionColor);
        sideText.setColor(selectionColor);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public int getOffsetForPosition(int x, int y) {
        if (sideText.getVisibility() != GONE){
            int correctX;
            switch (x){
                case Selectable.FIRST_SYMBOL:
                    correctX = sideText.getLeft();
                    break;
                case Selectable.LAST_SYMBOL:
                    correctX = sideText.getRight();
                    break;
                default:
                    correctX = x;
            }
            if (sideText.getTop() <= y && sideText.getBottom() >= y) {
                if (sideText.getLeft() <= correctX && sideText.getRight() >= correctX) {
                    return sideText.getOffsetForPosition(correctX - sideText.getLeft(), y - sideText.getTop());
                }

                if (x < sideText.getLeft()){
                    correctX = sideText.getLeft();
                } else if (x > sideText.getRight()){
                    correctX = sideText.getRight();
                }

                if (sideText.getLeft() <= correctX && sideText.getRight() >= correctX) {
                    return sideText.getOffsetForPosition(correctX - sideText.getLeft(), y - sideText.getTop());
                }
            }
        }
        if (centerText.getVisibility() != GONE){
            int correctX;
            switch (x){
                case Selectable.FIRST_SYMBOL:
                    correctX = centerText.getLeft();
                    break;
                case Selectable.LAST_SYMBOL:
                    correctX = centerText.getRight();
                    break;
                default:
                    correctX = x;
            }
            if (centerText.getLeft() <= correctX && centerText.getRight() >= correctX && centerText.getTop() <= y && centerText.getBottom() >= y) {
                int pos = centerText.getOffsetForPosition(correctX - centerText.getLeft(), y - centerText.getTop());
                pos += sideText.getVisibility() != GONE ? sideText.getText().length() : 0;
                return pos;
            }
        }
        return  -1;
    }

    @Override
    public Rect getScreenRect(Rect rect) {
        return getViewScreenRect(this, rect);
    }

    public float[] getScreenPositionForOffset(int pos, float[] position ) {
        if (sideText.getVisibility() != GONE){
            if (pos >= 0 && pos < sideText.length()){
                position = sideText.getScreenPositionForOffset(pos, position);
                return position;
            }
        }
        if (centerText.getVisibility() != GONE){
            position = centerText.getScreenPositionForOffset(sideText.getVisibility() != GONE ? pos - sideText.getText().length() : pos, position);
            return position;
        }

        return new float[]{-1, -1};
    }

    public SelectableTextView getSideText() {
        return sideText;
    }

    public SelectableTextView getCenterText() {
        return centerText;
    }

    public int getLineCount() {
        return Math.max(centerText.getLineCount(), sideText.getLineCount());
    }

    public void setLines(int num) {
        centerText.setLines(num);
        sideText.setLines(num);
    }

    @Override
    public void setContentDescription(CharSequence contentDescription) {
        centerText.setContentDescription(contentDescription);
    }
}
