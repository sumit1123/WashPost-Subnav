package com.wapo.flagship.features.print

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.radaee.view.WapoPDFViewPager.WapoReaderListener
import com.squareup.picasso.Picasso
import com.wapo.android.commons.engagement.EngagementTracker
import com.wapo.android.commons.engagement.PageEngagementTrace
import com.wapo.android.commons.util.Download
import com.wapo.android.remotelog.logger.EventTimerLog
import com.wapo.flagship.FlagshipApplication.Companion.getInstance
import com.wapo.flagship.Utils
import com.wapo.flagship.data.Archive
import com.wapo.flagship.data.ArchiveManager
import com.wapo.flagship.features.articles2.activities.ArticlesParcel.Companion.builder
import com.wapo.flagship.features.articles2.activities.CURRENT_TAB_NAME
import com.wapo.flagship.features.articles2.activities.PRINT_ORIGINATED
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.features.shared.fragments.TopBarFragment
import com.wapo.flagship.model.PrintSectionPage
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.data.CacheManager
import com.wapo.flagship.util.ReachabilityUtil
import com.wapo.flagship.util.WPUrlAnalyser
import com.wapo.flagship.util.WPUrlAnalyser.AnalysedUrlListener
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.flagship.wrappers.CrashWrapper
import com.wapo.view.ProportionalLayout
import com.washingtonpost.android.R
import com.washingtonpost.android.databinding.ActivityPdf2Binding
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallConstants.getWallReason
import dagger.hilt.android.AndroidEntryPoint
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class Pdf2Activity :
    BaseActivity(),
    WapoReaderListener,
    PrintActivityInterface,
    ThumbnailClickListener {
    private val pdf2ViewModel: Pdf2ViewModel by viewModels()

    private lateinit var binding: ActivityPdf2Binding

    private var mHandler: PdfHandler? = null
    private lateinit var printSectionPages: ArrayList<PrintSectionPage>
    private var pdfLocations: Array<String?> = arrayOf()
    private var pagerAdapter: ScreenSlidePagerAdapter? = null
    private var pageNo = 0
    private var mReceiver: BroadcastReceiver? = null
    private var incomingDownloadIdList: ArrayList<Long>? = null
    private var label: Long = 0
    private var sectionLetter: String? = null
    private var thumbnailListener: View.OnClickListener? = null
    private var animatorListenerAdapter: AnimatorListenerAdapter? = null
    private val engagementTracker = EngagementTracker.getInstance()
    private var engagementTrace: PageEngagementTrace? = null
    private var currentPageName: String? = null

    @Inject
    lateinit var cacheManager: CacheManager

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdf2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.apply {
            setSupportActionBar(this)
            setNavigationOnClickListener {
                onBackPressed()
            }
        }
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowTitleEnabled(false)
        }

        pdf2ViewModel.init()
        binding.pdfPager.apply {
            setBackgroundResource(android.R.color.transparent)
            /**
             * RecyclerView is an immediate child of ViewPager2. It is safe to access at index 0.
             * If by any chance it is changed in later major versions of ViewPager, then the below
             * logic should be updated.
             */
            if (childCount > 0) {
                (getChildAt(0) as? RecyclerView)?.let {
                    it.layoutManager?.isItemPrefetchEnabled = false
                    it.setItemViewCacheSize(0)
                }
            }
            visibility = View.INVISIBLE
            registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        this@Pdf2Activity.onPageChanged(position)
                        System.gc()
                    }
                },
            )
            offscreenPageLimit = pdf2ViewModel.offscreenPageLimit
        }

        val intent = intent
        label = intent.getLongExtra(ARCHIVE_LABEL, -1)
        sectionLetter = intent.getStringExtra(ARCHIVE_SECTION_LETTER)
        printSectionPages = intent.getSerializableExtra(PARAM_PDF) as ArrayList<PrintSectionPage>

        val pdfFolder = ArchiveManager.getArchiveFolder(this, label, sectionLetter)
        pdfLocations = arrayOfNulls(printSectionPages.size)
        printSectionPages.let {
            for (i in it.indices) {
                pdfLocations[i] =
                    if (isTutorial) {
                        ArchiveManager.getTutorialAssetFilePath(
                            this,
                            it[i].hiResPdfPath,
                        )
                    } else {
                        pdfFolder.path + File.separator + it[i].hiResPdfPath
                    }
            }
        }

        val adapter =
            ThumbnailAdapter(printSectionPages, label, this@Pdf2Activity, this@Pdf2Activity)
        incomingDownloadIdList =
            intent.getSerializableExtra(ARCHIVE_DOWNLOAD_ID) as ArrayList<Long>?
        var thumbnailVisibility = false
        if (savedInstanceState != null) {
            pageNo = savedInstanceState.getInt(PARAM_PAGE_NUMBER, 0)
            incomingDownloadIdList =
                savedInstanceState.getSerializable(PARAM_ARCHIVE_ID) as ArrayList<Long>?
            thumbnailVisibility = savedInstanceState.getBoolean(PARAM_THUMBNAILS_VISIBLE, false)
        } else {
            pageNo = intent.getIntExtra(PARAM_PAGE_NUMBER, 0)
        }
        binding.pdfThumbnails.apply {
            setHasFixedSize(true)
            this.adapter = adapter
            visibility = if (thumbnailVisibility) View.VISIBLE else View.GONE
        }

        animatorListenerAdapter =
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.thumbView.visibility = View.GONE
                    binding.pdfIndicator.visibility = View.GONE
                }
            }
        binding.pageNumber.setOnClickListener {
            if (binding.pdfThumbnails.visibility != View.VISIBLE) {
                binding.pdfThumbnails.visibility = View.VISIBLE
            } else {
                binding.pdfThumbnails.visibility = View.GONE
            }
            updateMenuItemTextColor()
        }
        updateMenuItemText()
        updateMenuItemTextColor()
        if (downloadPending()) {
            binding.downloadProgress.visibility = View.VISIBLE
        } else {
            binding.downloadProgress.visibility = View.INVISIBLE
        }
        loadDocument()
        mReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    val action = intent.action ?: "null"
                    Logger.d(TAG, action)
                    try {
                        val am = getInstance().archiveManager
                        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        val sectionLetter = arrayOfNulls<String>(1)
                        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                            am
                                .getArchiveByDownloadIdObs(downloadId)
                                .take(1)
                                .flatMap { archive ->
                                    if (archive != null) {
                                        sectionLetter[0] = archive.section
                                        am.getSynchronizedArchiveObs(archive.date, archive.section)
                                    } else {
                                        Logger.w(
                                            TAG,
                                            String.format(
                                                "No archive found with downloadId: %s,",
                                                downloadId,
                                            ),
                                        )
                                        Observable.empty()
                                    }
                                }.observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                    object : Subscriber<Archive?>() {
                                        override fun onCompleted() {}

                                        override fun onError(e: Throwable) {
                                            Logger.e(
                                                TAG,
                                                String.format(
                                                    "Error processing download in PDFActivity for %s-%s with downloadId: %s",
                                                    label,
                                                    sectionLetter[0],
                                                    downloadId,
                                                ),
                                                e,
                                            )
                                        }

                                        override fun onNext(archive: Archive?) {
                                            if (archive != null &&
                                                incomingDownloadIdList?.contains(
                                                    downloadId,
                                                ) == true
                                            ) {
                                                Logger.d(
                                                    TAG,
                                                    String.format(
                                                        "Finishing download of %s-%s with downloadId: %s.",
                                                        label,
                                                        sectionLetter[0],
                                                        downloadId,
                                                    ),
                                                )
                                                finishDownload(downloadId)
                                            }
                                        }
                                    },
                                )
                        }
                    } catch (e: Exception) {
                        Logger.e(TAG, Utils.exceptionToString(e))
                    }
                }
            }
        observeRetryEvent()
    }

    private fun observeRetryEvent() {
        pdf2ViewModel.retryEvent.observe(this) { itemPosition ->
            onPageChanged(itemPosition)
        }
    }

    private val isTutorial: Boolean
        private get() = sectionLetter != null && sectionLetter == ArchiveManager.TUTORIAL_SECTION_NAME

    private fun updateMenuItemText() {
        if (pageNo >= 0 && pageNo < printSectionPages.size) {
            binding.pageNumber.text =
                String.format(
                    Locale.US,
                    MENU_ITEM_FORMAT,
                    printSectionPages.let { it[pageNo].pageName },
                )
        }
    }

    private fun updateMenuItemTextColor() {
        val thumbnailsHidden = binding.pdfThumbnails.visibility != View.VISIBLE
        binding.pageNumber.setTextColor(
            ContextCompat.getColor(
                this,
                if (thumbnailsHidden) R.color.print_edition_pdf_thumbnail_text else R.color.print_edition_calendar_selected_background,
            ),
        )
        binding.pageNumber.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            if (thumbnailsHidden) R.drawable.icon_pagepicker else R.drawable.icon_pagepicker_blue,
            0,
        )
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
        super.onBackPressed()
    }

    private fun loadDocument() {
        Logger.d(TAG, "loadDocument")
        onDocumentOpen()
    }

    override fun onStart() {
        super.onStart()
        if (mHandler == null) {
            mHandler = PdfHandler(this)
        }
        mHandler?.sendEmptyMessageDelayed(0, 500)
        startEngagementTrace()
    }

    override fun onResume() {
        super.onResume()
        /**
         * When a user opens ArticlesActivity right before getting paywalled on this screen,
         * the user will continue to see the paywall on this screen even if they were to successfully login from
         * the paywall in ArticlesActivity. So, will hide the paywall in the onStart.
         */
        if (PaywallService.getInstance().isPremiumUser && isWallShowing) {
            dismissWall()
        }
        Measurement.resumeCollection(this)
        binding.pdfIndicator.visibility = View.GONE
        if (downloadPending()) {
            val incomingDownloadArray =
                arrayOfNulls<Long>(
                    incomingDownloadIdList?.size as Int,
                )
            incomingDownloadIdList?.toArray(incomingDownloadArray)
            for (incomingDownloadId in incomingDownloadArray) {
                val download =
                    Download.get(
                        this,
                        incomingDownloadId as Long,
                    )
                if (download == null || download.status == DownloadManager.STATUS_SUCCESSFUL) {
                    val am = getInstance().archiveManager
                    am
                        .getArchiveByDownloadIdObs(incomingDownloadId)
                        .take(1)
                        .flatMap { archive ->
                            if (archive != null) {
                                am.getSynchronizedArchiveObs(archive.date, archive.section)
                            } else {
                                Logger.w(
                                    TAG,
                                    String.format(
                                        "No archive found with downloadId: %s,",
                                        incomingDownloadId,
                                    ),
                                )
                                Observable.empty()
                            }
                        }.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            object : Subscriber<Archive?>() {
                                override fun onCompleted() {}

                                override fun onError(e: Throwable) {
                                    Logger.e(TAG, "Error performing auto-reload from onResume.", e)
                                }

                                override fun onNext(archive: Archive?) {
                                    if (archive != null) {
                                        finishDownload(incomingDownloadId)
                                    }
                                }
                            },
                        )
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            mReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED,
        )
        EventTimerLog.stopTimingEventAndLog(
            EventTimerLog.ARCHIVE_OPEN_PDF,
            EventTimerLog.ARCHIVE_OPEN_PDF,
            applicationContext,
            false,
        )
        invalidateOptionsMenu()
        CrashWrapper.logExtras("PdfActivity onResume ")
    }

    private fun finishDownload(incomingDownloadId: Long) {
        if (incomingDownloadIdList != null) {
            // The archive you're currently viewing just finished downloading.
            incomingDownloadIdList?.remove(incomingDownloadId)
            if (incomingDownloadIdList.isNullOrEmpty()) {
                binding.downloadProgress.visibility = View.INVISIBLE
            }
        } else {
            binding.downloadProgress.visibility = View.INVISIBLE
        }
        // Reload the document, and the newly processed PDFs should be there.
        loadDocument()
        updateAdapterContent()
    }

    override fun onPause() {
        unregisterReceiver(mReceiver)
        super.onPause()
    }

    private fun paywallTrackPageView(pageNo: Int) {
        if (pageNo > 0 && PaywallService.initialized()) {
            val reason = getWallReason(PaywallConstants.WallType.METERED_PAYWALL)
            showWallDialog(
                PaywallService.getInstance().isWpUserLoggedIn,
                reason,
                PaywallConstants.WallType.METERED_PAYWALL,
            )
        }
    }

    override fun onStop() {
        if (mHandler != null) {
            mHandler?.removeCallbacksAndMessages(null)
            mHandler = null
        }
        stopAndTrackEngagementTrace()
        super.onStop()
    }

    override fun onDestroy() {
        binding.thumbView.setImageDrawable(null)
        mReceiver = null
        pdf2ViewModel.release()
        super.onDestroy()
    }

    override fun onPageChanged(pageno: Int) {
        val isFirstPage = pageno == this.pageNo
        val oldPage = pageNo
        pageNo = pageno
        if (isTutorial) {
            Measurement.trackPrintTutorial()
        }
        updateMenuItemText()
        // Check to see if the section was changed.  If it was, it may mean a new download is necessary.
        if (!isTutorial) {
            currentPageName = (Measurement.PAGE_PDFFULL +
                    Measurement.archiveDateSectionPageNum(label, sectionLetter ?: "", printSectionPages[pageno].pageNumber))
                .lowercase()
        }
        val printSectionPage = printSectionPages.let { it[pageno] }
        val newSectionLetter = printSectionPage.sectionLetter
        val sectionLmt =
            if (printSectionPage.sectionLmt != null) printSectionPage.sectionLmt else 0
        sectionLetter = newSectionLetter
        val am = getInstance().archiveManager
        am
            .getSynchronizedArchiveObs(label, sectionLetter)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                object : Subscriber<Archive?>() {
                    override fun onCompleted() {}

                    override fun onError(e: Throwable) {
                        Logger.e(
                            TAG,
                            String.format(
                                "Error getting synchronized archive in PDFActivity onPageChanged for archive %s-%s",
                                label,
                                sectionLetter,
                            ),
                        )
                    }

                    override fun onNext(archive: Archive?) {
                        var archive: Archive? = archive
                        if (archive == null || archive.status == Archive.Status.Canceled || archive.status == Archive.Status.Deleted) {
                            // download
                            try {
                                archive =
                                    am.scheduleFileDownload(
                                        label,
                                        sectionLetter,
                                        sectionLmt as Long,
                                    )
                            } catch (e: IOException) {
                                Logger.e(
                                    TAG,
                                    String.format(
                                        "Error scheduling file download in PDFActivity for archive %s-%s",
                                        label,
                                        sectionLetter,
                                    ),
                                )
                            }
                        }
                        // If this section was somehow mid-download but not being tracked, add it to the download list.
                        if (archive != null &&
                            archive.downloadId != null &&
                            incomingDownloadIdList?.contains(
                                archive.downloadId,
                            ) == false
                        ) {
                            incomingDownloadIdList?.add(archive.downloadId)
                        }
                        if (!downloadPending() && !isTutorial) {
                            archive?.let {
                                val pageNum = printSectionPages[pageNo].pageNumber
                                val pubDateOrSerial =
                                    Measurement.archiveDateSectionPageNum(
                                        it.date,
                                        it.section,
                                        pageNum,
                                    )
                                Measurement.trackPrint(
                                    null,
                                    Measurement.PAGE_PDFFULL,
                                    pubDateOrSerial,
                                    isFirstPage,
                                )
                            }
                            // Only check paywall status if there's no download left to complete.
                            // This prevents the download BroadcastReceiver from being interrupted by the paywall.
                            paywallTrackPageView(pageNo)
                        }
                    }
                },
            )
        binding.pdfThumbnails.layoutManager?.scrollToPosition(pageNo)
        binding.pdfThumbnails.adapter?.notifyItemChanged(oldPage)
        binding.pdfThumbnails.adapter?.notifyItemChanged(pageNo)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PARAM_PAGE_NUMBER, pageNo)
        outState.putSerializable(PARAM_ARCHIVE_ID, incomingDownloadIdList)
        outState.putBoolean(
            PARAM_THUMBNAILS_VISIBLE,
            binding.pdfThumbnails.visibility == View.VISIBLE,
        )
    }

    override fun onOpenURI(
        uri: String,
        firstAttempt: Boolean,
    ) {
        if (uri.startsWith(CONTINUE_STRING)) {
            val re0 = "(continue:)"
            val re1 = "([a-z]+)"
            val re2 = "(\\d+)"
            val p = Pattern.compile(re0 + re1 + re2, Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
            val m = p.matcher(uri)
            if (m.find()) {
                val section = m.group(2)
                val pageNumber = m.group(3)
                val sb = StringBuilder()
                sb.append(section)
                if (pageNumber.length == 1) {
                    sb.append(0)
                }
                sb.append(pageNumber)
                val pageName = sb.toString()
                printSectionPages?.let {
                    for (i in it.indices) {
                        val page = it[i]
                        if (page.pageName.equals(
                                pageName,
                                ignoreCase = true,
                            ) &&
                            pagerAdapter != null
                        ) {
                            pagerAdapter?.scrollToPage(i, false)
                            return
                        }
                    }
                }
            }
            return
        }
        val fileMeta = cacheManager.getFileMetaByUrl(uri)
        if (fileMeta == null) {
            binding.pdfIndicator.visibility = View.VISIBLE
            // If the user has an internet connection, then it's not a problem to get the article from the internet.
            // Also try this if caching has failed once, as we don't want an endless loop here.
            if (ReachabilityUtil.isConnected(applicationContext) || !firstAttempt) {
                val analyser = WPUrlAnalyser.getWPUrlAnalyser()
                analyser.setAnalysedUrlListener(
                    object : AnalysedUrlListener {
                        override fun onCancelLoader() {
                            binding.pdfIndicator.visibility = View.GONE
                        }

                        override fun onModifyLaunchIntent(intent: Intent) {
                            intent.putExtra(
                                CURRENT_TAB_NAME,
                                getString(R.string.tab_print_edition),
                            )
                            intent.putExtra(
                                TopBarFragment.SectionDisplayName,
                                getIntent().getStringExtra(TopBarFragment.SectionDisplayName),
                            )
                            intent.putExtra(PRINT_ORIGINATED, true)
                        }
                    },
                )
                analyser.analyseAndStartIntent(this, uri, "")
            } else {
                // If the user isn't online, then we need to try to re-add the articles to cache.
                val am = getInstance().archiveManager
                Logger.w(
                    TAG,
                    String.format(
                        "Unable to find story %s in cache for archive %s-%s.  Attempting to refill cache for archive.",
                        uri,
                        label,
                        sectionLetter,
                    ),
                )
                var archive = am.getArchiveByLabelAndSection(label, sectionLetter)
                if (archive == null) {
                    archive = am.getPreviewArchiveForLabel(label)
                }
                am
                    .getProcessArticlesObs(archive)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        object : Subscriber<Boolean?>() {
                            override fun onCompleted() {}

                            override fun onError(e: Throwable) {
                                Logger.e(
                                    TAG,
                                    String.format(
                                        "Failed to reload cache for archive %s-%s",
                                        label,
                                        sectionLetter,
                                    ),
                                    e,
                                )
                                binding.pdfIndicator.visibility = View.GONE
                                onOpenURI(uri, false)
                            }

                            override fun onNext(aBoolean: Boolean?) {
                                if (aBoolean == false) {
                                    Logger.e(
                                        TAG,
                                        String.format(
                                            "Failed to reload cache for archive %s-%s",
                                            label,
                                            sectionLetter,
                                        ),
                                    )
                                }
                                binding.pdfIndicator.visibility = View.GONE
                                onOpenURI(uri, false)
                            }
                        },
                    )
            }
        } else {
            val intent =
                builder()
                    .setArticleSingleUrl(fileMeta.url)
                    .setTabName(getString(R.string.tab_print_edition))
                    .setSectionDisplayName(intent.getStringExtra(TopBarFragment.SectionDisplayName))
                    .printOriginated(true)
                    .buildIntent(this)
            startActivity(intent)
        }
    }

    private fun onDocumentOpen() {
        Logger.d(TAG, "onDocumentOpen")
        if (pagerAdapter == null || pageNo == binding.pdfPager.currentItem) {
            pagerAdapter =
                ScreenSlidePagerAdapter(this, pdfLocations).apply {
                    binding.pdfPager.adapter = this
                    scrollToPage(pageNo)
                    binding.pdfPager.visibility = View.VISIBLE
                    hideThumbView()
                }
        }
        Logger.d(TAG, "onDocumentOpen complete")
    }

    private fun downloadPending(): Boolean = !incomingDownloadIdList.isNullOrEmpty()

    override fun showOverlay(): Boolean = true

    override fun getOverlayLayoutId(): Int = R.layout.activity_pdf_overlay

    private fun updateAdapterContent() {
        runOnUiThread {
            binding.pdfThumbnails.adapter?.notifyDataSetChanged()
        }
    }

    override fun getThumbnailClickListener(): View.OnClickListener {
        if (thumbnailListener == null) {
            thumbnailListener =
                View.OnClickListener { v ->
                    pagerAdapter?.scrollToPage((v.tag as Int), false)
                    binding.pdfThumbnails.visibility = View.GONE
                    updateMenuItemTextColor()
                }
        }
        return thumbnailListener as View.OnClickListener
    }

    override fun getPageNo(): Int = pageNo

    private class ThumbnailAdapter internal constructor(
        pages: ArrayList<PrintSectionPage>?,
        label: Long,
        context: Context,
        listener: ThumbnailClickListener,
    ) : RecyclerView.Adapter<ThumbnailVH>() {
        private val pages: List<PrintSectionPage>?
        private val label: Long
        private val context: Context
        private val listener: ThumbnailClickListener

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ThumbnailVH {
            val thumbnailView =
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.view_pdf_thumbnail_item, parent, false)
            val thumbnailVH = ThumbnailVH(thumbnailView)
            thumbnailVH.thumbnail.setOnClickListener(listener.thumbnailClickListener)
            return thumbnailVH
        }

        override fun onBindViewHolder(
            holder: ThumbnailVH,
            position: Int,
        ) {
            holder.thumbnail.setImageDrawable(null)
            val page = pages?.let { it[position] }
            val sectionLetter = page?.sectionLetter
            val imageFile =
                if (ArchiveManager.TUTORIAL_SECTION_NAME == sectionLetter) {
                    File(
                        ArchiveManager.getTutorialAssetFilePath(
                            context,
                            page.thumbnailPath,
                        ),
                    )
                } else {
                    ArchiveManager.getFullFilePath(
                        context,
                        label,
                        sectionLetter,
                        page?.thumbnailPath,
                    )
                }
            val aspectRatio = page?.pageWidth?.toFloat()?.div(page.pageHeight)
            if (aspectRatio != null) {
                holder.imageFrame.aspectRatio = aspectRatio
            }
            Picasso
                .get()
                .load(imageFile)
                .placeholder(R.drawable.archives_placeholder)
                .noFade()
                .error(R.drawable.archives_placeholder)
                .fit()
                .centerInside()
                .into(holder.thumbnail)
            holder.thumbnail.tag = holder.adapterPosition
            holder.pageName.text = page?.pageName
            holder.itemView.isSelected = position == listener.pageNo
        }

        override fun getItemCount(): Int = pages?.size as Int

        init {
            this.pages = pages
            this.label = label
            this.context = context
            this.listener = listener
        }
    }

    private class ThumbnailVH internal constructor(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        var thumbnail: ImageView
        var pageName: TextView
        var imageFrame: ProportionalLayout

        init {
            thumbnail = itemView.findViewById<View>(R.id.pdfThumbnail) as ImageView
            pageName = itemView.findViewById<View>(R.id.pageName) as TextView
            imageFrame = itemView.findViewById<View>(R.id.image_frame) as ProportionalLayout
        }
    }

    private class PdfHandler(
        activity: Pdf2Activity?,
    ) : Handler() {
        private val mActivity: WeakReference<Pdf2Activity?>?

        override fun handleMessage(msg: Message) {
            if (mActivity?.get()?.isFinishing as Boolean) return
            if (!(mActivity.get()?.incomingDownloadIdList.isNullOrEmpty())) {
                val downloadBar = mActivity.get()?.binding?.downloadProgress
                val downloadId = mActivity.get()?.incomingDownloadIdList?.let { it[0] }
                var download: Download? = null
                try {
                    download = downloadId?.let { Download.get(mActivity.get(), it) }
                } catch (e: Exception) {
                    Logger.e(TAG, "handleMessage ", e)
                }
                if (download != null) {
                    val status = download.status
                    val isDownloading =
                        status == DownloadManager.STATUS_PAUSED ||
                            status == DownloadManager.STATUS_PENDING ||
                            status == DownloadManager.STATUS_RUNNING
                    if (downloadBar != null) {
                        if (isDownloading) {
                            val downloaded = download.downloadedBytes
                            val total = download.totalBytes
                            val percentage = downloaded / total.toFloat() * 100
                            downloadBar.progress = percentage.toInt()
                            downloadBar.visibility = View.VISIBLE
                        } else {
                            downloadBar.visibility = View.INVISIBLE
                        }
                    }
                } else {
                    downloadId?.let { mActivity.get()?.finishDownload(it) }
                }
            }
            if (mActivity.get()?.mHandler != null) {
                mActivity.get()?.mHandler?.sendEmptyMessageDelayed(0, 1000)
            }
        }

        init {
            mActivity = WeakReference(activity)
        }
    }

    private inner class ScreenSlidePagerAdapter(
        activity: FragmentActivity,
        val pdfLocationsPaths: Array<String?>,
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = pdfLocationsPaths.size

        override fun createFragment(position: Int): Fragment {
            val bundle =
                Bundle().apply {
                    putString("filePath", pdfLocationsPaths[position])
                    putInt("position", position)
                }
            return PdfPageFragment.newInstance(bundle)
        }

        fun scrollToPage(
            pageNo: Int,
            smoothScroll: Boolean = false,
        ) {
            binding.pdfPager.setCurrentItem(pageNo, smoothScroll)
        }
    }

    private fun hideThumbView() {
        val animator =
            ObjectAnimator.ofFloat(binding.thumbView, "translationX", binding.thumbView.alpha, 0f)
        animator.addListener(animatorListenerAdapter)
        animator.duration = 500
        animator.start()
    }

    private fun startEngagementTrace() {
        if (!isTutorial && engagementTrace == null) {
            engagementTrace = PageEngagementTrace(UUID.randomUUID().toString(), null, null)
            engagementTrace?.startTrace()
        }
    }

    private fun stopAndTrackEngagementTrace() {
        val trace = engagementTrace ?: return
        val pageName = currentPageName ?: return
        trace.stopTrace()
        trace.updateTrackingInfo(pageName, Measurement.CONTENT_TYPE_MAIN, "print")
        engagementTracker.trackEngagement(trace)
        engagementTrace = null
    }

    companion object {
        private val TAG = PdfActivity::class.java.simpleName
        const val PARAM_PAGE_NUMBER = "pagenum"
        const val PARAM_PDF = "pdf"
        const val ARCHIVE_DOWNLOAD_ID = "downloadId"
        const val ARCHIVE_LABEL = "archiveLabel"
        const val ARCHIVE_SECTION_LETTER = "archiveSectionLetter"
        private const val MENU_ITEM_FORMAT = "Page %s"
        private const val PARAM_ARCHIVE_ID = "archiveDownloadId"
        private const val PARAM_THUMBNAILS_VISIBLE = "thumbnailsVisible"
        private const val CONTINUE_STRING = "continue:"
    }
}
