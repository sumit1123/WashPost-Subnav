// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.washingtonpost.android.paywall.helper

fun Any?.componentTextToString(): String? {
    return this as? String
}

fun Any?.componentTextToListOfStrings(): List<String>? {
    return (this as? List<*>)?.mapNotNull { it as? String }
}