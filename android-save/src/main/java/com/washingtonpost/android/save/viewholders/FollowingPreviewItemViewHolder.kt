package com.washingtonpost.android.save.viewholders

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.follow.database.model.AuthorEntity
import com.washingtonpost.android.follow.model.AuthorItem
import com.washingtonpost.android.follow.ui.ArticleListDividerItemDecoration
import com.washingtonpost.android.follow.ui.ArticleListMarginItemDecoration
import com.washingtonpost.android.follow.ui.AuthorListMarginItemDecoration
import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.adapters.FollowArticleAdapter
import com.washingtonpost.android.save.adapters.FollowAuthorsAdapter
import com.washingtonpost.android.save.databinding.MyPostFollowPreviewBinding
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.MyPostArticleItem
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.models.PreviewItem.SectionPreviewItem
import com.washingtonpost.android.save.types.MyPostSection


class FollowingPreviewItemViewHolder(
    private val followingBinding: MyPostFollowPreviewBinding,
    private val onArticleItemClick: (ArticleActionItem) -> Unit,
    private val onOptionsClick: (ArticleActionItem) -> Unit,
    private val onViewMoreClick: (MyPostSection) -> Unit,
    private val onMoreFromAuthorClick: (AuthorItem) -> Unit,
    private val onAuthorDataRequested: ((String) -> Unit)?
) : ItemViewHolder(
    followingBinding.root
) {
    private var previewItem: SectionPreviewItem? = null
    private var selectedAuthorItem: AuthorItem? = null

    override fun bind(previewItem: PreviewItem) {
        super.bind(previewItem)


        this.previewItem = previewItem as SectionPreviewItem
        val context = followingBinding.root.context

        followingBinding.tvLabel.text =
            followingBinding.root.context.getString(R.string.my_post_following_label)
        followingBinding.tvDescription.text =
            followingBinding.root.context.getString(R.string.my_post_following_desc)

        val authors = previewItem.authors ?: return
        val list = previewItem.articleList.orEmpty()

        if (followingBinding.authorList.adapter == null) {
            followingBinding.authorList.layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            val authorsAdapter = FollowAuthorsAdapter()
            authorsAdapter.onItemSelected = { pos ->
                setupArticleList(this.previewItem?.authors.orEmpty(), pos, this.previewItem?.articleList.orEmpty())
            }
            followingBinding.authorList.adapter = authorsAdapter
            followingBinding.authorList.itemAnimator = null
            followingBinding.authorList.addItemDecoration(AuthorListMarginItemDecoration(context))
            authorsAdapter.setItems(authors)

            followingBinding.articleList.adapter = FollowArticleAdapter(
                previewItem,
                onArticleItemClick,
                onOptionsClick)
            val columnsNumber = context.resources.getInteger(R.integer.my_post_follow_columns)
            followingBinding.articleList.layoutManager = GridLayoutManager(context, columnsNumber)
            followingBinding.articleList.apply {
                addItemDecoration(
                    ArticleListDividerItemDecoration(
                        followingBinding.root.context, RecyclerView.HORIZONTAL
                    )
                )
                addItemDecoration(
                    ArticleListMarginItemDecoration(
                        followingBinding.root.context, RecyclerView.VERTICAL,
                        columns = columnsNumber
                    )
                )
            }
            setupArticleList(authors, 0, list)
        } else {
            val authorsAdapter = followingBinding.authorList.adapter as FollowAuthorsAdapter
            authorsAdapter.setItems(authors)
            setupArticleList(authors, authorsAdapter.selectedItem, list)
            (followingBinding.articleList.adapter as FollowArticleAdapter).sectionPreviewItem = previewItem
        }

        followingBinding.buttonViewMore.setOnClickListener {
            selectedAuthorItem?.let { onMoreFromAuthorClick(it) }
        }
    }

    private fun setupArticleList(
        authors: List<AuthorEntity>,
        pos: Int,
        list: List<MyPostArticleItem>
    ) {
        val articlesLimit = itemView.context.resources.getInteger(R.integer.my_post_follow_articles)
        if (pos < authors.size) {
            val selectedAuthor = authors[pos]
            val articles =
                list
                    .filter { it.authorId == selectedAuthor.authorId }
                    .take(articlesLimit)
            if (articles.isEmpty()) {
                onAuthorDataRequested?.invoke(selectedAuthor.authorId)
            }
            selectedAuthorItem = AuthorItem(selectedAuthor.authorId, selectedAuthor.name,
                selectedAuthor.bio, null, selectedAuthor.image, null, 0)
            followingBinding.buttonViewMore.text = "More from ${authors[pos].name}"
            val adapter = followingBinding.articleList.adapter as FollowArticleAdapter
            adapter.setItems(articles)

            followingBinding.emptyState.myPostFollowEmptyState.visibility = View.GONE
            followingBinding.buttonViewMore.visibility = View.VISIBLE
            followingBinding.articleList.isEnabled = true
        } else {
            val adapter = followingBinding.articleList.adapter as FollowArticleAdapter
            adapter.setItems(null)

            followingBinding.emptyState.myPostFollowEmptyState.visibility = View.VISIBLE
            followingBinding.buttonViewMore.visibility = View.INVISIBLE
            followingBinding.articleList.isEnabled = false
        }
    }
}


