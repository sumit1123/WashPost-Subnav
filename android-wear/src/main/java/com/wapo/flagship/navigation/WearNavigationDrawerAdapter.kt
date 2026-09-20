/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.navigation

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.wear.widget.drawer.WearableNavigationDrawerView
import com.wapo.flagship.WearAppContext
import com.wapo.flagship.models.MenuAction
import com.washingtonpost.android.R

/**
 *  Unused in new wear app. Reserve just in case.
 *
 */
class WearNavigationDrawerAdapter(private val context: Context) :
    WearableNavigationDrawerView.WearableNavigationDrawerAdapter() {

    override fun getCount(): Int = 4

    override fun getItemText(pos: Int): String {
        return when (MenuAction.fromInt(pos)) {
            MenuAction.ACTION_TOP_STORIES -> context.resources.getString(R.string.section_top_stories)
            MenuAction.ACTION_ALERTS -> context.resources.getString(R.string.section_alerts)
            MenuAction.ACTION_EXTRA_SECTION -> ""//WearAppContext.extraSection ?: ""
            MenuAction.ACTION_SETTINGS -> context.resources.getString(R.string.section_settings)
        }
    }

    override fun getItemDrawable(pos: Int): Drawable? {
        return when (MenuAction.fromInt(pos)) {
            MenuAction.ACTION_TOP_STORIES -> ContextCompat.getDrawable(
                context, R.drawable.icon_latestnews_white
            )
            MenuAction.ACTION_ALERTS -> ContextCompat.getDrawable(
                context, R.drawable.icon_alerts_white
            )
            MenuAction.ACTION_EXTRA_SECTION -> ContextCompat.getDrawable(
                context, R.drawable.image_placeholder
            )
            MenuAction.ACTION_SETTINGS -> ContextCompat.getDrawable(
                context, R.drawable.icon_settings
            )
        }
    }

}