package com.wapo.flagship.features.grid.model

import android.net.Uri
import com.wapo.flagship.features.grid.ActionsEntity
import com.wapo.flagship.features.grid.AlignmentEntity
import com.wapo.flagship.features.grid.ArrangementsEntity
import com.wapo.flagship.features.grid.BleedEntity
import com.wapo.flagship.features.grid.BlurbInfoEntity
import com.wapo.flagship.features.grid.BlurbItemEntity
import com.wapo.flagship.features.grid.BlurbStyleEntity
import com.wapo.flagship.features.grid.BlurbsEntity
import com.wapo.flagship.features.grid.BulletTypeEntity
import com.wapo.flagship.features.grid.CompoundLabelEntity
import com.wapo.flagship.features.grid.CompoundLabelTypeEntity
import com.wapo.flagship.features.grid.CountEntity
import com.wapo.flagship.features.grid.DefaultArrangementEntity
import com.wapo.flagship.features.grid.ExcerptEntity
import com.wapo.flagship.features.grid.FontStyleEntity
import com.wapo.flagship.features.grid.FootNoteEntity
import com.wapo.flagship.features.grid.FormEntity
import com.wapo.flagship.features.grid.FormFieldEntity
import com.wapo.flagship.features.grid.HeadlineEntity
import com.wapo.flagship.features.grid.HomepageStoryEntity
import com.wapo.flagship.features.grid.LabelIcon
import com.wapo.flagship.features.grid.LabelPositionEntity
import com.wapo.flagship.features.grid.LabelStyleEntity
import com.wapo.flagship.features.grid.LinkEntity
import com.wapo.flagship.features.grid.LinkTypeEntity
import com.wapo.flagship.features.grid.LiveBlogEntity
import com.wapo.flagship.features.grid.OlympicsCtaEntity
import com.wapo.flagship.features.grid.OlympicsLinkEntity
import com.wapo.flagship.features.grid.OlympicsMedalsEntity
import com.wapo.flagship.features.grid.OlympicsMedalsEntryEntity
import com.wapo.flagship.features.grid.OlympicsScheduleEntity
import com.wapo.flagship.features.grid.OlympicsScheduleEntryEntity
import com.wapo.flagship.features.grid.RelatedLinkItemEntity
import com.wapo.flagship.features.grid.RelatedLinksEntity
import com.wapo.flagship.features.grid.RelatedLinksInfoEntity
import com.wapo.flagship.features.grid.SignatureEntity
import com.wapo.flagship.features.grid.SizeEntity
import com.wapo.flagship.features.grid.SubItemTypeEntity
import com.wapo.flagship.features.grid.SubscriptionLinksEntity
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.grid.VerticalAlignmentEntity
import com.wapo.flagship.features.grid.WebComponentEntity
import com.wapo.flagship.features.grid.ZoneEntity
import com.wapo.flagship.json.OlympicsCta
import com.wapo.flagship.json.OlympicsLink
import com.wapo.flagship.json.OlympicsMedals
import com.wapo.flagship.json.OlympicsMedalsEntry
import com.wapo.flagship.json.OlympicsSchedule
import com.wapo.flagship.json.OlympicsScheduleEntry

object HomepageStoryMapper {

