package com.wapo.flagship.features.signin

import android.net.Uri
import android.os.Bundle
import com.wapo.android.commons.util.Logger
import androidx.appcompat.app.AppCompatActivity
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_LOGIN_REDIRECT
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_MAGIC_LINK

/**
 * Activity that receives the redirect uri sent by washingtonpost.com/subscribe
 *
 */
class OauthRedirectUriReceiverActivity : AppCompatActivity() {
    public override fun onCreate(savedInstanceBundle: Bundle?) {
        super.onCreate(savedInstanceBundle)

        if (intent.data != null) {
            val newIntent = IntentHelper.getMainActivityIntent(this)
            // if redirect_uri is present, we are handling the redirect from third party auth in the browser
            if (intent.data?.getQueryParameter("redirect_uri") != null) {
                newIntent.data = Uri.parse(intent.data?.getQueryParameter("redirect_uri"))
                newIntent.action = ACTION_OPEN_LOGIN_REDIRECT
            } else {
                // else this is a magic link, so complete that flow
                newIntent.data = intent.data
                newIntent.action = ACTION_OPEN_MAGIC_LINK
            }
            startActivity(newIntent)
        } else {
            Logger.e("OauthRedirectUriReceiverActivity", "redirect url missing")
        }

        finish()
    }
}
