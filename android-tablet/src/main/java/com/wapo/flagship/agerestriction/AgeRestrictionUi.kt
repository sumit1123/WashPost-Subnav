/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.agerestriction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.washingtonpost.android.R
import com.wpds.components.CtaButton
import com.wpds.components.HeadlineBold200
import com.wpds.components.MetaLight112
import com.wpds.components.NormalRoundedIcon
import com.wpds.theme.getLightThemeColor
import com.wpds.wptheme.WpTheme

@Composable
fun AgeRestrictionUi(
    ageRestrictionsUIModel: AgeRestrictionsUIModel,
    onCtaButton: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        getLightThemeColor(LocalContext.current, com.wpds.wpds.R.color.gray60),
                        getLightThemeColor(
                            LocalContext.current,
                            com.washingtonpost.android.recirculation.R.color.gray0
                        ),
                    )
                )
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WpTheme.Icon.NormalRoundedIcon(
            iconResource = R.drawable.wp_logo_white,
            iconColor = getLightThemeColor(LocalContext.current, R.color.white),
            iconBackgroundColor = getLightThemeColor(
                LocalContext.current,
                com.wpds.wpds.R.color.gray20
            )
        )
        Spacer(Modifier.height(40.dp))
        WpTheme.Text.HeadlineBold200(
            text = ageRestrictionsUIModel.title,
            textColor = Color.White
        )
        Spacer(Modifier.height(24.dp))
        WpTheme.Text.MetaLight112(
            modifier = Modifier.padding(horizontal = 65.dp),
            text = ageRestrictionsUIModel.message
        )
        Spacer(Modifier.height(24.dp))
        WpTheme.Button.CtaButton(
            text = ageRestrictionsUIModel.buttonText,
            horizontalPadding = 20,
            verticalPadding = 4,
            onClick = {
                onCtaButton()
            }
        )
    }
}

@Preview
@Composable
fun PreviewAgeRestriction() {
    AgeRestrictionUi(
        AgeRestrictionsUIModel(
            "We can\'t confirm your eligibility",
            "Message",
            "Button"
        )
    ) {}
}
