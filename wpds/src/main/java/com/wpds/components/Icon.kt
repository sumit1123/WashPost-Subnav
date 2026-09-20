package com.wpds.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wpds.wptheme.WpTheme


@Composable
fun WpTheme.Icon.NormalRoundedIcon(
    modifier: Modifier = Modifier,
    iconResource: Int,
    iconColor: Color,
    iconBackgroundColor: Color,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(iconBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .size(56.dp)
                .clickable(enabled = isClickable, onClick = onClick),
            painter = painterResource(id = iconResource),
            contentDescription = "",
            tint = iconColor
        )
    }
}
