package com.wapo.flagship.features.personalizedpodcasts.fragments

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.personalizedpodcasts.model.DialogueSegment
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import com.wpds.utils.disableParentScroll
import com.wpds.utils.verticalFadingEdge

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun TranscriptView(
    transcript: List<DialogueSegment>?,
    artworkUrl: String?,
    title: String?,
    onClose: () -> Unit
) {
    AndroidClassicTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(wpdsColors.mediaPlayerBackground)
        ) {
            // --- MINI HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .clickable(onClick = onClose),
                verticalAlignment = Alignment.Top
            ) {
                if (artworkUrl != null) {
                    GlideImage(
                        model = artworkUrl,
                        contentDescription = "Podcast Artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.LightGray)
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = title ?: stringResource(R.string.podcast),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.basicMarquee(),
                        color = wpdsColors.onSurface
                    )
                    Text(
                        text = stringResource(R.string.ai_generated_podcast),
                        style = MaterialTheme.typography.bodySmall,
                        color = wpdsColors.gray80
                    )
                }
            }

            // --- TRANSCRIPT LIST ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .disableParentScroll()
                    .verticalFadingEdge(topFadeHeight = 60.dp, bottomFadeHeight = 60.dp),
                contentPadding = PaddingValues(48.dp, 36.dp, 48.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (transcript != null) {
                    items(transcript) { segment ->
                        TranscriptItem(segment)
                    }
                }
            }
        }
    }
}

@Composable
fun TranscriptItem(segment: DialogueSegment) {
    AndroidClassicTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${segment.speaker}:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp),
                fontSize = 24.sp,
                lineHeight = 38.4.sp,
                textAlign = TextAlign.Start,
                fontFamily = FontFamily(Font(com.wpds.wpds.R.font.georgia_regular)),
                color = wpdsColors.gray0,
            )
            Text(
                text = segment.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 24.sp,
                    lineHeight = 38.4.sp,
                    textAlign = TextAlign.Start,
                    fontFamily = FontFamily(Font(com.wpds.wpds.R.font.georgia_regular)),
                    color = wpdsColors.gray0,
                )
            )
        }
    }
}