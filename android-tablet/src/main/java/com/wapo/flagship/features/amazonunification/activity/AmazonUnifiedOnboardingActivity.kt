package com.wapo.flagship.features.amazonunification.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.android.commons.util.ViewUtil.findActivityOfType
import com.wapo.android.commons.util.ViewUtil.findActivity
import com.wapo.flagship.IntentHelper
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.util.PrefUtils
import com.washingtonpost.android.R
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.helper.WpPaywallHelper.getLoggedInUser
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

class AmazonUnifiedOnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidClassicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = wpdsColors.gray20,
                ) {
                    Onboarding(getLoggedInUser()?.userId)
                }
            }
        }
    }
}

@Composable
private fun Onboarding(email: String?) {
    val horizontalPadding = dimensionResource(id = R.dimen.migration_screens_horizontal_padding)
    Column(
        modifier =
            Modifier
                .padding(horizontalPadding, 0.dp, horizontalPadding, 0.dp)
                .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
    ) {
        MigrationIcons()
        WelcomeTitle()
        WelcomeMessage(email)
        AdditionalInfo(
            R.drawable.ic_sparkle,
            "sparkle",
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_bold))),
                ) {
                    append("Everything’s right where you left it, ")
                }
                withStyle(
                    style = SpanStyle(fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_light))),
                ) {
                    append(
                        "including your Saved Stories, news alerts and personalized recommendations.",
                    )
                }
            },
        )
        AdditionalInfo(
            R.drawable.ic_trash_white,
            "trash",
            buildAnnotatedString {
                withStyle(
                    style = SpanStyle(fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_bold))),
                ) {
                    append("You can now delete the old app ")
                }
                withStyle(
                    style = SpanStyle(fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_light))),
                ) {
                    append("(the one with the black icon).")
                }
            },
        )
    }
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.padding(horizontalPadding, 0.dp, horizontalPadding, 20.dp),
    ) {
        ContinueButton()
        SignIn()
    }
}

@Composable
private fun MigrationIcons() {
    Row(modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 13.dp)) {
        Image(
            painter = painterResource(id = R.drawable.ic_migration_unified_icon),
            contentDescription = "blue Washington Post app icon",
        )
    }
}

@Composable
private fun WelcomeTitle() {
    Text(
        text = "Welcome!",
        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 4.dp),
        color = wpdsColors.secondary,
        fontSize = 28.sp,
        lineHeight = 30.8.sp,
        letterSpacing = 0.0.sp,
        fontFamily = FontFamily(Font(com.wapo.view.R.font.postoniwide_bold)),
    )
}

@Composable
private fun WelcomeMessage(email: String?) {
    val text =
        buildAnnotatedString {
            withStyle(style = SpanStyle(fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_light)))) {
                append("Thanks for downloading our app.")
            }
            email?.let {
                withStyle(style = SpanStyle(fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_light)))) {
                    append(" You’re now signed in as ")
                }
                withStyle(style = SpanStyle(fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_bold)))) {
                    append(email)
                }
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
private fun AdditionalInfo(
    drawableId: Int,
    drawableDescription: String,
    textString: AnnotatedString,
) {
    Row(modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 24.dp)) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = drawableDescription,
            modifier =
                Modifier
                    .align(Alignment.CenterVertically)
                    .padding(0.dp, 0.dp, 14.dp, 0.dp),
        )
        Text(
            text = textString,
            modifier = Modifier.align(Alignment.CenterVertically),
            color = wpdsColors.secondary,
            fontSize = 16.sp,
            letterSpacing = 0.0.sp,
        )
    }
}

@Composable
private fun ContinueButton() {
    val context = LocalContext.current
    Row(
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick =
                {
                    context.startActivity(IntentHelper.getMainActivityIntent(context))
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
                text = "Continue to today’s news",
                color = wpdsColors.gray20,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_bold)),
                letterSpacing = 0.0.sp,
            )
        }
    }
}

@Composable
private fun SignIn() {
    val context = LocalContext.current
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val color = wpdsColors.secondary
        ClickableText(
            text =
                buildAnnotatedString {
                    withStyle(style = ParagraphStyle(lineHeight = 17.5.sp)) {
                        withStyle(
                            style =
                                SpanStyle(
                                    fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_light)),
                                    letterSpacing = 0.0.sp,
                                    fontSize = 14.sp,
                                    color = color,
                                    textDecoration = TextDecoration.Underline,
                                ),
                        ) {
                            append("Sign in with a different account")
                        }
                    }
                },
            onClick = {
                val supportFragmentManager = context.findActivityOfType<BaseActivity>()?.supportFragmentManager
                if (supportFragmentManager != null) {
                    PaywallService.getConnector().showSignInScreen(
                        supportFragmentManager,
                        AuthIntentBuilder().build(),
                        null,
                        null,
                        false,
                        null
                    )
                    setScreenViewedAndCloseOut(context)
                }
            },
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp).height(18.dp),
        )
    }
}

private fun setScreenViewedAndCloseOut(context: Context) {
    PrefUtils.setUserActedOnAmazonUnificationOnboarding(context, true)
    context.findActivity()?.finish()
}

@Preview(
    showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=480,orientation=portrait",
)
@Composable
private fun OffboardingPreview() {
    AndroidClassicTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = wpdsColors.gray20,
        ) {
            Onboarding("first.last@email.com")
        }
    }
}
