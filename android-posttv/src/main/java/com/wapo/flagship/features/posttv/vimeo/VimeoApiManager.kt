package com.wapo.flagship.features.posttv.vimeo

import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import java.io.IOException

/*
The MIT License (MIT)
Copyright (c) 2016 Ed George
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE
.*/

/**
 * An API and request manager for Vimeo's web service.
 */
internal class VimeoApiManager {
    /**
     * Builds an HTTP call to Vimeo, from an identifier, to extract video information
     * @param identifier Vimeo video identifier
     * @param referrer Video referrer, null if none present
     * @return an OKHttp3 Call to use asynchronously or otherwise.
     * @throws IOException If a connection or other error occurs
     */
    @Throws(IOException::class)
    fun extractWithIdentifier(identifier: String, referrer: String?): okhttp3.Call {
        var referrerBaseURl = referrer
        val url: String =
            String.format(VIMEO_CONFIG_URL, identifier)
        if (VimeoUtils.isEmpty(referrer)) {
            //If no referrer exists, generate from base URL
            referrerBaseURl = String.format(VIMEO_URL, identifier)
        }
        val client: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder().apply {
            addInterceptor(DefaultHeadersInterceptor())
        }.build()
        val request: okhttp3.Request = okhttp3.Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Referer", referrerBaseURl!!)
            .build()
        return client.newCall(request)
    }

    /**
     * Generates an appropriate error for a given response
     * @param response The response that was not successful
     * @return An Exception based on the HTTP Status code of the response
     */
    fun getError(response: okhttp3.Response): Throwable {
        return when (response.code) {
            404 -> IOException("Video could not be found")
            403 -> IOException("Video has restricted playback")
            else -> IOException("An unknown error occurred")
        }
    }

    companion object {
        //Base URL for Vimeo videos
        private const val VIMEO_URL = "https://vimeo.com/%s"

        //Config URL containing video information
        private const val VIMEO_CONFIG_URL = "https://player.vimeo.com/video/%s/config"
    }
}
