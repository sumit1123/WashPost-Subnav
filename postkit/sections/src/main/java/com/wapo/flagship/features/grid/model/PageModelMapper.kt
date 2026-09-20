package com.wapo.flagship.features.grid.model

import com.wapo.flagship.features.grid.AdBaseItemEntity
import com.wapo.flagship.features.grid.AdItemEntity
import com.wapo.flagship.features.grid.BarEntity
import com.wapo.flagship.features.grid.BaseItemEntity
import com.wapo.flagship.features.grid.CardEntity
import com.wapo.flagship.features.grid.CarouselBaseItemEntity
import com.wapo.flagship.features.grid.CarouselItemEntity
import com.wapo.flagship.features.grid.ChainEntity
import com.wapo.flagship.features.grid.DividerLayoutEntity
import com.wapo.flagship.features.grid.DividerStyle
import com.wapo.flagship.features.grid.DividersEntity
import com.wapo.flagship.features.grid.ElectionsDelayEntity
import com.wapo.flagship.features.grid.InlineOfferEntity
import com.wapo.flagship.features.grid.GridEntity
import com.wapo.flagship.features.grid.GridLocationEntity
import com.wapo.flagship.features.grid.HabitTilesEntity
import com.wapo.flagship.features.grid.HomepageStoryEntity
import com.wapo.flagship.features.grid.ItemEntity
import com.wapo.flagship.features.grid.ItemType
import com.wapo.flagship.features.grid.LayoutAttributesEntity
import com.wapo.flagship.features.grid.RegionEntity
import com.wapo.flagship.features.grid.ScreenSizeLayout
import com.wapo.flagship.features.grid.SectionTopperEntity
import com.wapo.flagship.features.lowdatamodelbanner.model.LowDataBanner
import com.wapo.flagship.features.grid.SeparatorEntity
import com.wapo.flagship.features.grid.SeparatorSizeEntity
import com.wapo.flagship.features.grid.TableEntity
import com.wapo.flagship.features.grid.Tracking
import com.wapo.flagship.features.grid.VoteEntity
import com.wapo.flagship.features.newsprint.NewsprintHelper

object PageModelMapper {

    fun getGrid(gridResponse: GridEntity,
                isLoggedIn: Boolean = false,
                pageConfig: PageConfig): Grid {
        val grid = Grid(
                tracking = gridResponse.tracking,
                checksum = gridResponse.checksum,
                cards = getCards(gridResponse.cards))
        grid.regions.addAll(gridResponse.regions.map {
            getRegion(it, grid.tracking, isLoggedIn, pageConfig)
        })

        return grid
    }

    fun getRegion(region: RegionEntity, tracking: Tracking?, isLoggedIn: Boolean, pageConfig: PageConfig): Region {
        val regionChains = region.items.mapNotNull {
            getChainFromBaseItem(it, tracking, isLoggedIn, pageConfig)
        }.toMutableList()
        // Add the Global Banner Chain as the first item. Doing it this way allows us to leverage full bleed functionality
        if (tracking?.section?.contains("homepage") == true ||
            tracking?.section in listOf("audio", "games")) {
            regionChains.add(0, globalBannerChain())
            regionChains.add(1, lowDataBannerChain())
        }
        tracking?.section?.let {
            if (NewsprintHelper.isNewsprintSection(it)) {
                regionChains.add(0, newsprintChain())
            }
        }
        return Region(region.location, regionChains)
    }

    fun getChain(chainEntity: ChainEntity, tracking: Tracking?, pageConfig: PageConfig): Chain {
        return Chain().apply {
            id = chainEntity.id
            items = chainEntity.items.mapNotNull {
                getTable(it, chainEntity.linkGroup, tracking, pageConfig)
            }.toMutableList()
            label = HomepageStoryMapper.getLabel(chainEntity.label)
            cta = HomepageStoryMapper.getLabel(chainEntity.label)
            dividers = getDividerMap(chainEntity)

            //insert chain label
            val label = HomepageStoryMapper.getLabel(chainEntity.label)
            if (label != null) {
                val item = LabelItem(label)
                insertChainItemFromStart(this, item)
            }
            displayContext = chainEntity.displayContext

            chainEntity.layoutAttributes?.let {
                layoutAttributes = getLayoutAttributes(it)
            }
            insertChainCta(chainEntity, this)
        }
    }

