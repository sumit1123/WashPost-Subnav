@file:JvmName("RxUtils")

package com.wapo.flagship.util.rx

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.wapo.flagship.model.Status
import rx.Observable

/**
 * Turns [LiveData] into a single-event observable
 * @param liveDataCreator a function that creates [LiveData] when somebody subscribes to the observable
 */
fun <T> observeLiveData(liveDataCreator: () -> LiveData<T>): Observable<T> =
    Observable.create { subscriber ->
        subscriber.onStart()
        val liveData = liveDataCreator()
        liveData.observeForever(
            object : Observer<T> {
                override fun onChanged(t: T) {
                    liveData.removeObserver(this)
                    subscriber.onNext(t)
                    subscriber.onCompleted()
                }
            },
        )
    }

fun <T> Status<out T>.flatten(): Observable<T> =
    when (this) {
        is Status.Network -> Observable.just(data)
        is Status.Cache -> Observable.just(data)
        is Status.Error -> Observable.error(RuntimeException(message))
        is Status.Error415 -> Observable.error(RuntimeException(article415?.message))
    }
