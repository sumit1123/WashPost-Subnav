/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils

import java.io.BufferedInputStream
import java.io.InputStream
import java.util.*

object Utils {

    fun inputStreamToString(inputStream: InputStream): String {
        val stream =
            if (BufferedInputStream::class.java.isInstance(inputStream)) inputStream
            else BufferedInputStream(inputStream)
        val scanner = Scanner(stream).useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }

}