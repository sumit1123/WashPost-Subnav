/* Copyright (c) 2025 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.grid.views.carousel

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wapo.flagship.features.grid.GridAdapter
import com.wapo.flagship.features.grid.GridViewHolder
import com.wapo.flagship.features.grid.WPGridView
import com.wapo.flagship.features.grid.model.SectionTopper
import com.wapo.flagship.features.grid.toDp
import com.washingtonpost.android.sections.R
import com.wpds.theme.AndroidClassicTheme
import com.wpds.theme.wpdsColors

class SectionTopperHolder(
    val itemView: View,
    val parent: ViewGroup
) : GridViewHolder(itemView.rootView) {

    private val res = itemView.context.resources
    private val deviceDensity = res?.displayMetrics?.density!!

    private var wpGridView: WPGridView = parent as WPGridView
    private val composeView = itemView.findViewById<ComposeView?>(R.id.section_topper_form_wrapper)

    override fun bind(position: Int, gridAdapter: GridAdapter) {
        val item = gridAdapter.items[position] as? SectionTopper ?: return

        composeView?.setContent {
            AndroidClassicTheme {
                Surface(color = Color.Unspecified) {
                    Topper(item)
                }
            }
        }
    }

    @Composable
    private fun Topper(item: SectionTopper) {
        val isSingleColumn = wpGridView.getColumnCount() == 1
        val contentPadding =
            if (isSingleColumn) wpGridView.getCardDividerPadding().toDp(deviceDensity) else 0

        Box(
            modifier = Modifier
                .padding(bottom = if (isSingleColumn) 0.dp else 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = contentPadding.dp)
                    .padding(vertical = 16.dp)
                    .background(Color.Transparent),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Title(item.title)
                Tagline(item.tagline)
            }
        }
    }

    @Composable
    private fun Title(text: String?) {
        text ?: return
        Text(
            modifier = Modifier
                .background(Color.Transparent),
            text = text,
            color = wpdsColors.gray0,
            fontSize = 26.sp,
            letterSpacing = 0.26.sp,
            fontFamily = FontFamily(Font(R.font.franklinitcstd_bold)),
            lineHeight = 18.4.sp
        )
    }

    @Composable
    private fun Tagline(text: String?) {
        text ?: return
        Text(
            modifier = Modifier
                .background(Color.Transparent),
            text = text,
            color = wpdsColors.gray0,
            fontSize = 12.sp,
            letterSpacing = 0.sp,
            fontFamily = FontFamily(Font(R.font.franklinitcstd_light)),
            lineHeight = 13.9.sp
        )
    }
}

