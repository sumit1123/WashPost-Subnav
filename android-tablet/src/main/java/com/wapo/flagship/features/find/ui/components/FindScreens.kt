package com.wapo.flagship.features.find.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import com.wapo.flagship.features.ask.viewmodels.AskQuestionsViewModel
import com.wapo.flagship.features.find.events.FindClickEvent
import com.wapo.flagship.features.find.model.FindItem
import com.wapo.flagship.features.find.model.FindScreenUIState
import com.wapo.flagship.features.find.model.HeaderItem
import com.wapo.flagship.features.find.model.HighlightItem
import com.wapo.flagship.features.find.model.SectionBarItem
import com.wapo.flagship.features.find.model.SectionBoxItem
import com.wapo.flagship.features.find.viewmodel.FindViewModel
import com.wpds.theme.wpdsColors

@Composable
fun FindScreen(
    viewModel: FindViewModel,
    askQuestionsViewModel: AskQuestionsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    viewModel.updateAZ(screenWidth >= 600)

    val onClick: (FindClickEvent) -> Unit = {
        viewModel.onClick(it)
    }

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .nestedScroll(rememberNestedScrollInteropConnection()),
        color = wpdsColors.findBg,
    ) {
        when (uiState) {
            FindScreenUIState.Failure -> FailureScreen()
            FindScreenUIState.Loading -> LoadingScreen()
            is FindScreenUIState.Success ->
                SuccessScreen(
                    (uiState as FindScreenUIState.Success).list,
                    (uiState as FindScreenUIState.Success).isAzDouble,
                    askQuestionsViewModel,
                    onClick,
                )
        }
    }
}

@Composable
private fun SuccessScreen(
    items: List<FindItem>,
    isAZDouble: Boolean = false,
    askQuestionsViewModel: AskQuestionsViewModel,
    onClick: (FindClickEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = wpdsColors.appBarBg),
    ) {
        CollapsingTopBar(onClick = onClick)
        FindContent(items, isAZDouble, askQuestionsViewModel, onClick)
    }
}

@Composable
private fun FailureScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        Text(
            modifier =
                Modifier
                    .wrapContentSize()
                    .align(
                        Alignment.Center,
                    ),
            text = "Failure",
            style = MaterialTheme.typography.h1,
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .wrapContentSize()
                    .align(
                        Alignment.Center,
                    ),
        )
    }
}

@Composable
private fun ColumnScope.FindContent(
    list: List<FindItem>,
    isAZDouble: Boolean,
    askQuestionsViewModel: AskQuestionsViewModel,
    onClick: (FindClickEvent) -> Unit,
) {
    LazyVerticalGrid(
        modifier =
            Modifier
                .widthIn(max = 700.dp)
                .background(color = Color.Transparent)
                .padding(horizontal = 11.dp)
                .align(Alignment.CenterHorizontally),
        contentPadding = PaddingValues(bottom = 80.dp),
        columns = GridCells.Fixed(2),
    ) {
        items(list, contentType = { it.type }, span = {
            if (isAZDouble && it is SectionBarItem) {
                GridItemSpan(1)
            } else {
                GridItemSpan(it.span)
            }
        }) {
            when (it) {
                is HeaderItem -> Header(it)
                is SectionBoxItem -> SectionBox(it, onClick)
                is HighlightItem -> HighlightBox(it, onClick)
                is SectionBarItem -> SectionBar(it, onClick)
                else -> {}
            }
        }
    }
}
