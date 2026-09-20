package com.wapo.flagship.features.audio.fragments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.Utils
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.audio.config2.LabelStyle
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.config2.getStyle
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.models.MediaItemData
import com.wapo.flagship.features.audio.utils.AudioViewUtils
import com.wapo.flagship.features.personalizedpodcasts.fragments.TranscriptView
import com.wapo.flagship.features.personalizedpodcasts.viewmodel.PodcastGenerationState
import com.wapo.flagship.features.tts.domain.TtsState
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PODCAST
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.PostiniFontFamily
import com.wpds.theme.wpdsColors
import com.wpds.utils.DevicePreviews
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Main Composable for the Audio Player.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AudioPlayerScreen(
    state: AudioPlayerUiState,
    onEvent: (AudioPlayerUiEvent) -> Unit,
    player: Player?,
    modifier: Modifier = Modifier,
) {
    val mediaItemData = state.mediaItemData
    val isGenerating =
        mediaItemData?.audioType == PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER

    val density = LocalDensity.current
    val view = LocalView.current
    val horizontalPadding = dimensionResource(id = R.dimen.podcast_control_view_margin_horizontal)
    AndroidClassicTheme {

        val isSubViewVisible = state.isPlaylistVisible || state.isTranscriptVisible
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (!isSubViewVisible) {
                        Modifier
                            .wrapContentHeight()
                            .verticalScroll(rememberScrollState())
                    } else {
                        Modifier.wrapContentHeight()
                    }
                )
                .background(colorResource(id = R.color.podcast_background))
        ) {
            TopBarDragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

            // Container for both Content and Footer that we lock the height of
            @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isSubViewVisible && state.lockedHeightPx != null) {
                            val lockedHeightDp = with(density) { state.lockedHeightPx.toDp() }
                            Modifier.height(lockedHeightDp)
                        } else if (isSubViewVisible)
                            Modifier.weight(1f)
                        else
                            Modifier.wrapContentHeight()
                    )
                    .onSizeChanged { size ->
                        // Only capture height when we are in the "Main" state (not transcript, playlist, or generating)
                        if (!state.isTranscriptVisible && !state.isPlaylistVisible && !isGenerating && size.height > 0) {
                            onEvent(AudioPlayerUiEvent.HeightMeasured(size.height.toFloat()))
                        }
                    }
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSubViewVisible)
                                Modifier.weight(1f, fill = true)
                            else
                                Modifier
                        )
                ) {
                    when {
                        state.nowPlayingItem?.audioPlaybackState is AudioPlaybackState.Error -> {
                            ErrorStateView(
                                error = state.nowPlayingItem.audioPlaybackState as AudioPlaybackState.Error,
                                modifier = Modifier.padding(horizontal = horizontalPadding)
                            )
                        }

                        state.isTranscriptVisible -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = horizontalPadding)
                            ) {
                                TranscriptView(
                                    transcript = state.transcript,
                                    artworkUrl = mediaItemData?.albumArtUrl,
                                    title = mediaItemData?.title,
                                    onClose = { onEvent(AudioPlayerUiEvent.TranscriptToggle) }
                                )
                            }
                        }

                        state.isPlaylistVisible -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = horizontalPadding)
                            ) {
                                PlaylistView(
                                    nowPlayingItem = state.nowPlayingItem,
                                    upcomingItems = state.upcomingItems,
                                    onItemClicked = { onEvent(AudioPlayerUiEvent.ItemClicked(it)) },
                                    onEllipsisClicked = { onEvent(AudioPlayerUiEvent.EllipsisClicked(it)) },
                                    onClosePlaylist = { onEvent(AudioPlayerUiEvent.PlaylistToggle) }
                                )
                            }
                        }

                        else -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                if (isGenerating) {
                                    GeneratingStateView(
                                        generationState = state.generationState,
                                        mediaItemData = mediaItemData,
                                        onTitleClicked = { onEvent(AudioPlayerUiEvent.TitleClicked(it)) },
                                        isPersonalizedPodcast = state.isPersonalizedPodcast,
                                        horizontalPadding = horizontalPadding
                                    )
                                } else {
                                    MainPlayerContent(
                                        mediaItemData = mediaItemData,
                                        isPlayingAd = state.adUiState.isPlayingAd,
                                        onAskSamClicked = { onEvent(AudioPlayerUiEvent.AskSamClicked) },
                                        onTitleClicked = { onEvent(AudioPlayerUiEvent.TitleClicked(it)) },
                                        isPersonalizedPodcast = state.isPersonalizedPodcast,
                                        horizontalPadding = horizontalPadding
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                PlayerFooter(
                    player = player,
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                )

            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TopBarDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = dimensionResource(id = R.dimen.podcast_vertical_margin_medium))
            .size(
                width = dimensionResource(id = R.dimen.dialog_top_bar_width),
                height = dimensionResource(id = R.dimen.dialog_top_bar_height)
            )
            .background(
                color = Color(0xFFE2E2E2),
                shape = RoundedCornerShape(5.dp)
            )
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MainPlayerContent(
    mediaItemData: MediaItemData?,
    isPlayingAd: Boolean,
    onAskSamClicked: () -> Unit,
    onTitleClicked: (MediaItemData) -> Unit,
    isPersonalizedPodcast: Boolean,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier
) {
    if (mediaItemData == null) return

    val playerType = try {
        PlayerType.valueOf(mediaItemData.playerTypeName ?: PlayerType.UNKNOWN.name)
    } catch (e: Exception) {
        PlayerType.UNKNOWN
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(id = R.dimen.podcast_vertical_margin_large))
    ) {
        PlayerContent(
            mediaItemData = mediaItemData,
            isPlayingAd = isPlayingAd,
            isPersonalizedPodcast = isPersonalizedPodcast,
            horizontalPadding = horizontalPadding,
            onTitleClicked = onTitleClicked
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.podcast_vertical_margin_medium)))
        ArtworkImage(
            mediaItemData = mediaItemData,
            playerType = playerType,
            horizontalPadding = horizontalPadding,
            isPlayingAd = isPlayingAd,
        )
        // Image Caption
        if (!mediaItemData.imageCaption.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mediaItemData.imageCaption,
                color = colorResource(id = R.color.podcast_caption_color),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
            )
        }

        // Ask Sam button
        if (isPersonalizedPodcast) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.podcast_vertical_margin_large)))
            Button(
                onClick = onAskSamClicked,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = horizontalPadding),
                colors = ButtonDefaults.buttonColors(
                    containerColor = wpdsColors.onSurface,
                    contentColor = wpdsColors.surface
                ),
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(
                    painter = painterResource(id = com.wpds.wpds.R.drawable.soundwave),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.ask_a_question), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PlayerContent(
    mediaItemData: MediaItemData,
    isPlayingAd: Boolean,
    isPersonalizedPodcast: Boolean,
    horizontalPadding: Dp,
    onTitleClicked: (MediaItemData) -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Label area
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
        ) {
            if (!isPlayingAd && !mediaItemData.primaryLabel.isNullOrEmpty()) {
                if (getStyle(mediaItemData.style) == LabelStyle.OPINIONS) {
                    OpinionsLabel(
                        text = mediaItemData.primaryLabel,
                        fontSize = 16.sp
                    )
                } else {
                    Text(
                        text = mediaItemData.primaryLabel,
                        color = colorResource(id = R.color.podcast_text_color),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (!mediaItemData.secondaryLabel.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = mediaItemData.secondaryLabel,
                    color = colorResource(id = R.color.podcast_date_color),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.podcast_vertical_margin_medium)))

        // Title
        Text(
            text = if (isPlayingAd) stringResource(R.string.advertisement) else mediaItemData.title,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            fontFamily = PostiniFontFamily,
            color = colorResource(id = R.color.podcast_title_color),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .clickable { onTitleClicked(mediaItemData) }
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.podcast_vertical_margin_medium)))

        if (isPlayingAd) {
            Text(
                text = stringResource(R.string.now_playing_ad_2),
                color = colorResource(id = R.color.podcast_date_color),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
            )
        } else {
            // Date/Caption
            val date = mediaItemData.displayDate?.trim()?.toLongOrNull()
            if (date != null && date > 0L) {
                val formattedDate = Utils.getDate("MMMM dd, yyyy", date) ?: ""
                val dateText =
                    if (isPersonalizedPodcast) {
                        stringResource(id = R.string.created_for_you_on, formattedDate)
                    } else {
                        formattedDate
                    }
                Text(
                    text = dateText,
                    color = colorResource(id = R.color.podcast_date_color),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                )
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ArtworkImage(
    mediaItemData: MediaItemData,
    playerType: PlayerType,
    horizontalPadding: Dp,
    isPlayingAd: Boolean,
) {
    if (isPlayingAd) {
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 2f))
        return
    }

    // Image Handling based on PlayerType
    if (mediaItemData.albumArtUrl.isNullOrEmpty() || playerType == PlayerType.STANDALONE) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .aspectRatio(3f / 2f)
                .clip(RoundedCornerShape(8.dp))
                .background(colorResource(id = R.color.podcast_background)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_polly_icon),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.6f),
                tint = Color.Unspecified
            )
        }
    } else {
        GlideImage(
            model = mediaItemData.albumArtUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f),
            contentScale = if (playerType == PlayerType.PODCAST) ContentScale.Fit else ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun GeneratingStateView(
    generationState: PodcastGenerationState?,
    mediaItemData: MediaItemData?,
    onTitleClicked: (MediaItemData) -> Unit,
    isPersonalizedPodcast: Boolean,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier
) {
    if (mediaItemData == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = dimensionResource(id = R.dimen.podcast_vertical_margin_large)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerContent(
            mediaItemData = mediaItemData,
            isPlayingAd = false,
            isPersonalizedPodcast = isPersonalizedPodcast,
            horizontalPadding = horizontalPadding,
            onTitleClicked = onTitleClicked
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.podcast_vertical_margin_medium)))

        // Artwork in the middle (full width)
        GlideImage(
            model = mediaItemData.albumArtUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.podcast_vertical_margin_large)))

        GeneratingStatusInfo(
            generationState = generationState,
            horizontalPadding = horizontalPadding
        )
    }
}