    private fun insertChainItemFromStart(chainModel: Chain, item: Item) {
        for (table in chainModel.items) {
            shiftRows(table.layoutAttributes, 1)
        }

        for (divider in chainModel.dividers) {
            divider.value.horizontal.forEach {
                if (it.row >= 0) it.row++
            }
        }

        item.layoutAttributes = createDefaultLayoutAttributes()
        val wrappingTable = Table()
        wrappingTable.items.add(item)
        wrappingTable.layoutAttributes = createDefaultLayoutAttributes()
        chainModel.items.add(0, wrappingTable)
    }

    private fun insertChainCta(chainEntity: ChainEntity, chainModel: Chain) {
        val chainCta = HomepageStoryMapper.getLabel(chainEntity.cta)
        if (chainCta != null) {
            val labelItem = LabelItem(chainCta, true)
            labelItem.layoutAttributes = createDefaultLayoutAttributes()
            val wrappingTable = Table()
            wrappingTable.items.add(labelItem)
            //add cta in a wrapping table in the last row
            val lastRowExtraSmall = chainModel.items.lastOrNull { it.layoutAttributes.extraSmall != null }
            val lastRowSmall = chainModel.items.lastOrNull { it.layoutAttributes.small != null }
            val lastRowMedium = chainModel.items.lastOrNull { it.layoutAttributes.medium != null }
            val lastRowLarge= chainModel.items.lastOrNull { it.layoutAttributes.large != null }
            val lastRowExtraLarge= chainModel.items.lastOrNull { it.layoutAttributes.extraLarge != null }
            wrappingTable.layoutAttributes = LayoutAttributes().apply {
                extraSmall = GridLocation(span = 1, row = lastRowExtraSmall?.layoutAttributes?.extraSmall?.row?.plus( 1) ?: 0,
                    column = lastRowExtraSmall?.layoutAttributes?.extraSmall?.column ?: 0, rowSpan = 1)
                small = GridLocation(span = 10, row = lastRowSmall?.layoutAttributes?.small?.row?.plus( 1) ?: 0,
                    column = lastRowSmall?.layoutAttributes?.small?.column ?: 0, rowSpan = 1)
                medium = GridLocation(span = 12, row = lastRowMedium?.layoutAttributes?.medium?.row?.plus( 1) ?: 0,
                    column = lastRowMedium?.layoutAttributes?.medium?.column ?: 0, rowSpan = 1)
                large = GridLocation(span = 16, row = lastRowLarge?.layoutAttributes?.large?.row?.plus( 1) ?: 0,
                    column = lastRowLarge?.layoutAttributes?.large?.column ?: 0, rowSpan = 1)
                extraLarge = GridLocation(span = 20, row = lastRowExtraLarge?.layoutAttributes?.extraLarge?.row?.plus( 1) ?: 0,
                    column = lastRowExtraLarge?.layoutAttributes?.extraLarge?.column ?: 0, rowSpan = 1)
            }
            chainModel.items.add(wrappingTable)
        }
    }

    fun getChainFromBaseItem(item: BaseItemEntity?, tracking: Tracking?, isLoggedIn: Boolean, pageConfig: PageConfig): Chain? {
        item ?: return null
        return when (item) {
            is ChainEntity -> {
                // removes personalized podcast grid if user is not logged in
                if (item.id == "personalized-podcast-grid" && !isLoggedIn) {
                    return null
                }
                getChain(item, tracking, pageConfig)
            }
            is SeparatorEntity -> convertSeparatorIntoChain(item)
            is AdBaseItemEntity -> convertAdIntoChain(item, pageConfig)
            is BarEntity -> null // breaking news bar is supported but handled out of grid
            else -> convertFeatureIntoChain(item, pageConfig)
        }
    }

    /**
     * Takes a item that is not in a chain/table and puts it in one
     */
    fun convertFeatureIntoChain(baseItemEntity: BaseItemEntity, pageConfig: PageConfig): Chain {
        val chain = Chain()
        val table = Table()
        val layoutAttributes = createDefaultLayoutAttributes()
        table.layoutAttributes = layoutAttributes
        val item = getBaseItem(baseItemEntity, pageConfig)
        table.items.add(item!!)
        chain.items.add(table)
        return chain
    }

