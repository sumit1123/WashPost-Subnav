package com.wapo.flagship.features.onboarding

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class OnboardingService(
    val context: Context,
) {
    private var onboardingConfig: OnboardingConfig? = null

    private val PREFS_FILENAME: String = javaClass.`package`.toString() + ".cache.prefs"

    private fun customPrefs(
        context: Context,
        name: String,
    ): SharedPreferences =
        context.getSharedPreferences(
            name,
            Context.MODE_PRIVATE,
        )

    private val prefs: SharedPreferences = customPrefs(context, PREFS_FILENAME)

    init {
        if (onboardingConfig == null) {
            context.resources.openRawResource(R.raw.onboarding_config_variant).let {
                onboardingConfig =
                    Gson().fromJson(
                        BufferedReader(InputStreamReader(it)),
                        OnboardingConfig::class.java,
                    )
            }
        }
    }

    fun isShown(): Boolean = isShown(onboardingConfig?.id)

    fun isShown(onboardingId: String?): Boolean {
        var shown = true
        onboardingId?.let {
            shown = prefs.getBoolean(it, false)
        }
        return shown
    }

    fun setShownFlag(flag: Boolean) {
        setShownFlag(onboardingConfig?.id, flag)
    }

    fun setShownFlag(
        onboardingId: String?,
        flag: Boolean,
    ) {
        onboardingId?.let {
            prefs.edit().putBoolean(it, flag).commit()
        }
    }

    fun isFeatureEnabled(): Boolean = onboardingConfig?.enabled == true

    fun isSubscribed(): Boolean = PaywallService.getInstance().isPremiumUser

    fun isMigrated(): Boolean = PrefUtils.getHasMigratedFromRainbow(context).lowercase(Locale.US) == "true"

    fun getOnboardingConfig(): OnboardingConfig? = onboardingConfig
}
