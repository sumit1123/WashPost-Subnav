package com.washingtonpost.android.paywall.reminder.state

/**
 * Enum to identify dialogs with associating them to their Fragment instance type
 */
enum class DialogType {
    /**
     * Associated to ReminderScreenFragment
     */
    REMINDER,

    /**
     * Associated to AcquisitionReminderFragment
     */
    ACQUISITION,
    /**
     * Associated to Onboarding2Activity
     */
    ONBOARDING,

    /**
     * Default case for Unknown types
     */
    UNKNOWN
}