    /**
     * Create and add a chain for Global Banner. This way we can leverage bleed attribute.
     */
    private fun globalBannerChain(): Chain {
        val chain = Chain()
        val table = Table()
        val layoutAttributes = createDefaultLayoutAttributes()
        table.layoutAttributes = layoutAttributes
        val item = GlobalBanner(GlobalBannerState.Unknown)
        item.layoutAttributes = createDefaultLayoutAttributes()
        item.forceFullBleed = true
        table.items.add(item)
        chain.items.add(table)
        return chain
    }

    private fun newsprintChain(): Chain {
        val chain = Chain()
        val table = Table()
        val layoutAttributes = createDefaultLayoutAttributes()
        table.layoutAttributes = layoutAttributes
        val item = NewsprintTopCard()
        item.layoutAttributes = createDefaultLayoutAttributes()
        table.items.add(item)
        chain.items.add(table)
        return chain
    }

    /**
     * Create and add a chain for Global Banner. This way we can leverage bleed attribute.
     */
    private fun lowDataBannerChain(): Chain {
        val chain = Chain()
        val table = Table()
        val layoutAttributes = createDefaultLayoutAttributes()
        table.layoutAttributes = layoutAttributes
        val item = LowDataBanner(false)
        item.layoutAttributes = createDefaultLayoutAttributes()
        item.forceFullBleed = true
        table.items.add(item)
        chain.items.add(table)
        return chain
    }

    /**
     * Takes a separator object that is between chains and converts it into a chain/table and treats it like a feature item
     */
    fun convertSeparatorIntoChain(separatorEntity: SeparatorEntity): Chain {
        val chain = Chain()
        separatorEntity.layoutAttributes?.let {
            chain.layoutAttributes = getLayoutAttributes(it)
        }
        val table = Table()
        val layoutAttributes = createDefaultLayoutAttributes()
        table.layoutAttributes = layoutAttributes
        table.isSeparator = true
        val separator = Separator(getSeparatorSize(separatorEntity.size), separatorEntity.line)
        separator.layoutAttributes = createDefaultLayoutAttributes()
        table.items.add(separator)
        chain.items.add(table)
        chain.isSeparator = true
        return chain
    }

    fun convertAdIntoChain(adEntity: AdBaseItemEntity, pageConfig: PageConfig): Chain {
        val chain = Chain()
        adEntity.layoutAttributes?.let {
            chain.layoutAttributes = getLayoutAttributes(it)
        }

        val table = Table()
        table.layoutAttributes = createDefaultLayoutAttributes()

        val item = getBaseItem(adEntity, pageConfig)

        if (item != null) {
            table.items.add(item)
            chain.items.add(table)
        }

        return chain
    }

    private fun getSeparatorSize(separatorSizeEntity: SeparatorSizeEntity?): SeparatorSize {
        return when (separatorSizeEntity) {
            SeparatorSizeEntity.XSMALL -> SeparatorSize.XSMALL
            SeparatorSizeEntity.SMALL -> SeparatorSize.SMALL
            SeparatorSizeEntity.LARGE -> SeparatorSize.LARGE
            else -> SeparatorSize.SMALL
        }
    }

    fun getTable(tableEntity: TableEntity?, parentLinkGroup: String?, tracking: Tracking?, pageConfig: PageConfig): Table? {
        tableEntity ?: return null
        val tableModel = Table()
        val tabelModelItems: List<Item> = getTableItems(tableEntity.items, tableEntity.linkGroup ?: parentLinkGroup, tracking, pageConfig)
        tableModel.items = tabelModelItems.toMutableList()
        tableModel.dividers = getDividerMap(tableEntity)
        tableModel.layoutAttributes = getLayoutAttributes(tableEntity.layoutAttributes)
        tableModel.id = tableEntity.id
        tableModel.label = HomepageStoryMapper.getLabel(tableEntity.label)
        tableModel.cta = HomepageStoryMapper.getLabel(tableEntity.cta)

        //insert table label
        val tableLabel = HomepageStoryMapper.getLabel(tableEntity.label)
        if (tableLabel != null) {
            val item = LabelItem(tableLabel)
            insertTableItemFromStart(tableModel, item)
        }

        insertTableCta(tableEntity, tableModel)
        return tableModel
    }

    fun getTableItems(items: List<ItemEntity?>, parentLinkGroup: String?, tracking: Tracking?, pageConfig: PageConfig): List<Item> {
        return items.mapNotNull {
            getItem(it, parentLinkGroup, tracking, pageConfig)
        }
    }

