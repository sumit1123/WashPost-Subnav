package com.washingtonpost.android.wapocontent;

import rx.Observable;

public interface ILoader {
    Observable<ImageResponse> getImage(ImageRequestData imageRequestData);
}
