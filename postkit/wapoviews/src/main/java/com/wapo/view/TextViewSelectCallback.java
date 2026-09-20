package com.wapo.view;

import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;

public class TextViewSelectCallback extends AppCompatTextView {
    private SelectedCallback callback;

    public TextViewSelectCallback(Context context) {
        super(context);
    }

    public TextViewSelectCallback(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextViewSelectCallback(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void setSelected(boolean selected) {
        if (callback != null) {
            callback.onSelected(selected);
        }
        super.setSelected(selected);
    }

    public void setSelectedCallback(SelectedCallback callback) {
        this.callback = callback;
    }

    public SelectedCallback getSelectedCallback() {
        return callback;
    }

    public interface SelectedCallback {
        void onSelected(boolean selected);
    }
}
