package com.wapo.flagship.navigation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.washingtonpost.android.R
import com.wpds.theme.Typography
import com.wpds.theme.wpdsColors

enum class BottomTab(
    val route: String,
    val selected: Int,
    val unselected: Int,
    val title: String,
    val trackingName: String,
    val pageTitle: String? = null
) {
    Home("home", com.wpds.wpds.R.drawable.home_filled, com.wpds.wpds.R.drawable.home, "Home", "top stories"),
    Ask("ask", com.wpds.wpds.R.drawable.ai_filled, com.wpds.wpds.R.drawable.ai, "Ask", "ask"),
    Listen("listen", com.wpds.wpds.R.drawable.headphones, com.wpds.wpds.R.drawable.headphones_outline, "Listen", "listen"),
    Watch("watch", com.wapo.view.R.drawable.play_icon_btn, com.wapo.view.R.drawable.play_icon_btn_outlined, "Watch", "watch"),
    Games("games", com.wpds.wpds.R.drawable.trophy_filled, com.wpds.wpds.R.drawable.trophy, "Play", "games", "Games"),
    Search("search", com.wpds.wpds.R.drawable.search_filled, com.wpds.wpds.R.drawable.search, "Search", "Search", "Search"),
    Print("print", com.wpds.wpds.R.drawable.newspaper_filled, com.wpds.wpds.R.drawable.newspaper, "Print Edition", "print")
}

@Composable
fun BottomNavigationBar(
    isVisible: Boolean,
    topBarState: TopBarState,
    tabs: List<BottomTab>,
    currentTab: BottomTab,
    navTo: (BottomTab) -> Unit
) {
    if (isVisible) {
        AnimatedVisibility(
            visible = topBarState != TopBarState.COLLAPSED,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            BottomNavigation(
                elevation = 0.dp,
                backgroundColor = wpdsColors.appBarBg
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentTab.route == tab.route
                    BottomNavigationItem(
                        icon = {
                            val id = if (isSelected) tab.selected else tab.unselected
                            Icon(
                                modifier = Modifier.size(24.dp).padding(bottom = 2.dp),
                                painter = painterResource(id = id),
                                contentDescription = tab.title,
                                tint = wpdsColors.appBarIcon
                            )
                        },
                        label = {
                            Text(
                                style = Typography.body1,
                                text = tab.title,
                                fontWeight = FontWeight.Normal,
                                color = wpdsColors.appBarIcon
                            )
                        },
                        alwaysShowLabel = true,
                        selected = isSelected,
                        onClick = {
                            navTo(tab)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
//    BottomNavigationBar()
}
