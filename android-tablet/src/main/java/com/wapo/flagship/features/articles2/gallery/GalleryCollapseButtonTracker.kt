package com.wapo.flagship.features.articles2.gallery

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.Image

class GalleryCollapseButtonTracker(
    var items: List<Item>?,
    val showButton: (Boolean) -> Unit,
) : RecyclerView.OnScrollListener() {
    var buttonId: String = ""

    override fun onScrolled(
        recyclerView: RecyclerView,
        dx: Int,
        dy: Int,
    ) {
        super.onScrolled(recyclerView, dx, dy)
        val lm = recyclerView.layoutManager ?: return
        if (lm !is LinearLayoutManager) {
            return
        }
        var firstItem = false
        var secondItem = false

        val currentItem = lm.findFirstCompletelyVisibleItemPosition()
        if (currentItem >= 0 && currentItem < (items?.size ?: 0)) {
            items?.get(currentItem)?.let {
                firstItem = (it as? Image)?.galleryId != null
            }

            if (currentItem + 1 < (items?.size ?: 0)) {
                items?.get(currentItem + 1)?.let {
                    secondItem = (it as? Image)?.galleryId != null
                    buttonId =
                        if (firstItem && secondItem) {
                            (it as? Image)?.galleryId.toString()
                        } else {
                            ""
                        }
                }
            }
        }
        showButton.invoke(firstItem && secondItem)
    }

    fun updateList(items: List<Item>?) {
        this.items = items
    }
}
