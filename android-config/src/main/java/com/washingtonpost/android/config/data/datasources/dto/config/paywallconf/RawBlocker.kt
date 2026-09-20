package com.washingtonpost.android.config.data.datasources.dto.config.paywallconf

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywallconf.Blocker
import com.washingtonpost.android.config.domain.models.config.paywallconf.BlockerVersion
import com.washingtonpost.android.config.domain.models.config.paywallconf.Component
import com.washingtonpost.android.config.domain.models.config.paywallconf.ComponentType
import com.washingtonpost.android.config.domain.models.config.paywallconf.Product
import com.washingtonpost.android.config.domain.models.config.paywallconf.SplitType

@JsonClass(generateAdapter = true)
data class RawBlocker(
    val components: List<RawComponent>? = null,
    val name: String? = null,
    val category: String? = null,
    val promoId: String? = null,
    val trialType: String? = null,
    val items: List<RawProduct>? = null,
    // Legacy key: older configs used "products" instead of "items". Kept for backward compatibility.
    val products: List<RawProduct>? = null,
    val versions: List<RawBlockerVersion>? = null,
    val padding: Int? = null
) {
    fun mapToDomain(): Blocker {
        val effectiveItems = items ?: products
        return Blocker(
            components = components?.map { it.mapToDomain() },
            name = name,
            category = category,
            promoId = promoId,
            trialType = trialType,
            items = effectiveItems?.map { it.mapToDomain() },
            versions = versions?.map { it.mapToDomain() },
            padding = padding
        )
    }
}


