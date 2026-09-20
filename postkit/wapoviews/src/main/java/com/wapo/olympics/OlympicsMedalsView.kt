package com.wapo.olympics

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.bumptech.glide.Glide
import com.wapo.flagship.json.*
import com.wapo.view.R
import com.wapo.view.RippleHelper
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by kattim on 1/10/18.
 */

private const val COMPACT_MODE_THRESHOLD = 364 // 6-units column on homepage

class OlympicsMedalsView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attrs, defStyleAttr) {
    private lateinit var olympicsLink: TextView
    private lateinit var dataList: LinearLayout
    private lateinit var olympicsTitle: TextView
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    private val backgroundColors = arrayOf(
        ContextCompat.getColor(context, R.color.olympics_row_bg_1),
        ContextCompat.getColor(context, R.color.olympics_row_bg_2),
    )
    private var olympicsMedals: OlympicsMedals? = null
    private var olympicsSchedule: OlympicsSchedule? = null
    private val dynamicViews = mutableListOf<View>()
    private val totalMedalsTextViews = mutableListOf<TextView>()

    private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val outputDay = SimpleDateFormat("MMM d", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    val outputTime = SimpleDateFormat("hh:mm aaa", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
        val symbols = DateFormatSymbols(Locale.getDefault())
        symbols.amPmStrings = arrayOf("a.m.", "p.m.")
        dateFormatSymbols = symbols
    }

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        orientation = VERTICAL
        inflater.inflate(R.layout.olympics_view_merge, this, true)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        olympicsLink = findViewById(R.id.olympics_link)
        dataList = findViewById(R.id.olympics_list)
        olympicsTitle = findViewById(R.id.olympics_title)
        RippleHelper.addRippleEffectToView(olympicsLink)

        //design team explicitly asked us to prevent any clicks on olympics data
        dataList.setOnTouchListener { v, event -> true }
    }

    fun setMedals(olympicsMedals: OlympicsMedals) {
        this.olympicsMedals = olympicsMedals
        setupTitle(olympicsMedals.title)
        setupLink(olympicsMedals.cta)
        dataList.let {
            dynamicViews.clear()
            totalMedalsTextViews.clear()
            it.removeAllViews()
            it.addView(getMedalsHeader())
            olympicsMedals.data?.forEachIndexed { index, item ->
                val bgColor = backgroundColors[index.rem(backgroundColors.size)]
                it.addView(getMedalsRow(item, bgColor))
            }
        }
    }

    fun setSchedule(olympicsSchedule: OlympicsSchedule) {
        this.olympicsSchedule = olympicsSchedule
        setupTitle(olympicsSchedule.title)
        setupLink(olympicsSchedule.cta)
        dataList.let {
            dynamicViews.clear()
            totalMedalsTextViews.clear()
            it.removeAllViews()
            it.addView(getScheduleHeader())
            olympicsSchedule.data?.forEachIndexed { index, item ->
                val bgColor = backgroundColors[index.rem(backgroundColors.size)]
                it.addView(getScheduleRow(item, bgColor))
            }
        }
    }

    fun getTitle(): TextView {
        return olympicsTitle
    }

    fun getDataList(): LinearLayout {
        return dataList
    }

    fun getLink(): TextView {
        return olympicsLink
    }

    fun setCtaClickListener(ctaClickListener: (String) -> Unit) {
        olympicsLink.setOnClickListener {
            val medalsUrl = olympicsMedals?.cta?.link?.url
            val scheduleUrl = olympicsSchedule?.cta?.link?.url
            if (medalsUrl != null) {
                ctaClickListener(medalsUrl)
            } else if (scheduleUrl != null) {
                ctaClickListener(scheduleUrl)
            }
        }
    }

    @Deprecated("use new model OlympicsMedals")
    fun initMedalTable(title: String?, linkText: String?, data: Array<CountryMedalItem>?, clickListener: View.OnClickListener) {

    }

    private fun getMedalsHeader(): View? {
        val view = inflater.inflate(R.layout.olympics_medal_header_row_item, dataList, false)
        dynamicViews.add(view.findViewById(R.id.gold_medal_icon))
        dynamicViews.add(view.findViewById(R.id.silver_medal_icon))
        dynamicViews.add(view.findViewById(R.id.bronze_medal_icon))
        return view
    }

