// Copyright (c) 2024 The Washington Post. All rights reserved.
package com.wapo.flagship.features.habittiles

import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.washingtonpost.foryou.domain.HabitTilesCache
import com.washingtonpost.foryou.data.ApiDataListener
import kotlinx.coroutines.launch
import javax.inject.Inject

class HabitTilesApiDataListener
    @Inject
    constructor(
        private val coroutineScopeProvider: CoroutineScopeProvider,
        private val cache: HabitTilesCache,
    ) : ApiDataListener {
        var testGroup: String? = null
            private set

        init {
            coroutineScopeProvider.sync.launch {
                setABTestGroup(cache.getTilesList()?.testGroup)
            }
        }

        override fun setABTestGroup(group: String?) {
            testGroup = group
        }
    }
