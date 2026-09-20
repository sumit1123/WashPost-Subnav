/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.features.articles2.typeconverters

import com.wapo.android.commons.util.LogUtil
import androidx.room.TypeConverter
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.squareup.moshi.Moshi
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.*
import org.json.JSONObject

/**
 * [TypeConverter] for list of [Item] objects to be able to serialize and deserialize it
 */
open class ItemListTypeConverter {

    private var moshi: Moshi = MoshiAdapters.INSTANCE.moshi

    @TypeConverter
    fun toJson(items: List<Item>?): String? {

        if (items == null) return null

        val arr = JsonArray()
        items.forEach {
            when (it) {
                is Kicker -> {
                    arr.add(KickerJsonAdapter(moshi).toJson(it))
                }
                is Title -> {
                    arr.add(TitleJsonAdapter(moshi).toJson(it))
                }
                is ByLine -> {
                    arr.add(ByLineJsonAdapter(moshi).toJson(it))
                }
                is Date -> {
                    arr.add(DateJsonAdapter(moshi).toJson(it))
                }
                is Deck -> {
                    arr.add(DeckJsonAdapter(moshi).toJson(it))
                }
                is Image -> {
                    arr.add(ImageJsonAdapter(moshi).toJson(it))
                }
                is Correction -> {
                    arr.add(CorrectionJsonAdapter(moshi).toJson(it))
                }
                is SanitizedHtml -> {
                    arr.add(SanitizedHtmlJsonAdapter(moshi).toJson(it))
                }
                is ListItem -> {
                    arr.add(ListItemJsonAdapter(moshi).toJson(it))
                }
                is PullQuote -> {
                    arr.add(PullQuoteJsonAdapter(moshi).toJson(it))
                }
                is InterstitialLink -> {
                    arr.add(InterstitialLinkJsonAdapter(moshi).toJson(it))
                }
                is AuthorInfo -> {
                    arr.add(AuthorInfoJsonAdapter(moshi).toJson(it))
                }
                is TableItem -> {
                    arr.add(TableItemJsonAdapter(moshi).toJson(it))
                }
                is Link -> {
                    when (it) {
                        is Comments -> arr.add(CommentsJsonAdapter(moshi).toJson(it))
                        is Anchor -> arr.add(AnchorJsonAdapter(moshi).toJson(it))
                        else -> arr.add(LinkJsonAdapter(moshi).toJson(it))
                    }
                }
                is Divider -> {
                    arr.add(DividerJsonAdapter(moshi).toJson(it))
                }
                is Podcast -> {
                    arr.add(PodcastJsonAdapter(moshi).toJson(it))
                }
                is ElementGroup -> {
                    arr.add(ElementGroupJsonAdapter(moshi).toJson(it))
                }
                is Audio -> {
                    arr.add(AudioJsonAdapter(moshi).toJson(it))
                }
                is OlympicsMedals -> {
                    arr.add(OlympicsMedalsJsonAdapter(moshi).toJson(it))
                }
            }
        }
        return arr.toString()
    }

    @TypeConverter
    fun fromJson(data: String?): List<Item>? {
        if (data == null)
            return null
        val items = mutableListOf<Item>()
        val jsonArray = JsonParser.parseString(data) as JsonArray
        jsonArray.forEach {
            try {
                val item = when (JSONObject(it.asString)["type"]) {
                    "kicker" -> {
                        KickerJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "title" -> {
                        TitleJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "byline" -> {
                        ByLineJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "date" -> {
                        DateJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "deck" -> {
                        DeckJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "image" -> {
                        ImageJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "correction" -> {
                        CorrectionJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "sanitized_html" -> {
                        SanitizedHtmlJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "list" -> {
                        ListItemJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "pull_quote" -> {
                        PullQuoteJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "interstitial_link" -> {
                        InterstitialLinkJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "link" -> {
                        when (JSONObject(it.asString)["subtype"]) {
                            "comments" -> {
                                CommentsJsonAdapter(moshi).fromJson(it.asString)
                            }
                            "anchor" -> {
                                AnchorJsonAdapter(moshi).fromJson(it.asString)
                            }
                            else -> LinkJsonAdapter(moshi).fromJson(it.asString)
                        }
                    }
                    "author_info" -> {
                        AuthorInfoJsonAdapter(moshi).fromJson(it.asString)
                    }

                    "element_group" -> {
                        ElementGroupJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "divider" -> {
                        DividerJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "podcast" -> {
                        PodcastJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "audio" -> {
                        AudioJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "olympics" -> {
                        OlympicsMedalsJsonAdapter(moshi).fromJson(it.asString)
                    }
                    "table" -> {
                        TableItemJsonAdapter(moshi).fromJson(it.asString)
                    }
                    else -> {
                        LogUtil.e("ItemListTypeConverter", "Unexpected data found in feeds item: $it")
                        Item("default")
                    }
                }
                if (item != null)
                    items.add(item)
            }
            // If any exception occurs, we assume that the feeds has a corrupted item in the items list.
            catch (ex: Exception) {
                items.add(Item("default"))
                LogUtil.e("ItemListTypeConverter", "An unexpected item found in feeds: $it")
            }
        }
        return items
    }
}

class MoshiAdapters(val moshi: Moshi) {

    /**
     * Extra reference so [ItemListTypeConverter] can access moshi statically
     */

    companion object  {
        lateinit var INSTANCE: MoshiAdapters
    }

}
