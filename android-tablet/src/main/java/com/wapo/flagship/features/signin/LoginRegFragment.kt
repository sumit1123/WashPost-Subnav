package com.wapo.flagship.features.signin

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.flagship.Utils
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.wapo.view.NestedScrollWebView.PageLoadingListener
import com.wapo.view.R
import com.washingtonpost.android.databinding.FragmentLoginRegBinding
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PostIterableEventType
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PostIterableEventViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * LoginRegFragment is BottomSheetDialog containing a Webview and a ProgressBar.
 * It initiates the login flow based on the Fragment's arguments (bundle).
 * This can be initiated from PaywallService.getConnector().showSignInScreen(fragmentManager, bundle, wallName, paywallType, isAcquisition)
 * or PaywallService.getConnector().showSignUpScreen(FragmentManager fragmentManager, Bundle bundle, String wallName, PaywallConstants.WallType paywallType)
 */
@AndroidEntryPoint
class LoginRegFragment :
    BaseBottomSheetDialogFragment(),
    PageLoadingListener {

    @Inject
    lateinit var loadRenderMetrics: LoadRenderMetrics

    private var _binding: FragmentLoginRegBinding? = null
    private val binding get() = _binding

    private val loginRegViewModel: LoginRegViewModel by viewModels()
    private val postIterableEventViewModel: PostIterableEventViewModel by activityViewModels()

    private var codeDismissed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadRenderMetrics.startLoadRenderMetrics(LoadRenderMetricsEvent.SignInRenderEvent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentLoginRegBinding.inflate(inflater, container, false)
        binding?.root?.minimumHeight = resources.getDimension(R.dimen.bottom_sheet_min_height).toInt()
        binding?.progressCircular?.visibility = View.VISIBLE

        binding?.webview?.apply {
            initWebView(true, true, false, true, true, false)
            this.setPageLoadingListener(this@LoginRegFragment)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                settings.isAlgorithmicDarkeningAllowed = true
            }
        }

        arguments?.let {
            val isSignUp = it.getBoolean(AuthIntentBuilder.IS_SIGN_UP)
            val isMagicLinkOrSocialRedirect = it.getBoolean(AuthIntentBuilder.IS_MAGICLINK_AUTH)
            val magicLinkOrSocialRedirectResponseData =
                it
                    .getString(
                        AuthIntentBuilder.MAGIC_LINK_DATA,
                    )?.toUri()
            val promoId = it.getString(AuthIntentBuilder.PROMO_ID)
            val trialType = it.getString(AuthIntentBuilder.TRIAL_TYPE)
            val additionalParams: Map<String, String?> =
                mapOf(
                    AuthIntentBuilder.PROMO_ID to promoId,
                    AuthIntentBuilder.TRIAL_TYPE to trialType,
                )
            val redirectUri = it.getString(AuthIntentBuilder.REDIRECT_URL)?.toUri() ?: Uri.EMPTY
            loginRegViewModel.startLoginFlow(
                isSignUp,
                isMagicLinkOrSocialRedirect,
                magicLinkOrSocialRedirectResponseData,
                additionalParams,
                redirectUri,
                it
            )
        }

        return binding?.root
    }

    override fun onAttach(context: Context) {
        behaviorState = BottomSheetBehavior.STATE_EXPANDED
        super.onAttach(context)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        codeDismissed = false
        loginRegViewModel.webviewEvent.observe(viewLifecycleOwner) { webviewEvent ->
            when (webviewEvent) {
                is LoginRegWebviewEvent.Success -> {
                    loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.SignInRenderEvent)
                    if (activity is LoginRegHost) {
                        (activity as LoginRegHost).provideLoginRegActivityViewModel().reloadWebView()
                    }
                    webviewEvent.message?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                    codeDismissed = true
                    postIterableEventViewModel.postSignInOrSubscribeEvent(PostIterableEventType.SIGN_IN_OR_REGISTER)
                    dismiss()
                }
                is LoginRegWebviewEvent.Error -> {
                    loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.SignInRenderEvent)
                    webviewEvent.message?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    }
                    codeDismissed = true
                    dismiss()
                }
                is LoginRegWebviewEvent.LaunchBrowser -> {
                    loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.SignInRenderEvent)
                    Utils.startWebChromeCustomTab(webviewEvent.url, context)
                    codeDismissed = true
                    dismiss()
                }
                is LoginRegWebviewEvent.LoadUrl -> {
                    binding?.webview?.loadUrl(webviewEvent.url)
                }

                is LoginRegWebviewEvent.LaunchAppWebview -> {
                    Utils.startWeb(webviewEvent.url, context)
                }

                is LoginRegWebviewEvent.LaunchActivity -> {
                    startActivity(webviewEvent.intent)
                }
            }
        }
    }

    override fun onProgressChanged(newProgress: Int) {
        if (newProgress == 100) {
            binding?.progressCircular?.visibility = View.INVISIBLE
            binding?.webview?.visibility = View.VISIBLE
        }
    }

    override fun onPageStarted(url: String?) {
        binding?.progressCircular?.visibility = View.VISIBLE
        binding?.webview?.visibility = View.GONE
    }

    override fun onPageFinished(url: String?) {
        binding?.progressCircular?.visibility = View.INVISIBLE
        binding?.webview?.visibility = View.VISIBLE
    }

    override fun onReceiveError(
        errorCode: Int,
        description: String?,
    ) {
        loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.SignInRenderEvent)
        Toast.makeText(context, "Error loading sign in. Please try again", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.SignInRenderEvent)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        url: String?,
        isRedirect: Boolean,
    ): Boolean = loginRegViewModel.handleRedirect(url, arguments)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!codeDismissed) {
            PaywallService.getInstance()?.clearPreviousScreen()
        }
    }

    companion object {
        const val TAG = "login_reg_fragment"
    }
}
