package com.wapo.flagship.features.articles3.views

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.wapo.adsinf.BannerAdView
import com.wapo.adsinf.models.AdConfig
import com.wapo.flagship.features.articles3.models.ui.AdUiModel
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors
import com.wpds.utils.isTabletUi

@Composable
fun AdView(
    uiModel: AdUiModel,
    index: Int,
    adViewCache: MutableMap<Int, BannerAdView>
) {
    val adStyle = getAdTextStyle(uiModel.uiStyle)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isTabletUi()) {
                    Modifier.padding(top = 20.dp, bottom = 36.dp)
                } else {
                    Modifier.padding(bottom = 20.dp)
                }
            )
            .background(wpdsColors.gray600)
            .padding(vertical = 24.dp)
    ) {
        Text(
            text = "Advertisement",
            style = adStyle,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))

        BannerAd(uiModel.adConfig, index, adViewCache)
    }
}

@Composable
private fun BannerAd(
    adConfig: AdConfig,
    index: Int,
    adViewCache: MutableMap<Int, BannerAdView>
) {
    val context = LocalContext.current
    val bannerAdView = remember(index, adViewCache) {
        adViewCache.getOrPut(index) {
            BannerAdView(context).apply { loadAd(adConfig) }
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { _ ->
                bannerAdView.checkVisibility()
            },
        factory = {
            (bannerAdView.parent as? ViewGroup)?.removeView(bannerAdView)
            bannerAdView
        }
    )
}

@Composable
private fun getAdTextStyle(uiStyle: AdUiStyle): TextStyle {
    return when (uiStyle) {
        AdUiStyle.DEFAULT -> TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            color = wpdsColors.onSurface,
            lineHeight = 15.sp,
        )
    }
}

enum class AdUiStyle {
    DEFAULT
}

@Preview(showBackground = true)
@Composable
private fun AdViewPreview() {
    AndroidClassicTheme {
        val adViewCache = remember { mutableMapOf<Int, BannerAdView>() }
        AdView(
            uiModel = AdUiModel(
                adConfig = AdConfig("123"),
                breakpoints = null,
                position = null,
                uiStyle = AdUiStyle.DEFAULT
            ),
            index = 0,
            adViewCache = adViewCache
        )
    }
}
