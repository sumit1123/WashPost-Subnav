/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.base

interface Cacheable {
    fun getTimeToLive() : Long
    fun lastUpdated() : Long
}