@Composable
fun GeneratingStatusInfo(
    generationState: PodcastGenerationState?,
    horizontalPadding: Dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Status info at the bottom
        Text(
            text = stringResource(id = R.string.creating_your_podcast),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = colorResource(id = R.color.podcast_text_color),
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (generationState == PodcastGenerationState.Done) {
            Icon(
                painter = painterResource(id = R.drawable.podcast_generated_success),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(48.dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = colorResource(id = R.color.podcast_time_bar_played_color),
                strokeWidth = 2.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = generationState?.message ?: "",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = colorResource(id = R.color.podcast_caption_color),
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun PlaylistView(
    nowPlayingItem: NowPlayingAudioItem?,
    upcomingItems: List<MediaItemData>,
    onItemClicked: (MediaItemData) -> Unit,
    onEllipsisClicked: (MediaItemData) -> Unit,
    onClosePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Now Playing Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClosePlaylist() }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.Top
        ) {
            GlideImage(
                model = nowPlayingItem?.mediaItemData?.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(79.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val nowPlayingMediaData = nowPlayingItem?.mediaItemData
                if (!nowPlayingMediaData?.primaryLabel.isNullOrEmpty()) {
                    if (getStyle(nowPlayingMediaData!!.style) == LabelStyle.OPINIONS) {
                        OpinionsLabel(
                            text = nowPlayingMediaData.primaryLabel!!,
                            fontSize = 16.sp
                        )
                    } else {
                        Text(
                            text = nowPlayingMediaData.primaryLabel!!,
                            fontSize = 16.sp,
                            color = colorResource(id = R.color.podcast_text_color),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = nowPlayingItem?.mediaItemData?.title ?: "",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.podcast_title_color),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_playing_state),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.now_playing),
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.podcast_title_color),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // hide ellipsis menu for personalized podcast because it contains no items
                    if (!PersonalizedPodcastHelper.isPersonalizedPodcastItem(nowPlayingItem?.mediaItemData?.audioType)) {
                        IconButton(onClick = { nowPlayingItem?.mediaItemData?.let { onEllipsisClicked(it) } }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ellipsis_playlist),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFD4D4D4), thickness = 1.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.up_next),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.podcast_title_color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = pluralStringResource(R.plurals.items, upcomingItems.size, upcomingItems.size),
                fontSize = 14.sp,
                color = colorResource(id = R.color.podcast_caption_color)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            itemsIndexed(upcomingItems) { index, item ->
                PlaylistItemRow(
                    item = item,
                    onItemClicked = { onItemClicked(item) },
                    onEllipsisClicked = { onEllipsisClicked(item) }
                )
                if (index < upcomingItems.size - 1) {
                    HorizontalDivider(color = Color(0xFFD4D4D4), thickness = 1.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun PlaylistItemRow(
    item: MediaItemData,
    onItemClicked: () -> Unit,
    onEllipsisClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClicked() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        GlideImage(
            model = item.albumArtUrl,
            contentDescription = null,
            modifier = Modifier
                .size(60.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (!item.primaryLabel.isNullOrEmpty()) {
                if (getStyle(item.style) == LabelStyle.OPINIONS) {
                    OpinionsLabel(
                        text = item.primaryLabel,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = item.primaryLabel,
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.podcast_text_color),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = colorResource(id = R.color.podcast_title_color),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            item.duration?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.audio_play_icon_playlist),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.Unspecified
                    )
                    if (it > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AudioViewUtils.getDurationForPlaylist(
                                item.duration,
                                LocalContext.current
                            ),
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.podcast_caption_color)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onEllipsisClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ellipsis_playlist),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerFooter(
    player: Player?,
    state: AudioPlayerUiState,
    onEvent: (AudioPlayerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPodcast = state.mediaItemData?.audioType == PODCAST
    val isGenerating =
        state.mediaItemData?.audioType == PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        if (!isGenerating) {
            PlayerController(
                player = player,
                adState = state.adUiState,
                ttsState = state.ttsState,
                onPlayPauseClicked = { onEvent(AudioPlayerUiEvent.PauseOrPlay) },
                onTrackChanged = { onEvent(AudioPlayerUiEvent.TrackChanged) },
                onSkipAd = { onEvent(AudioPlayerUiEvent.SkipAdClicked) },
                modifier = Modifier.fillMaxWidth()
            )

            val isTts = state.ttsState?.isSpeaking == true || state.ttsState?.isPause == true
            if (!state.isTranscriptVisible && !isTts) {
                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPodcast) {
                        OutlinedButton(
                            onClick = { onEvent(AudioPlayerUiEvent.CreateClicked) },
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.height(40.dp),
                            border = BorderStroke(
                                1.dp,
                                colorResource(id = R.color.round_border_button_stoke_bg)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colorResource(id = R.color.podcast_background),
                                contentColor = colorResource(id = R.color.app_surface)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = com.wpds.wpds.R.drawable.ic_edit),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.create), fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Speed Button
                    val isPlayingAd = state.adUiState.isPlayingAd
                    val isEnabled = !isPlayingAd
                    val disabledColor = colorResource(id = R.color.button_disabled)
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .clickable(enabled = isEnabled) { onEvent(AudioPlayerUiEvent.SpeedClicked) }
                            .border(
                                1.dp,
                                colorResource(id = R.color.round_border_button_stoke_bg),
                                RoundedCornerShape(100.dp)
                            ),
                        color = colorResource(id = R.color.podcast_background)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.speed_control_key),
                                fontSize = 12.sp,
                                color = if (isEnabled) colorResource(id = R.color.round_button_text) else disabledColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            VerticalDivider(
                                modifier = Modifier
                                    .height(16.dp)
                                    .width(1.dp),
                                color = if (isEnabled) colorResource(id = R.color.round_button_key_value_divider) else disabledColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = state.playbackSpeedText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) colorResource(id = R.color.round_button_value_text) else disabledColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Playlist Toggle
                    if (!isPodcast) {
                        IconButton(
                            onClick = { onEvent(AudioPlayerUiEvent.PlaylistToggle) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = if (state.isPlaylistVisible) R.drawable.ic_playlist_active else R.drawable.ic_playlist),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        // Overflow Menu
                        PodcastMenu(
                            onEvent = { onEvent(AudioPlayerUiEvent.PodcastMenuAction(it)) },
                            isFeedbackEnabled = state.isFeedbackEnabled
                        )
                    }
                }

                if (isPodcast) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = com.wpds.wpds.R.drawable.ai),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colorResource(id = com.wpds.wpds.R.color.gray100)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.disclaimer),
                            fontSize = 12.sp,
                            color = colorResource(id = com.wpds.wpds.R.color.gray100),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerController(
    player: Player?,
    adState: AudioAdUiState,
    ttsState: TtsState?,
    onPlayPauseClicked: () -> Unit = {},
    onTrackChanged: () -> Unit = {},
    onSkipAd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(player?.isPlaying ?: false) }
    var currentPosition by remember { mutableStateOf(player?.currentPosition ?: 0L) }
    var duration by remember { mutableStateOf(player?.duration?.coerceAtLeast(0L) ?: 0L) }
    var hasPrevious by remember { mutableStateOf(player?.hasPreviousMediaItem() ?: false) }
    var hasNext by remember { mutableStateOf(player?.hasNextMediaItem() ?: false) }
    val isTts = ttsState?.isSpeaking == true || ttsState?.isPause == true

    val interactionSource =
        remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    // Update state from player
    DisposableEffect(player) {
        val currentPlayer = player
        if (currentPlayer == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    isPlaying = player.isPlaying
                    currentPosition = player.currentPosition
                    duration = player.duration.coerceAtLeast(0L)
                    hasPrevious = player.hasPreviousMediaItem()
                    hasNext = player.hasNextMediaItem()
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    currentPosition = newPosition.positionMs
                }
            }
            currentPlayer.addListener(listener)
            onDispose {
                currentPlayer.removeListener(listener)
            }
        }
    }

    LaunchedEffect(player, isPlaying) {
        if (player == null || !isPlaying) return@LaunchedEffect

        while (isActive) {
            currentPosition = player.currentPosition
            delay(500)
        }
    }

    val isSeekable = duration > 0
    var sliderPosition by remember(duration) {
        mutableFloatStateOf(
            if (isSeekable) currentPosition.toFloat() / duration.toFloat() else 0f
        )
    }
    var isUserSeeking by remember { mutableStateOf(false) }
    LaunchedEffect(currentPosition, duration) {
        if (!isUserSeeking && isSeekable) {
            val clampedPosition = currentPosition.coerceIn(0L, duration)
            sliderPosition = clampedPosition.toFloat() / duration.toFloat()
        } else if (!isSeekable) {
            sliderPosition = 0f
        }
    }
    Column(modifier = modifier) {
        if (!isTts) {
            // Custom Styled Slider
            Slider(
                value = sliderPosition,
                onValueChange = {
                    isUserSeeking = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    if (isSeekable) {
                        player?.seekTo((sliderPosition * duration).toLong())
                    }
                    isUserSeeking = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                interactionSource = interactionSource,
                track = { sliderState ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(
                                color = wpdsColors.gray500,
                                shape = RoundedCornerShape(3.dp)
                            )
                    ) {
                        val thumbOffset = (sliderState.value - sliderState.valueRange.start) /
                                (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(thumbOffset)
                                .background(
                                    color = colorResource(R.color.podcast_time_bar_scrubber_bar_color),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                },
                thumb = {
                    Box(
                        modifier = Modifier
                            .indication(interactionSource = interactionSource, indication = null)
                            .size(20.dp)
                            .shadow(
                                elevation = 2.dp,
                                shape = CircleShape,
                                ambientColor = Color(0xFF000000).copy(alpha = 0.5f),
                                spotColor = Color(0xFF000000).copy(alpha = 0.5f)
                            )
                            .background(
                                color = wpdsColors.gray700,
                                shape = CircleShape
                            )
                    )
                },
                enabled = !adState.isPlayingAd,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    fontSize = 12.sp,
                    color = colorResource(id = R.color.podcast_text_color)
                )
                Text(
                    text = formatTime(duration),
                    fontSize = 12.sp,
                    color = colorResource(id = R.color.podcast_text_color)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Buttons
        if (adState.isPlayingAd) {
            PlayerAdButtons(
                player = player,
                isPlaying = isPlaying,
                adState = adState,
                onTrackChanged = onTrackChanged,
                onSkipAd = onSkipAd,
            )
        } else {
            PlayerButtons(
                player = player,
                onTrackChanged = onTrackChanged,
                isPlaying = isPlaying,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                ttsState = ttsState,
                onPlayPauseClicked = onPlayPauseClicked
            )
        }
    }
}

@Composable
fun PlayerButtons(
    player: Player?,
    onTrackChanged: () -> Unit = {},
    ttsState: TtsState?,
    onPlayPauseClicked: () -> Unit = {},
    isPlaying: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val disableByTts = ttsState?.isSpeaking == true || ttsState?.isPause == true
        // Rewind 15s
        IconButton(
            onClick = { player?.seekBack() },
            enabled = !disableByTts
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_podcast_btn_rew),
                contentDescription = "Rewind 15 seconds",
                tint = if (disableByTts) colorResource(id = R.color.button_disabled) else Color.Unspecified
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Previous Track
        IconButton(
            onClick = {
                player?.seekToPrevious()
                onTrackChanged()
            },
            enabled = hasPrevious
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_podcast_btn_prev),
                contentDescription = "Previous Track",
                tint = if (hasPrevious) Color.Unspecified else colorResource(id = R.color.button_disabled)
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Play/Pause
        IconButton(
            onClick = {
                if (isPlaying) player?.pause() else player?.play()
                onPlayPauseClicked()
            },
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying || ttsState?.isSpeaking == true) R.drawable.podcast_btn_pause else R.drawable.podcast_btn_play
                ),
                contentDescription = if (isPlaying || ttsState?.isSpeaking == true) "Pause" else "Play",
                modifier = Modifier.fillMaxSize(),
                tint = Color.Unspecified
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Next Track
        IconButton(
            onClick = {
                player?.seekToNext()
                onTrackChanged()
            },
            enabled = hasNext
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_podcast_btn_next),
                contentDescription = "Next Track",
                tint = if (hasNext) Color.Unspecified else colorResource(id = R.color.button_disabled)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Fast Forward 15s
        IconButton(
            onClick = { player?.seekForward() },
            enabled = !disableByTts
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_podcast_btn_ffwd),
                contentDescription = "Fast forward 15 seconds",
                tint =  if (disableByTts) colorResource(id = R.color.button_disabled) else Color.Unspecified
            )
        }
    }
}

@Composable
fun PlayerAdButtons(
    player: Player?,
    isPlaying: Boolean,
    adState: AudioAdUiState,
    onTrackChanged: () -> Unit = {},
    onSkipAd: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        // Play/Pause
        IconButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = {
                if (isPlaying) player?.pause() else player?.play()
            },
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.podcast_btn_pause else R.drawable.podcast_btn_play
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.fillMaxSize(),
                tint = Color.Unspecified
            )
        }

        // Skip ad
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(horizontal = 16.dp)
                .clickable(enabled = adState.isSkipAdEnabled) {
                    onSkipAd()
                    onTrackChanged()
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fgColor = if (adState.isSkipAdEnabled) wpdsColors.primary else colorResource(id = R.color.button_disabled)
            Text(
                text = stringResource(R.string.skip_ad),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(com.wpds.wpds.R.font.franklinitcstd_bold)),
                color = fgColor,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.ic_podcast_btn_next),
                contentDescription = "Skip ad",
                tint = fgColor,
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun PodcastMenu(
    onEvent: (PodcastMenuUIEvent) -> Unit,
    isFeedbackEnabled: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 50.dp, height = 40.dp)
                .clip(CircleShape)
                .background(colorResource(id = R.color.podcast_background))
                .border(
                    border = BorderStroke(
                        width = 1.dp,
                        color = colorResource(id = R.color.round_border_button_stoke_bg)
                    ),
                    shape = CircleShape
                )
                .clickable {
                    onEvent(PodcastMenuUIEvent.MenuOpened)
                    expanded = true
                }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_ellipsis),
                contentDescription = "More options",
                tint = colorResource(id = R.color.app_surface)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = wpdsColors.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.widthIn(min = 250.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.share_btn),
                        modifier = Modifier.padding(end = 32.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = wpdsColors.onSurface
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = androidx.media3.session.R.drawable.media3_icon_share),
                        contentDescription = "Logo",
                        tint = wpdsColors.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                onClick = {
                    expanded = false
                    onEvent(PodcastMenuUIEvent.Share)
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.sources),
                        modifier = Modifier.padding(end = 32.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = wpdsColors.onSurface
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = com.wapo.view.R.drawable.ic_sources),
                        contentDescription = "Logo",
                        tint = wpdsColors.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                onClick = {
                    expanded = false
                    onEvent(PodcastMenuUIEvent.Sources)
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.transcript),
                        modifier = Modifier.padding(end = 32.dp),
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        color = wpdsColors.onSurface,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = com.wpds.wpds.R.drawable.ic_comment_elipse),
                        contentDescription = "Logo",
                        tint = wpdsColors.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                onClick = {
                    expanded = false
                    onEvent(PodcastMenuUIEvent.Transcript)
                }
            )
            if (isFeedbackEnabled) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.feedback),
                            modifier = Modifier.padding(end = 32.dp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = wpdsColors.onSurface,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = com.wpds.wpds.R.drawable.ic_megaphone),
                            contentDescription = "Logo",
                            tint = wpdsColors.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    onClick = {
                        expanded = false
                        onEvent(PodcastMenuUIEvent.Feedback)
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.up_next),
                        modifier = Modifier.padding(end = 32.dp),
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        color = wpdsColors.onSurface
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_playlist_active),
                        contentDescription = "Logo",
                        tint = wpdsColors.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                onClick = {
                    expanded = false
                    onEvent(PodcastMenuUIEvent.UpNext)
                }
            )
        }
    }
}

