package com.wapo.flagship.features.amazonunification.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.android.commons.util.ViewUtil.findActivity
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.tracking.Measurement
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

class DuplicateSubscriptionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidClassicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = wpdsColors.gray20,
                ) {
                    DuplicateSubscriptionScreen()
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Measurement.trackOnboardingSeen("unified_dup_sub")
    }
}

@Composable
private fun DuplicateSubscriptionScreen() {
    val horizontalPadding = dimensionResource(id = R.dimen.migration_screens_horizontal_padding)
    Column(
        modifier =
            Modifier
                .padding(horizontalPadding, 0.dp, horizontalPadding, 0.dp)
                .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
    ) {
        BurstIcon()
        DuplicateSubscriptionTitle()
        ViewSubscriptionsText()
    }
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.padding(horizontalPadding, 0.dp, horizontalPadding, 20.dp),
    ) {
        ManageSubscriptionsButton()
        ContinueWithMultipleSubscriptionsButton()
    }
}

@Composable
private fun BurstIcon() {
    Row(modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 19.dp)) {
        Image(
            painter = painterResource(id = R.drawable.ic_burst),
            contentDescription = "burst icon",
        )
    }
}

@Composable
private fun DuplicateSubscriptionTitle() {
    Text(
        text = "Heads up: You have more than one subscription",
        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 4.dp),
        color = wpdsColors.secondary,
        fontSize = 28.sp,
        lineHeight = 30.8.sp,
        letterSpacing = 0.0.sp,
        fontFamily = FontFamily(Font(com.wapo.view.R.font.postoniwide_bold)),
    )
}

@Composable
private fun ViewSubscriptionsText() {
    val text =
        buildAnnotatedString {
            withStyle(style = SpanStyle(fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_light)))) {
                append("View and manage your Washington Post subscriptions in the Amazon Appstore.")
            }
        }
    Text(
        text = text,
        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 14.dp),
        color = wpdsColors.secondary,
        fontSize = 16.sp,
        lineHeight = 18.5.sp,
        letterSpacing = 0.0.sp,
        fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_light)),
    )
}

@Composable
private fun ManageSubscriptionsButton() {
    val context = LocalContext.current
    Row(
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick =
                {
                    Measurement.trackOnboardingClick("unified_dup_store")
                    val openAppStoreIntent = Intent(Intent.ACTION_VIEW)
                    openAppStoreIntent.data = Uri.parse("amzn://apps/library/subscriptions")
                    context.startActivity(openAppStoreIntent)
                    setScreenViewedAndCloseOut(context)
                },
            modifier =
                Modifier
                    .padding(0.dp, 0.dp, 0.dp, 12.dp)
                    .height(42.dp)
                    .fillMaxWidth()
                    .align(Alignment.Bottom),
            colors = ButtonDefaults.buttonColors(backgroundColor = wpdsColors.gray500),
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = "Manage in Amazon Appstore",
                color = wpdsColors.gray20,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.0.sp,
                fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_bold)),
            )
        }
    }
}

@Composable
private fun ContinueWithMultipleSubscriptionsButton() {
    val context = LocalContext.current
    Row(
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick =
                {
                    Measurement.trackOnboardingClick("unified_dup_multi")
                    context.startActivity(IntentHelper.getMainActivityIntent(context))
                    setScreenViewedAndCloseOut(context)
                },
            modifier =
                Modifier
                    .padding(0.dp, 0.dp, 0.dp, 12.dp)
                    .height(42.dp)
                    .fillMaxWidth()
                    .align(Alignment.Bottom),
            colors = ButtonDefaults.buttonColors(backgroundColor = wpdsColors.gray20),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, wpdsColors.secondary),
        ) {
            Text(
                text = "Continue with multiple subscriptions",
                color = wpdsColors.secondary,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.0.sp,
                fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_bold)),
            )
        }
    }
}

private fun setScreenViewedAndCloseOut(context: Context) {
    PrefUtils.setUserActedOnDuplicateSubscriptionsScreen(context, true)
    context.findActivity()?.finish()
}

@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=480,orientation=portrait",
)
@Composable
private fun DuplicateSubscriptionsPreview() {
    AndroidClassicTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = wpdsColors.gray20,
        ) {
            DuplicateSubscriptionScreen()
        }
    }
}
