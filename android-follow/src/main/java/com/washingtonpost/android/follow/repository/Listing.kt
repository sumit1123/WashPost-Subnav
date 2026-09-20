package com.washingtonpost.android.follow.repository

import androidx.lifecycle.LiveData
import androidx.paging.PagedList

data class Listing<Any : kotlin.Any>(
        // the LiveData of paged lists for the UI to observe
        val pagedList: LiveData<PagedList<Any>>,
        // represents the network request status to show to the user
        val networkState: LiveData<NetworkState>,
        // represents the refresh status to show to the user. Separate from networkState, this
        // value is importantly only when refresh is requested.
        val refreshState: LiveData<NetworkState>,
        // refreshes the whole data and fetches it from scratch.
        val refresh: () -> Unit,
        // retries any failed requests.
        val retry: () -> Unit,
        // clears coroutine jobs
        val clearCoroutineJobs: () -> Unit)