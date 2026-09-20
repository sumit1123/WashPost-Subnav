/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.lowdatamodelbanner.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.washingtonpost.android.sections.R

@Composable
fun LowDataBannerView(modifier: Modifier = Modifier, isVisible: Boolean, onTurnOffClick: () -> Unit) {
    if (isVisible) {
        Row(
            modifier = modifier
                .background(color = colorResource(R.color.low_data_banner_background))
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.low_data_mode_title),
                color = colorResource(com.wapo.flagship.features.audio.R.color.white),
                fontSize = 16.sp,
                lineHeight = 17.sp
            )

            OutlinedButton(
                border = BorderStroke(1.dp, colorResource(com.wapo.flagship.features.audio.R.color.white)),
                onClick = onTurnOffClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(R.color.low_data_banner_background),
                )
            ) {
                Text(
                    text = stringResource(id = R.string.low_data_mode_button),
                    color = colorResource(com.wapo.flagship.features.audio.R.color.white),
                    fontSize = 14.sp
                )
            }
        }
    }
}
