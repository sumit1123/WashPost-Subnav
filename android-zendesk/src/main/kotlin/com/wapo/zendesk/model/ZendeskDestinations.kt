package com.wapo.zendesk.model

/**
 * Different destinations that you can go to from zendesk form.
 */
sealed class ZendeskDestinations{
    /**
     * Workflow finished after submitting the form and showing the success screen
     */
    object Finish: ZendeskDestinations()

    /**
     * User wants to resubmit another form request.
     */
    object Resubmit: ZendeskDestinations()

    /**
     * Submitted the form successfully
     */
    object SubmitSuccess: ZendeskDestinations()

    /**
     * User starts contact us workflow from help center
     */
    object StartFormSubmissionFromHelpCenter: ZendeskDestinations()
}
