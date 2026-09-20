package com.washingtonpost.android.config.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import rx.Observable
import rx.Subscriber
import rx.subscriptions.Subscriptions

@OptIn(DelicateCoroutinesApi::class)
fun <T> Flow<T>.asObservable(
    scope: CoroutineScope = GlobalScope,
): Observable<T> {
    return Observable.create { subscriber: Subscriber<in T> ->
        val job = scope.launch {
            try {
                collect { value ->
                    if (!subscriber.isUnsubscribed) {
                        subscriber.onNext(value)
                    }
                }
                if (!subscriber.isUnsubscribed) {
                    subscriber.onCompleted()
                }
            } catch (e: Throwable) {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onError(e)
                }
            }
        }

        subscriber.add(Subscriptions.create { job.cancel() })
    }
}