package com.wapo.flagship.features.comics

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.util.forEach
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.network.request.ComicsImageRequest
import com.wapo.flagship.util.UIUtil
import com.wapo.view.RippleHelper
import com.wapo.view.ShineFrameLayout
import com.washingtonpost.android.R
import com.washingtonpost.android.comics.model.AdItem
import com.washingtonpost.android.comics.model.ComicItem
import com.washingtonpost.android.comics.model.ComicStrip
import com.washingtonpost.android.volley.Response
import androidx.core.view.isVisible
import com.wapo.adsinf.databinding.AdLayoutBinding
import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdRequestTargets
import com.wapo.adsinf.utils.AdsUtil
import com.wapo.android.commons.util.ViewUtil.findActivity

class ComicsListAdapter(
    private var comicStripList: List<ComicItem>,
    val nightModeEnabled: Boolean,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val adViews = SparseArray<View>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            ComicListType.COMIC_STRIP_TYPE.id -> {
                val itemView =
                    inflater.inflate(
                        R.layout.comics_list_item,
                        parent,
                        false,
                    )
                ComicItemViewHolder(itemView)
            }

            ComicListType.AD_TYPE.id -> {
                val viewBinding = AdLayoutBinding.inflate(inflater, parent, false)
                AdViewHolder(viewBinding)
            }

            else -> {
                error("ComicAdapter item type unknown")
            }
        }
    }


    override fun getItemCount() = comicStripList.size

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (holder) {
            is ComicItemViewHolder -> holder.bind(comicStripList[position] as ComicStrip)
            is AdViewHolder -> {
                if (adViews[position] == null) {
                    adViews[position] = holder.itemView
                }
                holder.bind(comicStripList[position] as AdItem)
            }
        }
    }

    fun setData(list: List<ComicItem>) {
        comicStripList = list
    }

    override fun getItemViewType(position: Int): Int {
        val viewType = getComicItemViewType(comicStripList[position])
        return viewType.id
    }

    inner class AdViewHolder(
        val binding: AdLayoutBinding,
    ) : ComicViewHolder<AdItem>(binding.root) {

        override fun bind(item: AdItem) {
            val adConfig = AdConfig(
                adUnitId = AdsUtil.getAdUnitId(
                    context = binding.root.context,
                    contentType = item.commercialNode,
                    adType = item.adPosition,
                    adKey = item.commercialNode,
                ),
                adRequestTargets = AdRequestTargets.getDefault().apply {
                    addSlotSizeParameters(item.adSlotType)
                    addSection("comics")
                    addAdPosition(item.adPosition)
                },
                adDimensions = listOf(item.adDimension),
                adSlotType = item.adSlotType,
                section = "comics"
            )
            binding.adView.loadAd(adConfig)
        }

        override fun unBind() {
            binding.adView.release()
            super.unBind()
        }
    }

    inner class ComicItemViewHolder(
        val view: View,
    ) : ComicViewHolder<ComicStrip>(view) {
        private val titleView: TextView = view.findViewById(R.id.comics_item_byline)
        private val imageView: ImageView = view.findViewById(R.id.comics_item_image)
        private val imageContainerView: ShineFrameLayout =
            view.findViewById(
                R.id.comics_item_image_container,
            )
        private var imageRequest: ComicsImageRequest? = null


        override fun bind(item: ComicStrip) {
            RippleHelper.addRippleEffectToView(imageView)
            titleView.text = getByline(item.name, item.author)
            imageView.setOnClickListener {
                if (imageView.isVisible) {
                    val activity = view.findActivity()
                    if (activity != null) {
                        UIUtil.startComicsActivity(activity, item.name)
                    }
                }
            }

            imageView.apply {
                tag = item.url
                visibility = View.GONE
            }
            imageContainerView.apply {
                setNightMode(nightModeEnabled)
                startShineAnimation()
            }
            // Cancel previous request if any and create a new one.
            imageRequest?.cancel()
            imageRequest =
                ComicsImageRequest(
                    item.url,
                    Response.Listener { bitmap ->
                        if (imageView.tag != item.url) {
                            return@Listener
                        }
                        imageView.apply {
                            setImageBitmap(bitmap)
                            visibility = View.VISIBLE
                        }
                        imageContainerView.stopShineAnimation()
                    },
                    Response.ErrorListener {
                        if (imageView.tag != item.url) {
                            return@ErrorListener
                        }
                        imageView.visibility = View.GONE
                        imageContainerView.stopShineAnimation()
                    },
                ).apply {
                    FlagshipApplication.getInstance().requestQueue.add(this)
                }
        }
    }

    open class ComicViewHolder<T : ComicItem>(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        open fun bind(item: T) {

        }

        open fun unBind() {

        }
    }

    fun cleanup(recyclerView: RecyclerView) {
        adViews.forEach { key, value ->
            (recyclerView.getChildViewHolder(value) as? com.wapo.flagship.features.search2.ui.viewholder.AdViewHolder)?.unbind()
        }
        adViews.clear()
    }

    fun setRecyclerViewCacheExtension(recyclerView: RecyclerView) {
        recyclerView.recycledViewPool.setMaxRecycledViews(ComicListType.AD_TYPE.id, 0)

        recyclerView.setViewCacheExtension(object : RecyclerView.ViewCacheExtension() {
            override fun getViewForPositionAndType(
                recycler: RecyclerView.Recycler,
                position: Int,
                type: Int
            ): View? {
                return when (type) {
                    ComicListType.AD_TYPE.id -> {
                        return adViews[position]
                    }

                    else -> {
                        null
                    }
                }
            }
        })
    }

    enum class ComicListType(
        val id: Int,
    ) {
        COMIC_STRIP_TYPE(1),
        AD_TYPE(2)
    }

    fun getComicItemViewType(item: ComicItem): ComicListType =
        when (item) {
            is ComicStrip -> ComicListType.COMIC_STRIP_TYPE
            else -> ComicListType.AD_TYPE
        }

}
