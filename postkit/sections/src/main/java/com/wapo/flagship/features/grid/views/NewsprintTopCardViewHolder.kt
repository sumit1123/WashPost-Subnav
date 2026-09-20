package com.wapo.flagship.features.grid.views

import android.content.Context
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridEnvironment
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.newsprint.NewsprintState
import com.wapo.flagship.features.newsprint.NewsprintViewModel
import com.wapo.flagship.features.newsprint.NewsprintViewModel.Companion.VIDEO_2024
import com.washingtonpost.android.sections.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.PostiniFontFamily
import com.wpds.theme.FranklinItcStandardFontFamily
import com.wpds.theme.wpdsColors

class NewsprintTopCardViewHolder(
    itemView: View,
    private val newsprintViewModel: NewsprintViewModel?,
    private val onNewsprintButtonClicked: (() -> Unit)?,
    private val onNewsprintSpanClicked: (() -> Unit)?
): GridViewHolder(itemView) {

    lateinit var context: Context
    lateinit var environment: GridEnvironment
    private val composeView = itemView.findViewById<ComposeView>(R.id.newsprint_compose_view)

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        environment = gridAdapter.environment
        context = gridAdapter.context
        compose()
    }

    private fun compose() {
        composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )
            setContent {
                AndroidClassicTheme {
                    newsprintViewModel?.let { viewModel ->
                        val state by viewModel.topCardState.observeAsState(initial = viewModel.topCardState.value)

                        Surface(
                            color = wpdsColors.newsprintBg
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(
                                    top = 20.dp,
                                    bottom = 32.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                )
                            ) {
                                NewsprintLabel(state)
                                NewsprintTitle(state, viewModel)
                                NewsprintSubtitle(state)
                                NewsprintVisual(viewModel)
                                NewsprintButton(state)
                                NewsprintButtonSubscript(state)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NewsprintLabel(state: NewsprintState?) {
        if (state == NewsprintState.COMPLETED_NEWSPRINT) {
            Text(
                text = "YOUR READER TYPE",
                style = TextStyle(
                    fontFamily = FranklinItcStandardFontFamily,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = wpdsColors.gray700Static,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 16.dp)
            )
        }
    }

    @Composable
    private fun NewsprintTitle(state: NewsprintState?, viewModel: NewsprintViewModel) {
        val text = when (state) {
            NewsprintState.LOW_ENGAGED -> "What's your\nreader type?"
            NewsprintState.HIGH_ENGAGED -> "Your 2024\nNewsprint is here"
            NewsprintState.COMPLETED_NEWSPRINT -> viewModel.readerType.value?.displayName ?: ""
            else -> ""
        }

        if (text.isNotEmpty()) {
            val headlineBold200 = TextStyle(
                fontFamily = PostiniFontFamily,
                fontSize = 32.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = text,
                style = headlineBold200,
                color = wpdsColors.gray700Static,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 16.dp)
            )
        }
    }

    @Composable
    private fun NewsprintSubtitle(state: NewsprintState?) {
        val text = when (state) {
            NewsprintState.LOW_ENGAGED -> "Trailblazer? Deep Diver? Optimizer? Find out with your Washington Post Newsprint."
            NewsprintState.HIGH_ENGAGED -> "Your Newsprint is as unique as your fingerprint. Get stats on your 2024 year in news, and find out your reader type."
            NewsprintState.COMPLETED_NEWSPRINT -> "Discover what we curated just for you and browse all six reader types."
            else -> ""
        }

        if (text.isNotEmpty()) {
            val subtitleTextStyle = TextStyle(
                fontFamily = FranklinItcStandardFontFamily,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )

            Text(
                text = text,
                style = subtitleTextStyle,
                color = wpdsColors.gray700Static,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun NewsprintVisual(viewModel: NewsprintViewModel) {
        val (isPlaying, setIsPlaying) = remember { mutableStateOf(false) }
        val exoPlayer = remember { ExoPlayer.Builder(context).build() }
        val videoUrl by viewModel.videoUrl.observeAsState(initial = viewModel.videoUrl.value)

        LaunchedEffect(videoUrl) {
            val mediaItem = MediaItem.fromUri(videoUrl ?: VIDEO_2024)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE // Loop video
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true // Autoplay
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> setIsPlaying(false)
                        Player.STATE_READY -> setIsPlaying(true)
                        Player.STATE_ENDED -> setIsPlaying(false)
                        else -> {}
                    }
                }
            })
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (!isPlaying) {
                // Placeholder image while video is loading
                AsyncImage(
                    model = viewModel.imageUrl.value,
                    contentDescription = "Newsprint image",
                    modifier = Modifier
                        .width(275.dp)
                        .height(275.dp)
                        .zIndex(1f)
                        .align(Alignment.Center)
                )
            }

            // Video player
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier
                    .width(275.dp)
                    .height(275.dp)
                    .align(Alignment.Center)
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }
    }

    @Composable
    private fun NewsprintButton(state: NewsprintState?) {
        val text = when (state) {
            NewsprintState.LOW_ENGAGED -> "Take 2024 Newsprint quiz"
            NewsprintState.HIGH_ENGAGED -> "Discover my Newsprint"
            NewsprintState.COMPLETED_NEWSPRINT -> "See your curated picks"
            else -> ""
        }

        if (text.isNotEmpty()) {
            val buttonTextStyle = TextStyle(
                fontFamily = FranklinItcStandardFontFamily,
                fontSize = 16.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { newsprintButtonClick() },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = wpdsColors.gray700Static
                ),
                shape = RoundedCornerShape(percent = 50)
            ) {
                Text(
                    text = text,
                    style = buttonTextStyle,
                    color = wpdsColors.newsprintButtonText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                )
            }
        }
    }

    @Composable
    private fun NewsprintButtonSubscript(state: NewsprintState?) {
        if (state == NewsprintState.COMPLETED_NEWSPRINT) {
            val text = buildAnnotatedString {
                append("Or ")
                append(AnnotatedString(
                    text = "revisit my 2024 Newsprint",
                    spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
                ))
            }

            Text(
                text = text,
                style = TextStyle(
                    fontFamily = FranklinItcStandardFontFamily,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                color = wpdsColors.gray700Static,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable { newsprintSpanClick() }
            )
        }
    }

    private fun newsprintButtonClick() {
        onNewsprintButtonClicked?.let { it() }
    }

    private fun newsprintSpanClick() {
        onNewsprintSpanClicked?.let { it() }
    }
}
