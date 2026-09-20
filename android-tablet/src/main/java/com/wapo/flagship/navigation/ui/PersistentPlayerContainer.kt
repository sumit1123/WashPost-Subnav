package com.wapo.flagship.navigation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.washingtonpost.android.databinding.FragmentPersistentPlayerBinding
import com.wpds.theme.wpdsColors

@Composable
fun PersistentPlayerContainer(isShown: Boolean = false) {
    if (isShown) {
        AndroidViewBinding(
            modifier =
                Modifier
                    .height(62.dp)
                    .background(color = wpdsColors.appBarBg)
                    .width(580.dp),
            factory = FragmentPersistentPlayerBinding::inflate,
        )
    }
}
