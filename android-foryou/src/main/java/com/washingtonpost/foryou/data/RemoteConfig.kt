package com.washingtonpost.foryou.data

data class RemoteConfig(
    val ttls: Long,
    val checkReadList: Boolean,
    val maxSize: Int,
    val pageSize: Int? = null
)