    fun getHomepageStoryModel(homepageStory: HomepageStoryEntity, parentLinkGroup: String?, tracking: Tracking?, pageConfig: PageConfig): HomepageStory {
        val homepageStoryModel = HomepageStory(
                link = getLink(homepageStory.link),
                offlineLink = getLink(homepageStory.offlineLink),
                media = MediaMapper.getMedia(homepageStory.media, pageConfig),
                slideShow = MediaMapper.getSlideShow(homepageStory.slideshow, pageConfig),
                headline = getHeadline(homepageStory.headline),
                source = homepageStory.source,
                audio = AudioMapper.getAudio(homepageStory.audio, pageConfig),
                audioArticle = AudioArticleMapper.getAudioArticle(homepageStory.audioArticle, pageConfig = pageConfig),
                deck = getDeck(homepageStory.headline),
                blurbs = getBlurbs(homepageStory.blurbs),
                signature = getSignature(homepageStory.signature),
                relatedLinks = getRelatedLinks(homepageStory.relatedLinks),
                label = getLabel(homepageStory.label)?.apply { isStoryLabel = true },
                pageBuilderLabel = null,
                liveBlog = getLiveBlog(homepageStory.liveBlog),
                isGrid = true,
                textAlignment = getAlignment(homepageStory.textAlignment),
                wrapText = homepageStory.wrapText,
                arrangements = getArrangements(homepageStory.arrangements),
                olympicsMedals = getOlympicsMedals(homepageStory.olympicsMedals),
                olympicsSchedule = getOlympicsSchedule(homepageStory.olympicsSchedule),
                cta = getLabel(homepageStory.cta)?.apply { isStoryLabel = true },
                footNote = getFootNote(homepageStory.footNote),
                actions = getActions(homepageStory.actions, pageConfig),
                itId = getItId(homepageStory.link?.url, homepageStory.linkGroup, homepageStory.linkDetail, parentLinkGroup, tracking),
                topperLabel = getLabel(homepageStory.topperLabel)?.apply { isStoryLabel = true },
                webComponent = getWebComponent(homepageStory.webview),
                count = getCount(homepageStory.count),
                contentId = homepageStory.contentId,
        )

        homepageStoryModel.layoutAttributes = PageModelMapper.getLayoutAttributes(homepageStory.layoutAttributes)
        setStoryBleed(homepageStory, homepageStoryModel)
        homepageStoryModel.id = homepageStory.id

        return homepageStoryModel
    }

    private fun getActions(actions: ActionsEntity?, pageConfig: PageConfig): Actions? {
        actions ?: return null

        return Actions(
            audioArticle = AudioArticleMapper.getAudioArticle(actions.audioArticle, pageConfig = pageConfig),
            comments = CommentsActionMapper.getCommentsAction(actions.comments),
        )
    }

    private fun getFootNote(footNote: FootNoteEntity?): FootNote? {
        val text = footNote?.text ?: return null
        return FootNote(text, getAlignment(footNote.alignment), footNote.mime, getLink(footNote.link))
    }

    private fun getDeck(headlineEntity: HeadlineEntity?): String? {
        return headlineEntity?.deck
    }

    fun getLabel(labelEntity: CompoundLabelEntity?) : CompoundLabel? {
        labelEntity ?: return null
        return CompoundLabel(
                type = getLabelType(labelEntity.type),
                position = getLabelPosition(labelEntity.position),
                text = labelEntity.label?.text,
                style = getStyle(labelEntity.style),
                secondaryText = labelEntity.labelSecondary?.text,
                link = getLink(labelEntity.link),
                alignment = getAlignment(labelEntity.alignment),
                hasArrow = labelEntity.showArrow,
                icon = getIcon(labelEntity.icon),
                form = getForm(labelEntity.form)
        )
    }

    fun getWebComponent(webComponentEntity: WebComponentEntity?) : WebComponent? {
        webComponentEntity?.url ?: return null
        return webComponentEntity.sizes?.let {
            WebComponent(
                url = webComponentEntity.url,
                sizes = it
            )
        }
    }

    fun getCount(countEntity: CountEntity?): Count? {
        countEntity ?: return null
        countEntity.count ?: return null
        countEntity.size ?: return null
        return Count(countEntity.count, getSize(countEntity.size))
    }

