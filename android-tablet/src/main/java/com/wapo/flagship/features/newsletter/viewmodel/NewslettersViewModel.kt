package com.wapo.flagship.features.newsletter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.newsletter.repo.NewslettersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewslettersViewModel @Inject constructor(
    private val newslettersRepository: NewslettersRepository
) : ViewModel() {
        fun syncNewslettersIfNeeded() {
            viewModelScope.launch {
                newslettersRepository.syncNewslettersIfNeeded()
            }
        }
    }
