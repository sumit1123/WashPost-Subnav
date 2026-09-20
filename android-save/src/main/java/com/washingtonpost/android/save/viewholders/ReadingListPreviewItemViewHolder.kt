package com.washingtonpost.android.save.viewholders

import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.databinding.MyPostSectionPreviewBinding
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.types.MyPostSection


class ReadingListPreviewItemViewHolder(
    private val readingListBinding: MyPostSectionPreviewBinding,
    private val onArticleItemClick: (ArticleActionItem) -> Unit,
    private val onSaveClick: (ArticleActionItem) -> Unit,
    private val onOptionsClick: (ArticleActionItem) -> Unit,
    private val onViewMoreClick: (MyPostSection) -> Unit
) : PreviewItemViewHolder(
    readingListBinding,
    onArticleItemClick = onArticleItemClick,
    onAuthorClick = null,
    onSaveClick = onSaveClick,
    onOptionsClick = onOptionsClick
) {
    override fun bind(previewItem: PreviewItem) {
        super.bind(previewItem)
        readingListBinding.tvLabel.text =
            binding.root.context.getString(R.string.my_post_saved_stories_label)
        readingListBinding.tvDescription.text =
            binding.root.context.getString(R.string.my_post_saved_stories_desc)

        readingListBinding.buttonViewMore.setOnClickListener {
            onViewMoreClick(MyPostSection.SAVED_STORIES)
        }
    }
}