    private fun getIcon(icon: LabelIcon?): CompoundLabel.Icon? {
        return when (icon) {
            LabelIcon.CAMERA -> CompoundLabel.Icon.CAMERA
            LabelIcon.CHART -> CompoundLabel.Icon.CHART
            LabelIcon.HEADPHONES -> CompoundLabel.Icon.HEADPHONES
            LabelIcon.ELECTION_STAR -> CompoundLabel.Icon.ELECTION_STAR
            LabelIcon.PLAY -> CompoundLabel.Icon.PLAY
            LabelIcon.OLYMPICS -> CompoundLabel.Icon.OLYMPICS
            LabelIcon.THE_7 -> CompoundLabel.Icon.THE_7
            LabelIcon.WORLD_CUP -> CompoundLabel.Icon.WORLD_CUP
            LabelIcon.POST_PULSE -> CompoundLabel.Icon.POST_PULSE
            LabelIcon.COMMENTS -> CompoundLabel.Icon.COMMENTS
            LabelIcon.EXTERNAL_LINK -> CompoundLabel.Icon.EXTERNAL_LINK
            null -> null
        }
    }

    private fun getStyle(style: LabelStyleEntity?): CompoundLabel.LabelStyle? {
        return when (style) {
            LabelStyleEntity.OPINIONS -> CompoundLabel.LabelStyle.OPINIONS
            LabelStyleEntity.WP_INTELLIGENCE -> CompoundLabel.LabelStyle.WP_INTELLIGENCE
            LabelStyleEntity.THE_SEVEN_LIVE -> CompoundLabel.LabelStyle.THE_SEVEN_LIVE
            null -> null
        }
    }

    private fun getLabelPosition(position: LabelPositionEntity?): CompoundLabel.LabelPosition {
        return when (position) {
            LabelPositionEntity.ABOVE_HEADLINE -> CompoundLabel.LabelPosition.AboveHeadline
            else -> CompoundLabel.LabelPosition.Default
        }
    }

    private fun getLabelType(type: CompoundLabelTypeEntity?): CompoundLabel.Type {
        return when (type) {
            CompoundLabelTypeEntity.FULL_SPAN -> CompoundLabel.Type.FullSpan
            CompoundLabelTypeEntity.PACKAGE -> CompoundLabel.Type.Package
            CompoundLabelTypeEntity.PILL -> CompoundLabel.Type.Pill
            CompoundLabelTypeEntity.MINI_ALL_CAPS -> CompoundLabel.Type.MiniAllCaps
            CompoundLabelTypeEntity.KICKER -> CompoundLabel.Type.Kicker
            CompoundLabelTypeEntity.LIVE_UPDATES -> CompoundLabel.Type.LiveUpdates
            CompoundLabelTypeEntity.EXCLUSIVE -> CompoundLabel.Type.Exclusive
            CompoundLabelTypeEntity.PACKAGE_NESTED -> CompoundLabel.Type.PackageNested
            CompoundLabelTypeEntity.PROMO -> CompoundLabel.Type.Promo
            CompoundLabelTypeEntity.CTA -> CompoundLabel.Type.Cta
            CompoundLabelTypeEntity.NEWSLETTER -> CompoundLabel.Type.Newsletter
            CompoundLabelTypeEntity.BUTTON -> CompoundLabel.Type.Button
            CompoundLabelTypeEntity.COMMENT -> CompoundLabel.Type.Comment
            CompoundLabelTypeEntity.BRAND_PROMO -> CompoundLabel.Type.BrandPromo
            // use kicker as fallback
            else -> CompoundLabel.Type.Kicker
        }
    }


    fun getLink(link: LinkEntity?) : Link? {
        link ?: return null

        return Link(getLinkType(link.type) ?: LinkType.NONE, link.url, link.accessLevel, link.lastModified, link.displayDate, link.subtype)
    }

    private fun getLinkType(linkType: LinkTypeEntity?) : LinkType {
        linkType ?: return LinkType.NONE

        return when (linkType) {
            LinkTypeEntity.ARTICLE -> LinkType.ARTICLE
            LinkTypeEntity.GALLERY -> LinkType.GALLERY
            LinkTypeEntity.NONE -> LinkType.NONE
            LinkTypeEntity.WEB -> LinkType.WEB
            LinkTypeEntity.VIDEO -> LinkType.VIDEO
        }
    }

