package com.wapo.flagship.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class WapoPlaybackQueueScreen(
    carContext: CarContext,
    private val mediaCoordinator: CarMediaCoordinator,
) : Screen(carContext) {
    private val artworkLoader = CarArtworkLoader(carContext)
    private val artworkByMediaId = mutableMapOf<String, CarIcon>()
    private val requestedArtworkIds = mutableSetOf<String>()
    private val resolvedArtworkIds = mutableSetOf<String>()
    private var mediaState = mediaCoordinator.state.value

    init {
        lifecycleScope.launch {
            mediaCoordinator.state.collect { state ->
                mediaState = state
                invalidate()
                loadQueueArtwork(visibleQueueEntries(state).map { it.value })
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val itemList = ItemList.Builder()
        val queue = mediaState.queue
        val visibleEntries = visibleQueueEntries(mediaState)

        if (queue.isNotEmpty() && !isArtworkResolved(visibleEntries.map { it.value })) {
            return templateBuilder().setLoading(true).build()
        }

        if (queue.isEmpty()) {
            itemList.addItem(
                Row
                    .Builder()
                    .setTitle(carContext.getString(R.string.android_auto_queue_empty))
                    .build(),
            )
        } else {
            val currentIndex = mediaState.currentQueueIndex.coerceIn(queue.indices)

            visibleEntries
                .forEach { (index, item) ->
                    itemList.addItem(
                        Row
                            .Builder()
                            .setTitle(item.mediaMetadata.title ?: "")
                            .apply {
                                if (index == currentIndex) {
                                    addText(
                                        carContext.getString(R.string.android_auto_queue_now_playing),
                                    )
                                } else {
                                    item.mediaMetadata.subtitle?.let(::addText)
                                }
                                artworkByMediaId[item.mediaId]?.let { artwork ->
                                    setImage(artwork, Row.IMAGE_TYPE_LARGE)
                                }
                            }.setOnClickListener {
                                mediaCoordinator.playQueueItem(index)
                            }.build(),
                    )
                }
        }

        return templateBuilder()
            .setSingleList(itemList.build())
            .build()
    }

    private fun templateBuilder(): ListTemplate.Builder =
        ListTemplate
            .Builder()
            .setHeader(
                Header
                    .Builder()
                    .setStartHeaderAction(Action.BACK)
                    .setTitle(carContext.getString(R.string.android_auto_queue))
                    .build(),
            )

    private fun isArtworkResolved(items: List<MediaItem>): Boolean =
        items.all { item ->
            item.mediaMetadata.artworkUri == null || item.mediaId in resolvedArtworkIds
        }

    private fun visibleQueueEntries(state: CarMediaState): List<IndexedValue<MediaItem>> {
        if (state.queue.isEmpty()) return emptyList()

        val contentLimit =
            carContext
                .getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
                .coerceAtLeast(1)
        val currentIndex = state.currentQueueIndex.coerceIn(state.queue.indices)
        return state.queue
            .withIndex()
            .drop(currentIndex)
            .take(contentLimit)
    }

    private suspend fun loadQueueArtwork(items: List<MediaItem>) {
        val pendingItems =
            items.filter { item ->
                item.mediaMetadata.artworkUri != null &&
                        item.mediaId !in artworkByMediaId &&
                        requestedArtworkIds.add(item.mediaId)
            }
        if (pendingItems.isEmpty()) return

        coroutineScope {
            pendingItems
                .groupBy { it.mediaMetadata.artworkUri }
                .mapNotNull { (artworkUri, mediaItems) ->
                    if (artworkUri == null) {
                        null
                    } else {
                        async {
                            artworkLoader.load(artworkUri)?.let { artwork ->
                                mediaItems.forEach { item ->
                                    artworkByMediaId[item.mediaId] = artwork
                                }
                            }
                            mediaItems.forEach { item ->
                                resolvedArtworkIds.add(item.mediaId)
                            }
                        }
                    }
                }.awaitAll()
        }
    }
}
