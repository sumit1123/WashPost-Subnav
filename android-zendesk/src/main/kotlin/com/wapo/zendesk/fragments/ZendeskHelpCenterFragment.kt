package com.wapo.zendesk.fragments

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.onetrust.otpublishers.headless.Public.OTPublishersHeadlessSDK
import com.wapo.zendesk.R
import com.wapo.zendesk.databinding.FragmentZendeskHelpCenterBinding
import com.wapo.zendesk.viewmodel.Action
import com.wapo.zendesk.viewmodel.ZendeskDestinationViewModel
import com.wapo.zendesk.viewmodel.ZendeskSharedViewModel

class ZendeskHelpCenterFragment : Fragment() {

    private var _binding: FragmentZendeskHelpCenterBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: ZendeskSharedViewModel by activityViewModels()
    private val zendeskDestinationViewModel: ZendeskDestinationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZendeskHelpCenterBinding.inflate(inflater, container, false)
        setupWebView()
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().setTitle(R.string.zendesk_help)
    }

    private fun setupWebView() {
        binding.webview.loadUrl(getString(R.string.zd_help_center_url))
        val webSettings: WebSettings = binding.webview.settings
        webSettings.javaScriptEnabled = true
        binding.webview.webViewClient = ZendeskWebViewClient(binding, sharedViewModel, zendeskDestinationViewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ZendeskWebViewClient(
    val binding: FragmentZendeskHelpCenterBinding,
    val sharedViewModel: ZendeskSharedViewModel,
    val zendeskDestinationViewModel: ZendeskDestinationViewModel
) : WebViewClient() {

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (isFormSubmissionUrl(url, view.context)) {
            sharedViewModel.dispatchAction(Action.ContactUsClick)
            /**
             * This event is sent to hosting activity for starting the contact us form.
             */
            zendeskDestinationViewModel.startContactUsFromHelpCenter()
            return true
        }
        return super.shouldOverrideUrlLoading(view, url)
    }

    @Deprecated("Deprecated in Java")
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        binding.webview.visibility = View.GONE
        binding.errorText.visibility = View.VISIBLE
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view?.let {
                passConsent(view)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun passConsent(view: WebView) {
        val jsToPass = OTPublishersHeadlessSDK(view.context).otConsentJSForWebView
        view.evaluateJavascript("javascript:$jsToPass", null)
    }

    private fun isFormSubmissionUrl(url: String, context: Context): Boolean {
        return try {
            val contactUsPath = Uri.parse(context.getString(R.string.zd_contact_us_url)).path
            val givenPath = Uri.parse(url).path
            return contactUsPath == givenPath
        } catch (t: Throwable) {
            false
        }
    }
}