    private fun getHeadline(headline: HeadlineEntity?) : Headline? {
        headline ?: return null

        return Headline(
            headline.text,
            getSize(headline.size),
            getAlignment(headline.alignment),
            getFontStyle(headline.fontStyle),
            getBulletType(headline.type),
            getHeadlineStyle(headline.style),
            getHeadlineIcon(headline.icon),
        )
    }

    private fun getAlignment(alignment: AlignmentEntity?) : Alignment? {
        alignment ?: return null

        return when (alignment) {
            AlignmentEntity.CENTER -> Alignment.CENTER
            AlignmentEntity.INHERIT -> Alignment.INHERIT
            AlignmentEntity.LEFT -> Alignment.LEFT
            AlignmentEntity.RIGHT -> Alignment.RIGHT
        }
    }

    private fun getSize(size: SizeEntity?) : Size {
        size ?: return Size.MEDIUM

        return when(size) {
            SizeEntity.TINY -> Size.TINY
            SizeEntity.XSMALL -> Size.XSMALL
            SizeEntity.SMALL -> Size.SMALL
            SizeEntity.MEDIUM -> Size.MEDIUM
            SizeEntity.STANDARD -> Size.STANDARD
            SizeEntity.LARGE -> Size.LARGE
            SizeEntity.XLARGE -> Size.XLARGE
            SizeEntity.HUGE -> Size.HUGE
            SizeEntity.MASSIVE -> Size.MASSIVE
            SizeEntity.COLOSSAL -> Size.COLOSSAL
            SizeEntity.JUMBO -> Size.JUMBO
            SizeEntity.GARGANTUAN -> Size.GARGANTUAN
            SizeEntity.COLOSSAL_ALL_CAPS -> Size.COLOSSAL_ALL_CAPS
            SizeEntity.JUMBO_ALL_CAPS -> Size.JUMBO_ALL_CAPS
            SizeEntity.GARGANTUAN_ALL_CPS -> Size.GARGANTUAN_ALL_CAPS
        }
    }

    private fun getFontStyle(fontStyle: FontStyleEntity?) : FontStyle? {

        fontStyle ?: return null

        return when (fontStyle) {
            FontStyleEntity.HIGHLIGHT_STYLE -> FontStyle.HIGHLIGHT_STYLE
            FontStyleEntity.NORMAL_STYLE -> FontStyle.NORMAL_STYLE
            FontStyleEntity.THIN_STYLE -> FontStyle.THIN_STYLE
            FontStyleEntity.REGULAR_STYLE -> FontStyle.REGULAR_STYLE
            FontStyleEntity.BOLD_STYLE -> FontStyle.BOLD_STYLE
            FontStyleEntity.ITALIC_STYLE -> FontStyle.ITALIC_STYLE
            FontStyleEntity.LIGHT_STYLE -> FontStyle.LIGHT_STYLE
        }
    }

    private fun getBulletType(bulletType: BulletTypeEntity?) : BulletType {

        bulletType ?: return BulletType.NORMAL

        return when (bulletType) {
            BulletTypeEntity.BULLET -> BulletType.BULLET
            BulletTypeEntity.NORMAL -> BulletType.NORMAL
        }
    }

    private fun getHeadlineStyle(style: String?): Style? {
        return when (style) {
            "style" -> Style.STYLE
            "conversations" -> Style.CONVERSATIONS
            else -> null
        }
    }

    private fun getHeadlineIcon(icon: com.wapo.flagship.features.grid.HeadlineIcon?): HeadlineIcon? {
        return when (icon) {
            com.wapo.flagship.features.grid.HeadlineIcon.LOGO -> HeadlineIcon.LOGO
            com.wapo.flagship.features.grid.HeadlineIcon.RIPPLE -> HeadlineIcon.RIPPLE
            null -> null
        }
    }

    private fun getBlurbStyle(style: String?): BlurbStyle? {
        return when (style) {
            "conversations" -> BlurbStyle.CONVERSATIONS
            else -> null
        }
    }

