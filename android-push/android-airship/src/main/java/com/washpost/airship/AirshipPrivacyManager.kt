package com.washpost.airship

import com.urbanairship.PrivacyManager
import com.urbanairship.UAirship

/**
 * Class to manage Airship's PrivacyManager Features
 */
class AirshipPrivacyManager {

    /**
     * App level Feature class to map PrivacyManager Feature constants
     */
    enum class Feature(val id: Int) {
        ANALYTICS(PrivacyManager.FEATURE_ANALYTICS),
        IAA(PrivacyManager.FEATURE_IN_APP_AUTOMATION),
        MESSAGE_CENTER(PrivacyManager.FEATURE_MESSAGE_CENTER),
        PUSH(PrivacyManager.FEATURE_PUSH),
        TAGS_AND_ATTRIBUTES(PrivacyManager.FEATURE_TAGS_AND_ATTRIBUTES),
        CONTACTS(PrivacyManager.FEATURE_CONTACTS),
    }

    private fun privacyManager(): PrivacyManager? {
        return if (AirshipPushManager.isAirshipReady()) {
            UAirship.shared().privacyManager
        } else {
            null
        }
    }

    /**
     * Method to enable default Airship's features.
     * PUSH, TAGS_AND_ATTRIBUTES and CONTACTS (for namedUserId/supportId) should be enabled by default.
     * It works for first time installations.
     * But DataCollection (on previous Airship's version before PrivacyManager is introduced) enables all features
     * while migration if it was enabled before. So disable unused features to address an upgrade case.
     */
    fun enableDefaults() {
        privacyManager()?.apply {
            enable(*defaultFeatures())
            disable(Feature.MESSAGE_CENTER.id)
        }
    }

    fun enable(feature: Feature) {
        privacyManager()?.enable(feature.id)
    }

    fun disable(feature: Feature) {
        privacyManager()?.disable(feature.id)
    }

    fun isEnabled(feature: Feature): Boolean {
        return privacyManager()?.isEnabled(feature.id) ?: false
    }

    companion object {
        @JvmStatic
        fun defaultFeatures(): IntArray {
            return intArrayOf(Feature.PUSH.id, Feature.TAGS_AND_ATTRIBUTES.id, Feature.CONTACTS.id)
        }
    }
}