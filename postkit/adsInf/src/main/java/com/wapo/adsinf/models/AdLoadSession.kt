package com.wapo.adsinf.models

import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderConfig
import java.util.UUID

data class AdLoadSession(
    val id: String = UUID.randomUUID().toString(),
    val adLoadChain: List<AdLoaderConfig>,
) {
    private val _attempts = mutableListOf<AdLoadContext>()
    val attempts: List<AdLoadContext> = _attempts

    fun createAttempt() {
        _attempts.add(AdLoadContext())
    }

    fun updateAttempt(
        index: Int,
        update: (AdLoadContext) -> AdLoadContext
    ): AdLoadContext {
        require(index in _attempts.indices) {
            "updateAttempt() requires index to reference an existing attempt; call createAttempt() first and pass an index in ${_attempts.indices}."
        }
        val updated = update(_attempts[index])
        _attempts[index] = updated
        return updated
    }

    fun updateLastAttempt(
        update: (AdLoadContext) -> AdLoadContext
    ): AdLoadContext {
        return updateAttempt(attempts.lastIndex, update)
    }

    override fun toString(): String {
        return "AdLoadSession(id=$id, attempts=${attempts.joinToString()})"
    }
}