    private fun getSubscriptionLinks(subscriptionLinksEntity: SubscriptionLinksEntity?): SubscriptionLinks? {
        subscriptionLinksEntity ?: return null

        return SubscriptionLinks(
            subscriptionLinksEntity.alexa,
            subscriptionLinksEntity.applePodcasts,
            subscriptionLinksEntity.googlePlay,
            subscriptionLinksEntity.iheartRadio,
            subscriptionLinksEntity.radioPublic,
            subscriptionLinksEntity.rss,
            subscriptionLinksEntity.spotify,
            subscriptionLinksEntity.stitcher,
            subscriptionLinksEntity.tuneIn
        )
    }

    private fun getBlurbs(blurbs: BlurbsEntity?) : BlurbList? {
        blurbs ?: return null

        val blurbList = mutableListOf<BlurbItem>()

        blurbs.items?.forEach {
            val blurbItem = getBlurbItem(it)
            if (blurbItem != null) {
                blurbList.add(blurbItem)
            }
        }

        return BlurbList(if (blurbList.isEmpty()) null else blurbList, getBlurbInfo(blurbs.info), getBlurbStyle(blurbs.style))
    }

    private fun getBlurbInfo(blurbInfo: BlurbInfoEntity?) : BlurbInfo? {
        blurbInfo ?: return null

        return BlurbInfo(getSize(blurbInfo.size), getBlurbFontStyle(blurbInfo.fontStyle))
    }

    private fun getBlurbFontStyle(blurbStyle : BlurbStyleEntity?) : BlurbFontStyle? {
        blurbStyle ?: return null

        return when(blurbStyle) {
            BlurbStyleEntity.LIKE_ARTICLE_BODY -> BlurbFontStyle.LIKE_ARTICLE_BODY
            BlurbStyleEntity.NORMAL_STYLE -> BlurbFontStyle.NORMAL_STYLE
        }
    }

    private fun getBlurbItem(blurbItem: BlurbItemEntity?) : BlurbItem? {
        blurbItem ?: return null

        return BlurbItem(blurbItem.text, getBulletType(blurbItem.type), blurbItem.mime)
    }

    private fun getSignature(signature: SignatureEntity?) : Signature? {

        signature ?: return null

        return Signature(
                byLine = signature.byLine,
                alignment = getAlignment(signature.alignment),
                section = signature.section,
                timestamp = signature.timestamp,
                recencyThreshold = signature.recencyThreshold,
                ratingCharacter = signature.ratingCharacter,
                rating = signature.rating,
                dateFormat = signature.dateFormat
        )
    }

    private fun getRelatedLinks(relatedLinks: RelatedLinksEntity?) : RelatedLinks? {
        relatedLinks ?: return null

        val relatedLinkList = mutableListOf<RelatedLinkItem>()

        relatedLinks.items?.forEach {
            val relatedLinkItem = getRelatedLinksItem(it)
            if (relatedLinkItem != null) {
                relatedLinkList.add(relatedLinkItem)
            }
        }

        return RelatedLinks(if (relatedLinkList.isEmpty()) null else relatedLinkList, getRelatedLinksInfo(relatedLinks.info), getLabel(relatedLinks.compoundLabel))
    }

    private fun getRelatedLinksInfo(relatedLinksInfo: RelatedLinksInfoEntity?) : RelatedLinksInfo? {
        relatedLinksInfo ?: return null

        return RelatedLinksInfo(getSize(relatedLinksInfo.size), getRelatedLinksInfoPosition(relatedLinksInfo.position), getRelatedLinksInfoArrangement(relatedLinksInfo.arrangement))
    }

