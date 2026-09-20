package com.wapo.kmpshared.util

expect class KMPDate()

expect fun createKMPDateFromISO8601(isoString: String): KMPDate?
