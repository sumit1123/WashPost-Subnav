package com.wapo.flagship.json

import java.io.Serializable

/**
 * Created by kattim on 1/18/18.
 */
class CountryMedalItem(
        val rank: Int?,
        val icon: String?,
        val label: String?,
        val bronze: Int?,
        val silver: Int?,
        val gold: Int?
) : Serializable