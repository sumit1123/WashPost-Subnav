package com.washingtonpost.android.paywall.bottomsheet.ui

import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wapo.android.commons.iterable.AttributionInfo
import com.wapo.android.commons.iterable.toBundle
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.commons.util.UiUtils
import com.wapo.android.commons.util.setVisible
import com.washingtonpost.android.config.domain.models.config.paywallconf.Component
import com.washingtonpost.android.config.domain.models.config.paywallconf.ComponentType
import com.washingtonpost.android.config.domain.models.config.paywallconf.Product
import com.washingtonpost.android.config.domain.models.config.paywallconf.SplitType
import com.washingtonpost.android.paywall.PaywallOmniture
import com.washingtonpost.android.paywall.PaywallReactive
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.R
import com.washingtonpost.android.paywall.auth.AuthIntentBuilder
import com.washingtonpost.android.paywall.billing.NativePaywallListenerActivity
import com.washingtonpost.android.paywall.bottomsheet.GroupType
import com.washingtonpost.android.paywall.bottomsheet.SignInState
import com.washingtonpost.android.paywall.bottomsheet.SubState
import com.washingtonpost.android.paywall.bottomsheet.UserEvent
import com.washingtonpost.android.paywall.bottomsheet.ui.component.*
import com.washingtonpost.android.paywall.bottomsheet.ui.component.product.ProductButtonView
import com.washingtonpost.android.paywall.bottomsheet.ui.component.product.ProductNameView
import com.washingtonpost.android.paywall.bottomsheet.ui.component.product.ProductTextView
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PaywallSheet2ViewModel
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PostIterableEventType
import com.washingtonpost.android.paywall.bottomsheet.viewmodel.PostIterableEventViewModel
import com.washingtonpost.android.paywall.databinding.PaywallSheetContainerBinding
import com.washingtonpost.android.paywall.helper.PaywallSheetHelper
import com.washingtonpost.android.paywall.helper.componentTextToListOfStrings
import com.washingtonpost.android.paywall.helper.componentTextToString
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallConstants.WALL_NAME_AUDIO_ACTION_BUTTON
import com.washingtonpost.android.paywall.util.PaywallConstants.WALL_NAME_AUDIO_CAROUSEL
import com.washingtonpost.android.paywall.util.PaywallConstants.WallType
import com.washingtonpost.android.paywall.util.PaywallUtil
import com.washingtonpost.android.paywall.util.PaywallUtil.mapProductNameToProductId
import kotlin.getValue

/**
 * Paywall UI Specs as per [https://washpost.invisionapp.com/console/share/G610GHUO3CKH]
 */
class PaywallSheet2Fragment : BottomSheetDialogFragment() {
    private var originalWallType: WallType? = null
    private var reason: Int = -1
    private var paywallAnalytics: PaywallOmniture? = null
    private val paywallSheetViewModel: PaywallSheet2ViewModel by viewModels()
    private val postIterableEventViewModel: PostIterableEventViewModel by activityViewModels()
    private var _binding: PaywallSheetContainerBinding? = null
    private val binding get() = _binding!!
    private var wallName: String? = null
    private var analyticsWallName: String? = null
    private var defaultIntervalOverride: Int? = null
    private var fragmentManager: FragmentManager? = null
    private var tag: String = ""
    var shouldDismissWallWithoutFinishingActivity: Boolean = false
    private var tileIndex: Int = 0
    private var isOverlaidPaywall: Boolean? = false
    private var isRegWallOriginated: Boolean = false
    /** When true, the sheet won't auto-dismiss for active subscribers (e.g. ad-free-legal blocker). */
    private var preventAutoDismiss: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val blocker = paywallSheetViewModel.getBlocker(wallName)
            analyticsWallName = blocker?.name ?: analyticsWallName
            // Track paywall block overlay only for regwall or softwall to avoid duplicate events for paywall
            val category = blocker?.category?.lowercase()
            if (category == PaywallConstants.WallCategory.REGWALL.name.lowercase() ||
                category == PaywallConstants.WallCategory.SOFTWALL.name.lowercase()) {
                paywallAnalytics?.trackPaywallBlockOverlay(
                    this.originalWallType,
                    this.analyticsWallName,
                    isOverlaidPaywall,
                    isRegWallOriginated
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (savedInstanceState ?: arguments)?.let { state ->
            wallName = state.getString(ARG_WALL_NAME, wallName)
        }
        val info = arguments?.getParcelable<AttributionInfo>(ARG_ATTRIBUTION_INFO)
        _binding = PaywallSheetContainerBinding.inflate(inflater, container, false)
        initBottomSheet()
        paywallSheetViewModel.apply {
            setPaywallType(originalWallType)
            attributionInfo = info
            update()
            wallName = this@PaywallSheet2Fragment.wallName
            analyticsWallName = this@PaywallSheet2Fragment.analyticsWallName
            logMessageSource()
        }

        defaultIntervalOverride?.let {
            if (it == PaywallConstants.MONTHLY || it == PaywallConstants.ANNUAL) {
                paywallSheetViewModel.defaultInterval = it
            }
        }
        renderWall()
        return binding.root
    }

    override fun getTheme(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            R.style.Theme_NoWiredStrapInNavigationBar
        } else {
            super.getTheme()
        }
    }

