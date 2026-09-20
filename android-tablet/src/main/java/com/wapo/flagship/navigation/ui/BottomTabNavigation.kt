package com.wapo.flagship.navigation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * [NavHost] and [NavGraph] for Bottom Tab destinations
 */
@Composable
fun BottomTabNavigation(
    navController: NavHostController,
    containerId: Int,
    onBackListener: () -> Unit
) {
    // Set the startDestination dynamically
    NavHost(
        navController = navController,
        startDestination = BottomTab.Home.route,
    ) {
        enumValues<BottomTab>().forEach { item ->
            composable(item.route) {
                BackHandler {
                    onBackListener()
                }
            }
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            FragmentContainerView(context)
                .apply {
                    id = containerId
                    ViewCompat.setNestedScrollingEnabled(this, true)
                }
        },
    )
}
