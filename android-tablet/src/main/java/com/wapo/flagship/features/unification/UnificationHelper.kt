package com.wapo.flagship.features.unification

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import com.wapo.flagship.Utils
import com.wapo.flagship.features.amazonunification.MigrationHelper
import com.wapo.flagship.features.amazonunification.activity.AmazonUnifiedOnboardingActivity
import com.wapo.flagship.features.amazonunification.activity.DuplicateSubscriptionActivity
import com.wapo.flagship.features.amazonunification.activity.UnificationAmazonOnboardingActivity
import com.wapo.flagship.features.deeplinks.AirshipAttributes
import com.wapo.flagship.features.unification.activity.UnificationOnboardingActivity
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService
import java.util.Locale

object UnificationHelper {
    /**
     * Handles unified app tasks when the app opens
     * 1. Migrate Saved stories
     * 2. Delete Rainbow DB, local files, shared prefs
     * 3. Update Rainbow Migrated Airship Attribute
     */
    fun handleUnifiedAppTasks(context: Context) {
        MigrationHelper.migrateRainbowSavedStoriesIfNotDoneYet()
        MigrationHelper.deleteRainbowLocalFiles()
        MigrationHelper.deleteRainbowSharedPrefs()
        // Update Rainbow Migrated Attribute only when upgraded from the Play Store Rainbow app.
        if (Utils.isProductFlavorPlayStore() &&
            PrefUtils.getHasMigratedFromRainbow(context).lowercase(Locale.US) == "true"
        ) {
            AirshipAttributes.updateRainbowMigratedAttribute()
        }
    }

    fun determineNextScreen(context: Context) {
        when {
            shouldShowUnificationOnboardingScreen(context) -> startUnificationOnboarding(context)
            shouldShowAmazonUnificationOnboardingScreen(context) ->
                startAmazonUnificationOnboarding(
                    context,
                )
            shouldShowAmazonUnifiedOnboardingScreen(context) ->
                startAmazonUnifiedOnboarding(
                    context,
                )
            shouldShowAmazonDuplicateSubscriptionsScreen(context) ->
                startAmazonDuplicateSubscriptionsActivity(
                    context,
                )
            else -> {}
        }
    }

    /**
     * Whether or not to show the onboarding screen to a user who migrated from rainbow.
     * This is based on two conditions -
     * 1. User has rainbow app - This is tracked from rainbow app using content providers.
     * 2. User has not seen / acted on the unification onboarding screen.
     */
    private fun shouldShowUnificationOnboardingScreen(context: Context): Boolean =
        PrefUtils.getHasMigratedFromRainbow(context).lowercase(Locale.US) == "true" &&
            !PrefUtils.hasUserActedOnUnificationOnboardingScreen(context) &&
            !Utils.isProductFlavorAmazon()

    /**
     * This shows the onboarding screen for a migrated user.
     * check [shouldShowUnificationOnboardingScreen] to see when is this screen shown.
     */
    private fun startUnificationOnboarding(context: Context) {
        val intent = Intent(context, UnificationOnboardingActivity::class.java)
        startActivity(context, intent, null)
    }

    /**
     * Whether or not to show the onboarding screen to an Amazon user who migrated from rainbow.
     * This is based on two conditions -
     * 1. User has rainbow app - This is tracked from rainbow app using content providers.
     * 2. User has not seen / acted on the unification onboarding screen.
     */
    private fun shouldShowAmazonUnificationOnboardingScreen(context: Context): Boolean =
        PrefUtils.getIsExistingUser(context) &&
            !PrefUtils.hasUserActedOnUnificationOnboardingScreen(context) &&
            Utils.isProductFlavorAmazon()

    /**
     * This shows the onboarding screen for a migrated Amazon user.
     * check [shouldShowAmazonUnificationOnboardingScreen] to see when this screen is shown.
     */
    private fun startAmazonUnificationOnboarding(context: Context) {
        val intent = Intent(context, UnificationAmazonOnboardingActivity::class.java)
        startActivity(context, intent, null)
    }

    private fun shouldShowAmazonUnifiedOnboardingScreen(context: Context): Boolean =
        ConfigManager.getInstance().config
            .amazonMigrationConfig.onboarding
            ?.enabled == true &&
            PrefUtils.getHasMigratedFromAmazonClassic(context) == "true" &&
            !PrefUtils.hasUserActedOnAmazonUnificationOnboardingScreen(context) &&
            Utils.isProductFlavorAmazon()

    private fun startAmazonUnifiedOnboarding(context: Context) {
        val intent = Intent(context, AmazonUnifiedOnboardingActivity::class.java)
        startActivity(context, intent, null)
    }

    private fun shouldShowAmazonDuplicateSubscriptionsScreen(context: Context): Boolean =
        PaywallService.getConnector().hasDuplicateSubscription() &&
            !PrefUtils.hasUserActedOnDuplicateSubscriptionsScreen(context)

    private fun startAmazonDuplicateSubscriptionsActivity(context: Context) {
        val intent = Intent(context, DuplicateSubscriptionActivity::class.java)
        startActivity(context, intent, null)
    }
}
