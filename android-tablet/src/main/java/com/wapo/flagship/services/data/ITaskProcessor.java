package com.wapo.flagship.services.data;

import android.content.Context;
import com.wapo.flagship.data.CacheManager;
import com.washingtonpost.android.config.domain.models.config.Config;

public interface ITaskProcessor {
    boolean shouldUseNetwork();

    /**
     * Add task into the processing queue
     * @param task
     */
    Task enqueueTask(Task task);

    Context getContext();

    Config getConfig();

    CacheManager getCacheManager();
}
