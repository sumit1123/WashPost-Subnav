package com.wapo.zendesk.repository

import java.io.Serializable

sealed class Result : Serializable {
    object Success : Result()
    class Error(val message: String) : Result()
}
