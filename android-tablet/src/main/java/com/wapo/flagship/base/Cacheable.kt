package com.wapo.flagship.base

interface Cacheable {
    fun getTimeToLive(): Long

    fun lastUpdated(): Long
}
