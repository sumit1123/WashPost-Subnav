package com.wapo.flagship.features.newsprint

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.washingtonpost.android.sections.R

@Composable
fun NewsprintBadgeOverlay() {
    Image(
        modifier = Modifier
            .zIndex(1f)
            .height(89.dp)
            .width(89.dp),
        painter = painterResource(id = R.drawable.newsprint_this_is_you),
        contentDescription = "This is you!"
    )
}