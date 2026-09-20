package com.washingtonpost.android.save.viewholders

import com.washingtonpost.android.save.R
import com.washingtonpost.android.save.databinding.MyPostSectionPreviewBinding
import com.washingtonpost.android.save.models.ArticleActionItem
import com.washingtonpost.android.save.models.PreviewItem
import com.washingtonpost.android.save.types.MyPostSection

class PurchasedArticleViewHolder(
    private val purchasedArticlesBinding: MyPostSectionPreviewBinding,
    private val onArticleItemClick: (ArticleActionItem) -> Unit,
    private val onViewMoreClick: (MyPostSection) -> Unit,
    private val onOptionsClick: (ArticleActionItem) -> Unit,

) : PreviewItemViewHolder(
    purchasedArticlesBinding,
    onArticleItemClick,
    null,
    null,
    onOptionsClick
) {

    override fun bind(sectionPreviewItem: PreviewItem) {
        super.bind(sectionPreviewItem)
        purchasedArticlesBinding.tvLabel.text =
            binding.root.context.getString(R.string.my_post_purchased_articles_label)
        purchasedArticlesBinding.tvDescription.text =
            binding.root.context.getString(R.string.my_post_purchased_articles_desc)
        purchasedArticlesBinding.buttonViewMore.setOnClickListener {
            onViewMoreClick.invoke(MyPostSection.PURCHASE)
        }

    }

}