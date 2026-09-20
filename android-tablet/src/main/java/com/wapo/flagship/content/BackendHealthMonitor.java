/* Copyright (c) 2018 The Washington Post. All rights reserved. */
package com.wapo.flagship.content;

import androidx.annotation.NonNull;

import rx.Observable;

/**
 * @author Created by Jayesh Elamgodil on 09/26/2018
 */
public interface BackendHealthMonitor<T> {

    @NonNull
    Observable<T> getBackendHealthObservable();

    void checkBackendHealth();

}
