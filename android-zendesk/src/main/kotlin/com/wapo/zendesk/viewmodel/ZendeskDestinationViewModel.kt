package com.wapo.zendesk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.wapo.android.commons.util.LiveEvent
import com.wapo.zendesk.model.ZendeskDestinations

/**
 * This VM handles all of the destinations within/leading to contact us form.
 */
class ZendeskDestinationViewModel: ViewModel() {
    private val _destination = LiveEvent<ZendeskDestinations>()
    val destination: LiveData<ZendeskDestinations> = _destination

    /**
     * Contact us workflow finished.
     */
    fun finish(){
        _destination.value = ZendeskDestinations.Finish
    }

    /**
     * Contact us form submission successful
     */
    fun formSubmissionSuccessful(){
        _destination.value = ZendeskDestinations.SubmitSuccess
    }

    /**
     * Re-start the form submission by opening contact us again.
     */
    fun startFormReSubmission(){
        _destination.value = ZendeskDestinations.Resubmit
    }

    /**
     * Start form submission for opening the contact us screen from help center.
     */
    fun startContactUsFromHelpCenter(){
        _destination.value = ZendeskDestinations.StartFormSubmissionFromHelpCenter
    }
}