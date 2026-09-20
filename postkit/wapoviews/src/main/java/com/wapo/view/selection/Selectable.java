package com.wapo.view.selection;

import android.graphics.Rect;

public interface Selectable {

    int LAST_SYMBOL = -2;
    int FIRST_SYMBOL = -3;

    /**
     * @param x view's based, scale doesn't
     * @param y view's based, no scale
     * @return
     */
    int getOffsetForPosition(int x, int y);
    int getVisibility();
    CharSequence getText();
    void setText(CharSequence text);
    /**
     * @param rect, a rect to fill
     * @return @rect if it's not null, a new @Rect instance otherwise
     */
    Rect getScreenRect(Rect rect);
    int getHeight();
    int getWidth();
    float[] getScreenPositionForOffset(int offset, float[] position);
    void selectText(int start, int end);
    CharSequence getSelectedText();
    void setColor(int selectionColor);
    String getKey();
    void setKey(String key);
}
