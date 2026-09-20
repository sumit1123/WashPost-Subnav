/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.washingtonpost.foryou.ui

import com.washingtonpost.foryou.data.Tile

sealed class HabitTilesUiState {
    class Feed(val items: List<Tile>) : HabitTilesUiState()
    data object Loading : HabitTilesUiState()
    data object Error : HabitTilesUiState()
}