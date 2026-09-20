package com.wapo.flagship.features.articles2.viewholders

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import com.wapo.android.commons.util.Logger
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.features.articles2.adapters.Articles2ItemsRecyclerViewAdapter
import com.wapo.flagship.features.articles2.models.deserialized.tweet.Tweet
import com.wapo.text.GlobalFontAdjustmentSpan
import com.wapo.text.WpLinkAppearanceSpan
import com.washingtonpost.android.articles.R
import com.washingtonpost.android.databinding.ItemTweetBinding
import com.washingtonpost.android.volley.toolbox.NetworkAnimatedImageView
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class TweetViewHolder(
    private val binding: ItemTweetBinding,
) : Articles2ItemsRecyclerViewAdapter.ArticleItemViewHolder<Tweet>(
        binding.root,
    ) {
    override fun bind(
        item: Tweet,
        position: Int,
    ) {
        // All of the individual view components
        val tweetContainer = binding.tweetContainer
        val screenNameTextView = binding.screenName
        val realNameTextView = binding.realName
        val text = binding.text
        val dateTime = binding.dateTime
        val tweets = binding.tweets
        val profilePhoto = binding.profilePhoto
        val image = binding.image
        val replyButton = binding.replyButton
        val retweetButton = binding.retweetButton
        val favButton = binding.favButton
        val followButton = binding.followButton

        // All data
        val tweetText: String? = item.content?.text
        val createdAt: String? = item.content?.createdAt
        val retweets: Int? = item.content?.retweetCount
        val mediaImageUrl: String? =
            item.content
                ?.entities
                ?.media
                ?.first { it.type == "photo" }
                ?.mediaUrl
        val favs: Int? = item.content?.favoriteCount
        val profileUrl: String? = item.content?.user?.profileImageUrl
        val screenName: String? = "@" + item.content?.user?.screenName
        val realName: String? = item.content?.user?.name

        val id = item.content?.idStr
        val userId = item.content?.user?.idStr

        setTextClickListener(text, item.content?.user?.screenName, id)
        setReplyButtonClickListener(replyButton, id)
        setRetweetButtonClickListener(retweetButton, id)
        setFollowButtonClickListener(followButton, userId)
        favButton.setColorFilter(Color.argb(255, 153, 153, 153))
        setFavoriteButtonClickListener(favButton, id)
        if (profileUrl != null) {
            profilePhoto.setImageUrl(
                profileUrl,
                FlagshipApplication.getInstance().animatedImageLoader,
            )
            setProfilePhotoOnClickListener(profilePhoto, userId)
        } else {
            profilePhoto.visibility = View.GONE
        }

        if (realName != null) {
            val spannableRealName = SpannableString.valueOf(realName)
            spannableRealName.setSpan(
                GlobalFontAdjustmentSpan(),
                0,
                spannableRealName.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            realNameTextView.text = spannableRealName
            setNameClickListener(realNameTextView, userId)
        } else {
            realNameTextView.visibility = View.GONE
        }

        if (screenName != null) {
            val spannableScreenName = SpannableString.valueOf(screenName)
            spannableScreenName.setSpan(
                GlobalFontAdjustmentSpan(),
                0,
                spannableScreenName.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            screenNameTextView.text = spannableScreenName
            setNameClickListener(screenNameTextView, userId)
        } else {
            screenNameTextView.visibility = View.GONE
        }

        if (tweetText != null) {
            val usernameMatcher =
                TWEET_USERNAME_PATTERN.matcher(tweetText)
            val hashtagMatcher =
                TWEET_HASHTAG_PATTERN.matcher(tweetText)
            val ss = SpannableString(tweetText)
            while (usernameMatcher.find()) {
                var userName = usernameMatcher.group()
                userName = userName.replace(" ", "")
                val rawUserName = userName.replace("@", "")
                val span1: WpLinkAppearanceSpan =
                    object : WpLinkAppearanceSpan(
                        binding.root.context,
                        true,
                    ) {
                        override fun onClick(textView: View) {
                            onTweetClick(TWEET_INTENT_PROFILE_SCREENNAME + rawUserName)
                        }
                    }
                ss.setSpan(
                    span1,
                    tweetText.indexOf(userName),
                    tweetText.indexOf(userName) + userName.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            while (hashtagMatcher.find()) {
                var hashTag = hashtagMatcher.group()
                hashTag = hashTag.replace(" ", "")
                val rawTag = hashTag.replace("#", "")
                val span1: WpLinkAppearanceSpan =
                    object : WpLinkAppearanceSpan(
                        binding.root.context,
                        true,
                    ) {
                        override fun onClick(textView: View) {
                            onTweetClick(TWEET_INTENT_SEARCH + rawTag)
                        }
                    }
                ss.setSpan(
                    span1,
                    tweetText.indexOf(hashTag),
                    tweetText.indexOf(hashTag) + hashTag.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            ss.setSpan(GlobalFontAdjustmentSpan(), 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.text = ss
        } else {
            text.visibility = View.GONE
        }

        val format =
            SimpleDateFormat("EEE MMM dd HH:mm:ss +0000 yyyy", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")

        if (createdAt != null) {
            try {
                val myDate = format.parse(createdAt)
                val myLocalDate =
                    Calendar
                        .getInstance()
                        .apply {
                            time = myDate
                            timeZone = TimeZone.getDefault()
                        }.time
                val timeFormat =
                    SimpleDateFormat("h:mm a - dd MMM yyyy", Locale.US)
                val finalDate = timeFormat.format(myLocalDate)
                val spannableFinalDate = SpannableString.valueOf(finalDate)
                spannableFinalDate.setSpan(
                    GlobalFontAdjustmentSpan(),
                    0,
                    spannableFinalDate.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                dateTime.text = spannableFinalDate
                dateTime.setOnTouchListener { v, event ->
                    onTweetClick(TWEET_INTENT_RETWEET + id)
                    false
                }
            } catch (e: ParseException) {
                dateTime.visibility = View.GONE
            }
        } else {
            dateTime.visibility = View.GONE
        }

        val spannableRetweetFavs =
            SpannableString.valueOf(retweets.toString() + " RETWEETS " + favs + " FAVORITES")
        spannableRetweetFavs.setSpan(
            GlobalFontAdjustmentSpan(),
            0,
            spannableRetweetFavs.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        tweets.text = spannableRetweetFavs

        val list = listOf(tweets, dateTime, screenNameTextView, realNameTextView, text)
        list.forEach {
            it.setTextColor(ContextCompat.getColor(binding.root.context, R.color.tweet_text_color))
        }
        tweetContainer.background =
            ContextCompat.getDrawable(
                itemView.context,
                R.drawable.tweet_border,
            )

        if (mediaImageUrl != null) {
            image.setImageUrl(mediaImageUrl, FlagshipApplication.getInstance().animatedImageLoader)
        } else {
            image.visibility = View.GONE
        }
    }

    private fun setTextClickListener(
        text: TextView,
        userName: String?,
        id: String?,
    ) {
        if (userName == null) {
            Logger.e(TAG, "userName is null")
            return
        } else if (id == null) {
            Logger.e(TAG, "id is null")
            return
        }

        val builder = Uri.Builder()
        builder
            .scheme("https")
            .authority(TWITTER_DOMAIN)
            .appendPath(userName)
            .appendPath("status")
            .appendPath(id)
        val url = builder.build().toString()

        text.setOnClickListener {
            onTweetClick(url)
        }
    }

    private fun setReplyButtonClickListener(
        replyButton: ImageView,
        id: String?,
    ) {
        if (id == null) {
            Logger.e(TAG, "id is null")
            return
        }

        replyButton.setOnClickListener {
            onTweetClick(TWEET_INTENT_REPLY + it)
        }
    }

    private fun setFollowButtonClickListener(
        followButton: ImageView,
        userId: String?,
    ) {
        if (userId == null) {
            Logger.e(TAG, "userId is null")
            return
        }

        followButton.setOnClickListener {
            onTweetClick(TWEET_INTENT_PROFILE_WITH_ID + userId)
        }
    }

    private fun setRetweetButtonClickListener(
        retweetButtonBinding: ImageView,
        id: String?,
    ) {
        if (id == null) {
            Logger.e(TAG, "id is null")
            return
        }

        retweetButtonBinding.setOnClickListener {
            onTweetClick(TWEET_INTENT_RETWEET + id)
        }
    }

    private fun setFavoriteButtonClickListener(
        favButton: ImageView,
        id: String?,
    ) {
        if (id == null) {
            Logger.e(TAG, "id is null")
            return
        }

        favButton.setOnClickListener {
            onTweetClick(TWEET_INTENT_FAVORITE + id)
        }
    }

    private fun setNameClickListener(
        screenNameTextView: TextView,
        userId: String?,
    ) {
        if (userId == null) {
            Logger.e(TAG, "userId is null")
            return
        }

        screenNameTextView.setOnTouchListener { _, _ ->
            onTweetClick(
                TWEET_INTENT_PROFILE_WITH_ID + userId,
            )
            false
        }
    }

    private fun setProfilePhotoOnClickListener(
        profilePhoto: NetworkAnimatedImageView,
        userId: String?,
    ) {
        if (userId == null) {
            Logger.e(TAG, "userId is null")
            return
        }

        profilePhoto.setOnClickListener {
            onTweetClick(
                TWEET_INTENT_PROFILE_WITH_ID + userId,
            )
        }
    }

    private fun onTweetClick(url: String) {
        val openUrl = Intent(Intent.ACTION_VIEW)
        openUrl.data = Uri.parse(url)
        binding.root.context.startActivity(openUrl)
    }

    companion object {
        private val TWEET_USERNAME_PATTERN =
            Pattern.compile("(?:\\s|\\A)[@]+([A-Za-z0-9-_]{1,15})")
        private val TWEET_HASHTAG_PATTERN =
            Pattern.compile("(?:\\s|\\A)[##]+([A-Za-z0-9-_]+)")
        private const val TWEET_INTENT_PROFILE_WITH_ID =
            "https://twitter.com/intent/user?user_id="
        private const val TWEET_INTENT_PROFILE_SCREENNAME =
            "https://twitter.com/intent/user?screen_name="
        private const val TWEET_INTENT_RETWEET = "https://twitter.com/intent/retweet?tweet_id="
        private const val TWEET_INTENT_SEARCH = "https://twitter.com/search?q="
        private const val TWEET_INTENT_FAVORITE =
            "https://twitter.com/intent/favorite?tweet_id="
        private const val TWEET_INTENT_REPLY = "https://twitter.com/intent/tweet?in_reply_to="
        private const val TWITTER_DOMAIN = "twitter.com"
        private val TAG = TweetViewHolder::class.java.simpleName
    }
}
