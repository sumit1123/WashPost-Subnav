package com.wapo.flagship.features.conversations.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.conversations.util.CommentsConstant.MyCommentsTab
import com.wpds.theme.wpdsColors

/**
 * A row of pill-shaped tab chips that allows the user to switch between different
 * comment feeds (e.g. Featured, Top, My Comments).
 *
 * @param tabs The list of tab titles.
 * @param selectedIndex The index of the currently selected tab.
 * @param isUserLoggedIn Whether the current user is logged in. When false, the
 *   "My Comments" pill is hidden.
 * @param onTabSelected A callback to be invoked when the user selects a tab.
 */
@Composable
fun ConversationTabs(
    tabs: List<String>,
    selectedIndex: Int,
    isUserLoggedIn: Boolean = false,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            // Hide the "My Comments" tab for non-logged in users
            if (index == MyCommentsTab && !isUserLoggedIn) return@forEachIndexed
            val isSelected = index == selectedIndex
            Surface(
                onClick = { onTabSelected(index) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) wpdsColors.onSurface else wpdsColors.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) wpdsColors.onSurface else wpdsColors.outline
                )
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) wpdsColors.surface else wpdsColors.onSurface
                )
            }
        }
    }
}
