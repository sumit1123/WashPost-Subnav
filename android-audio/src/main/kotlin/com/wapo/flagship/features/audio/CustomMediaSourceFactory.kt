import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.wapo.flagship.features.audio.service2.media.library.CONCAT_CHILDREN_DURATIONS
import com.wapo.flagship.features.audio.service2.media.library.CONCAT_CHILDREN_URIS
import com.wapo.flagship.features.audio.service2.media.library.IS_CONCAT2

@UnstableApi
class CustomMediaSourceFactory(
    private val context: Context,
    private val defaultFactory: MediaSourceFactory = ProgressiveMediaSource.Factory(
        DefaultDataSource.Factory(context)
    )
) : MediaSourceFactory {

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val extras = mediaItem.mediaMetadata.extras
        val isConcat = extras?.getBoolean(IS_CONCAT2) == true

        return if (isConcat) {
            val uris = extras?.getStringArrayList(CONCAT_CHILDREN_URIS) ?: emptyList()
            val childrenDurations = extras?.getStringArrayList(CONCAT_CHILDREN_DURATIONS)?.toList() ?: emptyList()

            val list = uris.map { uri ->
                MediaItem.Builder().setUri(uri).build()
            }
            val durations = childrenDurations.map { it.toLong() }

            val builder = ConcatenatingMediaSource2.Builder().apply {
                setMediaSourceFactory(defaultFactory)
                    .setMediaItem(
                        mediaItem.buildUpon()
                            .setMediaId(mediaItem.mediaId)
                            .build()
                    )
            }

            list.forEachIndexed { idx, item ->
                builder.add(item, durations[idx])
            }

            return builder.build()
        } else {
            defaultFactory.createMediaSource(mediaItem)
        }
    }

    override fun getSupportedTypes(): IntArray {
        return defaultFactory.supportedTypes
    }

    override fun setDrmSessionManagerProvider(p0: DrmSessionManagerProvider): MediaSourceFactory {
        defaultFactory.setDrmSessionManagerProvider(p0)
        return this
    }

    override fun setLoadErrorHandlingPolicy(p0: LoadErrorHandlingPolicy): MediaSourceFactory {
        defaultFactory.setLoadErrorHandlingPolicy(p0)
        return this
    }
}