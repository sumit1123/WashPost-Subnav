/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.sections.domein

interface SectionRibbonRepo {

    fun getLastViewed(): String?

    fun setLastViewed(value: String?)

    fun removeLastViewed()

    fun setOpenedOnKey(key: String)

    fun removeOpenedOnKey()
}
