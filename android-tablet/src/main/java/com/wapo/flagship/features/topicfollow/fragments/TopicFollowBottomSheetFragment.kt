// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.wapo.flagship.features.topicfollow.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.wapo.android.commons.util.Logger
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.flagship.features.preferencesapi.models.Bullet
import com.wapo.flagship.features.preferencesapi.models.Followable
import com.wapo.flagship.features.preferencesapi.models.Newsletter
import com.wapo.flagship.features.preferencesapi.models.Notification
import com.wapo.flagship.features.topicfollow.events.TopicFollowEvent
import com.wapo.flagship.features.topicfollow.states.FollowingUiState
import com.wapo.flagship.features.topicfollow.states.NewsletterUiState
import com.wapo.flagship.features.topicfollow.states.NotificationUiState
import com.wapo.flagship.features.topicfollow.states.TopicFollowUiState
import com.wapo.flagship.features.topicfollow.viewmodels.TopicFollowBottomSheetViewModel
import com.wapo.flagship.features.topicfollow.viewmodels.TopicFollowCollaborationViewModel
import com.wapo.fragment.BaseBottomSheetDialogFragment
import com.washingtonpost.android.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import com.wpds.utils.IconUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class TopicFollowBottomSheetFragment(
    private val contentPackId: String,
    private val followable: Followable,
    private val trackingPageName: String,
) : BaseBottomSheetDialogFragment() {
    private val topicFollowBottomSheetViewModel: TopicFollowBottomSheetViewModel by viewModels()
    private val topicFollowCollaborationViewModel: TopicFollowCollaborationViewModel by activityViewModels()
    private var notificationsBlockedDialog: AlertDialog? = null
    private var pendingNotificationToggle = false
    private var returningFromSignIn = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val areNotificationsEnabled = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        topicFollowBottomSheetViewModel.initialize(
            contentPackId,
            followable,
            requireContext(),
            areNotificationsEnabled,
            trackingPageName,
        )
        observeEvents()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                AndroidClassicTheme {
                    Surface(
                        color = wpdsColors.wallPrimaryBg,
                    ) {
                        Wall()
                    }
                }
            }
        }

    override fun onResume() {
        super.onResume()
        context?.let {
            topicFollowBottomSheetViewModel.updateMainContentUiState(
                it,
                returningFromSignIn,
            )
        }
        if (returningFromSignIn) {
            returningFromSignIn = false
        }
        if (pendingNotificationToggle) {
            topicFollowCollaborationViewModel.dispatchPendingNotificationToggleEvent()
            pendingNotificationToggle = false
        }
    }

    private fun observeEvents() {
        topicFollowBottomSheetViewModel.topicFollowEvent.observe(this) {
            when (it) {
                is TopicFollowEvent.SignInStarted -> {
                    topicFollowBottomSheetViewModel.startSignIn(
                        activity?.supportFragmentManager,
                        it.isSignUp,
                    )
                }
                TopicFollowEvent.FollowToggled -> {
                    topicFollowBottomSheetViewModel.toggleFollowing(requireContext())
                }
                TopicFollowEvent.NewsletterToggled -> {
                    topicFollowBottomSheetViewModel.toggleNewsletter()
                }
                TopicFollowEvent.NotificationToggled -> {
                    topicFollowBottomSheetViewModel.toggleNotifications(requireContext())
                }
            }
        }

        topicFollowCollaborationViewModel.pendingNotificationToggleEvent.observe(this) {
            when (it) {
                true -> { // TODO toggled off if initially on when opening sheet for the first time
                    topicFollowBottomSheetViewModel.dispatchTopicFollowEvent(
                        TopicFollowEvent.NotificationToggled,
                    )
                }
                else -> {
                    // Do nothing, the user did not give system permissions
                }
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun Wall() {
        Column(
            modifier =
                Modifier
                    .padding(
                        top = 24.dp,
                        bottom =
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                                48.dp
                            } else {
                                0.dp
                            },
                    ).fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (topicFollowCollaborationViewModel.isLowDataModeEnable.value != true) {
                GlideImage(
                    model = topicFollowBottomSheetViewModel.followable.image,
                    contentDescription = "${topicFollowBottomSheetViewModel.followable.heading} icon",
                    modifier = Modifier.height(48.dp).width(48.dp),
                )
            }

            val uiState by topicFollowBottomSheetViewModel.mainContentUiState.observeAsState()
            when (uiState) {
                is TopicFollowUiState.Follow -> {
                    Follow()
                }
                is TopicFollowUiState.Register -> {
                    topicFollowBottomSheetViewModel.trackSoftwall()
                    Register()
                }
                else -> {
                    Loading()
                }
            }
        }
    }

    override fun onDestroy() {
        topicFollowCollaborationViewModel.updateTopicState(contentPackId)
        super.onDestroy()
    }

    @Composable
    private fun Follow() {
        val followingUiState by topicFollowBottomSheetViewModel.followingUiState.observeAsState()

        var followDescription by remember { mutableStateOf("") }
        var followButtonText by remember { mutableStateOf("") }
        var followButtonIcon by remember { mutableStateOf(com.wapo.flagship.features.aixp.R.drawable.check) }

        if (followingUiState is FollowingUiState.Following) {
            followDescription = topicFollowBottomSheetViewModel.followable.followedPrompt ?: ""
            followButtonText = "Following"
            followButtonIcon = com.wapo.flagship.features.aixp.R.drawable.check
        } else {
            followDescription = topicFollowBottomSheetViewModel.followable.unfollowedPrompt ?: ""
            followButtonText = "Follow"
            followButtonIcon = com.wpds.wpds.R.drawable.add
        }

        Title(topicFollowBottomSheetViewModel.followable.heading ?: "")
        AdditionalInfo(text = followDescription)
        Button(
            onClick = {
                topicFollowBottomSheetViewModel.dispatchTopicFollowEvent(
                    TopicFollowEvent.FollowToggled,
                )
            },
            modifier =
                Modifier
                    .padding(top = 12.dp)
                    .width(139.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(backgroundColor = wpdsColors.primary),
            border = BorderStroke(1.dp, wpdsColors.gray300),
            elevation = ButtonDefaults.elevation(0.dp),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 8.dp),
        ) {
            Text(
                text = followButtonText,
                color = wpdsColors.onPrimary,
                fontSize = 16.sp,
                letterSpacing = 0.sp,
                fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold)),
                modifier = Modifier.padding(end = 10.dp),
            )
            Icon(
                painter = painterResource(followButtonIcon),
                contentDescription = "follow status",
                modifier =
                    Modifier
                        .height(16.dp)
                        .width(16.dp)
                        .align(Alignment.CenterVertically),
                tint = wpdsColors.onPrimary,
            )
        }
        ToggleSection()
    }

    @Composable
    private fun Register() {
        Title(topicFollowBottomSheetViewModel.followable.registrationHeading ?: "")
        Column {
            topicFollowBottomSheetViewModel.followable.bullets?.forEach { bullet ->
                bullet ?: return@forEach
                val drawableId =
                    context?.let { context ->
                        bullet.icon?.let { icon ->
                            IconUtils(context).getDrawableId(icon)
                        }
                    }

                bullet.text?.let { text ->
                    AdditionalInfo(
                        drawableId = drawableId ?: com.wpds.wpds.R.drawable.for_you,
                        iconDescription = bullet.icon,
                        text = text,
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Button(
                onClick = {
                    returningFromSignIn = true
                    topicFollowBottomSheetViewModel.dispatchTopicFollowEvent(
                        TopicFollowEvent.SignInStarted(true),
                    )
                },
                modifier = Modifier.padding(top = 17.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(backgroundColor = wpdsColors.cta),
                border = BorderStroke(1.dp, wpdsColors.gray300),
                elevation = ButtonDefaults.elevation(0.dp),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 12.dp),
            ) {
                Text(
                    text = "Create free account",
                    color = wpdsColors.onCta,
                    fontSize = 16.sp,
                    letterSpacing = 0.sp,
                    fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold)),
                )
            }

            val annotatedText =
                buildAnnotatedString {
                    withStyle(
                        style =
                            SpanStyle(
                                fontFamily = FontFamily(Font(com.washingtonpost.android.paywall.R.font.franlinitcstd_light)),
                                color = wpdsColors.gray80,
                                letterSpacing = 0.sp,
                                fontSize = 14.sp,
                            ),
                    ) {
                        append("Already a subscriber? ")
                        pushStringAnnotation(tag = "SignIn", annotation = "SignIn")
                        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("Sign in")
                        }
                        pop()
                    }
                }

            ClickableText(
                text = annotatedText,
                onClick = { offset ->
                    annotatedText.getStringAnnotations(tag = "SignIn", start = offset, end = offset).firstOrNull()?.let {
                        returningFromSignIn = true
                        topicFollowBottomSheetViewModel.dispatchTopicFollowEvent(
                            TopicFollowEvent.SignInStarted(false),
                        )
                    }
                },
                modifier =
                    Modifier
                        .padding(top = 16.dp, bottom = 24.dp)
                        .align(Alignment.CenterHorizontally),
            )
        }
    }

    @Composable
    fun Loading() {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = wpdsColors.cta,
        )
    }

    @Composable
    private fun Title(text: String) {
        Text(
            text = text,
            color = wpdsColors.gray20,
            fontSize = 28.sp,
            letterSpacing = 0.sp,
            fontFamily = FontFamily(Font(com.wapo.view.R.font.postoniwide_bold)),
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Start,
        )
    }

    @Composable
    private fun AdditionalInfo(
        drawableId: Int? = null,
        iconDescription: String? = "bullet point",
        text: String,
    ) {
        Row(
            Modifier.padding(24.dp, 12.dp, 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            drawableId?.let {
                Icon(
                    painter = painterResource(id = drawableId),
                    contentDescription = iconDescription,
                    modifier =
                        Modifier
                            .align(Alignment.CenterVertically)
                            .padding(0.dp, 0.dp, 8.dp, 0.dp)
                            .height(16.dp)
                            .width(16.dp),
                    tint = wpdsColors.cta,
                )
            }
            Text(
                text = text,
                modifier = Modifier.align(Alignment.CenterVertically),
                textAlign = TextAlign.Center,
                color = wpdsColors.gray80,
                fontSize = 14.sp,
                lineHeight = 17.5.sp,
                letterSpacing = 0.sp,
            )
        }
    }

    @Composable
    private fun ToggleSection() {
        val newsletters = topicFollowBottomSheetViewModel.newsletter
        val notifications = topicFollowBottomSheetViewModel.notification

        Column(
            Modifier
                .padding(top = 20.dp)
                .background(wpdsColors.wallSecondaryBg),
        ) {
            if (newsletters != null && notifications != null) {
                ToggleRow(newsletters)
                Divider(
                    Modifier.padding(10.dp, 0.dp, 10.dp, 0.dp),
                    color = wpdsColors.wallPrimaryBg,
                )
                ToggleRow(notifications, true)
            } else if (newsletters != null) {
                ToggleRow(newsletters, true)
            } else if (notifications != null) {
                ToggleRow(notifications, true)
            }
        }
    }

    @Composable
    private fun ToggleRow(
        toggleable: Any,
        lastRow: Boolean = false,
    ) {
        var enabledState by remember { mutableStateOf(true) }
        val name: String
        val description: String
        when (toggleable) {
            is Newsletter -> {
                val newsletterUiState by topicFollowBottomSheetViewModel.newsletterUiState.observeAsState()
                enabledState = newsletterUiState is NewsletterUiState.Enabled
                name = toggleable.name ?: ""
                description = toggleable.description ?: ""
            }
            is Notification -> {
                val notificationUiState by topicFollowBottomSheetViewModel.notificationUiState.observeAsState()
                enabledState = notificationUiState is NotificationUiState.Enabled
                name = toggleable.name ?: ""
                description = toggleable.description ?: ""
            }
            else -> {
                Logger.d(TAG, "Unsupported toggleable type")
                return
            }
        }

        val bottomPadding =
            if (lastRow) {
                24.dp
            } else {
                12.dp
            }
        Row(
            Modifier
                .padding(24.dp, 24.dp, 24.dp, bottomPadding)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = name,
                    fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold)),
                    fontSize = 16.sp,
                    letterSpacing = 0.sp,
                    color = wpdsColors.gray20,
                )
                Text(
                    text = description,
                    fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_light)),
                    fontSize = 14.sp,
                    letterSpacing = 0.sp,
                    color = wpdsColors.gray80,
                )
            }
            Switch(
                modifier = Modifier.align(Alignment.CenterVertically),
                checked = enabledState,
                onCheckedChange = {
                    if (toggleable is Newsletter) {
                        topicFollowBottomSheetViewModel.dispatchTopicFollowEvent(
                            TopicFollowEvent.NewsletterToggled,
                        )
                    } else if (toggleable is Notification) {
                        if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                            topicFollowBottomSheetViewModel.dispatchTopicFollowEvent(
                                TopicFollowEvent.NotificationToggled,
                            )
                        } else {
                            pendingNotificationToggle = true
                            showNotificationsBlockedDialog()
                        }
                    }
                },
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = wpdsColors.toggleCheckedThumb,
                        checkedTrackColor = wpdsColors.toggleCheckedTrack,
                        uncheckedThumbColor = wpdsColors.toggleUncheckedThumb,
                        uncheckedTrackColor = wpdsColors.toggleUncheckedTrack,
                    ),
            )
        }
    }

    private fun showNotificationsBlockedDialog() {
        context?.let {
            val alertDialog = AlertDialog.Builder(context).create()
            alertDialog.setTitle(
                it.resources.getString(
                    com.washingtonpost.android.notifications.R.string.notifications_blocked_title,
                ),
            )
            alertDialog.setMessage(
                it.resources.getString(
                    com.washingtonpost.android.notifications.R.string.notifications_blocked_message,
                ),
            )
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL,
                it.resources.getString(
                    com.washingtonpost.android.notifications.R.string.go_settings_message,
                ),
            ) { _, _ ->
                launchSystemAppSettings()
            }
            alertDialog.setButton(
                AlertDialog.BUTTON_NEGATIVE,
                it.resources.getString(
                    com.washingtonpost.android.notifications.R.string.cancelLabel,
                ),
            ) { _, _ ->
                alertDialog.cancel()
            }

            notificationsBlockedDialog = alertDialog

            alertDialog.show()
        }
    }

    private fun launchSystemAppSettings() {
        context?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, it.packageName),
                )
            } else {
                it.startActivity(
                    Intent()
                        .setClassName(
                            "com.android.settings",
                            "com.android.settings.Settings\$AppNotificationSettingsActivity",
                        ).putExtra("app_package", it.packageName)
                        .putExtra("app_uid", it.applicationInfo.uid)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),
                )
            }
        }
    }

    companion object {
        private val TAG = TopicFollowBottomSheetFragment::class.java.simpleName
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
private fun Preview() {
    AndroidClassicTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = wpdsColors.secondary,
        ) {
            TopicFollowBottomSheetFragment(
                "",
                Followable(
                    false,
                    "Election 2024",
                    "Follow Election 2024",
                    "Election 2024",
                    "Election 2024",
                    "https://www.washingtonpost.com/wp-stat/wpds-icons/icon-election.png",
                    "You'll see more coverage on this topic in your recommendations.",
                    "Follow to see more coverage on this topic in your recommendations.",
                    listOf(
                        Newsletter(
                            "5894c84b-ff69-410d-b805-0796c9f10ffe",
                            "The Campaign Moment",
                            "Analysis delivered to your inbox weekly",
                        ),
                    ),
                    listOf(
                        Notification(
                            "politics",
                            "Politics",
                            "One to three alerts daily",
                        ),
                    ),
                    listOf(
                        Bullet(
                            "for-you",
                            null,
                            "Discover more elctions coverage",
                        ),
                        Bullet(
                            "email",
                            null,
                            "Get the weekly Campaign Moment newsletter",
                        ),
                        Bullet(
                            "bell",
                            null,
                            "Set alerts for major election developments",
                        ),
                    ),
                ),
                "",
            )
        }
    }
}
