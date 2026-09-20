package com.wapo.flagship.features.articles2.navigation_models

/**
 * This class represents various actions that can be done on individual articles from the top level Article activity e.g. Toolbar actions.
 * These toolbar clicks are handled in an activity however we need to perform such actions based on the article that's currently in front.
 */
sealed class ActionsOnIndividualArticles {
    /**
     * This object represents the gift action done from the toolbar in Articles Activity.
     */
    object ActionGift : ActionsOnIndividualArticles()

    /**
     * This object represents the share action done from the toolbar in Articles Activity.
     */
    object ActionShare : ActionsOnIndividualArticles()

    /**
     * This object represents the Audio icon click done from the toolbar in Articles Activity.
     */
    object ActionHeadphonesClick : ActionsOnIndividualArticles()

    /**
     * This object represents the bookmarks clicks done from the toolbar in Articles Activity.
     */
    object ActionBookmarkClick : ActionsOnIndividualArticles()

    /**
     * This object represents the Ellipsis clicks done from the toolbar in Articles Activity.
     */
    object ActionEllipsisClick : ActionsOnIndividualArticles()

    /**
     * This object represents the Summary icon from the toolbar in Articles Activity.
     */
    object ActionSummaryClick : ActionsOnIndividualArticles()

    /**
     * User clicked "New Updates" button to refresh content
     */
    object ActionLUFRefresh : ActionsOnIndividualArticles()

    object ActionCommentClick : ActionsOnIndividualArticles()

    object ActionTalkToThePost: ActionsOnIndividualArticles()
}
