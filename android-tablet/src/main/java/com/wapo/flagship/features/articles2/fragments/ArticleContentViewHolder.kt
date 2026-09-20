package com.wapo.flagship.features.articles2.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.wapo.flagship.features.articles2.models.Author
import com.wapo.flagship.features.articles2.models.Question
import com.wapo.flagship.features.articles2.models.deserialized.Image
import com.wapo.flagship.features.articles2.viewmodels.ArticlesPagerCollaborationViewModel
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.FragmentArticleContentBinding

abstract class ArticleContentViewHolder(
    private val context: Context,
    private val binding: FragmentArticleContentBinding,
    private val collaborationViewModel: ArticlesPagerCollaborationViewModel,
) {
    var expanded = false

    abstract fun bind(savedState: Bundle? = null)

    abstract fun unbind()

    abstract fun retry()

    open fun saveState(outState: Bundle) {
        /*
           Stubbed
         */
    }

    fun collapse(
        itemView: View,
        @DrawableRes backgroundResId: Int = com.washingtonpost.android.articles.R.drawable.card_article,
    ) {
        itemView.background =
            ResourcesCompat.getDrawable(context.resources, backgroundResId, null)
        collaborationViewModel.collapsePage()
        expanded = false
    }

    fun expand(
        itemView: View,
        @DrawableRes backgroundResId: Int = com.washingtonpost.android.articles.R.drawable.card_article_expanded,
    ) {
        itemView.background =
            ResourcesCompat.getDrawable(context.resources, backgroundResId, null)
        collaborationViewModel.expandPage()
        expanded = true
    }

    open fun onViewCommentsClicked() {
        /*
            Stubbed
         */
    }

    open fun onViewSpecificCommentClicked(commentIdUrl: String) {
        /* Stubbed */
    }

    open fun onAuthorClicked(author: Author) {
        /*
           Stubbed
         */
    }

    open fun onAskThePostClicked(questions: List<Question>) {
        /*
            Stubbed
         */
    }

    open fun onLinkClicked(url: String) {
        /*
           Stubbed
         */
    }

    open fun onImageClicked(image: Image) {
        /*
            Stubbed
         */
    }

    open fun onLufOutcomePostClicked(url: String) {
        /*
    Stubbed
         */
    }

    open fun onAudioClicked() {
        /*
            Stubbed
         */
    }

    /**
     * Method to delegate fragment's visibility to its views.
     * @param visible: true when fragment is visible, otherwise should be false.
     */
    open fun onVisibilityChanged(visible: Boolean) {
    }
}
