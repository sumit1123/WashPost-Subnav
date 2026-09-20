/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.audio.models

sealed class AudioEvent {

    object None : AudioEvent()
    object LaunchTts : AudioEvent()
    object Stop : AudioEvent()
}
