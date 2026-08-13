package com.channels.data

import com.channels.data.db.FeedItemDao
import com.channels.data.db.FeedItemEntity
import com.channels.data.youtube.YoutubeRepository
import com.channels.domain.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Builds the Home feed by pulling recent long-form uploads from every starred
 * channel, caching them in Room. The UI observes the cache; refreshes are
 * best-effort per channel so one failure doesn't sink the whole feed.
 */
class FeedRepository(
    private val youtube: YoutubeRepository,
    private val starred: StarredRepository,
    private val feedDao: FeedItemDao,
) {

    fun observeFeed(): Flow<List<VideoItem>> =
        feedDao.observeFeed().map { rows -> rows.map { it.toVideoItem() } }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val starredUrls = starred.starredUrls()
        if (starredUrls.isEmpty()) {
            feedDao.clear()
            return@withContext
        }
        val now = System.currentTimeMillis()

        val rows = coroutineScope {
            starredUrls.map { channelUrl ->
                async {
                    runCatching { youtube.channelUploads(channelUrl).take(PER_CHANNEL) }
                        .getOrDefault(emptyList())
                        .map { it.toEntity(channelUrl, now) }
                }
            }.awaitAll()
        }.flatten()

        feedDao.pruneUnstarred(starredUrls)
        if (rows.isNotEmpty()) feedDao.upsertAll(rows)
    }

    private fun VideoItem.toEntity(channelUrl: String, now: Long) = FeedItemEntity(
        videoUrl = url,
        channelUrl = channelUrl,
        title = title,
        uploader = uploader,
        durationSeconds = durationSeconds,
        thumbnailUrl = thumbnailUrl,
        isLive = isLive,
        publishedAt = publishedAt,
        fetchedAt = now,
    )

    private fun FeedItemEntity.toVideoItem() = VideoItem(
        id = videoUrl.substringAfterLast("v=", videoUrl.substringAfterLast('/')),
        url = videoUrl,
        title = title,
        uploader = uploader,
        uploaderUrl = channelUrl,
        durationSeconds = durationSeconds,
        thumbnailUrl = thumbnailUrl,
        isLive = isLive,
        publishedAt = publishedAt,
    )

    companion object {
        const val PER_CHANNEL = 15
    }
}