    private fun getScheduleHeader(): View? {
        val view = inflater.inflate(R.layout.olympics_schedule_header_row_item, dataList, false)
        val timeHeaderView = view.findViewById<TextView>(R.id.olympics_time)
        val timeZoneName = TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT)
        timeHeaderView.text = resources.getString(R.string.olympics_time, timeZoneName)
        dynamicViews.add(view.findViewById(R.id.olympics_date))
        return view
    }

    private fun getMedalsRow(item: OlympicsMedalsEntry? = null, bgColor: Int): View {
        val view: View = inflater.inflate(R.layout.olympics_medals_row_item, dataList, false)
        val rank: TextView = view.findViewById(R.id.rank)
        val icon: ImageView = view.findViewById(R.id.icon)
        val country: TextView = view.findViewById(R.id.country)
        val gold: TextView = view.findViewById(R.id.gold_medal_count)
        val silver: TextView = view.findViewById(R.id.silver_medal_count)
        val bronze: TextView = view.findViewById(R.id.bronze_medal_count)
        val total: TextView = view.findViewById(R.id.total_medal_count)
        rank.text = item?.rank
        country.text = item?.title
        gold.text = item?.gold
        gold.contentDescription = "${context.getString(R.string.cd_gold)}  ${item?.gold}"
        silver.text = item?.silver
        silver.contentDescription = "${context.getString(R.string.cd_silver)}  ${item?.silver}"
        bronze.text = item?.bronze
        bronze.contentDescription = "${context.getString(R.string.cd_bronze)}  ${item?.bronze}"
        total.text = item?.total
        total.contentDescription = "Total " + item?.total
        totalMedalsTextViews.add(total)

        Glide.with(view.context).load(item?.icon).into(icon)
        view.setBackgroundColor(bgColor)

        dynamicViews.add(rank)
        dynamicViews.add(gold)
        dynamicViews.add(silver)
        dynamicViews.add(bronze)

        return view
    }

    private fun getScheduleRow(item: OlympicsScheduleEntry? = null, bgColor: Int): View {
        val view: View = inflater.inflate(R.layout.olympics_schedule_row_item, dataList, false)
        val dateView: TextView = view.findViewById(R.id.date)
        val icon: ImageView = view.findViewById(R.id.icon)
        val titleView: TextView = view.findViewById(R.id.title)
        val subTitle: TextView = view.findViewById(R.id.sub_title)
        val timeView: TextView = view.findViewById(R.id.time)

        dateView.text = parseDate(item?.start, inputDateFormat, outputDay)
        if (item?.status.isNullOrEmpty()) {
            timeView.text = parseDate(item?.start, inputDateFormat, outputTime)
        } else {
            timeView.text = item?.status
        }

        titleView.text = item?.title
        subTitle.text = item?.subtitle
        Glide.with(view).load(item?.icon).into(icon)
        view.setBackgroundColor(bgColor)
        dynamicViews.add(dateView)

        return view
    }

    private fun parseDate(
        start: String?,
        inputDateFormat: SimpleDateFormat,
        outputDay: SimpleDateFormat
    ): CharSequence? {
        try {
            if (start != null) {
                val date = inputDateFormat.parse(start)
                if (date != null) {
                    return outputDay.format(date)
                }
            }
        } catch (t: Throwable) {
            return null
        }
        return null
    }

    fun setFixedHeight(numCountries: Int) {
        val padding = getDimen(10)
        val headerRowView: View? = getMedalsHeader()
        val title: TextView? = headerRowView?.findViewById(R.id.title)
        val headerRowSize = Math.max(getLineHeight(title), getDimen(20)) + padding + (padding / 2)
        val countryRowView: View? = getMedalsRow(bgColor = 0)
        val country: TextView? = countryRowView?.findViewById(R.id.country)
        val countryRowSize = Math.max(getLineHeight(country), getDimen(20)) + (padding * 2)
        val linkSize = getLineHeight(olympicsLink) + (padding * 2)
        layoutParams.height = headerRowSize + (countryRowSize * numCountries) + linkSize
    }

    private fun getLineHeight(textView: TextView?, numLines: Int = 1): Int {
        val fontMetrics = textView?.paint?.fontMetrics
        fontMetrics?.let {
            return (it.bottom - it.top + it.leading).toInt() * numLines
        }
        return 0
    }

    private fun getDimen(value: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
    }

    private fun setupLink(cta: OlympicsCta?) {
        val validLink = cta?.link != null &&
                !cta.title.isNullOrBlank() &&
                !cta.link?.url.isNullOrBlank()
        if (validLink) {
            olympicsLink.visibility = View.VISIBLE
            olympicsLink.text = cta?.title
        } else {
            olympicsLink.visibility = View.GONE
        }
    }

    private fun setupTitle(title: String?) {
        olympicsTitle.text = title
        olympicsTitle.visibility = if (title.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val providedWidth = MeasureSpec.getSize(widthMeasureSpec)
        val isCompactMode = providedWidth in 1..getDimen(COMPACT_MODE_THRESHOLD)
        val visibility = if (isCompactMode) View.GONE else View.VISIBLE
        for (dynamicView in dynamicViews) {
            dynamicView.visibility = visibility
        }
        if (isCompactMode) {
            totalMedalsTextViews.forEach {
                TextViewCompat.setTextAppearance(it, R.style.MedalsTotalTextCompact)
            }
        } else {
            totalMedalsTextViews.forEach {
                TextViewCompat.setTextAppearance(it, R.style.MedalsTotalTextFull)
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}