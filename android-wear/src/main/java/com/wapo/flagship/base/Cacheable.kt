/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.base

interface Cacheable {
    fun getTimeToLive() : Long
    fun lastUpdated() : Long
}