@JsonClass(generateAdapter = true)
data class RawBlockerVersion(
    @Json(name = "version") val version: Int? = null,
    @Json(name = "components") val components: List<RawComponent>? = null,
    @Json(name = "items") val items: List<RawProduct>? = null,
    // Legacy key: older configs used "products" instead of "items". Kept for backward compatibility.
    @Json(name = "products") val products: List<RawProduct>? = null,
) {
    fun mapToDomain(): BlockerVersion {
        val effectiveItems = items ?: products
        return BlockerVersion(
            version = version,
            components = components?.map { it.mapToDomain() },
            items = effectiveItems?.map { it.mapToDomain() }
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawComponent(
    /**
     * Specific for phones. Used to specify
     * spacing size.
     */
    @Json(name = "compact")
    val compact: Int? = null,

    /**
     * Nested Components list.
     */
    @Json(name = "components")
    val components: List<RawComponent>? = null,

    /**
     * Need to figure out what this is for
     */
    @Json(name = "defaultSelection")
    val defaultSelection: Int? = null,

    /**
     * Value for Terminated Sub
     */
    @Json(name = "ended-content")
    val endedContent: String? = null,

    /**
     * Value for Terminated Sub - including price
     */
    @Json(name = "ended-content-dynamic")
    val endedContentDynamic: String? = null,

    /**
     * Value for New Sub
     */
    @Json(name = "none-content")
    val noneContent: String? = null,

    /**
     * Value for New Sub - including price
     */
    @Json(name = "none-content-dynamic")
    val noneContentDynamic: String? = null,

    /**
     * Value for Gift Expired link
     */
    @Json(name = "gift-expired")
    val giftExpired: String? = null,

    /**
     * Value for Gift Invalid link
     */
    @Json(name = "gift-invalid")
    val giftInvalid: String? = null,

    /**
     * Value for Amazon Offer (currently used for $1 for 6 months)
     */
    @Json(name = "amazon-offer")
    val amazonOffer: String? = null,

    /**
     * Value for Only at The Post
     */
    @Json(name = "oatp")
    val oatp: String? = null,

    /**
     * List of components for separating product items
     */
    @Json(name = "productSeparator")
    val productSeparator: List<RawComponent>? = null,

    /**
     * Specific for tablets. Used to specify
     * spacing size.
     */
    @Json(name = "regular")
    val regular: Int? = null,

    /**
     * Used to specify spacing between items
     */
    @Json(name = "space")
    val space: Int? = null,

    /**
     * How to group products
     */
    @Json(name = "split")
    val split: RawSplitType? = null,

    /**
     * Text value for component
     * Value can be a string or a list of strings.
     * So type is `Any`.
     */
    @Json(name = "text")
    val text: Any? = null,

    /**
     * index at which separator should be placed
     */
    @Json(name = "index")
    val index: Int? = null,

    /**
     * Types of UI Components available for rendering
     */
    @Json(name = "type")
    val type: RawComponentType? = null,

    /**
     * Name of local image resource
     */
    @Json(name = "name")
    val name: String? = null,


    @Json(name = "note")
    val note: String? = null,

    /**
     * URL of remote image resource - light mode
     */
    @Json(name = "urlLight")
    val urlLight: String? = null,

    /**
     * URL of remote image resource - dark mode
     */
    @Json(name = "urlDark")
    val urlDark: String? = null,

    /**
     * URL of remote image resource - fallback / legacy
     */
    @Json(name = "imageUrl")
    val imageUrl: String? = null,

    /**
     * Width of image resource in dp
     */
    @Json(name = "width")
    val width: Int? = null,

    /**
     * Height of image resource in dp
     */
    @Json(name = "height")
    val height: Int? = null,

    /**
     * Designates variant of component
     * e.g. "h1" "h2" "h3" "h4" for different Title component sizes
     */
    @Json(name = "variation")
    val variation: String? = null,

    @Json(name = "textList")
    val textList: List<String>? = null,

    @Json(name = "separator")
    val separator: RawComponent? = null,

    @Json(name = "textSpacer")
    val textSpacer: RawComponent? = null,

    @Json(name = "textSeparatorSpacer")
    val textSeparatorSpacer: RawComponent? = null,

    /*
     * List of labels for Primary and Secondary strings for Promo
     */
    @Json(name = "labels")
    val labels: List<String?>? = null,

    /*
     * ComponentVisibility values, separated by a space.
     * Determines component visibility.
     */
    @Json(name = "visibility")
    val visibility: String? = null,

    @Json(name = "wide")
    val wide: Boolean? = null,

    @Json(name = "open")
    val open: String? = null,

    /**
     * Product identifier for the button component.
     */
    @Json(name = "product")
    val product: String? = null,
) {
    fun mapToDomain(): Component {
        return Component(
            compact = compact,
            components = components?.map { it.mapToDomain() },
            defaultSelection = defaultSelection,
            endedContent = endedContent,
            endedContentDynamic = endedContentDynamic,
            noneContent = noneContent,
            noneContentDynamic = noneContentDynamic,
            giftExpired = giftExpired,
            giftInvalid = giftInvalid,
            amazonOffer = amazonOffer,
            oatp = oatp,
            productSeparator = productSeparator?.map { it.mapToDomain() },
            regular = regular,
            space = space,
            split = split?.mapToDomain(),
            text = text,
            index = index,
            type = type?.mapToDomain(),
            name = name,
            urlLight = urlLight,
            urlDark = urlDark,
            imageUrl = imageUrl,
            width = width,
            height = height,
            variation = variation,
            textList = textList,
            separator = separator?.mapToDomain(),
            textSpacer = textSpacer?.mapToDomain(),
            textSeparatorSpacer = textSeparatorSpacer?.mapToDomain(),
            labels = labels,
            visibility = visibility,
            wide = wide,
            open = open,
            product = product
        )
    }
}

/**
 * Determines how to display choice toggle.
 */
enum class RawSplitType {
    @Json(name = "intervals") INTERVALS,
    @Json(name = "tiers") TIERS,
    @Json(name = "unknown") UNKNOWN;

    fun mapToDomain(): SplitType {
        return when (this) {
            INTERVALS -> SplitType.INTERVALS
            TIERS -> SplitType.TIERS
            UNKNOWN -> SplitType.UNKNOWN
        }
    }
}

/**
 * Enum for string representation of type in component element. See config.json "blockers" section for more details.
 */
enum class RawComponentType {
    @Json(name = "spacer") SPACER,
    @Json(name = "promo") PROMO,
    @Json(name = "title") TITLE,
    @Json(name = "subtitle") SUBTITLE,
    @Json(name = "contact_us") CONTACT_US,
    @Json(name = "subscribe") SUBSCRIBE,
    @Json(name = "choice") CHOICE,
    @Json(name = "offer") OFFER,
    @Json(name = "name") NAME,
    @Json(name = "text") TEXT,
    @Json(name = "button") BUTTON,
    @Json(name = "separator") SEPARATOR,
    @Json(name = "tiles") TILES,
    @Json(name = "restore") RESTORE,
    @Json(name = "terms") TERMS,
    @Json(name = "image") IMAGE,
    @Json(name = "list") LIST,
    @Json(name = "unknown") UNKNOWN;

    fun mapToDomain(): ComponentType {
        return when (this) {
            SPACER -> ComponentType.SPACER
            PROMO -> ComponentType.PROMO
            TITLE -> ComponentType.TITLE
            SUBTITLE -> ComponentType.SUBTITLE
            CONTACT_US -> ComponentType.CONTACT_US
            SUBSCRIBE -> ComponentType.SUBSCRIBE
            CHOICE -> ComponentType.CHOICE
            OFFER -> ComponentType.OFFER
            TILES -> ComponentType.TILES
            NAME -> ComponentType.NAME
            TEXT -> ComponentType.TEXT
            BUTTON -> ComponentType.BUTTON
            SEPARATOR -> ComponentType.SEPARATOR
            RESTORE -> ComponentType.RESTORE
            TERMS -> ComponentType.TERMS
            IMAGE -> ComponentType.IMAGE
            LIST -> ComponentType.LIST
            UNKNOWN -> ComponentType.UNKNOWN
        }
    }
}

@JsonClass(generateAdapter = true)
data class RawProduct(
    /**
     * Product productId defined in config
     */
    @Json(name = "id") val id: String? = null,
    /**
     * Name (Title) of product (ie. Basic Digital, Premium, etc.)
     */
    @Json(name = "name") val name: String? = null,
    /**
     * Name (Title) of product - Supports dynamic price info.
     */
    @Json(name = "nameDynamic") val nameDynamic: String? = null,
    /**
     * List of selling points for given product.
     */
    @Json(name = "text") val text: List<String>? = null,
    /**
     * List of selling points for given product - Supports dynamic price info.
     */
    @Json(name = "textDynamic") val textDynamic: List<String>? = null,
    /**
     * separator component for list of text
     */
    @Json(name = "separator") val separator: RawComponent? = null,
    @Json(name = "note") val note: String? = null,
    /**
     * Text to be shown on badge.
     */
    @Json(name = "badge") val badge: String? = null,
    /**
     * Intro offer text to be shown on button
     * Amazon Only: Currently only used for Amazon since it is
     * not returned by the IAP library.
     */
    @Json(name = "introOfferText") val introOfferText: String? = null,
    /**
     * Custom terms shown at the bottom of the paywall offer when this product is selected.
     */
    @Json(name = "terms") val terms: String? = null,

    @Json(name = "type") val type: String? = "IAP",
    @Json(name = "url") val url: String? = null,
    @Json(name = "authorize") val authorize: String? = null,
    @Json(name = "tileLabel") val tileLabel: String? = null,
    @Json(name = "tileTitle") val tileTitle: String? = null,
    @Json(name = "tileCaption") val tileCaption: String? = null,
    @Json(name = "action") val action: String? = null,
) {
    fun mapToDomain(): Product {
        return when (type) {
            "external" -> Product.ExternalProduct(
                id = id,
                url = url,
                tileLabel = tileLabel,
                tileTitle = tileTitle,
                tileCaption = tileCaption,
                action = action,
                text = text,
                textDynamic = textDynamic,
                separator = separator?.mapToDomain(),
                note = note,
                terms = terms
            )
            "authorization" -> Product.Registration(
                id = id,
                authorize = authorize,
                tileLabel = tileLabel,
                tileTitle = tileTitle,
                tileCaption = tileCaption,
                action = action,
                text = text,
                textDynamic = textDynamic,
                separator = separator?.mapToDomain(),
                note = note,
                terms = terms
            )
            else -> Product.IapProduct(
                id = id,
                name = name,
                nameDynamic = nameDynamic,
                text = text,
                textDynamic = textDynamic,
                separator = separator?.mapToDomain(),
                badge = badge,
                note = note,
                introOfferText = introOfferText,
                terms = terms
            )
        }
    }
}