@Composable
fun ErrorStateView(
    error: AudioPlaybackState.Error,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.press_to_minimize),
            fontSize = 12.sp,
            color = colorResource(id = R.color.podcast_text_color),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = error.errorMessage?.toString() ?: stringResource(id = R.string.error_message),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = colorResource(id = R.color.podcast_text_color),
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(id = R.string.error_description),
            fontSize = 16.sp,
            color = colorResource(id = R.color.podcast_text_color),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PreviewAudioPlayerScreen() {
    val sampleMediaItem = MediaItemData(
        mediaId = "123",
        title = "The Seven: A daily news podcast",
        primaryLabel = "The Seven",
        secondaryLabel = "Feb 23, 2026",
        albumArtUrl = "https://example.com/image.jpg",
        playerTypeName = "PODCAST",
        playbackState = 0,
        audioType = "podcast"
    )
    val state = AudioPlayerUiState(
        mediaItemData = sampleMediaItem,
        playbackSpeedText = "1.5x"
    )
    AudioPlayerScreen(
        state = state,
        onEvent = {},
        player = null
    )
}

@Composable
@DevicePreviews
fun PreviewAudioAdScreen() {
    val sampleMediaItem = MediaItemData(
        mediaId = "123",
        title = "The Seven: A daily news podcast",
        primaryLabel = "The Seven",
        secondaryLabel = "Feb 23, 2026",
        albumArtUrl = "https://example.com/image.jpg",
        playerTypeName = "PODCAST",
        playbackState = 0,
        audioType = "podcast"
    )
    val state = AudioPlayerUiState(
        mediaItemData = sampleMediaItem,
        playbackSpeedText = "1.5x",
        adUiState = AudioAdUiState(
            isPlayingAd = true,
            isSkipAdEnabled = false
        )
    )
    AudioPlayerScreen(
        state = state,
        onEvent = {},
        player = null
    )
}

