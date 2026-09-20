package com.wapo.flagship.content

import rx.Observable

interface ContentActivity {
    fun getContentManagerObs(): Observable<ContentManager>
}
