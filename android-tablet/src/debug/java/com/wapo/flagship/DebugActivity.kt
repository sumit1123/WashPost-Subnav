package com.wapo.flagship

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.wapo.flagship.content.ContentManager
import com.wapo.flagship.content.notifications.NotificationData
import com.wapo.flagship.features.shared.activities.BaseActivity
import com.wapo.flagship.push.PushListener
import com.wapo.view.segmentedview.SegmentedView
import com.wapo.view.stack.FlexibleStackView
import com.washingtonpost.android.R
import com.washingtonpost.android.wapocontent.RequestData
import dagger.hilt.android.AndroidEntryPoint
import java.util.Random

private const val PUSH_URL =
    "https://www.washingtonpost.com/sports/nationals/the-secret-to-the-nationals-success-its-two-out-magic--and-its-no-accident/2019/10/24/63363cfc-f5d3-11e9-8cf0-4cc99f74d127_story.html"

@AndroidEntryPoint
class DebugActivity : BaseActivity() {
    private var contentManager: ContentManager? = FlagshipApplication.getInstance().contentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        findViewById<Button>(R.id.ping3).setOnClickListener {
            contentManager
                ?.performPagesSync(listOf("sports"))
                ?.subscribe()
        }

        processIntent(intent)

        setupSegmentedView()

        setupStackView()
    }

    private fun setupStackView() {
        val stackView = findViewById<FlexibleStackView>(R.id.stackView)
        stackView.adapter = MyStackAdapter()

        findViewById<View>(R.id.next).setOnClickListener {
            stackView.next()
        }

        findViewById<View>(R.id.previous).setOnClickListener {
            stackView.back()
        }

        findViewById<View>(R.id.enableLoop).setOnClickListener {
            stackView.mode = FlexibleStackView.Mode.LOOP
        }

        findViewById<View>(R.id.disableLoop).setOnClickListener {
            stackView.mode = FlexibleStackView.Mode.EMPTY_CARD
        }
    }

    private fun setupSegmentedView() {
        val items = listOf("Page 1", "Page 2", "Page 3", "Page 4")

        val segmentedView = findViewById<SegmentedView>(R.id.segmentedView)

        val viewPager = findViewById<ViewPager>(R.id.viewPager)

        viewPager.adapter =
            object : PagerAdapter() {
                override fun getCount(): Int = items.count()

                override fun isViewFromObject(
                    view: View,
                    `object`: Any,
                ): Boolean = view == `object`

                override fun instantiateItem(
                    container: ViewGroup,
                    position: Int,
                ): Any {
                    val textView = TextView(this@DebugActivity)
                    textView.text = items[position]
                    textView.gravity = Gravity.CENTER
                    container.addView(
                        textView,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    return textView
                }

                override fun destroyItem(
                    container: ViewGroup,
                    position: Int,
                    `object`: Any,
                ) {
                    container.removeView(`object` as View)
                }

                override fun getPageTitle(position: Int): CharSequence? = items[position]
            }

        segmentedView.setupWithViewPager(viewPager)
    }

    private fun processIntent(intent: Intent?) {
        when (intent?.action) {
            // adb shell am start -n com.washingtonpost.android/com.wapo.flagship.DebugActivity -a push
            "push" -> createPush(intent.data?.toString() ?: PUSH_URL)
        }
    }

    private fun createPush(url: String) {
        val pushListener = PushListener()
        pushListener.onMessage(
            Intent().apply {
                putExtra("default", jsonString(url))
            },
        )
        finish()
    }

    private fun jsonString(url: String): String =
        """
        {
            "title": "Title",
            "message": "Message",
            "headline": "Headline",
            "targetTopic": "Top stories",
            "url": "$url",
            "datetime": "${System.currentTimeMillis()}"
        }
        """.trimIndent()

    private fun getRequestData(): RequestData =
        RequestData(
            "https://www.washingtonpost.com/politics/supreme-court-strikes-down-texas-abortion-clinic-restrictions/2016/06/27/ba55d526-3c70-11e6-a66f-aa6c1883b6b1_story.html",
            null,
        )

    private fun createNotification(url: String): NotificationData =
        NotificationData("alert ${System.currentTimeMillis()}").apply {
            storyUrl = url
            type = ""
            notifId = "${Random().nextInt(100) + 100}"
            timestamp = (System.currentTimeMillis() / 1000).toString()
        }
}

class MyStackAdapter : BaseAdapter() {
    private val colors =
        arrayOf(
            Color.parseColor("#377448"),
            Color.parseColor("#2955a0"),
            Color.MAGENTA,
            Color.RED,
            Color.BLACK,
        )

    override fun getCount(): Int = 5

    override fun getItem(position: Int): Any = position

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        var convertView = convertView
        val holder: ViewHolder
        if (convertView == null) {
            convertView =
                LayoutInflater.from(parent.context).inflate(R.layout.stack_item, parent, false)
            holder = ViewHolder()
            holder.imageView = (convertView as ViewGroup).findViewById(R.id.image)
            holder.textView = (convertView as ViewGroup).findViewById(R.id.text)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.imageView?.setImageResource(R.drawable.app_icon)
        (convertView as CardView).setCardBackgroundColor(colors[position.rem(colors.size)])
        holder.textView?.text = "Card #${position + 1}"
        return convertView
    }

    inner class ViewHolder {
        internal var imageView: ImageView? = null
        internal var textView: TextView? = null
    }
}
