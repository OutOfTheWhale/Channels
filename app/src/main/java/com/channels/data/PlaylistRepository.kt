package com.channels.data

import com.channels.data.db.PlaylistDao
import com.channels.data.db.PlaylistEntity
import com.channels.data.db.PlaylistItemEntity
import com.channels.domain.model.Playlist
import com.channels.domain.model.VideoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** CRUD for user playlists and the videos in them. */
class PlaylistRepository(private val dao: PlaylistDao) {

    fun observePlaylists(): Flow<List<Playlist>> =
        dao.observePlaylistsWithCounts().map { rows ->
            rows.map { Playlist(id = it.id, name = it.name, itemCount = it.itemCount) }
        }

    fun observeName(id: Long): Flow<String?> = dao.observeName(id)

    fun observeItems(playlistId: Long): Flow<List<VideoItem>> =
        dao.observeItems(playlistId).map { rows -> rows.map { it.toVideoItem() } }

    suspend fun create(name: String): Long =
        dao.createPlaylist(PlaylistEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    suspend fun delete(id: Long) {
        dao.clearItems(id)
        dao.deletePlaylist(id)
    }

    suspend fun addVideo(playlistId: Long, video: VideoItem) = dao.addItem(
        PlaylistItemEntity(
            playlistId = playlistId,
            videoUrl = video.url,
            title = video.title,
            uploader = video.uploader,
            uploaderUrl = video.uploaderUrl,
            durationSeconds = video.durationSeconds,
            thumbnailUrl = video.thumbnailUrl,
            addedAt = System.currentTimeMillis(),
        ),
    )

    suspend fun removeVideo(playlistId: Long, videoUrl: String) = dao.removeItem(playlistId, videoUrl)

    private fun PlaylistItemEntity.toVideoItem() = VideoItem(
        id = videoUrl.substringAfterLast("v=", videoUrl.substringAfterLast('/')),
        url = videoUrl,
        title = title,
        uploader = uploader,
        uploaderUrl = uploaderUrl,
        durationSeconds = durationSeconds,
        thumbnailUrl = thumbnailUrl,
        isLive = false,
    )
}
