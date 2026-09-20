package com.wapo.flagship.features.sections;

import com.wapo.flagship.features.sections.model.Section;

import java.util.List;

import rx.Observable;

public interface PageManager {
    Observable<List<Section>> getPages(String pageName);
    Observable<PageLayout> listenToPage(final String pageName, boolean forceRefresh);
    Observable<PageLayout> updatePage(final String pageName, final boolean forceUpdate);
    void onPageStop(String pageName);
    boolean shouldClearPage(String pageName);
    boolean shouldUpdatePage(String pageName);
    void onLowDataMode(Boolean enable);

    abstract class FourFifteenException extends Exception {
        abstract public String getContentUrl();
    }
}
