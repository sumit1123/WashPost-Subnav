package com.wapo.flagship.sync;

public interface UpdateSfTaskListener extends ProgressTaskListener {
    void onSectionComplete(String section);
}
