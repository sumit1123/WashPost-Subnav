package com.washingtonpost.android.paywall.features.ftc

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.DialogFragment
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import kotlinx.coroutines.launch

class ComposeDialogFragment : DialogFragment() {
    private var onConfirm: (() -> Unit)? = null
    private var onDismiss: (() -> Unit)? = null
    private var templateText: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PurchaseConfirmationDialog(
                    onConfirm = {
                        dismiss()
                        onConfirm?.invoke()
                    },
                    onDismiss = {
                        dismiss()
                        onDismiss?.invoke()
                    }
                )
            }
        }
    }

    companion object {
        fun newInstance(
            templateText: String? = "",
            onConfirm: () -> Unit,
            onDismiss: () -> Unit
        ) = ComposeDialogFragment().apply {
            this.templateText = templateText
            this.onConfirm = onConfirm
            this.onDismiss = onDismiss
        }
    }

    @Composable
    fun PurchaseConfirmationDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        AndroidClassicTheme {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = wpdsColors.surface.copy(alpha = 0.9f),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.paywall_automatic_renewal_terms),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.h6.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = wpdsColors.onSurface,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            templateText?.let { text ->
                                val uriHandler = LocalUriHandler.current
                                val annotatedString = buildAnnotatedString {
                                    val linkStart = text.indexOf("<a href=\"")
                                    val linkEnd = text.indexOf("</a>")
                                    if (linkStart != -1 && linkEnd != -1) {
                                        val hrefStart = linkStart + 9
                                        val hrefEnd = text.indexOf("\">", hrefStart)
                                        val url = text.substring(hrefStart, hrefEnd)
                                        val linkText = text.substring(hrefEnd + 2, linkEnd)

                                        // Add text before link
                                        append(text.substring(0, linkStart))

                                        // Add clickable link
                                        pushStringAnnotation(
                                            tag = "URL",
                                            annotation = url
                                        )
                                        pushStyle(
                                            SpanStyle(
                                                color = wpdsColors.blue100,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        )
                                        append(linkText)
                                        pop()
                                        pop()

                                        // Add text after link
                                        append(text.substring(linkEnd + 4))
                                    } else {
                                        append(text)
                                    }
                                }

                                ClickableText(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.body2.copy(
                                        color = wpdsColors.onSurface,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    onClick = { offset ->
                                        annotatedString.getStringAnnotations(
                                            tag = "URL",
                                            start = offset,
                                            end = offset
                                        ).firstOrNull()?.let { annotation ->
                                            PaywallService.getConnector().openCancelSubscriptionPage(annotation.item, context)
                                        }
                                    }
                                )
                            }
                        }
                        Divider(color = wpdsColors.gray60.copy(alpha = 0.2f), modifier = Modifier.height(0.5.dp).clipToBounds())
                        Text(
                            text = stringResource(id = R.string.paywall_agree_and_continue),
                            textAlign = TextAlign.Center,
                            color = wpdsColors.blue100,
                            style = MaterialTheme.typography.h6.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        // Note: Commenting out the consent storage logic for now, will be replaced with actual implementation later
//                                        FTCConsentStorage.saveConsent(
//                                            context = context,
//                                            termsText = templateText ?: ""
//                                        )
//                                        val consent = FTCConsentStorage.getStoredConsent(context)
//                                        LogUtil.d("FTCConsent", "Initial consent saved - terms: ${consent?.termsText}, status: ${consent?.status}, timestamp: ${consent?.timestamp}")
                                        onConfirm()
                                    }
                                }
                                .padding(12.dp)
                                .fillMaxWidth()
                        )
                        Divider(color = wpdsColors.gray60.copy(alpha = 0.2f), modifier = Modifier.height(0.5.dp).clipToBounds())
                        Text(
                            text = stringResource(id = R.string.paywall_cancel),
                            color = wpdsColors.blue100,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.h6.copy(
                                fontWeight = FontWeight.Normal,
                            ),
                            modifier = Modifier
                                .clickable { onDismiss.invoke() }
                                .padding(12.dp)
                                .fillMaxWidth()
                        )

                    }
                }
            }
        }
    }
}