    fun getItem(item: ItemEntity?, parentLinkGroup: String?, tracking: Tracking?, pageConfig: PageConfig): Item? {
        item ?: return null
        return when (item) {
            is HomepageStoryEntity -> HomepageStoryMapper.getHomepageStoryModel(item, parentLinkGroup, tracking, pageConfig)
            is AdItemEntity -> AdMapper.getAd(item)
            is VoteEntity -> getVote(item)
            is ElectionsDelayEntity -> getElectionsDelayMessage(item)
            is CarouselItemEntity ->
                when (item.itemType) {
                    ItemType.CAROUSEL.toString() -> CarouselMapper.getCarouselItem(item, pageConfig)
                    ItemType.CAROUSEL_VIDEO.toString() -> CarouselMapper.getCarouselVideoItem(item, getLayoutAttributes(item.layoutAttributes), pageConfig)
                    ItemType.CAROUSEL_AUDIO.toString() -> CarouselMapper.getCarouselAudioItem(item, getLayoutAttributes(item.layoutAttributes), pageConfig)
                    ItemType.IMMERSION_CAROUSEL.toString() -> CarouselMapper.getCarouselImmersionItem(item, getLayoutAttributes(item.layoutAttributes), parentLinkGroup, tracking, pageConfig)
                    ItemType.CAROUSEL_PERSONALIZED_PODCAST.toString() -> CarouselMapper.getCarouselPersonalizedPodcastItem(item, getLayoutAttributes(item.layoutAttributes))
                    ItemType.CAROUSEL_AUDIO_PLAYLIST_DEPRECATED.toString(), ItemType.CAROUSEL_AUDIO_PLAYLIST.toString() -> CarouselMapper.getCarouselAudioPlaylistItem(item, getLayoutAttributes(item.layoutAttributes))
                    ItemType.CAROUSEL_RECIPE.toString() -> CarouselMapper.getCarouselRecipeItem(item, getLayoutAttributes(item.layoutAttributes), parentLinkGroup, tracking, pageConfig)
                    ItemType.CAROUSEL_LIVE_IMAGE.toString() -> CarouselMapper.getCarouselImmersionItem(item, getLayoutAttributes(item.layoutAttributes), parentLinkGroup, tracking, pageConfig)
                    ItemType.CAROUSEL_COMMENTS.toString() -> CarouselMapper.getCarouselComments(item, getLayoutAttributes(item.layoutAttributes), parentLinkGroup, tracking)
                    ItemType.CAROUSEL_EXTERNAL.toString() -> CarouselMapper.getCarouselExternalItems(item, getLayoutAttributes(item.layoutAttributes), parentLinkGroup, tracking, pageConfig)
                    ItemType.CAROUSEL_SEVEN_LIVE.toString() -> CarouselMapper.getCarouselSevenLive(item, getLayoutAttributes(item.layoutAttributes), parentLinkGroup, tracking, pageConfig)
                    else -> null
                }
            is HabitTilesEntity -> HabitTilesMapper.getHabitTiles(item)
            is InlineOfferEntity -> InlineOfferMapper.getInlineOffer(item)
            is SectionTopperEntity -> SectionTopperMapper.getSectionTopper(item)
            else ->
                null
        }
    }

    fun getBaseItem(baseItemEntity: BaseItemEntity?, pageConfig: PageConfig): Item? {
        return when (baseItemEntity) {
            is AdBaseItemEntity -> AdMapper.getBaseAd(baseItemEntity)
            is CarouselBaseItemEntity -> CarouselMapper.getCarouselBaseItem(baseItemEntity, pageConfig)
            is HabitTilesEntity -> HabitTilesMapper.getHabitTiles(baseItemEntity, true)
            is SectionTopperEntity -> SectionTopperMapper.getSectionTopper(baseItemEntity, true)
            is InlineOfferEntity -> InlineOfferMapper.getInlineOffer(baseItemEntity)
            else ->
                null
        }
    }