    private fun getRelatedLinksInfoArrangement(relatedLinksInfoArrangement: RelatedLinksInfoEntity.Arrangement?) : Arrangement? {
        relatedLinksInfoArrangement ?: return null

        return when(relatedLinksInfoArrangement) {
            RelatedLinksInfoEntity.Arrangement.NORMAL -> Arrangement.NORMAL
            RelatedLinksInfoEntity.Arrangement.SIDE_BY_SIDE -> Arrangement.SIDE_BY_SIDE
            RelatedLinksInfoEntity.Arrangement.SIDE_BY_SIDE_PIPES -> Arrangement.SIDE_BY_SIDE_PIPES
        }
    }

    private fun getRelatedLinksInfoPosition(relatedLinksInfoPosition: RelatedLinksInfoEntity.Position?) : Position? {
        relatedLinksInfoPosition ?: return null

        return when(relatedLinksInfoPosition) {
            RelatedLinksInfoEntity.Position.BELOW_SIGLINE -> Position.BELOW_SIGLINE
            RelatedLinksInfoEntity.Position.BOTTOM -> Position.BOTTOM
        }
    }

    private fun getRelatedLinksItem(relatedLinksItem: RelatedLinkItemEntity?) : RelatedLinkItem? {
        relatedLinksItem ?: return null

        return RelatedLinkItem(relatedLinksItem.text, relatedLinksItem.link)
    }

    private fun getLiveBlog(liveBlogEntity: LiveBlogEntity?) : LiveBlog? {
        liveBlogEntity ?: return null

        return LiveBlog(liveBlogEntity.primeTimeURL, liveBlogEntity.numberToShow, liveBlogEntity.subtypes?.filterNotNull(), liveBlogEntity.showTimestamps, getLabel(liveBlogEntity.compoundLabel))
    }

    private fun getArrangements(arrangementsEntity: ArrangementsEntity?) : Arrangements? {
        arrangementsEntity ?: return null

        return arrangementsEntity.run {
            Arrangements(getDefaultArrangement(default), getZone(left), getZone(right), getZone(top), getZone(bottom), getZone(sidebar))
        }
    }

    private fun getDefaultArrangement(defaultArrangementEntity: DefaultArrangementEntity?): DefaultArrangement? {
        defaultArrangementEntity ?: return null

        return defaultArrangementEntity.run {
            DefaultArrangement(getZone(left), getZone(right), getZone(top), getZone(bottom), getZone(sidebar))
        }
    }

    private fun getZone(zoneEntity: ZoneEntity?): Zone? {
        zoneEntity ?: return null

        val items = zoneEntity.items?.mapNotNull {
            getSubItemType(it)
        }
            ?.toMutableList()

        return Zone(
            items = items,
            width = zoneEntity.width?.let { MediaMapper.getArtWidth(it) } ?: ArtWidth.FULL_WIDTH,
            valign = getVerticalAlignment(zoneEntity.valign)
        )
    }

    private fun getVerticalAlignment(valign: VerticalAlignmentEntity?): VerticalAlignment? {
        return when (valign) {
            VerticalAlignmentEntity.CENTER -> VerticalAlignment.CENTER
            VerticalAlignmentEntity.BOTTOM -> VerticalAlignment.BOTTOM
            null -> null
        }
    }

    fun getSubItemType(subItemTypeEntity: SubItemTypeEntity?) : SubItemType? {
        return when(subItemTypeEntity) {
            SubItemTypeEntity.AUDIO -> SubItemType.AUDIO
            SubItemTypeEntity.AUDIO_ARTICLE -> SubItemType.AUDIO_ARTICLE
            SubItemTypeEntity.BLURB -> SubItemType.BLURB
            SubItemTypeEntity.BYLINE -> SubItemType.BYLINE
            SubItemTypeEntity.HEADLINE -> SubItemType.HEADLINE
            SubItemTypeEntity.MEDIA -> SubItemType.MEDIA
            SubItemTypeEntity.SLIDESHOW -> SubItemType.SLIDESHOW
            SubItemTypeEntity.LABEL -> SubItemType.LABEL
            SubItemTypeEntity.LIVE_TICKER -> SubItemType.LIVE_TICKER
            SubItemTypeEntity.RELATED_LINKS -> SubItemType.RELATED_LINKS
            SubItemTypeEntity.OLYMPICS_MEDALS -> SubItemType.OLYMPICS_MEDALS
            SubItemTypeEntity.OLYMPICS_SCHEDULE -> SubItemType.OLYMPICS_MEDALS
            SubItemTypeEntity.CTA -> SubItemType.CTA
            SubItemTypeEntity.FOOT_NOTE -> SubItemType.FOOT_NOTE
            SubItemTypeEntity.TOPPER_LABEL -> SubItemType.TOPPER_LABEL
            SubItemTypeEntity.WEB_VIEW -> SubItemType.WEB_EMBED
            SubItemTypeEntity.COUNT -> SubItemType.COUNT
            else -> null
        }
    }

