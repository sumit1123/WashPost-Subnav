package com.wapo.flagship.features.onboarding

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Config is pulled from raw json file.
 */
class OnboardingConfig : Serializable {
    @SerializedName(value = "enabled")
    val enabled: Boolean? = null

    @SerializedName(value = "id")
    val id: String? = null

    @SerializedName(value = "screens")
    var screens: List<Screen>? = null

    /**
     * This fun filters out screens that should not be shown for Sub or Migrated users. Following are the cases
     * Migrated / Sub -> No First Install Onboarding screen is shown (Won't hit this function)
     * Sub / Not Migrated -> Only First screen of First Install OB is shown (handled below)
     * No Sub / Migrated -> Only Second screen of First install OB is shown (handled below)
     * No Sub / Not Migrated -> Both screens are shown (handled below)
     */
    fun setFilteredScreens(
        isSubscriber: Boolean,
        isMigratedUser: Boolean,
    ) {
        screens =
            when {
                isSubscriber -> screens?.filter { !it.onlyForNonSubscriber }
                isMigratedUser -> screens?.filter { it.onlyForMigratedUser }
                else -> screens
            }
    }
}

/**
 * Screen model for First Install Onboarding Dialog.
 * [onlyForMigratedUser] - flag to only show for migrated user
 * [onlyForNonSubscriber] - flag ot only show for Non Subscriber
 * [title] [description] - text content on specific screen
 * [command1] [command2] [command3] - Three buttons at bottom that will be configured based on screen
 */
class Screen : Serializable {
    @SerializedName(value = "onlyForMigratedUser")
    val onlyForMigratedUser: Boolean = true

    @SerializedName(value = "onlyForNonSubscriber")
    val onlyForNonSubscriber: Boolean = false

    @SerializedName(value = "title")
    val title: String? = null

    @SerializedName(value = "description")
    val description: String? = null

    @SerializedName(value = "command1")
    val command1: Command? = null

    @SerializedName(value = "command2")
    val command2: Command? = null

    @SerializedName(value = "command3")
    val command3: Command? = null

    @SerializedName(value = "resubscribe")
    val resubscribe: Command? = null

    @SerializedName(value = "contexts")
    val contexts: Map<String, Screen>? = null

    @SerializedName(value = "deviceImage")
    val deviceImage: String? = null

    @SerializedName(value = "magnifierImage")
    val magnifierImage: String? = null

    @SerializedName(value = "magnifierImageBias")
    val magnifierImageBias: Array<Float>? = null

    @SerializedName(value = "backgroundImage")
    val backgroundImage: String? = null
}

/**
 * Holds data for various buttons in Onboarding Screen
 * [text] [color] [style] - text to be displayed on button and button style
 * [loggedInText] - text to be displayed if user is logged in
 * [action] - Action to be taken when button is clicked (Subscribe, Sign In, etc.)
 */
class Command : Serializable {
    @SerializedName(value = "text")
    var text: String? = null

    @SerializedName(value = "loggedInText")
    var loggedInText: String? = null

    @SerializedName(value = "subscriberText")
    var subscriberText: String? = null

    @SerializedName(value = "action")
    val action: String? = null

    @SerializedName(value = "style")
    val style: String? = null

    @SerializedName(value = "color")
    val color: String? = null
}

enum class ActionType {
    NEXT,
    SKIP,
    SUBSCRIBE,
    LOGIN,
}

enum class CommandStyle {
    BUTTON,
    TEXT,
}

enum class CommandColor {
    GREY,
    WHITE,
}
