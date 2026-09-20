package com.washingtonpost.android.config.domain.models.config.paywallconf

data class Blocker(
    val components: List<Component>?,
    val name: String?,
    val category: String?,
    val promoId: String?,
    val trialType: String?,
    val items: List<Product>?,
    val versions: List<BlockerVersion>?,
    val padding: Int? = null
)

/**
 * BlockerVersion: per-version overrides for a blocker.
 * version: integer used for ordering; highest version is considered “latest”.
 */

data class BlockerVersion(
    val version: Int?,
    val components: List<Component>?,
    val items: List<Product>?
)

data class Component(
    /**
     * Specific for phones. Used to specify
     * spacing size.
     */
    val compact: Int?,

    /**
     * Nested Components list.
     */
    val components: List<Component>?,

    /**
     * Need to figure out what this is for
     */
    val defaultSelection: Int?,

    /**
     * Value for Terminated Sub
     */
    val endedContent: String?,

    /**
     * Value for Terminated Sub - including price
     */
    val endedContentDynamic: String?,

    /**
     * Value for New Sub
     */
    val noneContent: String?,

    /**
     * Value for New Sub - including price
     */
    val noneContentDynamic: String?,

    /**
     * Value for Gift Expired link
     */
    val giftExpired: String?,

    /**
     * Value for Gift Invalid link
     */
    val giftInvalid: String?,

    /**
     * Value for Amazon Offer (currently used for $1 for 6 months)
     */
    val amazonOffer: String?,

    /**
     * Value for Only at The Post
     */
    val oatp: String?,

    /**
     * List of components for separating product items
     */
    val productSeparator: List<Component>?,

    /**
     * Specific for tablets. Used to specify
     * spacing size.
     */
    val regular: Int?,

    /**
     * Used to specify spacing between items
     */
    val space: Int?,

    /**
     * How to group products
     */
    val split: SplitType?,

    /**
     * Text value for component
     */
    val text: Any?,

    /**
     * index at which separator should be placed
     */
    val index: Int?,

    /**
     * Types of UI Components available for rendering
     */
    val type: ComponentType?,

    /**
     * Name of local image resource
     */
    val name: String?,

    /**
     * URL of remote image resource - light mode
     */
    val urlLight: String?,

    /**
     * URL of remote image resource - dark mode
     */
    val urlDark: String?,

    /**
     * URL of remote image resource - fallback / legacy
     */
    val imageUrl: String?,

    /**
     * Width of image resource in dp
     */
    val width: Int?,

    /**
     * Height of image resource in dp
     */
    val height: Int?,

    /**
     * Designates variant of component
     * e.g. "h1" "h2" "h3" "h4" for different Title component sizes
     */
    val variation: String?,

    val textList: List<String>?,

    val separator: Component?,

    val textSpacer: Component?,

    val textSeparatorSpacer: Component?,

    val labels: List<String?>?,

    val visibility: String?,

    val wide: Boolean?,

    val open: String?,

    /**
     * Product identifier for the button component.
     */
    val product: String?
)

/**
 * Determines how to display choice toggle.
 */
enum class SplitType(val type: String) {
    INTERVALS("intervals"),
    TIERS("tiers"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(type: String) =
            SplitType.values().associateBy(SplitType::type)[type] ?: UNKNOWN
    }
}

/**
 * Enum for string representation of type in component element. See config.json "blockers" section for more details.
 */
enum class ComponentType(val type: String) {
    SPACER("spacer"),
    PROMO("promo"),
    TITLE("title"),
    SUBTITLE("subtitle"),
    CONTACT_US("contact_us"),
    SUBSCRIBE("subscribe"),
    CHOICE("choice"),
    OFFER("offer"),
    NAME("name"),
    TILES("tiles"),
    TEXT("text"),
    BUTTON("button"),
    SEPARATOR("separator"),
    RESTORE("restore"),
    TERMS("terms"),
    IMAGE("image"),
    LIST("list"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(type: String) = values().associateBy(ComponentType::type)[type] ?: UNKNOWN
    }
}

sealed class Product {
    /**
     * List of selling points for given product.
     */
    abstract val text: List<String>?
    /**
     * List of selling points for given product - Supports dynamic price info.
     */
    abstract val textDynamic: List<String>?
    /**
     * separator component for list of text
     */
    abstract val separator: Component?
    /**
     * Text to be below Tiles.
     */
    abstract val note: String?
    /**
     * Custom terms shown at the bottom of the paywall offer when this product is selected.
     */
    abstract val terms: String?

    data class IapProduct(
        /**
         * Product productId defined in config
         */
        val id: String?,
        /**
         * Name (Title) of product (ie. Basic Digital, Premium, etc.)
         */
        val name: String?,
        /**
         * Name (Title) of product - Supports dynamic price info.
         */
        val nameDynamic: String?,
        override val text: List<String>?,
        override val textDynamic: List<String>?,
        override val separator: Component?,
        val badge: String?,
        override val note: String?,
        /**
         * Intro offer text to be shown on button
         * Amazon Only: Currently only used for Amazon since it is
         * not returned by the IAP library.
         */
        val introOfferText: String?,
        override val terms: String?
    ) : Product()

    data class ExternalProduct(
        /**
         * Optional product identifier used to match against Iterable
         * custom text field Item.<n>.ID for reordering.
         */
        val id: String?,
        /**
         * The URL to open when the user clicks the subscribe button.
         */
        val url: String?,
        val tileLabel: String?,
        val tileTitle: String?,
        val tileCaption: String?,
        /**
         * Button text shown when this tile is selected (e.g. "Continue to site").
         * Analogous to the dynamic CTA text used for IapProduct.
         */
        val action: String?,
        override val text: List<String>?,
        override val textDynamic: List<String>?,
        override val separator: Component?,
        override val note: String?,
        override val terms: String?
    ) : Product()

    /**
     * Represents the free "create an account" tile previously modeled as
     * Component.heroTile. It now lives in the same products array as
     * IapProduct/ExternalProduct, so tile rendering and default-selection
     * logic no longer need special-case offset math.
     */
    data class Registration(
        /**
         * Optional product identifier used to match against Iterable
         * custom text field Item.<n>.ID for reordering.
         */
        val id: String?,
        /**
         * URL to open when the registration tile/button is tapped, if it
         * should deep-link instead of triggering the standard registration flow.
         */
        val authorize: String?,
        val tileLabel: String?,
        /**
         * Price label shown on the tile (e.g. "Free").
         */
        val tileTitle: String?,
        /**
         * Subtitle shown below the price on the tile.
         */
        val tileCaption: String?,
        /**
         * Button text shown when this tile is selected (e.g. "Continue to site").
         * Analogous to the dynamic CTA text used for IapProduct.
         */
        val action: String?,
        override val text: List<String>?,
        override val textDynamic: List<String>?,
        override val separator: Component?,
        override val note: String?,
        override val terms: String?
    ) : Product()
}