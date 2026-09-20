package com.wapo.view.tooltip

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.preference.PreferenceManager
import android.text.SpannableString
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import java.lang.ref.WeakReference

class TooltipPopupManager(val appContext: Context) {
    private val TAG: String = TooltipPopupManager::class.java.simpleName
    var context: WeakReference<Context>? = null
    /**
     * Member variable is not required in most cases. However, whenever we want to explicitly dismiss the tooltip (without outside touch),
     * we need this reference to call [dismissTooltip].
     * The above stated scenario occurs when we need to dismiss the tooltip before showing Pay-wall.
     */
    var tooltip: TooltipPopup? = null
    private var tooltipListener: TooltipListener? = null

    companion object Instance {
        private var tooltipPopupManager: TooltipPopupManager? = null
        fun get(context: Context): TooltipPopupManager {
            if (tooltipPopupManager == null) {
                tooltipPopupManager = TooltipPopupManager(context.applicationContext)
            }
            if (context is Activity) {
                tooltipPopupManager?.context = WeakReference(context)
            }
            return tooltipPopupManager as TooltipPopupManager
        }

        /**
         * Force dismisses the tooltip and clears the reference for the given context.
         * This should be called from Activity.onDestroy() to prevent memory leaks.
         * [callerContext] is the context from which cleanup is being called.
         * Cleanup only occurs if the tooltip belongs to this activity context.
         */
        fun forceCleanup(callerContext: Context) {
            tooltipPopupManager?.let { manager ->
                if (manager.tooltip?.activityContext?.get() == callerContext) {
                    // Only cleanup if the tooltip belongs to this activity context
                    if (manager.tooltip?.isShowing() == true) {
                        manager.tooltip?.dismissTooltip()
                    }
                    manager.tooltip = null
                    if (manager.context?.get() == callerContext) {
                        manager.context = null
                        tooltipPopupManager = null
                    }
                }
            }
        }

        /**
         * Returns the string with some portion bold.
         * [context] is needed for drawing resources.
         * [tooltipTextResId] res id of entire string that should be shown on tooltip.
         * [boldPortionResId] res id of the portion/substring that needs to be bold.
         */
        fun getSpannableStringWithBoldText(context: Context, @StringRes tooltipTextResId: Int, @StringRes boldPortionResId: Int): SpannableString {
            val tooltipText = context.resources.getString(tooltipTextResId)
            val tooltipBoldPortion = context.resources.getString(boldPortionResId)
            return SpannableString(tooltipText).also {
                it.setSpan(StyleSpan(Typeface.BOLD),
                    tooltipText.indexOf(tooltipBoldPortion),
                    tooltipText.indexOf(tooltipBoldPortion) + tooltipBoldPortion.length,
                    0
                )
            }
        }
    }

    /**
     * This function is called to show the tooltip. [tooltipProperties] has all the required properties to show the tooltip.
     * Following conditions are checked before showing -
     * if tooltip is shown before - [isTooltipShown]
     * if ANY tooltip is currently showing - [isTooltipShowing]
     * if this tooltip has been prioritized over other - [hasPriority]
     */
    fun showToolTip(tooltipProperties: TooltipProperties): Boolean {
        if (isTooltipShown(tooltipProperties.tooltipData.sharedPrefKey)) {
            if (hasPriority(tooltipProperties.tooltipData)) {
                // Need to log error as an already shown tooltip should not have priority.
                updatePriority(tooltipProperties)
                context?.get()?.let {
                    //RemoteLog.nonFatalError(RemoteLog.NonFatalErrors.InvalidState, TAG, "showToolTip", it)
                }
            }
            return false
        }
        if (!isTooltipShowing() && hasPriority(tooltipProperties.tooltipData)) {
            context?.get()?.let {
                tooltip = TooltipPopup(tooltipProperties, it, tooltipListener).also { toolTip ->
                    toolTip.show()
                }
                setTooltipShown(tooltipProperties.tooltipData.sharedPrefKey)
                return true
            }
        }
        return false
    }

    /**
     * Update priority of given tooltip
     */
    fun updatePriority(tooltipProperties: TooltipProperties) {
        //Increment the priority by 1 to enable showing next tooltip (In next session)
        context?.get()?.let {
            val priority = tooltipProperties.tooltipData.priorityData
            get(it).setTooltipPriority(
                priority.screenType.screenTypeTooltipPriorityPrefKey,
                priority.priority + 1
            )
        }
    }

    /**
     * This function is called to show the tooltip.
     * [tooltipProperties] has all the required properties to show the tooltip.
     * [showWithoutChecks] flag to show the tooltip without any preference and priority conditions. false by default.
     */
    fun showToolTip(tooltipProperties: TooltipProperties, showWithoutChecks: Boolean = false): Boolean {
        return if (showWithoutChecks) {
            if (isTooltipShowing()) {
                dismissTooltip()
            }
            context?.get()?.let {
                tooltip = TooltipPopup(tooltipProperties, it, tooltipListener).also { it.show() }
            }
            true
        } else {
            showToolTip(tooltipProperties)
        }
    }

    /**
     * Returns true if the tooltip is currently being shown, false otherwise
     */
    private fun isTooltipShowing() = tooltip?.isShowing() ?: false

    /**
     * Determines whether or not this specific tooltip is prioritized over other ones.
     * e.g. TTS has a priority over follow tooltip hence it should be shown first (but not in the same session)
     * [tooltipData] is an instance of an [TooltipData] which is used to determine if
     * it has the priority over any other [TooltipData]
     */
    private fun hasPriority(tooltipData: TooltipData): Boolean  =
        getTooltipPriority(tooltipData.priorityData.screenType.screenTypeTooltipPriorityPrefKey) >= tooltipData.priorityData.priority


    /**
     * Dismisses the tooltip if it is currently showing and context is same as the caller context.
     */
    fun dismissTooltip() {
        if (isTooltipShowing() && tooltip?.activityContext?.get() == context?.get()) {
            tooltip?.dismissTooltip()
            tooltip = null
        }
    }

    /**
     * Checks if this tooltip has been shown in the past. [prefKey] is the key used to check fot shared preferences.
     */
    fun isTooltipShown(prefKey:String): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        return sharedPreferences.getBoolean(prefKey, false)
    }

    /**
     * Sets the tooltip shown flag to true so that next time this tooltip with [prefKey] is not shown.
     */
    fun setTooltipShown(prefKey: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        val editor = sharedPreferences.edit()
        editor.putBoolean(prefKey, true)
        editor.apply()
    }

    /**
     * Sets the priority number for the next tooltip in the article activity
     */
    fun setTooltipPriority(tooltipPriorityPrefKey: String, priority: Int){
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        val editor = sharedPreferences.edit()
        editor.putInt(tooltipPriorityPrefKey, priority)
        editor.apply()
    }

    /**
     * Gets the priority number for the next tooltip in the article activity
     */
    private fun getTooltipPriority(tooltipPriorityPrefKey: String): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        return sharedPreferences.getInt(tooltipPriorityPrefKey, 0)
    }

    fun setTooltipListener(tooltipListener: TooltipListener?) {
        this.tooltipListener = tooltipListener
    }
}