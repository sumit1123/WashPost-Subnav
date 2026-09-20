/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.washingtonpost.android.paywall.events

import com.washingtonpost.android.paywall.features.tetro.Prompt

sealed class WallState {
    class Regwall(val codes: List<String>) : WallState()
    class GiftWall(val giftState: GiftState) : WallState()
    class Paywall(val codes: List<String>?) : WallState()
    class Softwall(val codes: List<String>?) : WallState()
    class MapWall(val prompts: List<Prompt>?) : WallState()
    object NoWall : WallState()
    class Failed(val message:String) : WallState()
}