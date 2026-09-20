package com.wapo.flagship.features.grid.views.vote

import rx.Observable

interface VoteGuideService {
    fun getVoteGuide() : Observable<VoteGuide>
}