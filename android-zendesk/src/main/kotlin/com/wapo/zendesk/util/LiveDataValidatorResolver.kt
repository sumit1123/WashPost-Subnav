/* Copyright (c) 2021 The Washington Post. All rights reserved. */

package com.wapo.zendesk.util

//LiveDataValidatorResolver takes in a list of LiveDataValidator objects and returns true if each one is valid.
class LiveDataValidatorResolver(private val validators: List<LiveDataValidator>) {
    fun isValid(): Boolean {
        for (validator in validators) {
            if (!validator.isValid()) return false
        }
        return true
    }
}