    fun getLayoutAttributes(layoutAttributes: LayoutAttributesEntity?): LayoutAttributes {
        val defaults = createDefaultLayoutAttributes()

        return LayoutAttributes().apply {
            extraLarge = getGridLocation(layoutAttributes?.extraLarge, defaults.extraLarge)
            large = getGridLocation(layoutAttributes?.large, defaults.large)
            medium = getGridLocation(layoutAttributes?.medium, defaults.medium)
            small = getGridLocation(layoutAttributes?.small, defaults.small)
            extraSmall = getGridLocation(layoutAttributes?.extraSmall, defaults.extraSmall)
        }
    }

    fun getGridLocation(gridLocation: GridLocationEntity?, defaultLocation: GridLocation?): GridLocation? {
        gridLocation ?: return null
        val resolvedSpan = if (gridLocation.span > 0) gridLocation.span else defaultLocation?.span ?: 0

        return GridLocation(resolvedSpan, gridLocation.row, gridLocation.column, gridLocation.rowSpan)
    }

    fun getDividerMap(table: TableEntity): MutableMap<String, Dividers> {

        val dividerMap = mutableMapOf<String, Dividers>()
        val xlargeDividers = getDividers(table.getDividers(ScreenSizeLayout.XLARGE))
        if (xlargeDividers != null) {
            dividerMap["xlarge"] = xlargeDividers
        }

        val largeDividers = getDividers(table.getDividers(ScreenSizeLayout.LARGE))
        if (largeDividers != null) {
            dividerMap["large"] = largeDividers
        }

        val mediumDividers = getDividers(table.getDividers(ScreenSizeLayout.MEDIUM))
        if (mediumDividers != null) {
            dividerMap["medium"] = mediumDividers
        }

        val smallDividers = getDividers(table.getDividers(ScreenSizeLayout.SMALL))
        if (smallDividers != null) {
            dividerMap["small"] = smallDividers
        }

        val xsmallDividers = getDividers(table.getDividers(ScreenSizeLayout.XSMALL))
        if (xsmallDividers != null) {
            dividerMap["xsmall"] = xsmallDividers
        }

        return dividerMap
    }

    fun getDividerMap(chain: ChainEntity): MutableMap<String, Dividers> {

        val dividerMap = mutableMapOf<String, Dividers>()
        val xlargeDividers = getDividers(chain.getDividers(ScreenSizeLayout.XLARGE))
        if (xlargeDividers != null) {
            dividerMap["xlarge"] = xlargeDividers
        }

        val largeDividers = getDividers(chain.getDividers(ScreenSizeLayout.LARGE))
        if (largeDividers != null) {
            dividerMap["large"] = largeDividers
        }

        val mediumDividers = getDividers(chain.getDividers(ScreenSizeLayout.MEDIUM))
        if (mediumDividers != null) {
            dividerMap["medium"] = mediumDividers
        }

        val smallDividers = getDividers(chain.getDividers(ScreenSizeLayout.SMALL))
        if (smallDividers != null) {
            dividerMap["small"] = smallDividers
        }

        val xsmallDividers = getDividers(chain.getDividers(ScreenSizeLayout.XSMALL))
        if (xsmallDividers != null) {
            dividerMap["xsmall"] = xsmallDividers
        }

        return dividerMap
    }

    fun getDividers(dividers: DividersEntity?): Dividers? {

        dividers ?: return null

        val horizontalDividers = dividers.horizontal.map {
            getDividerLayout(it)
        }

        val verticalDividers = dividers.vertical.map {
            getDividerLayout(it)
        }

        return Dividers().apply {
            vertical.addAll(verticalDividers)
            horizontal.addAll(horizontalDividers)
        }
    }

    fun getDividerLayout(dividerLayout: DividerLayoutEntity): DividerLayout {

        return DividerLayout().apply {
            row = dividerLayout.row
            column = dividerLayout.column
            span = dividerLayout.span
            rowSpan = dividerLayout.rowSpan
            style = dividerLayout.style ?: DividerStyle.NORMAL
        }
    }

    private fun insertTableItemFromStart(tableModel: Table, wrapperItem: Item) {
        for (item in tableModel.items) {
            shiftRows(item.layoutAttributes, 1)
        }
        for (divider in tableModel.dividers) {
            divider.value.horizontal.forEach {
                if (it.row >= 0) it.row++
            }
            divider.value.vertical.forEach {
                if (it.row >=0) it.row++
            }
        }
        wrapperItem.layoutAttributes = createDefaultLayoutAttributes()
        // copy spans from parent table spans
        wrapperItem.layoutAttributes.traverse(tableModel.layoutAttributes) { us, them -> us.span = them.span }
        tableModel.items.add(0, wrapperItem)
    }

