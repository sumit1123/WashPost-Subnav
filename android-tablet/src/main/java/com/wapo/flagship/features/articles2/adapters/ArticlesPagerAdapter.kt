package com.wapo.flagship.features.articles2.adapters

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.wapo.flagship.features.articles2.activities.AUDIO_ARTICLE_POSITION
import com.wapo.flagship.features.articles2.activities.PUSH_TOPIC
import com.wapo.flagship.features.articles2.activities.SHOULD_PLAY_AUDIO_ARTICLE
import com.wapo.flagship.features.articles2.fragments.ArticleContentFragment
import com.wapo.flagship.model.ArticleMeta

/**
 * This pager adapter is used as the data source for the articles view pager to populate multiple articles content fragments.
 */
class ArticlesPagerAdapter(
    fragment: Fragment,
    private val articlesMetaData: MutableList<ArticleMeta>,
    private val pushTopic: String? = "",
    private val shouldPlayAudioArticle: Boolean = false,
    private val playAudioForArticlePosition: Int? = null
) : FragmentStateAdapter(
        fragment,
    ) {
    private lateinit var viewPager: ViewPager2

    val currentArticlesMetaData: List<ArticleMeta> get() = articlesMetaData

    fun setViewPager(viewPager: ViewPager2) {
        this.viewPager = viewPager
    }

    override fun getItemCount(): Int = articlesMetaData.size

    override fun createFragment(position: Int): Fragment {
        val fragment = ArticleContentFragment()
        fragment.arguments =
            Bundle().apply {
                putParcelable(ARTICLE_METADATA_KEY, articlesMetaData[position])
                putString(PUSH_TOPIC, pushTopic)
                putInt(ARTICLE_POSITION, position)
                putBoolean(SHOULD_PLAY_AUDIO_ARTICLE, shouldPlayAudioArticle)
                putInt(AUDIO_ARTICLE_POSITION, playAudioForArticlePosition ?: 0)
            }
        return fragment
    }

    /**
     * Returns a unique ID for each item.
     * This way FragmentStateAdapter can correctly determine which fragments to remove when the data changes.
     * This ensures that the fragment associated with the removed item is properly destroyed.
     */
    override fun getItemId(position: Int): Long {
        // Return a unique ID for each item
        return articlesMetaData[position].id.hashCode().toLong()
    }

    /**
     * Ensures that FragmentStateAdapter knows whether a fragment should still exist based on the current data.
     */
    override fun containsItem(itemId: Long): Boolean {
        // Check if the item still exists in the list
        return articlesMetaData.any { it.id.hashCode().toLong() == itemId }
    }

    /**
     * This method is called when an article is removed from the list.
     * It updates the articles list and notifies the adapter of the change.
     *
     * @param position The position of the article to be removed.
     */
    fun removeArticleAt(position: Int) {
        val newList = articlesMetaData.filterIndexed { i, _ ->
            i != position
        }
        updateArticles(newList)
        Log.d(
            TAG,
            "Removing article at position: $position, Updated article list: $articlesMetaData"
        )
    }

    /**
     * This method is called when the articles list is updated.
     * It calculates the difference between the old and new lists and dispatches updates to the adapter.
     * This allows for efficient updates without reloading the entire list.
     *
     * @param newArticles The new list of articles to be displayed.
     */
    private fun updateArticles(newArticles: List<ArticleMeta>) {
        val diffCallback = ArticlesDiffCallback(articlesMetaData, newArticles)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        articlesMetaData.clear()
        articlesMetaData.addAll(newArticles)
        Handler(Looper.getMainLooper()).post {
            diffResult.dispatchUpdatesTo(this)
        }
    }

    companion object {
        const val ARTICLE_METADATA_KEY = "ARTICLE_METADATA_KEY"
        const val ARTICLE_POSITION = "ARTICLE_POSITION"
        const val TAG = "ArticlesPagerAdapter"
    }
}

class ArticlesDiffCallback(
    private val oldList: List<ArticleMeta>,
    private val newList: List<ArticleMeta>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Compare unique IDs
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Compare content
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }
}