    private fun getOlympicsMedals(olympicsMedalsEntity: OlympicsMedalsEntity?): OlympicsMedals? {
        olympicsMedalsEntity ?: return null
        return OlympicsMedals(
            title = olympicsMedalsEntity.title,
            data = getOlympicsMedalsData(olympicsMedalsEntity.data),
            cta = getOlympicsCta(olympicsMedalsEntity.cta)
        )
    }

    private fun getOlympicsMedalsData(dataEntity: List<OlympicsMedalsEntryEntity?>?): List<OlympicsMedalsEntry> {
        dataEntity ?: return emptyList()
        return dataEntity
            .filterNotNull()
            .map {
            OlympicsMedalsEntry(
                rank = it.rank,
                title = it.title,
                subtitle = it.subtitle,
                icon = it.icon,
                bronze = it.bronze?.toString(),
                silver = it.silver?.toString(),
                gold = it.gold?.toString(),
                total = it.total?.toString()
            )
        }
    }

    private fun getOlympicsSchedule(olympicsScheduleEntity: OlympicsScheduleEntity?): OlympicsSchedule? {
        olympicsScheduleEntity ?: return null
        return OlympicsSchedule(
            title = olympicsScheduleEntity.title,
            data = getOlympicsScheduleData(olympicsScheduleEntity.data),
            cta = getOlympicsCta(olympicsScheduleEntity.cta)
        )
    }

    private fun getOlympicsScheduleData(dataEntity: List<OlympicsScheduleEntryEntity?>?): List<OlympicsScheduleEntry> {
        dataEntity ?: return emptyList()
        return dataEntity
            .filterNotNull()
            .map {
                OlympicsScheduleEntry(
                    title = it.title,
                    subtitle = it.subtitle,
                    icon = it.icon,
                    start = it.start,
                    status = it.status
                )
            }
    }

    private fun getOlympicsCta(ctaEntity: OlympicsCtaEntity?): OlympicsCta? {
        ctaEntity ?: return null
        return OlympicsCta(
            title = ctaEntity.title,
            link = getOlympicsLink(ctaEntity.link)
        )
    }

    private fun getOlympicsLink(linkEntity: OlympicsLinkEntity?): OlympicsLink? {
        linkEntity ?: return null
        return OlympicsLink(
            url = linkEntity.url
        )
    }

