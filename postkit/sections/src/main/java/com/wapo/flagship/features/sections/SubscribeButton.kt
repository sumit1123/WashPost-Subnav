package com.wapo.flagship.features.sections

import rx.Observable

interface SubscribeButton {
    fun isEnabled(): Boolean
    fun getTextObservable(): Observable<CharSequence>?
}