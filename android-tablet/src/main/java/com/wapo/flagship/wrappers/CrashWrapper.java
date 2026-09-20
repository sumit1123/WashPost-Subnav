/*
 * Copyright (C) 2014 Washington Post Android Application
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wapo.flagship.wrappers;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import rx.Subscriber;

public class CrashWrapper {
    public static void init() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
    }

    public static void logExtras(String str) {
        FirebaseCrashlytics.getInstance().log(str);
    }

    public static void sendException(Throwable t) {
        FirebaseCrashlytics.getInstance().recordException(t);
    }

    public static void setUserIdentifier(String name) {
        FirebaseCrashlytics.getInstance().setUserId("User: " + name);
    }

    public static class CrashWrapperSubscriber<T> extends Subscriber<T> {
        @Override
        public void onCompleted() { }

        @Override
        public void onError(Throwable throwable) {
            CrashWrapper.sendException(throwable);
        }

        @Override
        public void onNext(T t) { }
    }
}