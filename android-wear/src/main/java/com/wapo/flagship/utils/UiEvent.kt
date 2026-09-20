/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.utils

sealed class UiEvent {
    data class ShowSnackbar(val message: UiText): UiEvent()
}
