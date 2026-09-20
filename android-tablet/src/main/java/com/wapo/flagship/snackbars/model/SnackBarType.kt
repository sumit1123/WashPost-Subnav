package com.wapo.flagship.snackbars.model

sealed class SnackBarType(
    val persistent: Boolean = false,
) {
    class NoNetworkConnection : SnackBarType(true)

    class AddedToPlaylist : SnackBarType()

    class LowDataConnection : SnackBarType(true)
}

sealed interface SnackBarEvent {
    object OpenSettings : SnackBarEvent

    object OpenListenToThePost : SnackBarEvent

    object Dismiss : SnackBarEvent

    object TurnOnLowDataMode : SnackBarEvent

    object DismissLowDataMode : SnackBarEvent
}
