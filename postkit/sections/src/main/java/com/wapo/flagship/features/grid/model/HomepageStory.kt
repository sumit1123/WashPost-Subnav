package com.wapo.flagship.features.grid.model

import com.wapo.flagship.json.OlympicsMedals
import com.wapo.flagship.json.OlympicsSchedule

data class HomepageStory(
    val link: Link?,
    val offlineLink: Link?,
    val media: Media?,
    val slideShow: SlideShow? = null,
    val headline: Headline?,
    val source: String?,
    val audio: Audio?,
    val audioArticle: AudioArticle?,
    val deck: String?,
    val blurbs: BlurbList?,
    val signature: Signature?,
    val relatedLinks: RelatedLinks?,
    val liveBlog: LiveBlog?,
    val label: CompoundLabel?,
    val pageBuilderLabel: Label?,
    val isGrid : Boolean = false,
    val textAlignment: Alignment? = null,
    val wrapText: Boolean = false,
    val arrangements: Arrangements?,
    val olympicsMedals: OlympicsMedals? = null,
    val olympicsSchedule: OlympicsSchedule? = null,
    val cta: CompoundLabel? = null,
    val footNote: FootNote? = null,
    val actions: Actions? = null,
    val itId: String? = null, // TODO probably need to move this field into Link model
    val topperLabel: CompoundLabel? = null,
    var webComponent: WebComponent? = null,
    var count: Count? = null,
    val contentId: String? = null
) : Item()
