package com.wapo.flagship.features.notification;

import androidx.annotation.NonNull;

public interface AlertManagerProvider {
    @NonNull
    rx.Observable<? extends AlertManager> getAlertManager();
}
