/*
 *  Copyright (c) 2024 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.placeholder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter.ArticleItemLowDataModeViewHolder.PlaceHolderState
import com.washingtonpost.android.sections.R
import com.wpds.theme.AndroidClassicTheme

@Composable
fun PlaceHolderView(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    data: PlaceHolderData,
    state: LiveData<PlaceHolderState>,
    onLoad: () -> Unit,
) {
    val stateValue: PlaceHolderState? by state.observeAsState()

    if (stateValue !is PlaceHolderState.PlaceHolderDisable) {
        BoxWithConstraints(
            modifier
                .background(color = colorResource(com.wapo.view.R.color.wapo_views_gray))
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .modifyIf(data.aspectRatio != null && data.aspectRatio > 0.0) {
                    aspectRatio(data.aspectRatio!!)
                }.clickable {
                    onLoad()
                },
        ) {
            if (stateValue !is PlaceHolderState.PlaceHolderLoading) {
                val boxHeight = this.maxHeight
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .height(boxHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        modifier =
                            Modifier
                                .padding(16.dp),
                        textAlign = TextAlign.Center,
                        text = data.message,
                        fontSize = 18.sp,
                    )
                }
                data.reloadLabel?.let {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .height(boxHeight),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Button(
                            modifier =
                                Modifier
                                    .padding(
                                        top = 0.dp,
                                        bottom = 10.dp,
                                        start = 0.dp,
                                        end = 0.dp,
                                    ),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black),
                            onClick = onLoad,
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .wrapContentHeight(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                data.reloadIcon?.let {
                                    Image(
                                        modifier =
                                            Modifier
                                                .padding(
                                                    top = 0.dp,
                                                    bottom = 0.dp,
                                                    start = 0.dp,
                                                    end = 8.dp,
                                                ).size(12.dp),
                                        painter = painterResource(it),
                                        contentDescription =
                                            stringResource(
                                                id = R.string.low_data_mode_icon_description,
                                            ),
                                    )
                                }
                                Text(
                                    text = data.reloadLabel,
                                    color = colorResource(com.wapo.flagship.features.audio.R.color.white),
                                    fontSize = 18.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.modifyIf(
    condition: Boolean,
    modify: Modifier.() -> Modifier,
) = if (condition) modify() else this

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PlaceHolderViewPreview() {
    AndroidClassicTheme {
        PlaceHolderView(
            isVisible = true,
            data =
                PlaceHolderData(
                    message = "Live Image hidden to minimize data usage. Tap to view.",
                    reloadLabel = "Load live image",
                    reloadIcon = com.wpds.wpds.R.drawable.play,
                    aspectRatio = 0.4F,
                ) {},
            state = MutableLiveData(PlaceHolderState.PlaceHolderEnable),
        ) {}
    }
}
