package com.wapo.flagship.features.posttv.model

sealed class ControllerViewEvent {
    object Share : ControllerViewEvent()
    object Captions : ControllerViewEvent()
    object Volume : ControllerViewEvent()
    object Mute : ControllerViewEvent()
    object Unmute : ControllerViewEvent()
    object Play : ControllerViewEvent()
    object Pause : ControllerViewEvent()
    object FullScreen : ControllerViewEvent()
    object PictureInPicture : ControllerViewEvent()
}