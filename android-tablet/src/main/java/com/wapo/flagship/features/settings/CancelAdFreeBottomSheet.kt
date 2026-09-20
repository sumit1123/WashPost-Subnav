package com.wapo.flagship.features.settings

import android.os.Bundle
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.getSpans
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.PostiniFontFamily
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

class CancelAdFreeBottomSheet : BaseBottomSheetDialogFragment() {

    var onContinue: (() -> Unit)? = null
    var onNevermind: (() -> Unit)? = null

    private val planName: String?
        get() = arguments?.getString(ARG_PLAN_NAME)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        makeBackgroundTransparent()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AndroidClassicTheme {
                Surface(
                    color = wpdsColors.wallPrimaryBg,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ) {
                    CancelAdFreeContent()
                }
            }
        }
    }

    @Composable
    fun CancelAdFreeContent() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Title row with close button
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = getString(R.string.cancel_ad_free_bottom_sheet_title, planName),
                    color = wpdsColors.gray0,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    fontFamily = PostiniFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .align(Alignment.Center)
                )

                IconButton(
                    onClick = { dismiss() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = getString(R.string.close),
                        modifier = Modifier.size(16.dp),
                        tint = wpdsColors.gray0
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

                // Body
                Text(
                    text = getString(R.string.cancel_ad_free_bottom_sheet_body, planName),
                    color = wpdsColors.gray20,
                    fontSize = 16.sp,
                    letterSpacing = 0.sp,
                    fontFamily = FranklinItcStandardFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Instruction text with bold "Subscribe"
                Text(
                    text = remember {
                        val spanned = getText(R.string.cancel_ad_free_bottom_sheet_instruction)
                        spanned.toAnnotatedString()
                    },
                    color = wpdsColors.gray20,
                    fontSize = 14.sp,
                    fontFamily = FranklinItcStandardFontFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Continue button (filled)
                Button(
                    onClick = {
                        onContinue?.invoke()
                        dismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(44.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(backgroundColor = wpdsColors.cta),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    Text(
                        text = getString(R.string.cancel_ad_free_bottom_sheet_continue),
                        color = wpdsColors.onCta,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FranklinItcStandardFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nevermind button (outlined)
                OutlinedButton(
                    onClick = {
                        onNevermind?.invoke()
                        dismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, wpdsColors.gray300),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = wpdsColors.wallPrimaryBg
                    )
                ) {
                    Text(
                        text = getString(R.string.cancel_ad_free_bottom_sheet_nevermind),
                        color = wpdsColors.gray40,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FranklinItcStandardFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
        }
    }

    companion object {
        const val TAG = "CancelAdFreeBottomSheet"
        private const val ARG_PLAN_NAME = "plan_name"

        fun newInstance(planName: String): CancelAdFreeBottomSheet {
            return CancelAdFreeBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLAN_NAME, planName)
                }
            }
        }
    }
}

/**
 * Converts an Android [CharSequence] (potentially [android.text.Spanned]) to a Compose
 * [AnnotatedString], preserving bold [StyleSpan]s as [SpanStyle] with [FontWeight.Bold].
 */
private fun CharSequence.toAnnotatedString(): AnnotatedString {
    if (this !is android.text.Spanned) return AnnotatedString(this.toString())
    return buildAnnotatedString {
        append(this@toAnnotatedString.toString())
        getSpans<StyleSpan>(0, length).forEach { span ->
            if (span.style == android.graphics.Typeface.BOLD || span.style == android.graphics.Typeface.BOLD_ITALIC) {
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    getSpanStart(span),
                    getSpanEnd(span)
                )
            }
        }
    }
}