    /**
     * Show Paywall
     */
    fun show(
        @NonNull fragmentManager: FragmentManager,
        tag: String,
        paywallAnalytics: PaywallOmniture,
        originalWallType: WallType?,
        reason: Int,
        wallName: String?,
        analyticsWallName: String?,
        defaultIntervalOverride: Int?,
        isOverlaidPaywall: Boolean? = false,
        isRegWallOriginated: Boolean = false,
        preventAutoDismiss: Boolean = false,
        attributionInfo: AttributionInfo? = null
    ) {
        this.fragmentManager = fragmentManager
        this.tag = tag
        this.originalWallType = originalWallType
        this.reason = reason
        this.paywallAnalytics = paywallAnalytics
        // TODO We have a common wall from different places but analytics names are different.
        // It may not be required a new config in that case, but there should be a way to differentiate analytics wall name.
        this.wallName = if (wallName == WALL_NAME_AUDIO_ACTION_BUTTON) WALL_NAME_AUDIO_CAROUSEL else (wallName ?: DEFAULT_WALL)
        this.analyticsWallName = analyticsWallName
        this.defaultIntervalOverride = defaultIntervalOverride
        if (PaywallService.getInstance().isSubscriptionPaused && originalWallType != WallType.REGWALL) {
            // Override any wall with the pause wall if the user has a paused subscription
            this.wallName = PaywallConstants.WALL_NAME_PAUSED
            this.originalWallType = WallType.PAUSEWALL
            this.reason = PaywallConstants.getWallReason(WallType.PAUSEWALL)
        }
        this.isOverlaidPaywall = isOverlaidPaywall
        this.isRegWallOriginated = isRegWallOriginated
        this.preventAutoDismiss = preventAutoDismiss
        // Save final wallName to arguments after all modifications are complete
        arguments = (arguments ?: Bundle()).apply {
            putString(ARG_WALL_NAME, this@PaywallSheet2Fragment.wallName)
            putParcelable(ARG_ATTRIBUTION_INFO, attributionInfo)
        }
        showNow(fragmentManager, tag)
    }

    /**
     * Logic to render full paywall
     */
    private fun renderWall() {
        val blocker = paywallSheetViewModel.getBlocker(wallName)
        val components = paywallSheetViewModel.getComponentList()

        // Apply per-blocker horizontal padding if specified in the config.
        // This overrides the default fixed container width to match_parent
        // so the padding value controls the actual horizontal inset.
        blocker?.padding?.let { paddingDp ->
            val paddingPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                paddingDp.toFloat(),
                resources.displayMetrics
            ).toInt()
            binding.paywallContainer.layoutParams = binding.paywallContainer.layoutParams.apply {
                width = FrameLayout.LayoutParams.MATCH_PARENT
            }
            binding.paywallContainer.setPadding(paddingPx, 0, paddingPx, 0)
        }

