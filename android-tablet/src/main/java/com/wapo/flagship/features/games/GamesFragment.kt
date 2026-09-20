package com.wapo.flagship.features.games

import com.wapo.flagship.features.grid.FusionSectionFragment

class GamesFragment : FusionSectionFragment() {

    override fun onResume() {
        super.onResume()
        startEngagementTrace()
    }
}
