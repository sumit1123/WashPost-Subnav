package com.wapo.flagship.features.articles2.interfaces

/**
 * This interface is used to communicate with the native article fragment whenever share action is performed in the text selection state.
 */
interface TextSelection {
    /**
     * triggered when share is clicked after the selection.
     */
    fun onSharePreformed(selectedText: String?)
}
