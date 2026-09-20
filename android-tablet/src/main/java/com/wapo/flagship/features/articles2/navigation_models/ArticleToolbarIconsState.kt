package com.wapo.flagship.features.articles2.navigation_models

/**
 * This class represents the different states the article toolbar can be in.
 */
sealed class ArticleToolbarIconsState {
    /**
     * This is used for article type that is not a native article. E.g. Webview article. In such cases, we need to make sure that Audio and
     * Bookmark options are not shown on the toolbar.
     */
    object NonNative : ArticleToolbarIconsState()

    /**
     * This is used for native articles.
     */
    object Native : ArticleToolbarIconsState()
}
