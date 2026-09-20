package com.wapo.flagship;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OrientationLock {
    int size();
    int orientation();
}
