package com.wapo.flagship.features.notification;

import com.wapo.flagship.content.notifications.NotificationData;
import com.washingtonpost.android.volley.RequestQueue;
import com.washingtonpost.android.volley.toolbox.ImageLoader;

import rx.Observable;

public interface NotificationBuilderProvider {

    Observable<NotificationData> getArticleDataHolder(String storyUrl);

    ImageLoader getImageLoader();

    Class<?> getMainActivity();

    Class<?> getReadArticleReceiver();

    Class<?> getSaveArticleReceiver();

    int getReadActionButtonResId();

    int getSaveActionButtonResId();

    int getPushIcon();

    int getLargePushIcon();

    RequestQueue getRequestQueue();
}
