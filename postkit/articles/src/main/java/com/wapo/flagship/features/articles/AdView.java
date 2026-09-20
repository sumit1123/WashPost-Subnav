package com.wapo.flagship.features.articles;

import android.view.View;

public interface AdView {
    void bind(AdViewInfo item);

    void unbind();

    View getView();
}