    /**
     * Method to construct itid based on the Chris Kankle's algorithm
     * Algorithm:
     * 1. If the link in question already has an itid param, use that as the itid and don’t carry on with the following steps.
     * 2. Harvest link_detail from the feature. It will often be absent.
     * 3. Harvest link_group from the parent table. If absent, harvest link_group from parent chain.
     * 4. Check tracking.section.
     *  4.a. If it contains "homepage" and the link_group does not start with "hp", prepend "hp_" to the link_group.
     *  4.b. If it does NOT contain "homepage" and the link_group does not start with "sf", prepend "sf_" to the link_group.
     * 5. If link_detail is absent
     *  5.a: then set itid to link_group
     *  5.b: otherwise, set itid to [link_group, link_detail].join("_");
     */
    fun getItId(
        url: String?,
        linkGroup: String?,
        linkDetail: String?,
        parentLinkGroup: String?,
        tracking: Tracking?
    ): String? {
        var itId: String? = url?.run {
            // Usually there are no exceptions here. But getQueryParameter can throw "UnsupportedOperationException"
            // if given url isn't a hierarchical URI.
            try {
                Uri.parse(this).getQueryParameter("itid")
            } catch (e: Exception) {
                null
            }
        }
        if (itId == null) {
            itId = linkGroup ?: parentLinkGroup
            if (itId != null) {
                val hpSection = tracking?.section?.contains("homepage") ?: false
                val itIdPrefix = when {
                    itId.startsWith("hp") -> ""
                    itId.startsWith("sf") -> ""
                    hpSection -> "hp_"
                    tracking?.section != null -> {
                        arrayOf("sf", tracking.section, tracking.subsection)
                            .filterNotNull()
                            .joinToString(separator = "_", postfix = "_")
                    }
                    else -> "sf_"
                }
                if (itIdPrefix.isNotEmpty()) {
                    itId = "${itIdPrefix}${itId}"
                }
                if (!linkDetail.isNullOrEmpty()) {
                    itId = "${itId}_${linkDetail}"
                }
            }
        }

        //sanitize itid
        itId = itId
            ?.replace("\\W+".toRegex(), "_")
            ?.lowercase()

        return itId
    }

    fun getExcerpt(excerpt: ExcerptEntity?): Excerpt? {
        excerpt ?: return null
        return Excerpt(excerpt.text)
    }

    fun getForm(formEntity: FormEntity?) : Form? {
        formEntity ?: return null
        val formFields = formEntity.fields?.mapNotNull { getFormField(it) }
        if (formFields.isNullOrEmpty()) return null
        formEntity.action ?: return null
        return Form(
            action = formEntity.action,
            fields = formFields
        )
    }

    private fun getFormField(formFieldEntity: FormFieldEntity?) : FormField? {
        formFieldEntity ?: return null
        formFieldEntity.type ?: return null
        formFieldEntity.param ?: return null
        return FormField(
            type = formFieldEntity.type,
            param = formFieldEntity.param,
            placeHolder = formFieldEntity.placeholder.orEmpty()
        )
    }
}

private fun setStoryBleed(homepageStoryEntity: HomepageStoryEntity, homepageStoryModel: HomepageStory) {
    val mediaBleed = getBleedFromEntity(homepageStoryEntity.media?.bleed)
    val slideShowBleed = getBleedFromEntity(homepageStoryEntity.slideshow?.bleed)
    val olympicsMedalsBleed = getBleedFromEntity(homepageStoryEntity.olympicsMedals?.bleed)
    val olympicsScheduleBleed = getBleedFromEntity(homepageStoryEntity.olympicsSchedule?.bleed)

    if (mediaBleed != Bleed.NONE) {
        homepageStoryModel.bleed = mediaBleed
        homepageStoryModel.bleedItemType = BleedItemType.MEDIA
    } else if (slideShowBleed != Bleed.NONE) {
        homepageStoryModel.bleed = slideShowBleed
        homepageStoryModel.bleedItemType = BleedItemType.SLIDESHOW
    } else if (olympicsMedalsBleed != Bleed.NONE) {
        homepageStoryModel.bleed = olympicsMedalsBleed
        homepageStoryModel.bleedItemType = BleedItemType.OLYMPICS
    } else if (olympicsScheduleBleed != Bleed.NONE) {
        homepageStoryModel.bleed = olympicsScheduleBleed
        homepageStoryModel.bleedItemType = BleedItemType.OLYMPICS
    } else {
        Bleed.NONE
        homepageStoryModel.bleedItemType = BleedItemType.NONE
    }
}

private fun getBleedFromEntity(bleedEntity: BleedEntity?): Bleed {
    return when (bleedEntity) {
        BleedEntity.FULL -> Bleed.FULL
        BleedEntity.CONTAINER -> Bleed.CONTAINER
        else -> Bleed.NONE
    }
}
