package com.wapo.flagship

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.sdk.iterable.IterableSdk
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_LOGIN_REDIRECT
import com.wapo.flagship.wapomain.MainConstants.ACTION_OPEN_MAGIC_LINK
import com.washingtonpost.android.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.iterable.iterableapi.IterableApi
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.util.tracking.states.TrafficSource

/**
 * Class to delegate external App links to MainActivity.
 * This is a singleTask activity. So it is started in its own task and remaining standard
 * activities will be opened in the same task.
 */
@AndroidEntryPoint
class DeepLinkSingleTaskActivity : AppCompatActivity() {

    @Inject
    lateinit var iterableSdk: IterableSdk

    override fun onCreate(savedInstanceState: Bundle?) {
        intent = removeExtraLaunchUri(intent)
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri != null && uri.toString() == DeepLinksProcessor.WASHPOST_SCHEMA) {
            finish()
            return
        }

        val newIntent = IntentHelper.getMainActivityIntent(this, true).apply {
            fillIn(intent, 0)
        }

        when {
            //  Handle auth links
            intent.data?.scheme == BuildConfig.AUTH_SCHEME || intent.data?.host == BuildConfig.AUTH_REDIRECT_PATH -> {
                // if redirect_uri is present, we are handling the redirect from third party auth in the browser
                newIntent.apply {
                    if (intent.data?.getQueryParameter("redirect_uri") != null) {
                        data = Uri.parse(intent.data?.getQueryParameter("redirect_uri"))
                        action = ACTION_OPEN_LOGIN_REDIRECT
                    } else {
                        // else this is a magic link, so complete that flow
                        data = intent.data
                        action = ACTION_OPEN_MAGIC_LINK
                    }
                }
                forwardToDestination(newIntent)
            }

            //  Handle iterable deep links
            IterableApi.isIterableDeeplink(intent.dataString) -> {
                val originalUri = intent.dataString
                if (originalUri.isNullOrEmpty()) {
                    forwardToDestination(newIntent)
                    return
                }

                iterableSdk.getIterableApi().handleAppLink(originalUri)
                newIntent.data = null
                forwardToDestination(newIntent)
            }

            else -> forwardToDestination(newIntent)
        }
    }

    private fun forwardToDestination(intent: Intent) {
        intent.data?.let { Measurement.mark(TrafficSource.fromDeepLinkUri(it)) }
        startActivity(intent)
        finish()
    }

    /**
     * Creates a new copy of the provided [Intent] with the [EXTRA_LAUNCH_URI] extra removed.
     *
     * This function removes the EXTRA_LAUNCH_URI from the intent to avoid binder transaction failures (AWA-12310)
     *
     * @param intent The original [Intent] containing extras to sanitize.
     * @return A new [Intent] instance containing all original properties and extras,
     *         except for [EXTRA_LAUNCH_URI].
     *
     * @see Intent.removeExtra
     */
    private fun removeExtraLaunchUri(intent: Intent): Intent {
        return Intent(intent).apply {
            removeExtra(EXTRA_LAUNCH_URI)
        }
    }

    private companion object {
        // Key observed on inbound deep-link intents; removing it prevents crashes
        // (e.g., TransactionTooLarge/BinderProxy.transactNative) when forwarding the intent.
        const val EXTRA_LAUNCH_URI = "extra_launch_uri"
    }
}
