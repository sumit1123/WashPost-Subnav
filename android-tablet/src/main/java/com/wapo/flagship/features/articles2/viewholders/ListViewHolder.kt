package com.wapo.flagship.features.articles2.viewholders

import android.os.Build
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.QuoteSpan
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.interfaces.ArticlesInteractionHelper
import com.wapo.flagship.features.articles2.models.deserialized.ListItem
import com.wapo.flagship.features.articles2.models.deserialized.Style
import com.wapo.flagship.features.articles2.utils.KeyHelper
import com.wapo.flagship.features.articles2.utils.StylesHelper
import com.wapo.text.BlockquoteSpan
import com.wapo.text.ListSpan
import com.wapo.text.WpTextAppearanceSpan
import com.washingtonpost.android.databinding.ItemListBinding

class ListViewHolder(
    private val binding: ItemListBinding,
    val articlesInteractionHelper: ArticlesInteractionHelper,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<ListItem>(
        binding.root,
    ) {
    var isBrief = false
    var arcId: String? = null

    override fun bind(
        item: ListItem,
        position: Int,
    ) {
        arcId = item.arcId
        binding.articleText.movementMethod = LinkMovementMethod.getInstance()

        isBrief = item.style == "briefs"

        val builder = processListItems(item)

        val style: Int =
            when (item.subtype) {
                "subhead" ->
                    StylesHelper.getTextItemSubheadStyle(
                        binding.root.context,
                        Style.getValue(item.style),
                    )
                "extra", "trailer" -> StylesHelper.getTextItemFooterStyle(binding.root.context)
                "intro" -> StylesHelper.getTextItemIntroStyle(binding.root.context)
                "letter" -> StylesHelper.getTextItemLetterStyle(binding.root.context)
                "blockquote" -> {
                    val blockQuoteSpan =
                        BlockquoteSpan(
                            StylesHelper.getBlockQuoteLineColor(binding.root.context),
                            StylesHelper.getBlockQuoteMargin(binding.root.context),
                            StylesHelper.getBlockQuoteLineWidth(binding.root.context),
                        )
                    builder.setSpan(
                        blockQuoteSpan,
                        0,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    StylesHelper.getTextItemStyle(binding.root.context, Style.getValue(item.style))
                }
                else -> {
                    val quoteSpans: Array<QuoteSpan>? =
                        builder.getSpans(
                            0,
                            builder.length,
                            QuoteSpan::class.java,
                        )
                    if (quoteSpans != null) {
                        for (quoteSpan in quoteSpans) {
                            builder.removeSpan(quoteSpan)
                        }
                    }
                    StylesHelper.getTextListItemStyle(binding.root.context, Style.getValue(item.style))
                }
            }

        builder.setSpan(
            WpTextAppearanceSpan(binding.root.context, style),
            0,
            builder.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        val stringBuilderWithLinks =
            StylesHelper.makeLinkClickable(
                builder,
                binding.root.context,
                articlesInteractionHelper,
            )

        binding.articleText.setLineSpacing(
            StylesHelper.getTextSpacingExtra(binding.root.context),
            StylesHelper.getTextSpacingMult(binding.root.context),
        )

        binding.articleText.text = stringBuilderWithLinks

        binding.articleText.key = KeyHelper.createKey(position, stringBuilderWithLinks.toString())
    }

    companion object {
        /**
         * Method to process list of string items and make it as one single spannable item.
         */
        @JvmStatic
        fun processListItems(item: ListItem): Spannable {
            val builder = SpannableStringBuilder()
            item.content?.let { list ->
                for (i in list.indices) {
                    if (!TextUtils.isEmpty(list[i])) {
                        builder.append(getFormattedItem(item, i))
                        if (i + 1 < list.size) {
                            builder.append("\n\n")
                        }
                    }
                }
            }
            return builder
        }

        /**
         * Returns a fully formatted line of text according to the given position and list's subtype
         */
        private fun getFormattedItem(
            listItem: ListItem?,
            position: Int,
        ): CharSequence? {
            val item: CharSequence =
                if ("text/plain" == listItem?.mime) {
                    listItem.content?.get(position) as CharSequence
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(
                            listItem?.content?.get(position),
                            Html.FROM_HTML_MODE_LEGACY,
                        )
                    } else {
                        Html.fromHtml(listItem?.content?.get(position))
                    }
                }
            // Display prefix even item is empty
            val spannableString = SpannableString(item.ifEmpty { " " })
            val span: Any = ListSpan(getPrefix(position + 1, listItem?.subtype))
            spannableString.setSpan(
                span,
                0,
                spannableString.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            return spannableString
        }

        private fun getPrefix(
            position: Int,
            subtype: String?,
        ): String? =
            if ("ordered" == subtype) {
                "$position."
            } else {
                "\u2022"
            }
    }
}
