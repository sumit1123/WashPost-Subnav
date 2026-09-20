package com.wapo.adsinf.models

sealed class AdsModel {
    object Enabled : AdsModel()
    object Disabled : AdsModel()
}
