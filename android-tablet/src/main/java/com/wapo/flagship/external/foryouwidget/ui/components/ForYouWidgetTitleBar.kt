package com.wapo.flagship.external.foryouwidget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.components.CircleIconButton
import com.washingtonpost.android.R
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.layout.Box
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider

@Composable
fun ForYouTitleBar(widgetTitle: String ) {
    TitleBar(
        modifier = GlanceModifier.height(44.dp),
        startIcon = ImageProvider(R.drawable.ic_widget_wp24),
        title = widgetTitle,
        textColor = ColorProvider(Color.Black),
        iconColor = ColorProvider(Color.Black),
        actions = {}
    )
}

@Composable
private fun ListenToForYou(){
    val context = LocalContext.current
    Box(modifier = GlanceModifier.padding(end = 18.dp, top = 2.dp)) {
        CircleIconButton(
            imageProvider = ImageProvider(com.washingtonpost.android.sections.R.drawable.ic_ab_headphones_filled),
            contentDescription = context.getString(R.string.for_you_widget_listen),
            backgroundColor = GlanceTheme.colors.inversePrimary,
            contentColor = GlanceTheme.colors.primary,
            onClick = {},
            enabled = true,
            modifier = GlanceModifier.size(34.dp)
        )
    }
}