package com.channels.data

import com.channels.data.db.PlaybackPositionDao
import com.channels.data.db.PlaybackPositionEntity

/** Stores/reads the last playback position for a video so it can resume. */
class PlaybackPositionRepository(private val dao: PlaybackPositionDao) {

    suspend fun getPosition(videoUrl: String): Long = dao.getPosition(videoUrl) ?: 0L

    suspend fun save(videoUrl: String, positionMs: Long) =
        dao.upsert(PlaybackPositionEntity(videoUrl, positionMs, System.currentTimeMillis()))

    suspend fun clear(videoUrl: String) = dao.delete(videoUrl)
}
