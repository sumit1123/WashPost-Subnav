package com.wapo.flagship.external.foryouwidget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.ActionParameters.Pair
import androidx.glance.action.MutableActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.wapo.flagship.wapomain.MainActivity
import com.wapo.flagship.external.foryouwidget.actions.ArticleClickAction
import com.wapo.flagship.external.foryouwidget.actions.ArticleClickAction.Companion.widgetUpdateKey
import com.washingtonpost.android.R

@Composable
fun ShowEmptyState() {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = GlanceModifier
            .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ){
            Text(
                text = context.getString(R.string.for_you_widget_empty_state),
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 14.sp
                ),
                modifier = GlanceModifier.padding(bottom = 10.dp)
            )
            CircleIconButton(
                imageProvider = ImageProvider(com.wapo.flagship.features.audio.R.drawable.ic_external_link),
                contentDescription = context.getString(R.string.for_you_widget_empty_state),
                backgroundColor = GlanceTheme.colors.inversePrimary,
                contentColor = GlanceTheme.colors.primary,
                onClick = actionStartActivity<MainActivity>(actionParametersOf(
                    widgetUpdateKey to true
                )),
                enabled = true,
                modifier = GlanceModifier.size(48.dp)
            )
        }
    }
}