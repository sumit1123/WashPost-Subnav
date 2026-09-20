package com.wapo.view;

import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Thad Cox
 */
public class TextFitter {
    public static final Pattern DEFAULT_END_PUNCTUATION = Pattern.compile("[.,;:…\"'!%?]? +[^\\w]*$");
    private Pattern endWordPattern;
    private float lineSpacingMultiplier;
    private float lineAdditionalVerticalPadding;

    //Limiting Parameters
    private int maxLines;
    private int maxHeight;
    private int width;
    private TextPaint paint;


    public TextFitter() {
        reset();
    }

    public void reset() {
        this.endWordPattern = DEFAULT_END_PUNCTUATION;

        lineSpacingMultiplier = 1.0f;
        lineAdditionalVerticalPadding = 0.0f;

        maxLines = Integer.MAX_VALUE;
        maxHeight = width = -1;
        paint = null;
    }

    public void setEndWordPattern(Pattern endWordPattern) {
        this.endWordPattern = endWordPattern;
    }

    public void setDisplayParametersMeasured(int maxHeight, int width, float lineSpacingMultiplier, float lineAdditionalVerticalPadding){
        this.maxHeight = maxHeight;
        this.width = width;
        this.lineAdditionalVerticalPadding = lineAdditionalVerticalPadding;
        this.lineSpacingMultiplier = lineSpacingMultiplier;
    }

    public void setDisplayParametersMeasured(int maxHeight, int width){
        this.maxHeight = maxHeight;
        this.width = width;
    }

    public void setDisplayParametersLines(int maxLines, int width, float lineSpacingMultiplier, float lineAdditionalVerticalPadding){
        this.maxLines = maxLines;
        this.width = width;
        this.lineAdditionalVerticalPadding = lineAdditionalVerticalPadding;
        this.lineSpacingMultiplier = lineSpacingMultiplier;
    }

    public void setLineSpacingMultiplier(float lineSpacingMultiplier) {
        this.lineSpacingMultiplier = lineSpacingMultiplier;
    }

    public void setLineAdditionalVerticalPadding(float lineAdditionalVerticalPadding) {
        this.lineAdditionalVerticalPadding = lineAdditionalVerticalPadding;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public TextPaint getPaint() {
        return paint;
    }

    public void setPaint(TextPaint paint) {
        this.paint = paint;
    }

    public int getFittedLength(CharSequence fullText, CharSequence ellipsis) {
        SpannableStringBuilder workingText = new SpannableStringBuilder(fullText);

        final int ellipsisLength = ellipsis != null ? ellipsis.length() : 0;
        Layout layout = createWorkingLayout(workingText);
        int linesCount = getLinesCount(layout);
        if(linesCount <= 0){
            return 0;
        } else if (layout.getLineCount() > linesCount) {
            // We have more lines of text than we are allowed to display
            final int start = layout.getLineEnd(linesCount - 1) - ellipsisLength;
            if(start > 0){
                workingText.delete(start, workingText.length());

                //Truncate to the last space (or other defined end of a word)
                final Matcher endWordMatcher = endWordPattern.matcher(workingText);
                if(endWordMatcher.find()){
                    final int lastWordBreakBegining = endWordMatcher.start();
                    final int oldWorkingTextLength = workingText.length();

                    if(ellipsis != null) workingText.append(ellipsis);
                    if(createWorkingLayout(workingText).getLineCount() > linesCount)
                        return lastWordBreakBegining;
                    else return oldWorkingTextLength;
                } else {
                    //If this doesn't match we probably have an empty string so we will just return it's length to be safe
                    return workingText.length();

                }
            } else {
                // we can't fit any of it, just return -1.  This shouldn't happen because
                // in this case it should be caught by a linesCount of 0 but we will be safe
                return 0;
            }
        } else { //The entire text will fit, just return the last char position
            return workingText.length();
        }

    }

    /**
     * Get how many lines of text we are allowed to display.
     */
    private int getLinesCount(Layout layout) {
        if (maxLines == Integer.MAX_VALUE) {
            int fullyVisibleLinesCount = getVisibleLinesCount(layout);
            if (fullyVisibleLinesCount == -1) {
                return 1;
            } else {
                return fullyVisibleLinesCount;
            }
        } else {
            return maxLines;
        }
    }

    /**
     * Get how many lines of text we can display so their full height is visible.
     * @param layout
     */
    private int getVisibleLinesCount(Layout layout) {
        int lines = 0;
        final int lastLine = layout.getLineCount();
        while(lines < lastLine && layout.getLineTop(lines) <= maxHeight) lines++;
        return lines;
    }

    private Layout createWorkingLayout(CharSequence workingText) {


        return new StaticLayout(workingText, getPaint(),
                width,
                Layout.Alignment.ALIGN_NORMAL, lineSpacingMultiplier,
                lineAdditionalVerticalPadding, true /* includepad */);
    }
}
