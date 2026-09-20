/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.homepage.models


open class HomepageItem(
    open val type: String
) {
    override fun equals(other: Any?): Boolean {
        return if (other as? HomepageItem == null) {
            false
        } else {
            other == this
        }
    }

    override fun hashCode(): Int {
        var result = this.hashCode()
        result *= 31
        return result
    }
}