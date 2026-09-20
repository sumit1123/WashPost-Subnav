package com.wapo.flagship.features.sections;

import androidx.annotation.NonNull;

public interface PageManagerProvider {
    @NonNull
    rx.Observable<? extends PageManager> getPageManager();
}
