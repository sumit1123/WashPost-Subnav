package com.wapo.view;

import android.text.method.MovementMethod;

public interface ITextView {
    void setLineSpacing(float add, float mult);

    void setMovementMethod(MovementMethod movementMethod);

    void setText(CharSequence text);

    void setKey(String key);

    void setTextColor(int textColor);
}