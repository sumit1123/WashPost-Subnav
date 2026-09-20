package com.wapo.flagship.services.data;

public interface ITaskStatusListener {
    void onTaskStatusChanged(int status);
    void onTaskError(Throwable e);
}
