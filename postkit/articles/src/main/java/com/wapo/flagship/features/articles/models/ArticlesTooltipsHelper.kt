package com.wapo.flagship.features.articles.models

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager.getDefaultSharedPreferences

/**
 * This object os used to set and reset different shared preferences that are helpers for making a decision whether or not to show the tooltip
 * in one single instance of an article activity.
 */
object ArticlesTooltipsHelper {

    private const val ARTICLES_TOOL_TIP_JUST_CLOSED = "ARTICLES_TOOL_TIP_JUST_CLOSED"
    private const val ARTICLES_TOOL_TIP_COUNTER = "ARTICLES_TOOL_TIP_COUNTER"

    /**
     * If tooltip is previously shown in current instance of article actiity.
     */
    fun isTooltipShownInCurrentInstanceOfArticleActivity(context: Context): Boolean {
        val sharedPreferences: SharedPreferences = getDefaultSharedPreferences(context)
        return sharedPreferences.getBoolean(ARTICLES_TOOL_TIP_JUST_CLOSED, false)
    }

    /**
     * Reset the flag so next instance can show the tooltip.
     */
    fun resetTooltipShownInCurrentArticleActivityInstance(context: Context) {
        val sharedPreferences: SharedPreferences = getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putBoolean(ARTICLES_TOOL_TIP_JUST_CLOSED, false)
        editor.apply()
    }

    /**
     * Set the flag so no additional tooltips are shown in the current instance of articles activity.
     */
    fun setTooltipShownInCurrentArticleActivityInstance(context: Context) {
        val sharedPreferences: SharedPreferences = getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putBoolean(ARTICLES_TOOL_TIP_JUST_CLOSED, true)
        editor.apply()
    }
}