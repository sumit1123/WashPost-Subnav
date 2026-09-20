package com.wapo.flagship.features.fusion

import android.content.Context
import com.wapo.flagship.features.grid.BarEntity
import java.util.concurrent.TimeUnit

/**
 * A class that tracks closed breaking bars so that we don't show them again
 * The data is saved in shared preferences
 */
class BreakingNewsTracker(
    context: Context,
) {
    private val context: Context = context.applicationContext

    init {
        cleanUp(context)
    }

    private fun cleanUp(context: Context) {
        val now = System.currentTimeMillis()
        val oldTimeStamps =
            context
                .getSharedPreferences(PREF_FUSION_POPUP_TRACKER, Context.MODE_PRIVATE)
                .all
                .keys
                .map {
                    try {
                        it.toLong()
                    } catch (t: Throwable) {
                        0L
                    }
                }.filter { now - it > TimeUnit.DAYS.toMillis(2) }

        val editor = context.getSharedPreferences(PREF_FUSION_POPUP_TRACKER, Context.MODE_PRIVATE).edit()
        oldTimeStamps.forEach { editor.remove(it.toString()) }
        editor.apply()
    }

    fun isBarClosed(barEntity: BarEntity): Boolean {
        val id = getId(barEntity)
        return if (id.isNullOrEmpty()) {
            true
        } else {
            context
                .getSharedPreferences(PREF_FUSION_POPUP_TRACKER, Context.MODE_PRIVATE)
                .all
                .containsValue(id)
        }
    }

    fun setBarClosed(barEntity: BarEntity) {
        val id = getId(barEntity)
        if (!id.isNullOrEmpty()) {
            val sharedPreferences =
                context.getSharedPreferences(
                    PREF_FUSION_POPUP_TRACKER,
                    Context.MODE_PRIVATE,
                )
            sharedPreferences
                .edit()
                .putString(System.currentTimeMillis().toString(), id)
                .apply()
        }
    }

    private fun getId(barEntity: BarEntity): String? =
        if (barEntity.getTimestampMs() > 0) {
            barEntity.getTimestampMs().toString()
        } else {
            barEntity.getText()?.toString()
        }
}

private const val PREF_FUSION_POPUP_TRACKER = "PREF_FUSION_POPUP_TRACKER"
