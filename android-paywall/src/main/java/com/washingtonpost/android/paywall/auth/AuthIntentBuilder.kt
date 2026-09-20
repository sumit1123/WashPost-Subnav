package com.washingtonpost.android.paywall.auth

import android.os.Bundle

/**
 * Builder class for Sign-In intent.
 */
class AuthIntentBuilder() {
    private val bundle = Bundle()

    /**
     * Used for passing info about free trial/promo registration to WPAA.
     */
    fun addRegistrationParams(promoId: String, trialType: String): AuthIntentBuilder {
        bundle.putString(PROMO_ID, promoId)
        bundle.putString(TRIAL_TYPE, trialType)
        return this
    }

    /**
     * Data taken from magic link url used for verifying validity of user.
     */
    fun addMagicLinkData(value: String): AuthIntentBuilder {
        bundle.putString(MAGIC_LINK_DATA, value)
        return this
    }

    /**
     * Check if signed-in from a magic link or social redirect.
     */
    fun addIsMagicLinkOrSocialRedirect(value: Boolean): AuthIntentBuilder {
        bundle.putBoolean(IS_MAGICLINK_AUTH, value)
        return this
    }

    /**
     * Check if signed-in from a magic link.
     */
    fun addIsSignUp(value: Boolean): AuthIntentBuilder {
        bundle.putBoolean(IS_SIGN_UP, value)
        return this
    }

    /**
     * Redirect url to launch after returning to the app from third party auth
     */
    fun addRedirectUrl(value: String): AuthIntentBuilder {
        bundle.putString(REDIRECT_URL, value)
        return this
    }

    fun addEntryPoint(value: AuthEntryPoint): AuthIntentBuilder {
        bundle.putString(ENTRY_POINT, value.name)
        return this
    }

    /**
     * Return intent.
     */
    fun build(): Bundle {
        return bundle;
    }


    companion object {

        /**
         * key value for magic link data extra
         */
        @JvmStatic
        val MAGIC_LINK_DATA = "AuthData"

        /**
         * key value for is magic link extra
         */
        @JvmStatic
        val IS_MAGICLINK_AUTH = "IsDeepLinkAuth"

        /**
         * Key value for regwall registration promo extra
         */
        @JvmStatic
        val PROMO_ID = "promoId"

        /**
         * Key value for regwall registration free trial extra
         */
        @JvmStatic
        val TRIAL_TYPE = "trialType"

        /**
         * Key value for showing sign up page
         */
        @JvmStatic
        val IS_SIGN_UP = "isSignUp"

        /**
         * Redirect url to launch after returning to the app from third party auth
         */
        @JvmStatic
        val REDIRECT_URL = "redirectUrl"

        /**
         * Key value for entry point that started the auth flow
         */
        @JvmStatic
        val ENTRY_POINT = "entryPoint"
    }

}