        components?.let {
            loadComponent(paywallSheetViewModel.wallName, it, binding.paywallContainer)
        }
    }

    /**
     * Dismiss paywall but prevent activity from finishing
     */
    fun dismissWall() {
        shouldDismissWallWithoutFinishingActivity = true
        dismissAllowingStateLoss()
    }

    /**
     * onSaveInstanceState will help preserve the state properly during configuration changes
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_WALL_NAME, wallName)
    }

    /**
     * Logic to render top level components
     */
    private fun loadComponent(
        wallName: String?,
        components: List<Component>,
        container: ViewGroup?,
        isWide: Boolean = false
    ) {
        components.forEach { component ->
            val componentView: View? = when (component.type) {
                ComponentType.SPACER -> {
                    val view = View(this.requireContext())
                    view.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val space = when {
                        component.space != null -> component.space ?: 0
                        component.compact != null && !DeviceUtils.isTablet(context) -> component.compact ?: 0
                        component.regular != null && DeviceUtils.isTablet(context) -> component.regular ?: 0
                        else -> 0
                    }
                    view.layoutParams.height = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        space.toFloat(),
                        resources.displayMetrics
                    ).toInt()
                    //view.setHeightDp(space, requireContext())
                    view
                }
                ComponentType.PROMO -> {
                    val view = PromoLabelView(requireContext())
                    val labelDivider = "|"
                    var secondaryLabelText = ""
                    component.labels?.takeIf { it.isNotEmpty() }?.let { labels ->
                        if (labels.size > 1) {
                            labels[1]?.let { secondaryLabelText = it.trim() }
                        }
                        view.setVisible(true)
                        view.setLabel(labels[0]?.trim(), secondaryLabelText)
                    } ?: paywallSheetViewModel.getAppropriateText(component).observe(this) { text ->
                        text?.let {
                            val labels = it.split(labelDivider)
                            if (labels.size > 1) {
                                secondaryLabelText = labels[1].trim()
                            }
                            view.setVisible(true)
                            view.setLabel(labels[0].trim(), secondaryLabelText)
                        } ?: view.setVisible(false)
                    }
                    view
                }
                ComponentType.TITLE -> {
                    val view = TitleView(requireContext())
                    view.setFontSize(paywallSheetViewModel.getComponentFontSize(component.variation))
                    // Configure title TextView to prevent text truncation
                    view.configureTextBehavior()
                    paywallSheetViewModel.getAppropriateText(component).observe(this) {
                        it?.apply {
                            view.setTitle(this)
                        }
                    }
                    view
                }
                ComponentType.TEXT -> {
                    val rawText = component.text as? String
                    val text = PaywallUtil.buildDynamicTextForPaywall(rawText) ?: rawText
                    if (!text.isNullOrBlank()) {
                        val view = SubtitleView(requireContext())
                        view.setSubtitle(text)
                        view.setFontSize(paywallSheetViewModel.getComponentFontSize(component.variation))
                        if (wallName == WALL_NAME_AD_FREE_LEGAL) {
                            view.setTextColor(ContextCompat.getColor(requireContext(), com.wpds.wpds.R.color.gray20))
                        }
                        view
                    } else {
                        null
                    }
                }
                ComponentType.SUBTITLE -> {
                    val view = SubtitleView(requireContext())
                    paywallSheetViewModel.getAppropriateText(component).observe(this) { subtitle ->
                        subtitle?.let {
                            view.setVisible(true)
                            view.setSubtitle(it)
                            view.setFontSize(paywallSheetViewModel.getComponentFontSize(component.variation))
                        } ?: view.setVisible(false)
                    }
                    view
                }
                ComponentType.SUBSCRIBE -> {
                    if (paywallSheetViewModel.subStateLiveEvent.value == SubState.ActiveSub) {
                        null
                    } else {
                        val view = SubscribeSubtitleView(requireContext())
                        paywallSheetViewModel.getAppropriateText(component).observe(this) {
                            view.setVisible(true)
                            view.setSubtitle() {
                                paywallSheetViewModel.userEvent.value = UserEvent.Paywall
                            }
                        }
                        view
                    }
                }
                ComponentType.CONTACT_US -> {
                    val view = ContactUsSubtitleView(requireContext())
                    paywallSheetViewModel.getAppropriateText(component).observe(this) {
                        view.setVisible(true)
                        view.setSubtitle() {
                            paywallSheetViewModel.userEvent.value = UserEvent.ContactUs
                        }
                    }
                    view
                }
                ComponentType.CHOICE -> {
                    val view = ToggleView(requireContext())
                    val default = paywallSheetViewModel.defaultInterval ?: component.defaultSelection // deeplink Interval overrides blocker default Interval
                    view.init(component.split, default) { _, checkedId, isChecked ->
                        if (isChecked) {
                            val groupType: GroupType? = when (checkedId) {
                                R.id.option_a ->
                                    when (component.split) {
                                        SplitType.INTERVALS -> GroupType.Monthly
                                        SplitType.TIERS -> GroupType.Core
                                        else -> null
                                    }
                                R.id.option_b ->
                                    when (component.split) {
                                        SplitType.INTERVALS -> GroupType.Yearly
                                        SplitType.TIERS -> GroupType.Premium
                                        else -> null
                                    }
                                else -> null
                            }

                            groupType?.apply {
                                paywallSheetViewModel.periodSelectedLiveData.value = this
                            }
                        }
                    }
                    paywallSheetViewModel.periodSelectedLiveData.value = view.groupType
                    view
                }
                ComponentType.OFFER -> {
                    val entry = component.components?.firstOrNull { it.type == ComponentType.TILES }
                    when (entry?.type) {
                        ComponentType.TILES -> {
                            tileContainer(component, wallName)
                        }

                        else -> {
                            offerContainer(wallName, component)
                        }
                    }
                }
                ComponentType.SEPARATOR -> {
                    val view = SeparatorView(requireContext())
                    view.setText(component.text.componentTextToString())
                    view
                }
                ComponentType.RESTORE -> {
                    val view = SignInView(requireContext())
                    paywallSheetViewModel.signInStateLiveEvent.observe(this) {
                        view.setSignInText(it == SignInState.SignedIn) {
                            paywallSheetViewModel.userEvent.value = UserEvent.SignIn
                        }
                    }
                    view
                }
                ComponentType.TERMS -> {
                    val view = TermsView(requireContext())
                    view
                }
                ComponentType.BUTTON -> {
                    val view = ProductButtonView(requireContext())
                    var ctaEvent: UserEvent = UserEvent.Register(paywallSheetViewModel.promoId, paywallSheetViewModel.trialType)
                    var buttonTextFallback = "Register"
                    val wallCategory = paywallSheetViewModel.category?.lowercase()
                    val componentOpen = component.open
                    when {
                        !componentOpen.isNullOrEmpty() -> {
                            ctaEvent = UserEvent.OpenUrl(componentOpen)
                        }
                        wallCategory == PaywallConstants.WallCategory.PAYWALL.name.lowercase() -> {
                            buttonTextFallback = "Subscribe"
                            /* ctaEvent = UserEvent.Subscribe(sku) */ // TODO: Implement when we have a usecase for this
                        }
                        wallCategory == PaywallConstants.WallCategory.REGWALL.name.lowercase() -> {
                            buttonTextFallback = "Register"
                            ctaEvent = UserEvent.Register(paywallSheetViewModel.promoId, paywallSheetViewModel.trialType)
                        }
                        wallCategory == PaywallConstants.WallCategory.SOFTWALL.name.lowercase() -> {
                            buttonTextFallback = "Register"
                            ctaEvent = UserEvent.Register(paywallSheetViewModel.promoId, paywallSheetViewModel.trialType)
                        }
                        wallCategory == PaywallConstants.WallCategory.PAUSEWALL.name.lowercase() -> {
                            buttonTextFallback = "Resume"
                            ctaEvent = UserEvent.Resume
                        }
                    }
                    val buttonText = component.text.componentTextToString() ?: buttonTextFallback
                    view.setText(buttonText)
                    view.setTypeface(null, Typeface.BOLD) // TODO (AWA-6748): remove when bold tags from config work on all Fire devices
                    view.fullWidth(component.wide ?: false)
                    view.setButtonClickListener {
                        paywallSheetViewModel.userEvent.value = ctaEvent
                    }
                    view
                }
                ComponentType.IMAGE -> {
                    val view = IconView(requireContext())
                    val darkMode = context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
                    view.setImage(component.name, darkMode, component.urlLight, component.urlDark, component.imageUrl, component.width, component.height)
                    view
                }
                ComponentType.LIST -> {
                    val items = mutableListOf<Any>()
                    component.textList?.let { textList ->
                        items.addAll(textList)
                    } ?: component.text.componentTextToListOfStrings()?.let { textList ->
                        items.addAll(textList)
                    }
                    component.separator?.let { separator ->
                        separator.index?.let { separatorIndex ->
                            items.add(separatorIndex, separator)
                        }
                    }
                    val view = ListTextView(requireContext())
                    view.createList(items, component)
                    view
                }
                else -> null
            }

            componentView?.apply {
                if(paywallSheetViewModel.isComponentVisible(component)){
                    container?.addView(this)
                }
            }
        }
    }

    /**
     * Tile-only rendering for OFFER blocks that present a tiles chooser.
     *   - components: child components inside the OFFER (SPACER/TEXT/TILES/BUTTON)
     *   - container: parent view group to render into
     *   - SPACER/TEXT/TILES delegate to shared helpers for consistency with product flow.
     *   - TILES composes a grid and wires selection:
     *     • When a product tile is selected → re-render product components via loadProductComponents.
     *     • When the free tile (no product) is selected → re-render tiles via loadTileComponents.
     *   - Default selection index is 0 in the tiles-only path.
     */

    private fun loadTileComponents(
        components: List<Component>?,
        container: ViewGroup?
    ) {
        components?.forEach { component ->
            val componentView: View? = when (component.type) {
                ComponentType.SPACER -> {
                    val view = View(this.requireContext())
                    view.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val space = when {
                        component.space != null -> component.space ?: 0
                        component.compact != null && !DeviceUtils.isTablet(context) -> component.compact
                            ?: 0

                        component.regular != null && DeviceUtils.isTablet(context) -> component.regular
                            ?: 0

                        else -> 0
                    }
                    view.layoutParams.height = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        space.toFloat(),
                        resources.displayMetrics
                    ).toInt()
                    view
                }

                ComponentType.TEXT -> {
                    val text = component.text as? String
                    if (!text.isNullOrBlank()) {
                        val view = SubtitleView(requireContext())
                        view.setSubtitle(text)
                        view.setFontSize(paywallSheetViewModel.getComponentFontSize(component.variation))
                        view
                    } else {
                        null
                    }
                }

                ComponentType.TILES -> {
                    val maximumWidth = resources.displayMetrics.widthPixels
                    val widthPx = minOf(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        400f,
                        resources.displayMetrics
                    ).toInt(), maximumWidth)

                    // Override the parent container width only for TILES
                    _binding?.paywallContainer?.layoutParams = FrameLayout.LayoutParams(
                        widthPx,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                    }
                    val tilesView = TilesView(requireContext())
                    paywallSheetViewModel.getProducts(wallName).observe(this) { productList ->
                        tilesView.apply {
                            init(
                                { product -> paywallSheetViewModel.getProductTileButtonText(product) },
                                productList,
                                tileIndex,
                                { product -> paywallSheetViewModel.getProductPrice(product) },
                            ) { product, checked, index ->
                                container?.removeAllViews()
                                tileIndex = index
                                if (product != null) {
                                    loadProductComponents(
                                        components,
                                        container,
                                        product
                                    )
                                } else {
                                    loadTileComponents(components, container)
                                }
                            }
                        }

                    }
                    tilesView
                }

                ComponentType.BUTTON -> {
                    val view = ProductButtonView(requireContext())
                    view.setTypeface(null, Typeface.BOLD)
                    paywallSheetViewModel.setPaywallType(WallType.REGWALL_TILE)
                    paywallSheetViewModel.getProducts(wallName).observe(this) { productList ->
                        val registrationProduct = productList
                            .filterIsInstance<Product.Registration>()
                            .firstOrNull()
                        // Take button text from the registration product as a fallback when button text is not set.
                        val text = component.text.componentTextToString()
                            ?: registrationProduct?.text?.componentTextToString()
                        if (!text.isNullOrEmpty())
                            view.setText(text)
                        else
                            view.visibility = View.GONE
                        val registrationUrl = registrationProduct?.authorize
                        val userEvent = when {
                            registrationProduct != null && !registrationProduct.text.isNullOrEmpty() && !registrationUrl.isNullOrEmpty() ->
                                UserEvent.OpenUrl(registrationUrl)

                            else -> UserEvent.Register(
                                paywallSheetViewModel.promoId,
                                paywallSheetViewModel.trialType
                            )
                        }
                        view.setButtonClickListener {
                            paywallSheetViewModel.userEvent.value = userEvent
                        }
                    }
                    view
                }

                else -> null
            }
            componentView?.apply {
                if(paywallSheetViewModel.isComponentVisible(component)){
                    container?.addView(this)
                }
            }
        }
    }

    // Builds and returns a container layout for displaying tilesWall  based on the wall type and product data.
    private fun tileContainer(
        component: Component,
        wallName: String?
    ): LinearLayout {
        val tileContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val paddingDp = 26
            val scale = resources.displayMetrics.density
            val paddingPx = (paddingDp * scale + 0.5f).toInt()
            setPadding(paddingPx, 0, paddingPx, 0)
        }

        paywallSheetViewModel.setPaywallType(originalWallType)
        paywallAnalytics?.trackPaywallBlockOverlay(
            originalWallType,
            this.analyticsWallName,
            isOverlaidPaywall,
            isRegWallOriginated
        )

        component.components?.forEach { childComponent ->
            if (childComponent.type == ComponentType.TILES) {
                val defaultValue = childComponent.defaultSelection ?: 0
                paywallSheetViewModel.getProducts(wallName).observe(this) { productList ->
                    tileIndex = defaultValue
                    val defaultProduct = productList.getOrNull(defaultValue)
                    if (defaultProduct != null) {
                        loadProductComponents(
                            component.components,
                            tileContainer,
                            defaultProduct
                        )
                    }
                }
            }
        }
        return tileContainer
    }

    // Builds and returns a container layout for displaying paywall offers based on available product data.
    private fun offerContainer(
        wallName: String?,
        component: Component
    ): LinearLayout {
        val offerContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        paywallSheetViewModel.setPaywallType(this.originalWallType)
        paywallAnalytics?.trackPaywallBlockOverlay(
            this.originalWallType,
            this.analyticsWallName,
            false,
            false
        )
        paywallSheetViewModel.getProducts(wallName)
            .observe(this) { productList ->
                offerContainer.removeAllViews()
                productList.forEach { product ->
                    loadProductComponents(
                        component.components,
                        offerContainer,
                        product
                    )
                    if (productList.last() != product) {
                        loadProductComponents(
                            component.productSeparator,
                            offerContainer,
                            product
                        )
                    }
                }
            }
        return offerContainer
    }

    /**
     * Logic to render offer components
     */
    private fun loadProductComponents(
        components: List<Component>?,
        container: ViewGroup?,
        product: Product,
        isWide: Boolean = false
    ) {
        components?.forEach { component ->
            val componentView: View? = when (component.type) {
                ComponentType.SPACER -> {
                    val view = View(this.requireContext())
                    view.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    val space = when {
                        component.space != null -> component.space ?: 0
                        component.compact != null && !DeviceUtils.isTablet(context) -> component.compact ?: 0
                        component.regular != null && DeviceUtils.isTablet(context) -> component.regular ?: 0
                        else -> 0
                    }
                    view.layoutParams.height = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        space.toFloat(),
                        resources.displayMetrics
                    ).toInt()
                    //view.setHeightDp(space, requireContext())
                    view
                }
                ComponentType.NAME -> {
                    val view = ProductNameView(requireContext())
                    paywallSheetViewModel.getAppropriateProductName(product)?.apply {
                        view.setName(this)
                        view.setFontSize(paywallSheetViewModel.getComponentFontSize(component.variation))
                    }
                    view
                }
                ComponentType.TEXT -> {
                    if (component.variation != PRODUCT_NOTE) {
                        val view = ProductTextView(requireContext())
                        paywallSheetViewModel.getAppropriateProductTextList(product)?.apply {
                            view.setTextList(this)
                            product.separator?.apply {
                                view.insertSeparator(this)
                            }
                        }
                        view
                    } else {
                        val view = SubtitleView(requireContext())
                        if (!product.note.isNullOrEmpty()) {
                            view.setSubtitle(product.note.toString())
                            view.setFontSize(paywallSheetViewModel.getComponentFontSize(component.variation))
                            view.setVisible(true)
                        } else {
                            view.setVisible(false)
                        }
                        view
                    }
                }

                ComponentType.TILES -> {
                    val maximumWidth = resources.displayMetrics.widthPixels
                    val widthPx = minOf(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        400f,
                        resources.displayMetrics
                    ).toInt(), maximumWidth)

                    // Override the parent container width only for TILES
                    _binding?.paywallContainer?.layoutParams = FrameLayout.LayoutParams(widthPx, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = android.view.Gravity.CENTER_HORIZONTAL
                    }
                    val tilesView = TilesView(requireContext())
                    paywallSheetViewModel.getProducts(wallName).observe(this) { productList ->
                        tilesView.apply {
                            init(
                                { product -> paywallSheetViewModel.getProductTileButtonText(product) },
                                productList,
                                tileIndex,
                                { product -> paywallSheetViewModel.getProductPrice(product) },
                            ) { product, checked, index ->
                                container?.removeAllViews()
                                tileIndex = index
                                if (product != null) {
                                    loadProductComponents(
                                        components,
                                        container,
                                        product
                                    )
                                } else {
                                    loadTileComponents(components, container)
                                }
                            }
                        }

                    }
                    tilesView
                }
                ComponentType.BUTTON -> {
                    val view = ProductButtonView(requireContext())
                    val buttonText = when (product) {
                        is Product.Registration ->
                            product.action ?: component.text.componentTextToString()
                        is Product.ExternalProduct -> product.action ?: component.text.componentTextToString()
                        else -> paywallSheetViewModel.getProductButtonText(product)
                    }
                    if (!buttonText.isNullOrEmpty()) {
                        view.setText(buttonText)
                    } else {
                        view.visibility = View.GONE
                    }

                    if (product is Product.IapProduct) {
                        product.badge?.let { productBadgeText ->
                            view.setBadgeVisible(productBadgeText)
                        }
                    }

                    view.setButtonClickListener {
                        val userEvent = when (product) {
                            is Product.IapProduct -> product.id?.let {
                                val mappedId = mapProductNameToProductId(it) ?: it
                                if (PaywallReactive.PREMIUM_PRODUCT.equals(
                                        it,
                                        ignoreCase = true
                                    ) || PaywallReactive.BASIC_PRODUCT.equals(it, ignoreCase = true)
                                ) {
                                    UserEvent.UpdateSub(mappedId)
                                } else {
                                    UserEvent.Subscribe(mappedId)
                                }
                            }
                            is Product.ExternalProduct -> product.url?.let {
                                UserEvent.OpenUrl(it)
                            }
                            is Product.Registration -> {
                                val registrationUrl = product.authorize
                                if (!registrationUrl.isNullOrEmpty()) {
                                    UserEvent.OpenUrl(registrationUrl)
                                } else {
                                    UserEvent.Register(
                                        paywallSheetViewModel.promoId,
                                        paywallSheetViewModel.trialType
                                    )
                                }
                            }
                        }
                        userEvent?.let { paywallSheetViewModel.userEvent.value = it }
                    }
                    view
                }
                ComponentType.SEPARATOR -> {
                    val view = SeparatorView(requireContext())
                    view.setText(component.text.componentTextToString())
                    view
                }
                ComponentType.TERMS -> {
                    val view = TermsView(requireContext())
                    product.terms?.let { view.setTermsText(it) }
                    view
                }
                else -> null
            }

            componentView?.apply {
                if (paywallSheetViewModel.isComponentVisible(component)) {
                    container?.addView(this)
                }
            }
        }
    }

    /**
     * handle various user events
     */
    private fun observeUserEvent() {
        paywallSheetViewModel.userEvent.observe(this) {
            when (it) {
                UserEvent.Close -> {
                    postIterableEventViewModel.postSignInOrSubscribeEvent(PostIterableEventType.SUBSCRIBE)
                    dismiss()
                }
                UserEvent.PrivacyPolicy -> PaywallService.getConnector()
                    .showPolicy(PaywallConstants.PRIVACY_POLICY, context)
                UserEvent.SignIn -> {
                    paywallSheetViewModel.logOutUser {
                        PaywallService.getConnector().showSignInScreen(activity?.supportFragmentManager, AuthIntentBuilder().build(), paywallSheetViewModel.analyticsWallName, paywallSheetViewModel.paywallType.value, true, null)
                    }
                    paywallSheetViewModel.trackMessageEvent(
                        paywallAnalytics = paywallAnalytics,
                        eventType = PaywallSheet2ViewModel.EventType.SIGN_IN_EVENT
                    )
                }
                is UserEvent.Subscribe -> {
                    val dialogId =
                        PaywallSheetHelper.getResultId(paywallSheetViewModel.paywallType.value)
                    activity?.apply {
                        if (!this.isFinishing) {
                            val entry = it.skuEntry
                            this.startActivityForResult(
                                NativePaywallListenerActivity.getPurchaseIntent(
                                    activity,
                                    entry.resolvedSku,
                                    entry.basePlanId,
                                    PaywallUtil.getOfferId(it.productId, wallName),
                                    paywallSheetViewModel.getPurchaseExtras(),
                                    null
                                ), dialogId
                            )
                        }
                    }
                    paywallSheetViewModel.trackMessageEvent(
                        paywallAnalytics = paywallAnalytics,
                        eventType = PaywallSheet2ViewModel.EventType.OFFER_PURCHASE_EVENT,
                        productId = it.productId,
                        offerId = PaywallUtil.getOfferId(it.productId, wallName)
                    )
                }
                UserEvent.Resume -> {
                    context?.apply {
                        if (PaywallService.getInstance().isSubSource(PaywallConstants.SubscriptionSource.WAPO_PROFILE)) {
                            PaywallService.getConnector().openSiteSubManagement(this, true, PaywallConstants.ManageSubUrlItids.APPS_WALL_PAUSE.value)
                            PaywallService.getOmniture().trackWallProfileResume(PaywallService.getConnector().getSiteSubManagementUrl(true, PaywallConstants.ManageSubUrlItids.APPS_WALL_PAUSE.value))

                        } else {
                            PaywallService.getConnector().openPlaystore(activity)
                            PaywallService.getOmniture().trackWallProfileResume(PaywallService.getConnector().getPlayStoreUrl(context))
                        }
                    }
                }
                is UserEvent.Register -> {
                    paywallSheetViewModel.logOutUser {
                        activity?.apply {
                            val authIntentBuilder = AuthIntentBuilder()
                            if (paywallSheetViewModel.promoId != null && paywallSheetViewModel.trialType != null) {
                                authIntentBuilder.addRegistrationParams(paywallSheetViewModel.promoId!!, paywallSheetViewModel.trialType!!)
                            }
                            authIntentBuilder.addIsSignUp(true)
                            PaywallService.getConnector().showSignUpScreen(activity?.supportFragmentManager, authIntentBuilder.build(), paywallSheetViewModel.analyticsWallName, paywallSheetViewModel.paywallType.value)

                        }
                    }
                }
                is UserEvent.Paywall -> {
                    val isRegWallOriginated = paywallSheetViewModel.isRegwall && paywallSheetViewModel.isSignedIn.not()
                    // show a new paywall on top of this wall
                    fragmentManager?.let { fm ->
                        PaywallSheet2Fragment().show(
                            fm,
                            tag,
                            paywallAnalytics!!,
                            originalWallType,
                            reason,
                            DEFAULT_WALL,
                            analyticsWallName,
                            defaultIntervalOverride,
                            true,
                            true
                        )
                    }
                }
                is UserEvent.ContactUs -> {
                    PaywallService.getConnector().showContactUs(context)
                }
                UserEvent.TermsOfService -> PaywallService.getConnector()
                    .showPolicy(PaywallConstants.TERMS_OF_SERVICE, context)

                is UserEvent.OpenUrl -> {
                    PaywallService.getConnector().openUrl(it.url, paywallSheetViewModel.getPurchaseExtras())
                    dismissAllowingStateLoss()
                }

                is UserEvent.UpdateSub -> {
                    val service = PaywallService.getInstance()
                    val baseProductId = service?.inAppSubProductId

                    activity?.apply {
                        if (!this.isFinishing) {
                            if (!baseProductId.isNullOrEmpty()) {
                                startActivity(
                                    NativePaywallListenerActivity.getSubscriptionUpdateIntent(
                                        this,
                                        baseProductId,
                                        it.targetProductId,
                                        paywallSheetViewModel.attributionInfo.toBundle()
                                    )
                                )
                            }
                        }
                    }
                    dismissAllowingStateLoss()
                }

                else -> {
                }
            }
        }
    }

    /**
     * Observer Sub State
     * Automatically closes the paywall if it appears for active subs. Some free trials are exceptions.
     * Also closes the regwall if it appears for users who are already logged in.
     */
    private fun observeSubState() {
        paywallSheetViewModel.subStateLiveEvent.observe(this) {
            // Don't auto-dismiss if this sheet was opened with preventAutoDismiss
            // (e.g. ad-free-legal blocker shown intentionally to active subscribers)
            if (preventAutoDismiss) return@observe

            if ((it is SubState.ActiveSub && !canShowPaywallToActiveSub()) || (it !is SubState.ActiveSub
                        && paywallSheetViewModel.isRegwallCategory && PaywallService.getInstance().isWpUserLoggedIn) ||
                (it !is SubState.ActiveSub
                        && paywallSheetViewModel.isSoftwallCategory && PaywallService.getInstance().isWpUserLoggedIn)
            ) {
                paywallSheetViewModel.userEvent.value = UserEvent.Close
            }
        }
    }

    /**
     * Active Subs can see the paywall in following cases:
     * - User has a Free Days free trial subscription
     * - User is seeing a regwall
     */
    private fun canShowPaywallToActiveSub(): Boolean {
        return PaywallService.getInstance().isFreeDaysUser || PaywallService.getInstance().isMobileFreeDaysUser || (paywallSheetViewModel.isRegwallCategory && !PaywallService.getInstance().isWpUserLoggedIn)
    }

    /**
     * Initialize all live data observables
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeUserEvent()
        observeSubState()
    }

    /**
     * Update viewmodel live data
     */
    override fun onResume() {
        super.onResume()
        paywallSheetViewModel.update()
        paywallSheetViewModel.trackMessageEvent(
            paywallAnalytics = paywallAnalytics,
            eventType = PaywallSheet2ViewModel.EventType.START_IMPRESSION_EVENT
        )
    }

    override fun onPause() {
        paywallSheetViewModel.trackMessageEvent(
            paywallAnalytics = paywallAnalytics,
            eventType = PaywallSheet2ViewModel.EventType.PAUSE_IMPRESSION_EVENT
        )
        super.onPause()
    }

    /**
     * Initialize sheet behavior for bottomsheet paywall dialog
     */
    private fun initBottomSheet() {
        dialog?.setOnShowListener {
            val bottomSheet: FrameLayout? =
                (it as? BottomSheetDialog)?.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.apply {
                val behavior = BottomSheetBehavior.from(this)
                behavior.peekHeight = UiUtils.screenSizeInPx(context).y - PEEK_HEIGHT_OFFSET
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                this.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_DRAGGING && isArticleBlocking()) {
                            behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    }
                })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (activity?.isChangingConfigurations == false) {
            handlePaywallClose()
        }
        _binding = null
    }

    /**
     * When paywall is closed
     *  - Article should also be closed
     */
    private fun handlePaywallClose() {
        activity?.apply {
            if (!isFinishing &&
                (isArticleBlocking()
                        && !PaywallService.getInstance().isPremiumUser // Check if user does not have a Sub
                        && !(paywallSheetViewModel.isRegwall && paywallSheetViewModel.isSignedIn) // If regwall is dismissed after user signs in, we should not close article and call tetro again to determine wall.
                        && !isSectionFrontWall() // calling finish() on MainActivity will close the app, so avoid doing that
                        && !isSearchActivity() // don't close SearchActivity
                        || shouldFinishActivity()) && !shouldDismissWallWithoutFinishingActivity
            ) {
                PaywallService.getConnector().trackBackFromWall()
                finish()
            }
        }
    }

    /**
     * Determines Wall Dismiss behavior
     * If true, article should close when Wall is dismissed
     */
    private fun isArticleBlocking(): Boolean {
        // TODO fix paywallSheetViewModel.paywallType.value
        val paywallType = paywallSheetViewModel.paywallType.value
        return paywallType == WallType.METERED_PAYWALL
                || paywallType == WallType.ARTICLE_DEEP_LINK_PAYWALL
                || paywallType == WallType.WIDGET_PAYWALL
                || paywallType == WallType.WEBVIEW_PAYWALL
                || paywallType == WallType.GIFT_EXPIRED_PAYWALL
                || paywallType == WallType.GIFT_INVALID_PAYWALL
                || paywallType == WallType.GIFT_SENDER_NO_SUB
                || paywallType == WallType.REGWALL
                || paywallType == WallType.NAMED_PAYWALL
                || paywallType == WallType.REGWALL_TILE
    }

    /**
     * TODO: Merge with isArticleBlocking()?
     */
    private fun shouldFinishActivity(): Boolean {
        // TODO fix paywallSheetViewModel.paywallType.value
        val paywallType = originalWallType//paywallSheetViewModel.paywallType.value
        return paywallType == WallType.IAA_WALL
                || paywallType == WallType.DEFAULT_DEEP_LINK_PAYWALL
                || paywallType == WallType.ONELINK_WALL
                || paywallType == WallType.WEBVIEW_PRODUCT_PAGE
                || paywallType == WallType.PAUSEWALL
    }

    /**
     * Calling finish() on MainActivity would close the app,
     * so these MainActivity walls avoid doing that.
     */
    private fun isSectionFrontWall(): Boolean {
        return wallName == WALL_NAME_AUDIO_CAROUSEL
                || wallName == WALL_NAME_AUDIO_ACTION_BUTTON
    }

    private fun isSearchActivity(): Boolean {
        return activity?.javaClass?.simpleName == "Search2Activity"
    }

    fun setWallName(wallName: String?) {
        this.wallName = wallName
    }

    fun isRegwall(): Boolean {
        return PaywallConstants.regwalls.contains(wallName)
    }

    companion object {
        private const val PEEK_HEIGHT_OFFSET = 100
        private const val DEFAULT_WALL = ""
        private const val PRODUCT_NOTE = "note"
        private const val ARG_WALL_NAME = "wallName"
        private const val ARG_ATTRIBUTION_INFO = "attributionInfo"
        private const val WALL_NAME_AD_FREE_LEGAL = "ad-free-legal"
    }


}