@Composable
fun PreviewPlaylistView() {
    val sampleMediaItem = MediaItemData(
        mediaId = "123",
        title = "The Seven: A daily news podcast",
        primaryLabel = "The Seven",
        secondaryLabel = "Feb 23, 2026",
        albumArtUrl = "https://example.com/image.jpg",
        playbackState = 0
    )
    val upcomingItems = listOf(
        MediaItemData(
            mediaId = "456",
            title = "Another Podcast Episode",
            displayDate = "Feb 22, 2026",
            playbackState = 0
        ),
        MediaItemData(
            mediaId = "789",
            title = "Weekly Review",
            displayDate = "Feb 21, 2026",
            playbackState = 0
        )
    )
    PlaylistView(
        nowPlayingItem = NowPlayingAudioItem(
            playlistId = "playlist_1",
            audioMediaConfigItemIndex = 0,
            audioMediaConfig = null,
            nowPlayingItemIndex = 0,
            mediaItemData = sampleMediaItem,
            audioPlaybackState = AudioPlaybackState.None
        ),
        upcomingItems = upcomingItems,
        onItemClicked = {},
        onEllipsisClicked = {},
        onClosePlaylist = {}
    )
}

@Composable
fun PreviewGeneratingState() {
    GeneratingStateView(
        generationState = PodcastGenerationState.FindingArticles,
        mediaItemData = null,
        {},
        false,
        0.dp,
        Modifier
    )
}

@Composable
fun PreviewPodcastMenu() {
    Box(modifier = Modifier.padding(16.dp)) {
        PodcastMenu(
            onEvent = {}
        )
    }
}
