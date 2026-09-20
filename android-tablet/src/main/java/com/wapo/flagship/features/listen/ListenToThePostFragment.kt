package com.wapo.flagship.features.listen

import com.wapo.flagship.features.grid.FusionSectionFragment

class ListenToThePostFragment : FusionSectionFragment() {

    override fun onResume() {
        super.onResume()
        startEngagementTrace()
    }
}
