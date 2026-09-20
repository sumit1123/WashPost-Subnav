/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.querypolicies

import com.wapo.flagship.features.articles2.models.Article2

/**
 * A policy that will request new data from the network if
 * the cache is missing or
 * the specified [lmt] is newer than the cached lmt
 */
class LMTQueryPolicy(lmt: Long?) :
    DefaultQueryPolicy<Article2>(true) {

    private val lmt : Long = lmt ?: 0L

    override fun needUpdate(t: Article2?): Boolean {
        super.needUpdate(t)
        return t == null || this.lmt > (t.updatedAt ?: 0L)
    }

}