    private fun insertTableCta(tableEntity: TableEntity, tableModel: Table) {
        val tableCta = HomepageStoryMapper.getLabel(tableEntity.cta)
        if (tableCta != null) {
            val labelItem = LabelItem(tableCta, true)
            val lastRowExtraSmall = tableModel.items.lastOrNull { it.layoutAttributes.extraSmall != null }
            val lastRowSmall = tableModel.items.lastOrNull { it.layoutAttributes.small != null }
            val lastRowMedium = tableModel.items.lastOrNull { it.layoutAttributes.medium != null }
            val lastRowLarge= tableModel.items.lastOrNull { it.layoutAttributes.large != null }
            val lastRowExtraLarge= tableModel.items.lastOrNull { it.layoutAttributes.extraLarge != null }

            labelItem.layoutAttributes = LayoutAttributes().apply {
                extraSmall = GridLocation(span = 1, row = lastRowExtraSmall?.layoutAttributes?.extraSmall?.row?.plus( 1) ?: 0,
                    column = lastRowExtraSmall?.layoutAttributes?.extraSmall?.column ?: 0, rowSpan = 1)
                small = GridLocation(span = 10, row = lastRowSmall?.layoutAttributes?.small?.row?.plus( 1) ?: 0,
                    column = lastRowSmall?.layoutAttributes?.small?.column ?: 0, rowSpan = 1)
                medium = GridLocation(span = 12, row = lastRowMedium?.layoutAttributes?.medium?.row?.plus( 1) ?: 0,
                    column = lastRowMedium?.layoutAttributes?.medium?.column ?: 0, rowSpan = 1)
                large = GridLocation(span = 16, row = lastRowLarge?.layoutAttributes?.large?.row?.plus( 1) ?: 0,
                    column = lastRowLarge?.layoutAttributes?.large?.column ?: 0, rowSpan = 1)
                extraLarge = GridLocation(span = 20, row = lastRowExtraLarge?.layoutAttributes?.extraLarge?.row?.plus( 1) ?: 0,
                    column = lastRowExtraSmall?.layoutAttributes?.extraLarge?.column ?: 0, rowSpan = 1)
            }
            // copy label spans from parent table spans
            labelItem.layoutAttributes.traverse(tableModel.layoutAttributes) { us, them -> us.span = them.span }
            tableModel.items.add(labelItem)
        }
    }

    fun createDefaultLayoutAttributes(): LayoutAttributes {
        return LayoutAttributes().apply {
            extraSmall = GridLocation(span = 1, row = 0, column = 0, rowSpan = 1)
            small = GridLocation(span = 10, row = 0, column = 0, rowSpan = 1)
            medium = GridLocation(span = 12, row = 0, column = 0, rowSpan = 1)
            large = GridLocation(span = 16, row = 0, column = 0, rowSpan = 1)
            extraLarge = GridLocation(span = 20, row = 0, column = 0, rowSpan = 1)
        }
    }

    fun LayoutAttributes?.traverse(other: LayoutAttributes?, f: (GridLocation, GridLocation) -> Unit) {
        this ?: return
        extraSmall?.let { us -> other?.extraSmall?.let { them -> f(us, them) } }
        small?.let { us -> other?.small?.let { them -> f(us, them) } }
        medium?.let { us -> other?.medium?.let { them -> f(us, them) } }
        large?.let { us -> other?.large?.let { them -> f(us, them) } }
        extraLarge?.let { us -> other?.extraLarge?.let { them -> f(us, them) } }
    }

    private fun shiftRows(layoutAttributes: LayoutAttributes?, number: Int) {
        layoutAttributes?.extraSmall?.run { row += number }
        layoutAttributes?.small?.run { row += number }
        layoutAttributes?.medium?.run { row += number }
        layoutAttributes?.large?.run { row += number }
        layoutAttributes?.extraLarge?.run { row += number }
    }

    fun getCards(cardEntity: CardEntity?) : Cards? {
        cardEntity ?: return null
        cardEntity.extraSmall ?: return null

        //only support xsmall for now
        val grids = cardEntity.extraSmall.grids
        val tables = cardEntity.extraSmall.tables
        val features = cardEntity.extraSmall.features

        return Cards(CardLayout(grids, tables, features))
    }
}