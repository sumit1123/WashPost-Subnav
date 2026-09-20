package com.wapo.flagship.sync;

import com.wapo.flagship.services.data.ITaskStatusListener;

public interface ProgressTaskListener extends ITaskStatusListener {
    void onProgress(int progress);
}
