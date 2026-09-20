package com.washingtonpost.android.save.viewholders

import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.databinding.MyPostSectionPreviewBinding
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.types.MyPostSection


class ReadingHistoryPreviewItemViewHolder(
    private val readingHistoryBinding: MyPostSectionPreviewBinding,
    private val onArticleItemClick: (ArticleActionItem) -> Unit,
    private val onOptionsClick: (ArticleActionItem) -> Unit,
    private val onViewMoreClick: (MyPostSection) -> Unit
) : PreviewItemViewHolder(
    readingHistoryBinding,
    onArticleItemClick = onArticleItemClick,
    onAuthorClick = null,
    onSaveClick = null,
    onOptionsClick = onOptionsClick
) {
    override fun bind(previewItem: PreviewItem) {
        super.bind(previewItem)
        readingHistoryBinding.tvLabel.text =
            binding.root.context.getString(R.string.my_post_reading_history_label)
        readingHistoryBinding.tvDescription.text =
            binding.root.context.getString(R.string.my_post_reading_history_desc)

        readingHistoryBinding.buttonViewMore.setOnClickListener {
            onViewMoreClick(MyPostSection.READING_HISTORY)
        }
    }
}