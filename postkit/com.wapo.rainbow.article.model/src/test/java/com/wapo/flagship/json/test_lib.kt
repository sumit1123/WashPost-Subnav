package com.wapo.flagship.json

inline fun <reified T> T.resourceToString(name: String): String? {
    return T::class.java.classLoader?.getResourceAsStream(name)
            ?.bufferedReader()
            .use { it?.readText() }
}