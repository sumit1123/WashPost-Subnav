package com.wapo.flagship

import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import com.wapo.android.commons.util.Logger
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.flagship.content.UpdateFusionPageObservable
import com.wapo.flagship.features.audio.AudioTracker
import com.wapo.flagship.features.pagebuilder.ClassicAdViewFactory
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.playlist.Playlist
import com.wapo.flagship.features.grid.GridEntity
import com.wapo.flagship.features.grid.GridEnvironment
import com.wapo.flagship.features.grid.ScreenTypeListener
import com.wapo.flagship.features.grid.WPGridView
import com.wapo.flagship.features.grid.model.AudioArticle
import com.wapo.flagship.features.grid.model.CarouselAudio
import com.wapo.flagship.features.grid.model.CarouselAudioItem
import com.wapo.flagship.features.grid.model.CarouselAudioPlaylist
import com.wapo.flagship.features.grid.model.CompoundLabel
import com.wapo.flagship.features.grid.model.EllipsisActionItem
import com.wapo.flagship.features.grid.model.Grid
import com.wapo.flagship.features.grid.model.SectionInlineMessage
import com.wapo.flagship.features.grid.model.HomepageStory
import com.wapo.flagship.features.grid.model.Link
import com.wapo.flagship.features.grid.model.PageModelMapper
import com.wapo.flagship.features.grid.model.RelatedLinkItem
import com.wapo.flagship.features.grid.model.PageConfig
import com.wapo.flagship.features.grid.model.ScreenSizeLayout
import com.wapo.flagship.features.grid.model.Video
import com.wapo.flagship.features.grid.parseGridJson
import com.wapo.flagship.features.grid.views.vote.VoteGuideService
import com.wapo.flagship.features.pagebuilder.AdViewFactory
import com.wapo.flagship.features.sections.SectionsPagerView
import com.wapo.flagship.features.sections.SubscribeButton
import com.wapo.flagship.features.sections.model.TargetingContent
import com.wapo.flagship.features.settings.AppPreferences
import com.wapo.view.habittiles.Tile
import com.washingtonpost.android.R
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.recirculation.carousel.listeners.CarouselProvider
import com.washingtonpost.android.volley.toolbox.AnimatedImageLoader
import com.washingtonpost.android.volley.toolbox.ImageLoaderProvider
import com.washingtonpost.android.wapocontent.Priority
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class GridDebugActivity :
    AppCompatActivity(),
    ImageLoaderProvider,
    GridEnvironment {
    private lateinit var gridView: WPGridView
    private lateinit var menuView: View
    private val grids = mutableMapOf<String, GridEntity?>()
    private var key: String? = null
    private var adViewFactory: AdViewFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_debug)
        savedInstanceState?.let {
            key = savedInstanceState.getString("STATE_KEY")
            grids["News"] = savedInstanceState.getSerializable("News") as? GridEntity
            grids["Wide Right"] = savedInstanceState.getSerializable("Wide Right") as? GridEntity
            grids["Custom Grid"] = savedInstanceState.getSerializable("Custom Grid") as? GridEntity
        }
        grids["Labels"] = parseGridJson(this, R.raw.labels)
        grids["Art Positions With Labels"] = parseGridJson(this, R.raw.art_position_labels)
        grids["Headline sizes"] = parseGridJson(this, R.raw.headline_sizes)
        grids["Table Dividers"] = parseGridJson(this, R.raw.table_dividers)
        grids["Art Widths"] = parseGridJson(this, R.raw.art_widths)

        menuView = findViewById(R.id.menuView)
        gridView =
            findViewById<WPGridView>(R.id.gridView).also {
                it.setImageLoader(imageLoader)
                it.setScreenTypeListener(
                    object : ScreenTypeListener {
                        override fun onScreenTypeChanged(screenSizeLayout: ScreenSizeLayout) {
                            title = screenSizeLayout.toString()
                        }
                    },
                )
            }
        val key = key
        if (key == null) {
            gridView.visibility = View.GONE
            menuView.visibility = View.VISIBLE
        } else {
            displayGrid(grids[key]!!)
        }

        gridView.setEnvironment(this)

        findViewById<View>(R.id.customGridView).setOnClickListener {
            if (grids["Custom Grid"] == null) {
                showURlChooser()
            } else {
                displayGrid(grids["Custom Grid"]!!)
            }
        }

        findViewById<View>(R.id.customGridView).setOnLongClickListener {
            grids["Custom Grid"] = null
            Toast.makeText(this, "Custom Grid has been reset", Toast.LENGTH_SHORT).show()
            true
        }

        findViewById<View>(R.id.multiTables).setOnClickListener {
            displayGrid(grids["Multi Tables"]!!)
        }

        findViewById<View>(R.id.fullBleed).setOnClickListener {
            displayGrid(grids["Full Bleed"]!!)
        }

        findViewById<View>(R.id.selectiveBreakpoint).setOnClickListener {
            displayGrid(grids["Selective Breakpoints"]!!)
        }

        findViewById<View>(R.id.liveBlogExample).setOnClickListener {
            displayGrid(grids["Live Ticker example"]!!)
        }

        findViewById<View>(R.id.labels).setOnClickListener {
            displayGrid(grids["Labels"]!!)
        }

        findViewById<View>(R.id.art_labels).setOnClickListener {
            displayGrid(grids["Art Positions With Labels"]!!)
        }

        findViewById<View>(R.id.headline_sizes).setOnClickListener {
            displayGrid(grids["Headline sizes"]!!)
        }

        findViewById<View>(R.id.table_dividers).setOnClickListener {
            displayGrid(grids["Table Dividers"]!!)
        }

        findViewById<View>(R.id.art_widths).setOnClickListener {
            displayGrid(grids["Art Widths"]!!)
        }
    }

    private fun openGrid(
        key: String,
        url: String,
    ) {
        this.key = key
        if (grids[key] == null) {
            initializeAndShowGrid(key, url)
            return
        }
        displayGrid(grids[key]!!)
    }

    private fun displayGrid(grid: GridEntity) {
        gridView.setGrid(
            PageModelMapper.getGrid(
                grid,
                false,
                PageConfig.build(ConfigManager.getInstance().config)
            )
        )
        gridView.visibility = View.VISIBLE
        menuView.visibility = View.GONE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        showMenu()
        return super.onSupportNavigateUp()
    }

    private fun showMenu() {
        gridView.visibility = View.GONE
        menuView.visibility = View.VISIBLE
        key = null
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.grid_debug, menu)
        val item = menu.findItem(R.id.grid_switch)
        val switchView = item.actionView?.findViewById<SwitchCompat>(R.id.gridSwitch)
        switchView?.setOnCheckedChangeListener { buttonView, isChecked ->
            gridView.setShowGrid(isChecked)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = super.onOptionsItemSelected(item)

    override fun getImageLoader(): AnimatedImageLoader {
        val app = application as FlagshipApplication
        return app.animatedImageLoader
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("STATE_KEY", key)
        outState.putSerializable("News", grids["News"])
        outState.putSerializable("Wide Right", grids["Wide Right"])
        outState.putSerializable("Custom Grid", grids["Custom Grid"])
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (gridView.visibility == View.VISIBLE) {
            showMenu()
        } else {
            super.onBackPressed()
        }
    }

    fun showURlChooser() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.grid_url_chooser_dialog, null)
        val layoutUri = view.findViewById(R.id.layoutUri) as EditText
        builder.setTitle("Enter layout json url")
        builder
            .setView(view)
            .setNegativeButton(com.washingtonpost.android.notifications.R.string.cancelLabel) { dialog, which ->
                dialog.cancel()
            }.setPositiveButton(R.string.archive_open_button) { dialog, which ->
                val urlString = layoutUri.text.toString()
                initializeAndShowGrid("Custom Grid", urlString)
            }
        builder.create().show()
    }

    fun initializeAndShowGrid(
        gridName: String,
        url: String,
    ) {
        UpdateFusionPageObservable
            .create(
                url,
                FlagshipApplication.getInstance().requestQueue,
                Priority(Priority.Group.FOREGROUND, System.currentTimeMillis()),
                true,
            ).timeout(5, TimeUnit.SECONDS)
            .subscribe(
                { response ->
                    grids[gridName] = response
                    displayGrid(response)
                },
                { t ->
                    Logger.e(
                        GridDebugActivity::class.java.simpleName,
                        "Network error for $gridName: $url",
                        t,
                    )
                    Toast.makeText(this, "Unable to download $gridName", Toast.LENGTH_SHORT).show()
                },
            )
    }

    override fun getAdViewFactory(): AdViewFactory {
        if (adViewFactory == null) {
            adViewFactory = ClassicAdViewFactory(this)
        }

        return adViewFactory!!
    }

    override fun isNightModeEnabled(): Boolean = false

    override fun isPhone(): Boolean = !DeviceUtils.isTablet(this)

    override fun isPortraitPhone(): Boolean = !DeviceUtils.isTablet(this) && resources.configuration.orientation == ORIENTATION_PORTRAIT

    override fun openArticle(
        story: HomepageStory,
        grid: Grid,
        sectionDisplayName: String,
        bundleId: String,
        position: Int,
    ) {
    }

    override fun openArticleUrl(
        url: String?,
        story: HomepageStory,
        grid: Grid,
        sectionDisplayName: String,
        bundleId: String,
        position: Int
    ) {
    }

    override fun openMedia(
        story: HomepageStory,
        link: Link,
        grid: Grid,
        sectionDisplayName: String,
        bundleId: String,
    ) {
    }

    override fun openRelatedLink(
        relatedLink: RelatedLinkItem,
        story: HomepageStory,
        grid: Grid,
        sectionDisplayName: String,
        bundleId: String,
    ) {
    }

    override fun openLabel(
        label: CompoundLabel,
        itId: String?,
    ) {
    }

    override fun getPager(): SectionsPagerView? {
        TODO("Not yet implemented")
    }

    override fun openLiveBlog(
        link: String,
        grid: Grid,
        sectionDisplayName: String,
        bundleId: String,
        itId: String?
    ) {
    }

    override fun bookMarkClicked(
        ellipseActionItem: EllipsisActionItem,
        view: ImageView,
        isStatusChecked: Boolean,
    ) {
        TODO("Not yet implemented")
    }

    override fun openHabitTileLink(
        link: String?,
        sectionDisplayName: String,
        trackingSection: String?,
        trackingSubsection: String?,
    ) {
        TODO("Not yet implemented")
    }

    override fun trackHabitTileClicked(link: String?) {
        TODO("Not yet implemented")
    }

    override fun isLowDataModeEnable(): Boolean = AppPreferences.isLowDataModeEnabled()
    override fun isPremiumAccount(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getLiveBlogProxyUrl(): String = ConfigManager.getInstance().config.liveBlogServiceURL

    override fun remoteLogError(
        tag: String,
        throwable: Throwable,
    ) {
        TODO("Not yet implemented")
    }

    override fun isConnected(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCarouselNetworkRequestsHelper(): CarouselProvider {
        TODO("Not yet implemented")
    }

    override fun openStackCard(
        links: List<Link>,
        sectionDisplayName: String,
        position: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun playAudioCarouselAudioArticleItem(
        carouselAudio: CarouselAudio,
        position: Int,
        sectionDisplayName: String?,
        audioTracker: AudioTracker?
    ) {
        TODO("Not yet implemented")
    }

    override fun playAudioCarouselAudioPlaylistArticleItem(
        carouselAudioPlaylist: CarouselAudioPlaylist,
        playListItems: List<Playlist>,
        position: Int,
        sectionDisplayName: String?,
    ) {
        TODO("Not yet implemented")
    }

    override fun playAudioIfHasAccess(audioMediaConfig: AudioMediaConfig) {
        TODO("Not yet implemented")
    }

    override fun generateAudioMediaConfig(
        carouselAudioItem: CarouselAudioItem?,
        sectionDisplayName: String?,
        audioTracker: AudioTracker?
    ): AudioMediaConfig? {
        TODO("Not yet implemented")
    }

    override fun generateAudioMediaConfig(
        audioArticle: AudioArticle,
        isFlexFeature: Boolean,
        isActionButton: Boolean,
    ): AudioMediaConfig {
        TODO("Not yet implemented")
    }

    override fun getHabitTiles(): List<Tile> {
        TODO("Not yet implemented")
    }

    override fun getNewsprintEngagedStatus(): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun getNewsprintHasViewed(): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override fun getNewsprintReaderType(): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun isLoggedInUser(): Boolean = true

    override fun isSaveEnabled(): Boolean = true

    override fun getAdTagUrl(
        video: Video,
        story: HomepageStory,
        targetingContent: TargetingContent?
    ): String? = null

    override fun getSubscribeButton(sectionDisplayName: String?): SubscribeButton? = null

    override fun onRefresh(
        bundleName: String,
        sectionDisplayName: String,
    ) {
    }

    override fun getVoteGuideService(): VoteGuideService {
        TODO("Not yet implemented")
    }

    override fun showSaveRegwall(context: Context) {
        TODO("Not yet implemented")
    }

    override fun isAdsContentContextualTargetingEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getSectionInlineMessage(): SectionInlineMessage? {
        TODO("Not yet implemented")
    }

    override fun shouldSuppressAds(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPageConfig(): PageConfig {
        return PageConfig.build(ConfigManager.getInstance().config)
    }
}
