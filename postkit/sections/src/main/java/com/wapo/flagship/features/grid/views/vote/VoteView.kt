package com.wapo.flagship.features.grid.views.vote

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Html
import android.util.AttributeSet
import android.view.View
import android.widget.*
import com.washingtonpost.android.sections.R
import kotlin.math.min


class VoteView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var progressBar: View
    private lateinit var voteContent: View
    private lateinit var retry: View
    private lateinit var stateSpinner: Spinner
    private lateinit var stateGuideView: TextView
    private lateinit var defaultLayout: View
    private lateinit var contentLayout: View
    private lateinit var moreInfo: TextView
    private lateinit var titleGroup: View
    private var voteGuide: VoteGuide? = null
    private val maxWidth = resources.getDimensionPixelSize(R.dimen.voter_guide_max_width)
    private val paint = Paint().apply {
        color = resources.getColor(R.color.how_to_vote_side_border_color)
    }
    private val sideBorderWidth = resources.getDimension(R.dimen.voter_guide_side_border_width)
    private val bottomBorderWidth = resources.getDimension(R.dimen.voter_guide_bottom_border_width)

    init {
        setWillNotDraw(false)
    }


    var isLoading: Boolean = false
        set(value) {
            field = value
            progressBar.visibility = if (value) View.VISIBLE else View.GONE
            voteContent.visibility = if (!value) View.VISIBLE else View.GONE
            retry.visibility = View.GONE
        }

    var onRetryClicked: (() -> Unit)? = null
    var onLinkClicked: ((String) -> Unit)? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        defaultLayout = findViewById(R.id.defaultLayout)
        contentLayout = findViewById(R.id.contentLayout)
        moreInfo = findViewById(R.id.moreInfo)
        progressBar = findViewById(R.id.progress)
        voteContent = findViewById(R.id.voteContent)
        stateGuideView = findViewById(R.id.stateGuideView)
        titleGroup = findViewById(R.id.titleGroup)
        retry = findViewById(R.id.retry)
        retry.setOnClickListener { onRetryClicked?.invoke() }
        val absenteeInfo = findViewById<View>(R.id.absenteeInfo)
        absenteeInfo.setOnClickListener {
            val url = voteGuide?.getDefaultState()?.voterRegistration?.readMore
            if (!url.isNullOrEmpty()) {
                onLinkClicked?.invoke(url)
            }
        }

        stateSpinner = findViewById(R.id.stateSpinner)
        stateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val stateName = parent.getItemAtPosition(position) as String
                val state = voteGuide?.getStateInfo(stateName)
                val stateGuide = voteGuide?.getStateInfo(stateName)?.instructions
                if (stateGuide != null) {
                    defaultLayout.visibility = View.GONE
                    contentLayout.visibility = View.VISIBLE
                    stateGuideView.text = Html.fromHtml(stateGuide.replace("<br>", "<br /><br />")) // no good control over paragraphs line spacing
                    moreInfo.text = context.resources.getString(R.string.more_about_voting_in_state, state?.stateAbbrev
                            ?: stateName)
                    setupButton(state?.voterRegistration?.readMore, moreInfo)
                } else {
                    defaultLayout.visibility = View.VISIBLE
                    contentLayout.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun setupButton(url: String?, button: View) {
        if (url.isNullOrEmpty()) {
            button.visibility = GONE
        } else {
            button.visibility = VISIBLE
            button.setOnClickListener {
                onLinkClicked?.invoke(url)
            }
        }
    }

    fun setGuide(voteGuide: VoteGuide) {
        isLoading = false
        this.voteGuide = voteGuide

        val states = ArrayList<String>()
        voteGuide.getStates().let { states.addAll(it) }
        states.add(0, "Select your state")
        val adapter = ArrayAdapter(context, R.layout.vote_item, states)
        adapter.setDropDownViewResource(R.layout.vote_item_drop_down)
        stateSpinner.adapter = adapter

        defaultLayout.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE
    }

    fun setError(throwable: Throwable) {
        isLoading = false
        voteContent.visibility = View.GONE
        retry.visibility = View.VISIBLE
    }

    override fun measureChildWithMargins(child: View?, parentWidthMeasureSpec: Int, widthUsed: Int, parentHeightMeasureSpec: Int, heightUsed: Int) {
        if (child == voteContent && voteContent.visibility != View.GONE) {
            val parentSize = MeasureSpec.getSize(parentWidthMeasureSpec)
            if (parentSize > 0) {
                val availableWidth = min(parentSize, maxWidth)
                child.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST), parentHeightMeasureSpec)
            } else {
                super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed)
            }
        } else {
            super.measureChildWithMargins(child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.strokeWidth = sideBorderWidth
        canvas.drawLine(
                voteContent.left.toFloat(),
                titleGroup.top + titleGroup.height / 2f,
                voteContent.left.toFloat(),
                voteContent.bottom.toFloat(),
                paint)

        canvas.drawLine(
                voteContent.right.toFloat(),
                titleGroup.top + titleGroup.height / 2f,
                voteContent.right.toFloat(),
                voteContent.bottom.toFloat(),
                paint)

        paint.strokeWidth = bottomBorderWidth
        canvas.drawLine(
                voteContent.left.toFloat(),
                voteContent.bottom.toFloat(),
                voteContent.right.toFloat(),
                voteContent.bottom.toFloat(),
                paint)
    }
    fun getVoteGuide() = voteGuide
}